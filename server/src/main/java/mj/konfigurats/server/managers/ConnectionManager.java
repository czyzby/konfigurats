package mj.konfigurats.server.managers;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mj.konfigurats.logic.GameRoomUser;
import mj.konfigurats.network.ConnectionPackets.CltLogin;
import mj.konfigurats.network.ConnectionPackets.CltRegister;
import mj.konfigurats.network.ConnectionPackets.SrvAlreadyLogged;
import mj.konfigurats.network.ConnectionPackets.SrvCorruptedData;
import mj.konfigurats.network.ConnectionPackets.SrvLogged;
import mj.konfigurats.network.ConnectionPackets.SrvPasswordInvalid;
import mj.konfigurats.network.ConnectionPackets.SrvRegistered;
import mj.konfigurats.network.ConnectionPackets.SrvUsernameInvalid;
import mj.konfigurats.network.ConnectionPackets.SrvUsernameTaken;
import mj.konfigurats.server.ServerManager;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.minlog.Log;

public class ConnectionManager extends AbstractManager {
	// Currently logged users, <connection.toString.hashCode(),connection>:
	private final ConcurrentHashMap<Integer,Connection> loggedUsers;
	// Currently logged elite users, <connection.toString.hashCode(),connection>:
	private final ConcurrentHashMap<Integer,Connection> eliteUsers;
	// Used to validate passwords:
	private final Pattern regexPattern;
	private Matcher regexMatcher;
	private static SecureRandom SALT;
	private static MessageDigest PASSWORD_ENCRYPTOR;
	// User database:
	private java.sql.Connection databaseConnection;
	// Prepared statement for easy SQL queries:
	private PreparedStatement 	getPasswordStatement,
								createUserStatement,
								checkIfEliteStatement,
								updateScoreStatement,
								getScoreStatement,
								getTopKillsStatement,
								getTopDeathsStatement,
								getTopRatioStatement;
	// Ranking update thread:
	private final Timer managerTimer;
	// Environment settings:
	private final boolean skipEliteCheck;
	
	public ConnectionManager(ServerManager serverManager) {
		super(serverManager);
		// Preparing data validation regex:
		regexPattern = Pattern.compile("[\\da-zA-Z-]+");
		try {
			SALT = SecureRandom.getInstance("SHA1PRNG");
			PASSWORD_ENCRYPTOR = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {
			Log.error("SRV: unable to prepare encrypting algorythms: "+e.getMessage());
			System.exit(4);
		}
		// Creating collections:
		loggedUsers = new ConcurrentHashMap<Integer,Connection>();
		eliteUsers = new ConcurrentHashMap<Integer,Connection>();
		// Preparing another manager thread:
		managerTimer = new Timer();
		// Reading environment settings:
		skipEliteCheck = Boolean.parseBoolean(System.getProperty("SKIP_ELITE_CHECK", "true"));

		Log.info("SRV: attempting to connect with the database...");
		try {
			// Connecting with the database:
			databaseConnection = DriverManager.getConnection
				("jdbc:hsqldb:file:konfigurats-database;shutdown=true", "admin", "konfigurateli");
			databaseConnection.setAutoCommit(false);
			// Preparing SQL statements:
			getPasswordStatement = databaseConnection.prepareStatement
				("SELECT key,password FROM Users WHERE id=?;");
			createUserStatement = databaseConnection.prepareStatement
				("INSERT INTO Users VALUES(?,?,?,?,0,0,0);");
			checkIfEliteStatement = databaseConnection.prepareStatement
				("SELECT elite FROM Users WHERE id=?;");
			updateScoreStatement = databaseConnection.prepareStatement
				("UPDATE Users SET kills=kills+?, deaths=deaths+? WHERE id=?;");
			getScoreStatement = databaseConnection.prepareStatement
				("SELECT kills,deaths FROM Users WHERE id=?;");
			getTopKillsStatement = databaseConnection.prepareStatement
				("SELECT TOP 10 username, kills, deaths FROM Users ORDER BY kills DESC, deaths ASC;");
			getTopDeathsStatement = databaseConnection.prepareStatement
				("SELECT TOP 10 username, kills, deaths FROM Users ORDER BY deaths DESC, kills ASC;");
			getTopRatioStatement = databaseConnection.prepareStatement
				("SELECT TOP 10 username, kills, deaths FROM Users WHERE (kills+deaths)>=100 ORDER BY (kills*10000)/(deaths+kills) DESC;");
		}
		catch (SQLException e) {
			Log.error("SRV: unable to connect with the database: "+e.getMessage());
			System.exit(3);
		}
		finally {
			Log.info("SRV: database connection established");
			// Scheduling ranking update:
			managerTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					managerThread.execute(new Runnable() {
						@Override
						public void run() {
							updateTopPlayers();
						}
					});
				}
			}, 0, 3600000);
		}
	}
	
	@Override
	protected void analyzePacket(Connection connection, Object packet) {
		// If user tries to log in...
		if(packet instanceof CltLogin) {
			CltLogin loginData = (CltLogin) packet;
			
			// Making sure that the user isn't trying to screw with our database...
			regexMatcher = regexPattern.matcher(loginData.username);
			if(regexMatcher.matches()) {
				// Getting password from the database:
				try {
					getPasswordStatement.setInt(1, loginData.username.hashCode());
					ResultSet result = getPasswordStatement.executeQuery();
					if(result.next()) {
						// If password matches the one in the database:
						if(encryptPassword(loginData.password,result.getString(1).getBytes())
							.equals(result.getString(2))) {
							// Valid password and not logged in:
							if(!loggedUsers.containsKey(loginData.username.hashCode())) {
								// Adding user to the logged users lists:
								loggedUsers.put(loginData.username.hashCode(),connection);
								// Assigning username to the connection:
								connection.setName(loginData.username);
								// Checking if the user is elite:
								if (!skipEliteCheck) {
									checkIfEliteStatement.setInt(1, connection.toString().hashCode());
									ResultSet eliteResult = checkIfEliteStatement.executeQuery();
									if (eliteResult.next()) {
										if (eliteResult.getBoolean(1)) {
											eliteUsers.put(connection.toString().hashCode(), connection);
										}
									}
									eliteResult.close();
								}
								
								// Sending a packet with the player's username:
								SrvLogged success = new SrvLogged();
								success.username = connection.toString();
								connection.sendTCP(success);
								
								// Adding user to the lobby:
								serverManager.getLobbyManager().enterLobby(connection);
								
								Log.info("ID"+connection.getID()+": "+connection.toString()+": logged in");
							}
							else { // Account already logged:
								connection.sendTCP(new SrvAlreadyLogged());
								Log.debug("ID"+connection.getID()+": tried to log in as an already logged user: "+loginData.username);
							}
						}
						else { // Wrong password:
							connection.sendTCP(new SrvPasswordInvalid());
							Log.debug("ID"+connection.getID()+": unsuccessfully tried to log in as: "+loginData.username);
						}
					}
					else { // Wrong username:
						connection.sendTCP(new SrvUsernameInvalid());
						Log.debug("ID"+connection.getID()+": entered invalid username: "+loginData.username);
					}
					result.close();
				}
				catch (SQLException e) {
					Log.error("SRV: unable to execute query - logging error: "+e.getMessage());
				}
			}
			else { // Username is too short/long or contains forbidden characters.
				connection.sendTCP(new SrvCorruptedData());
				Log.warn("ID"+connection.getID()+": sent corrupted logging data");
			}
		}
		// If user tries to register a new account...
		else if(packet instanceof CltRegister) {
			CltRegister registrationData = (CltRegister) packet;
			
			// If username is OK:
			regexMatcher = regexPattern.matcher(registrationData.username);
			if(registrationData.username.length()>1 &&
					registrationData.username.length()<11 &&
					regexMatcher.matches()) {
				// If password is OK:
				regexMatcher = regexPattern.matcher(registrationData.password);
				if(registrationData.password.length()>7 &&
						registrationData.password.length()<21 &&
						regexMatcher.matches()) {
					try {
						getPasswordStatement.setInt(1, registrationData.username.hashCode());
						ResultSet result = getPasswordStatement.executeQuery();
						// If no one used the same login:
						if(!result.next()) {
							createUserStatement.setInt(1, registrationData.username.hashCode());
							createUserStatement.setString(2, registrationData.username);
							byte[] salt = getSalt();
							// Encrypted password data:
							createUserStatement.setString(3, encryptPassword(registrationData.password,salt));
							createUserStatement.setString(4, new String(salt));
							createUserStatement.executeUpdate();
							databaseConnection.commit();
							
							connection.sendTCP(new SrvRegistered());
							Log.info("ID"+connection.getID()+": registered a new account: "+registrationData.username);
						}
						else {
							connection.sendTCP(new SrvUsernameTaken());
							Log.debug("ID"+connection.getID()+": tried to register as "+registrationData.username);
						}
						result.close();
					}
					catch (SQLException e) {
						Log.error("SRV: unable to execute query - registration error: " + e.getMessage());
					}
				}
				else { // Password is too short/long or contains forbidden characters.
					connection.sendTCP(new SrvCorruptedData());
					Log.warn("ID"+connection.getID()+": sent corrupted registration data");
				}
			}
			else { // Username is too short/long or contains forbidden characters.
				connection.sendTCP(new SrvCorruptedData());
				Log.warn("ID"+connection.getID()+": sent corrupted registration data");
			}
		}
	}
	
	@Override
	protected boolean disconnectUser(Connection connection) {
		if(loggedUsers.containsKey(connection.toString().hashCode())) {
			loggedUsers.remove(connection.toString().hashCode());
			Log.info("ID"+connection.getID()+": "+connection.toString()+": logged out");
			return true;
		}
		return false;
	}
	
	/**
	 * Finds a logged player with the given username.
	 * @param username player's username.
	 * @return user's connection or null if not logged.
	 */
	public Connection getUser(String username) {
		return loggedUsers.get(username.hashCode());
	}
	
	/**
	 * Checks if the player is elite.
	 * @param username player's nickname.
	 * @return true for elite player.
	 */
	public boolean isElite(String username) {
		return skipEliteCheck || eliteUsers.containsKey(username.hashCode());
	}
	
	/**
	 * Updates the amount of kills and deaths according to the scores that the
	 * player got in the last game room. Needs a connection to the database
	 * and is run by the connection thread.
	 * @param userData user's data updated by the game room.
	 */
	public void updatePlayerScore(final GameRoomUser userData) {
		if(userData != null) {
			managerThread.execute(new Runnable() {
				@Override
				public void run() {
					try {
						updateScoreStatement.setInt(1, userData.getKills());
						updateScoreStatement.setInt(2, userData.getDeaths());
						updateScoreStatement.setInt(3, userData.getUserConnection().toString().hashCode());
						updateScoreStatement.executeUpdate();
						databaseConnection.commit();
					} catch (SQLException e) {
						Log.error("SRV: unable to execute query - score saving error: "+e.getMessage());
					}
				}
			});
		}
	}
	
	/**
	 * Gets player's score from the database.
	 * @param player user's connection.
	 */
	public void requestUserScore(final Connection player) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				try {
					getScoreStatement.setInt(1, player.toString().hashCode());
					ResultSet result = getScoreStatement.executeQuery();
					if(result.next()) {
						serverManager.getLobbyManager().sendRankingData
							(player, result.getInt(1), result.getInt(2));
					}
					result.close();
				} catch (SQLException e) {
					Log.error("SRV: unable to execute query - getting score error: " + e.getMessage());
				}
			}
		});
	}
	
	/**
	 * Updates ranking info in the lobby.
	 */
	private void updateTopPlayers() {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				try {
					// Getting top players from the database and
					// updating ranking info in the lobby:
					serverManager.getLobbyManager().updateRankingData
						(fillRankingTable(getTopKillsStatement.executeQuery()),
						fillRankingTable(getTopDeathsStatement.executeQuery()),
						fillRankingTable(getTopRatioStatement.executeQuery()));
				} catch (SQLException e) {
					Log.error("SRV: unable to update ranking: "+e.getMessage());
				}
			}
		});
	}
	
	/**
	 * Helper method to fill each ranking table.
	 * @param result database result.
	 * @return filled data array.
	 * @throws SQLException may throw exception due to SQL stuff.
	 */
	private String[] fillRankingTable(ResultSet result) throws SQLException {
		String[] rankingData = new String[10];
		int index = 0;
		while(result.next()) {
			rankingData[index] = result.getString(1) + " " +
				result.getInt(2) + "/" + result.getInt(3);
			index ++;
			if(index == 10) {
				break;
			}
		}
		for(;index<10;index++) {
			rankingData[index] = "- -";
		}
		result.close();
		return rankingData;
	}
	
	/**
	 * Encrypts a password with SHA-512 algorithm using salt.
	 * @param password password to be encrypted.
	 * @param salt secure random bytes array.
	 * @return encrypted password hash.
	 */
	private static String encryptPassword(String password, byte[] salt) {
		// Resetting password encryptor instance:
	    PASSWORD_ENCRYPTOR.reset();
	    // Updating encryptor with salt (secure random) bytes:
	    PASSWORD_ENCRYPTOR.update(salt);
	    // Encrypting password:
	    return String.format("%0128x", new BigInteger
	    	(1,PASSWORD_ENCRYPTOR.digest(password.getBytes())));
	}
	
	/**
	 * @return secure random array of bytes.
	 */
	private static byte[] getSalt() {
		byte[] salt = new byte[16];
		SALT.nextBytes(salt);
        return String.format("%016x", new BigInteger(1,salt)).getBytes();
	}
	
	/**
	 * Attempts to close (and save) users database.
	 * Should be run when the server application is about to close.
	 */
	public void shutdown() {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				try {
					// Closing prepared statements:
					getPasswordStatement.close();
					createUserStatement.close();
					updateScoreStatement.close();
					getScoreStatement.close();
					getTopKillsStatement.close();
					getTopDeathsStatement.close();
					getTopRatioStatement.close();
					// Closing database connection:
					databaseConnection.close();
					Log.info("SRV: database closed");
				} catch (SQLException e) {
					Log.error("SRV: unable to close the database: "+e.getMessage());
				}
			}
		});
	}
}
