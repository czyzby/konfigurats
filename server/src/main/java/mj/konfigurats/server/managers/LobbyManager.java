package mj.konfigurats.server.managers;

import java.util.concurrent.ConcurrentHashMap;

import mj.konfigurats.network.GamePackets.SrvGameChatMessage;
import mj.konfigurats.network.LobbyPackets.CltCreateGame;
import mj.konfigurats.network.LobbyPackets.CltJoinGame;
import mj.konfigurats.network.LobbyPackets.CltJoinPlayer;
import mj.konfigurats.network.LobbyPackets.CltJoinRandomGame;
import mj.konfigurats.network.LobbyPackets.CltLobbyLogOut;
import mj.konfigurats.network.LobbyPackets.CltLobbyMessage;
import mj.konfigurats.network.LobbyPackets.CltPrivateMessage;
import mj.konfigurats.network.LobbyPackets.CltRoomPassword;
import mj.konfigurats.network.LobbyPackets.CltShowRanking;
import mj.konfigurats.network.LobbyPackets.PctGameInfo;
import mj.konfigurats.network.LobbyPackets.SrvAddLobbyUser;
import mj.konfigurats.network.LobbyPackets.SrvLobbyLoggedOut;
import mj.konfigurats.network.LobbyPackets.SrvLobbyMessage;
import mj.konfigurats.network.LobbyPackets.SrvRankingData;
import mj.konfigurats.network.LobbyPackets.SrvRemoveGame;
import mj.konfigurats.network.LobbyPackets.SrvRemoveLobbyUser;
import mj.konfigurats.network.LobbyPackets.SrvUpdateGame;
import mj.konfigurats.server.ServerManager;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.minlog.Log;

public class LobbyManager extends AbstractManager {
	// All users present in the lobby, <connection.toString().hashCode(),connection>:
	private final ConcurrentHashMap<Integer,Connection> lobbyUsers;
	// Packets with informations about currently active games, <packet.game.name.hashCode(),packet>:
	private final ConcurrentHashMap<Integer,SrvUpdateGame> activeGames;
	// Ranking data:
	private final String[][] rankingData;
	
	public LobbyManager(ServerManager serverManager) {
		super(serverManager);
		lobbyUsers = new ConcurrentHashMap<Integer,Connection>();
		activeGames = new ConcurrentHashMap<Integer,SrvUpdateGame>();
		rankingData = new String[3][];
	}

	@Override
	protected void analyzePacket(Connection connection, Object packet) {
		if(packet instanceof CltLobbyMessage) {
			// Sending the message to all users in the lobby:
			SrvLobbyMessage message = new SrvLobbyMessage();
			message.message = connection.toString()+": "+((CltLobbyMessage)packet).message;
			for(Connection user : lobbyUsers.values()) {
				user.sendUDP(message);
			}
		}
		else if(packet instanceof CltPrivateMessage) {
			// If the message target is in the lobby:
			if(lobbyUsers.containsKey(((CltPrivateMessage)packet).target.hashCode())) {
				CltPrivateMessage privateMessage = (CltPrivateMessage)packet;
				
				// Informing the sending user of the message:
				if(lobbyUsers.containsKey(connection.toString().hashCode())) {
					SrvLobbyMessage message = new SrvLobbyMessage();
					message.message = "To "+privateMessage.target+": "+privateMessage.message;
					connection.sendUDP(message);
				}
				else {
					// The user was not in the lobby:
					SrvGameChatMessage message = new SrvGameChatMessage();
					message.message = "To "+privateMessage.target+": "+privateMessage.message;
					connection.sendUDP(message);
				}
				
				// Sending message to the target:
				SrvLobbyMessage message = new SrvLobbyMessage();
				message.message = "From "+connection.toString()+": "+privateMessage.message;
				lobbyUsers.get(privateMessage.target.hashCode()).sendUDP(message);
			}
			else {
				// Target is not in the lobby. Finding out if he's logged:
				Connection target = serverManager.getConnectionManager()
					.getUser(((CltPrivateMessage)packet).target);
				if(target != null) {
					// Target is logged - telling the game thread to send him a message:
					serverManager.getGamesManager()
						.sendPrivateMessage(connection, target,
						((CltPrivateMessage)packet).message);					
				}
				else {
					// Target does not exist or isn't logged - informing the user:
					if(lobbyUsers.containsKey(connection.toString().hashCode())) {
						SrvLobbyMessage message = new SrvLobbyMessage();
						message.message = "User not logged.";
						connection.sendUDP(message);
					}
					else {
						// The user was not in the lobby:
						SrvGameChatMessage message = new SrvGameChatMessage();
						message.message = "User not logged.";
						connection.sendUDP(message);
					}
				}
			}
		}
		else if(packet instanceof CltLobbyLogOut) {
			// Logging out a user, telling connection thread to sign him out:
			disconnectUser(connection);
			serverManager.getConnectionManager().disconnect(connection);
			connection.sendTCP(new SrvLobbyLoggedOut());
		}
		else if(packet instanceof CltShowRanking) {
			// Sending database access request to the connection manager:
			serverManager.getConnectionManager().requestUserScore(connection);
		}
		else if(packet instanceof CltCreateGame) {
			// Creating a game room with a specified name:
			serverManager.getGamesManager().createGame(connection, (CltCreateGame)packet);
		}
		else if(packet instanceof CltJoinRandomGame) {
			// Putting the player in a random game room:
			serverManager.getGamesManager().enterGame(connection);
		}
		else if(packet instanceof CltJoinGame) {
			// Putting the player in a specific game room:
			serverManager.getGamesManager().enterGame(connection,((CltJoinGame)packet).name);
		}
		else if(packet instanceof CltJoinPlayer) {
			// Putting the player in a room which contains a specific player:
			serverManager.getGamesManager().joinPlayer(connection,((CltJoinPlayer)packet).username);
		}
		else if(packet instanceof CltRoomPassword) {
			// Putting the player in a specific, password-protected game room:
			serverManager.getGamesManager().enterGame
				(connection,((CltRoomPassword)packet).gameRoomName,((CltRoomPassword)packet).password);
		}
	}
	
	/**
	 * Signs a user to the lobby.
	 * @param connection user's connection.
	 */
	public void enterLobby(final Connection connection) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				if(!lobbyUsers.containsKey(connection.toString().hashCode())) {
					lobbyUsers.put(connection.toString().hashCode(),connection);
					Log.debug("ID"+connection.getID()+": "+connection.toString()+": entered the lobby");
					
					// Creating packet with the player's username:
					SrvAddLobbyUser newUser = new SrvAddLobbyUser();
					newUser.username = connection.toString();
	
					// Sending active games list to the player:
					for(SrvUpdateGame gameRoom : activeGames.values()) {
						connection.sendUDP(gameRoom);
					}
					
					// Sending packets with player nicknames:
					for(Connection user : lobbyUsers.values()) {
						// Sending player's username to each user in the lobby:
						if(user != connection) {
							user.sendUDP(newUser);
						}
						// Sending each player's username to the entering player:
						SrvAddLobbyUser nickname = new SrvAddLobbyUser();
						nickname.username = user.toString();
						connection.sendUDP(nickname);
					}
				}
				else {
					Log.warn("SRV: tried to put already present user "+connection.toString()+" in the lobby");
				}
			}
		});
	}

	@Override
	protected boolean disconnectUser(Connection connection) {
		if(lobbyUsers.remove(connection.toString().hashCode()) != null) {
			Log.debug("ID"+connection.getID()+": "+connection.toString()+": left the lobby");
			// Sending packets to each player in the lobby to remove the leaving user from the lobby list:
			SrvRemoveLobbyUser packet = new SrvRemoveLobbyUser();
			packet.username = connection.toString();
			for(Connection user : lobbyUsers.values()) {
				user.sendUDP(packet);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Updates informations about a single game room.
	 * Run by the manager's thread, this method is safe to be used by other managers.
	 * @param gameRoom packet containing informations about a single game room.
	 */
	public void updateGameRoomInfo(final PctGameInfo gameRoom) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				// Creating packet:
				SrvUpdateGame packet = new SrvUpdateGame();
				packet.game = gameRoom;
				// Adding packet to the active games list:
				activeGames.put(packet.game.name.hashCode(), packet);
				
				// Sending packet to all players in the lobby:
				for(Connection user : lobbyUsers.values()) {
					user.sendUDP(packet);
				}
			}
		});
	}
	
	/**
	 * Removes informations about a single game room, usually when it's removed.
	 * Run by the manager's thread, this method is safe to be used by other managers.
	 * @param gameRoom removed room's name.
	 */
	public void removeGameRoomInfo(final String gameRoom) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				if(activeGames.containsKey(gameRoom.hashCode())) {
					// Creating packet:
					SrvRemoveGame packet = new SrvRemoveGame();
					packet.name = gameRoom;
					// Removing packet from the active games list:
					activeGames.remove(gameRoom.hashCode());
					
					// Sending packet to all players in the lobby:
					for(Connection user : lobbyUsers.values()) {
						user.sendUDP(packet);
					}
				}
			}
		});
	}
	
	/**
	 * Sends a packet with current ranking data and player's scores to a user.
	 * @param player user's connection.
	 * @param kills user's kills amount.
	 * @param deaths user's deaths amount.
	 */
	public void sendRankingData(final Connection player,final int kills,final int deaths) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				SrvRankingData packet = new SrvRankingData();
				packet.topKills = rankingData[0];
				packet.topDeaths = rankingData[1];
				packet.topRatio = rankingData[2];
				packet.userKills = kills;
				packet.userDeaths = deaths;
				player.sendTCP(packet);
			}
		});
	}
	
	public void updateRankingData(final String[] topKills,
		final String[] topDeaths,final String[]topRatio) {
		rankingData[0] = topKills;
		rankingData[1] = topDeaths;
		rankingData[2] = topRatio;
		Log.info("SRV: ranking updated");
	}
}
