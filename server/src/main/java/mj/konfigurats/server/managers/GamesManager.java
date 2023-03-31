package mj.konfigurats.server.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.Game.GameMode;
import mj.konfigurats.logic.GameRoomUser;
import mj.konfigurats.logic.maps.Maps.MapInfo;
import mj.konfigurats.network.GamePackets.CltCreateCharacter;
import mj.konfigurats.network.GamePackets.CltGameChatMessage;
import mj.konfigurats.network.GamePackets.CltGameInitiated;
import mj.konfigurats.network.GamePackets.CltHandleAirCast;
import mj.konfigurats.network.GamePackets.CltHandleClick;
import mj.konfigurats.network.GamePackets.CltHandleEarthCast;
import mj.konfigurats.network.GamePackets.CltHandleFireCast;
import mj.konfigurats.network.GamePackets.CltHandleWaterCast;
import mj.konfigurats.network.GamePackets.CltLeaveGame;
import mj.konfigurats.network.GamePackets.CltTeamMessage;
import mj.konfigurats.network.GamePackets.SrvAddRoomUser;
import mj.konfigurats.network.GamePackets.SrvGameChatMessage;
import mj.konfigurats.network.GamePackets.SrvLeaveGame;
import mj.konfigurats.network.GamePackets.SrvRemoveRoomUser;
import mj.konfigurats.network.GamePackets.SrvSwitchMap;
import mj.konfigurats.network.LobbyPackets.CltCreateGame;
import mj.konfigurats.network.LobbyPackets.PctGameInfo;
import mj.konfigurats.network.LobbyPackets.SrvEnterRoomPassword;
import mj.konfigurats.network.LobbyPackets.SrvGameAlreadyExists;
import mj.konfigurats.network.LobbyPackets.SrvGameFull;
import mj.konfigurats.network.LobbyPackets.SrvGameNotFound;
import mj.konfigurats.network.LobbyPackets.SrvInvalidRoomPassword;
import mj.konfigurats.network.LobbyPackets.SrvLobbyMessage;
import mj.konfigurats.network.LobbyPackets.SrvNoGamesOpen;
import mj.konfigurats.network.LobbyPackets.SrvPlayerNotFound;
import mj.konfigurats.network.LobbyPackets.SrvStartGame;
import mj.konfigurats.network.LobbyPackets.SrvWrongMapIndex;
import mj.konfigurats.server.ServerManager;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.minlog.Log;

public class GamesManager extends AbstractManager {
	// List of currently active games, <Game.hashCode(),Game>.
	private final ConcurrentHashMap<Integer,Game> activeGames;
	// List of players connected with their current game rooms, <Connection.toString().hashCode(),Game>.
	private final ConcurrentHashMap<Integer,Game> playersGameRooms;
	// List of currently open (not full) games. Used only to get a random game from the list.
	private final List<Game> openGames;
	
	public GamesManager(ServerManager serverManager) {
		super(serverManager);
		activeGames = new ConcurrentHashMap<Integer,Game>();
		playersGameRooms = new ConcurrentHashMap<Integer,Game>();
		openGames = new CopyOnWriteArrayList<Game>(); // Replace with a hash map if it turns out to be inefficient.
	}

	@Override
	protected void analyzePacket(Connection connection, Object packet) {
		// Game management packets:
		if(packet instanceof CltGameChatMessage) {
			// Got a game room chat message - check if the player is in a game room:
			if(playersGameRooms.containsKey(connection.toString().hashCode())) {
				// Sending the message to all users in the player's game room:
				SrvGameChatMessage message = new SrvGameChatMessage();
				message.message = connection.toString()+": "+((CltGameChatMessage)packet).message;
				playersGameRooms.get(connection.toString().hashCode()).sendMessage(message);
			}
		}
		else if(packet instanceof CltTeamMessage) {
			// Got a game team chat message - check if the player is in a game room:
			if(playersGameRooms.containsKey(connection.toString().hashCode())) {
				if(playersGameRooms.get(connection.toString().hashCode()).getGameMode() == GameMode.TEAM) {
					SrvGameChatMessage message = new SrvGameChatMessage();
					message.message = "(Team) " + connection.toString()+": "+((CltTeamMessage)packet).message;
					playersGameRooms.get(connection.toString().hashCode()).sendTeamMessage(connection,message);
				}
			}
		}
		else if(packet instanceof CltLeaveGame) {
			// Player is trying to leave the game - check if he's playing:
			if(disconnectUser(connection)) {
				serverManager.getLobbyManager().enterLobby(connection);
				// Send him a packet to enter lobby:
				connection.sendTCP(new SrvLeaveGame());
			}
		}
		// Game logic packets:
		else if(packet instanceof CltGameInitiated) {
			// Player's client initiated a game - tell him about current entities:
			if(playersGameRooms.containsKey(connection.toString().hashCode())) {
				playersGameRooms.get(connection.toString().hashCode())
					.sendCurrentEntities(connection);
			}
		}
		else if(packet instanceof CltCreateCharacter) {
			// Player is trying to enter the game with a new character:
			if(playersGameRooms.containsKey(connection.toString().hashCode())) {
				playersGameRooms.get(connection.toString().hashCode())
					.createCharacter(connection, (CltCreateCharacter)packet);
			}
		}
		else if(packet instanceof CltHandleClick) {
			// If the player is actually in a game:
			if(playersGameRooms.containsKey(connection.toString().hashCode())) {
				// Telling the game room to update its logic:
				playersGameRooms.get(connection.toString().hashCode())
					.handleClick(connection, ((CltHandleClick)packet).x,
					((CltHandleClick)packet).y);
			}
		}
		else if(packet instanceof CltHandleFireCast) {
			// If the player is actually in a game:
			if(playersGameRooms.containsKey(connection.toString().hashCode())) {
				// Telling the game room to update its logic:
				playersGameRooms.get(connection.toString().hashCode())
					.handleFireCast(connection, ((CltHandleFireCast)packet).x,
					((CltHandleFireCast)packet).y);
			}
		}
		else if(packet instanceof CltHandleWaterCast) {
			// If the player is actually in a game:
			if(playersGameRooms.containsKey(connection.toString().hashCode())) {
				// Telling the game room to update its logic:
				playersGameRooms.get(connection.toString().hashCode())
					.handleWaterCast(connection, ((CltHandleWaterCast)packet).x,
					((CltHandleWaterCast)packet).y);
			}
		}
		else if(packet instanceof CltHandleEarthCast) {
			// If the player is actually in a game:
			if(playersGameRooms.containsKey(connection.toString().hashCode())) {
				// Telling the game room to update its logic:
				playersGameRooms.get(connection.toString().hashCode())
					.handleEarthCast(connection, ((CltHandleEarthCast)packet).x,
					((CltHandleEarthCast)packet).y);
			}
		}
		else if(packet instanceof CltHandleAirCast) {
			// If the player is actually in a game:
			if(playersGameRooms.containsKey(connection.toString().hashCode())) {
				// Telling the game room to update its logic:
				playersGameRooms.get(connection.toString().hashCode())
					.handleAirCast(connection, ((CltHandleAirCast)packet).x,
					((CltHandleAirCast)packet).y);
			}
		}
	}
	
	public void sendPrivateMessage(final Connection creator,final Connection target,final String message) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				// Checking if the player is in a game room:
				if(playersGameRooms.containsKey(target.toString().hashCode())) {
					// Informing the sending user of the message:
					if(playersGameRooms.containsKey(creator.toString().hashCode())) {
						SrvGameChatMessage messagePacket = new SrvGameChatMessage();
						messagePacket.message = "To "+target.toString()+": "+message;
						creator.sendUDP(messagePacket);
					}
					else {
						// The user was not in the game room:
						SrvLobbyMessage messagePacket = new SrvLobbyMessage();
						messagePacket.message = "To "+target.toString()+": "+message;
						creator.sendUDP(messagePacket);
					}
					
					// Sending message to the target:
					SrvGameChatMessage messagePacket = new SrvGameChatMessage();
					messagePacket.message = "From "+creator.toString()+": "+message;
					target.sendUDP(messagePacket);
				}
				else {
					// User was logged and not in the lobby at the time of
					// scheduling, but now is not in a game room.
					if(playersGameRooms.containsKey(creator.toString().hashCode())) {
						SrvGameChatMessage messagePacket = new SrvGameChatMessage();
						messagePacket.message = "Could not locate the player.";
						creator.sendUDP(messagePacket);
					}
					else {
						// The user was not in the game room:
						SrvLobbyMessage messagePacket = new SrvLobbyMessage();
						messagePacket.message = "Could not locate the player.";
						creator.sendUDP(messagePacket);
					}
				}
			}
		});
	}

	@Override
	protected boolean disconnectUser(final Connection connection) {
		if(playersGameRooms.containsKey(connection.toString().hashCode())) {			
			// Getting the player's game room:
			final Game game = playersGameRooms.get(connection.toString().hashCode());
			
			// If the game is full, now it won't be - adding it to the open games list:
			if(game.isFull() && !game.isPasswordProtected()) {
				openGames.add(game);
			}
			
			// Removing the player from his game room:
			game.removePlayer(connection);
			playersGameRooms.remove(connection.toString().hashCode());
			Log.info("ID"+connection.getID()+": "+connection.toString()+": left game room: "+game.toString());
			
			// Telling the game thread to schedule a validity check on the game:
			game.getGameThread().schedule(new TimerTask() {
				@Override
				public void run() {
					validateGame(game,connection);
				}
			}, 50);
			
			return true;
		}
		// User is not in a game room.
		return false;
	}
	
	/**
	 * Should be run after removing a player from the room.
	 * @param game game room being validated.
	 * @param leavingPlayer player leaving the game room.
	 */
	private void validateGame(final Game game,final Connection leavingPlayer) {
		managerThread.execute(new Runnable() {
			public void run() {
				if(game.isEmpty()) {
					// If the game room is empty - deleting game room:
					activeGames.remove(game.hashCode());
					openGames.remove(game);
					serverManager.getLobbyManager().removeGameRoomInfo(game.toString());
					game.dispose();
					Log.info("SRV: removed empty game room: "+game.toString());
				}
				else {
					// If it isn't: updating game room informations in the lobby:
					PctGameInfo gameInfo = new PctGameInfo();
					gameInfo.name = game.toString();
					gameInfo.mode = game.getGameMode().getModeName();
					gameInfo.players = game.getPlayersAmount();
					gameInfo.limit = game.getPlayersLimit();
					serverManager.getLobbyManager().updateGameRoomInfo(gameInfo);
					
					// Removing the leaving user from the rest users' chat lists:
					final SrvRemoveRoomUser removeUser = new SrvRemoveRoomUser();
					removeUser.username = leavingPlayer.toString();
					game.getGameThread().schedule(new TimerTask() {
						@Override
						public void run() {
							for(Connection user : game.getPlayers()) {
								user.sendUDP(removeUser);
							}
						}
					}, 0);
				}
			}
		});
	}
	
	/**
	 * Creates a game with the specified name. Takes the creator out of the lobby and puts him in the game room.
	 * @param creator game's creator.
	 * @param packet game's informations.
	 */
	public void createGame(final Connection creator,final CltCreateGame packet) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				if(!playersGameRooms.containsKey(creator.toString().hashCode())) {
					if(!activeGames.containsKey(packet.name.hashCode())) {
						// Getting map informations:
						MapInfo mapInfo = MapInfo.getMapInfo(packet.mapIndex);
						
						if(mapInfo != null) {
							// Getting game mode info:
							GameMode gameMode = GameMode.getGameMode(packet.gameMode);
							
							if(gameMode != null) {
								// Creating game object and adding it to the lists:
								Game game = new Game(packet.name,packet.password,mapInfo,gameMode,-1,-1);
								activeGames.put(game.hashCode(),game);
								if(!game.isPasswordProtected()) {
									openGames.add(game);
								}
								Log.info("ID"+creator.getID()+": "+creator.toString()+": created a new game room: "+game.toString());
								
								// Adding creator to the game room:
								addPlayerToGameRoom(creator, game, true, false);
							}
							else {
								// Received corrupted game map index:
								Log.warn("ID"+creator.getID()+": "+creator.toString()+": send packet with corrupted game mode index");
								creator.sendTCP(new SrvWrongMapIndex());
							}
						}
						else {
							// Received corrupted game map index:
							Log.warn("ID"+creator.getID()+": "+creator.toString()+": send packet with corrupted map index");
							creator.sendTCP(new SrvWrongMapIndex());
						}
					}
					else {
						// Game room name is taken:
						creator.sendTCP(new SrvGameAlreadyExists());
					}
				}
			}
		});
	}
	
	/**
	 * Makes the player join a random game.
	 * @param connection user's connection.
	 */
	public void enterGame(final Connection connection) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				if(!playersGameRooms.containsKey(connection.toString())) {
					if(!openGames.isEmpty()) {
						// If the are some open rooms - get one at random:
						addPlayerToGameRoom(connection,openGames.get(RandomNumber
							.RANDOM.getRandom(openGames.size())), false, false);
					}
					else {
						// If there aren't - send a packet:
						connection.sendTCP(new SrvNoGamesOpen());
					}
				}
			}
		});
	}
	
	/**
	 * Lets the player join a game with a specific name.
	 * @param connection user's connection.
	 * @param name game's name.
	 */
	public void enterGame(final Connection connection,final String name) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				if(!playersGameRooms.containsKey(connection.toString())) {
					if(activeGames.containsKey(name.hashCode())) {
						addPlayerToGameRoom(connection,
							activeGames.get(name.hashCode()), false, false);
					}
					else {
						// Game doesn't exist:
						connection.sendTCP(new SrvGameNotFound());
					}
				}
			}
		});
	}
	
	/**
	 * Lets the player join a password-protected game room.
	 * @param connection user's connection.
	 * @param name game's name.
	 * @param password room's password.
	 */
	public void enterGame(final Connection connection,final String name,final String password) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				if(!playersGameRooms.containsKey(connection.toString())) {
					if(activeGames.containsKey(name.hashCode())) {
						if(activeGames.get(name.hashCode()).getPassword().equals(password)) {
							addPlayerToGameRoom(connection,activeGames
								.get(name.hashCode()),true,false);
						}
						else {
							// Invalid password:
							connection.sendTCP(new SrvInvalidRoomPassword());
						}
					}
					else {
						// Game doesn't exist:
						connection.sendTCP(new SrvGameNotFound());
					}
				}
			}
		});
	}

	/**
	 * Allows the player to join another player's game.
	 * @param connection the player that is trying to join.
	 * @param name the nickname of the one he's trying to join.
	 */
	public void joinPlayer(final Connection connection,final String name) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				if(!playersGameRooms.containsKey(connection.toString())) {
					if(playersGameRooms.containsKey(name.hashCode())) {
						addPlayerToGameRoom(connection,playersGameRooms
							.get(name.hashCode()),false, false);
					}
					else {
						// Player isn't playing:
						connection.sendTCP(new SrvPlayerNotFound());
					}
				}
			}
		});
	}
	
	/**
	 * Adds a player to a game room.
	 * Should be used only by this manager and after making sure the player is not already playing.
	 * @param connection the player added to the game room.
	 * @param game the game room.
	 * @param enteredPassword true if the player already entered room's password
	 * or should enter the room without password check (for example - if he's the creator).
	 * @param isMapSwitched true if the player was already in the room, but
	 * its map changed and now he has to be added once again.
	 */
	private void addPlayerToGameRoom(final Connection connection,final Game game,
		boolean enteredPassword,boolean isMapSwitched) {
		if(!game.isFull()) {
			if(enteredPassword || !game.isPasswordProtected()) {
				// Adding player to the game room:
				game.addPlayer(connection);
				playersGameRooms.put(connection.toString().hashCode(),game);
	
				if(isMapSwitched) {
					// Sending packet to let the client switch his map:
					SrvSwitchMap packet = new SrvSwitchMap();
					packet.name = game.toString();
					packet.roomIndex = game.getRoomIndex();
					packet.mapIndex = game.getMapIndex();
					connection.sendTCP(packet);
				}
				else {
					// Sending packet to make the client join a game:
					SrvStartGame packet = new SrvStartGame();
					packet.name = game.toString();
					packet.roomIndex = game.getRoomIndex();
					packet.mapIndex = game.getMapIndex();
					connection.sendTCP(packet);
					Log.info("ID"+connection.getID()+": "+connection.toString()+": entered game room: "+game.toString());		
		
					// Disconnecting player from the lobby:
					serverManager.getLobbyManager().disconnect(connection);
				}
				
				// Sending packets with names of the players in the room:
				game.getGameThread().schedule(new TimerTask() {
					@Override
					public void run() {
						SrvAddRoomUser newUserNickname = new SrvAddRoomUser();
						newUserNickname.username = connection.toString();
						newUserNickname.teamIndex = game.getTeamIndex(connection);
						
						for(GameRoomUser user : game.getUsersInfo()) {
							if(user.getUserConnection() != connection) {
								// If it's an old user, inform the new user of his presence:
								SrvAddRoomUser userNickname = new SrvAddRoomUser();
								userNickname.username = user.toString();
								userNickname.teamIndex = user.getTeamIndex();
								connection.sendUDP(userNickname);
							}
							// Inform all users of the new user presence:
							user.sendUDP(newUserNickname);
						}
						
						// Validating game room info in the lobby:
						validateGame(game);
					}
				}, 50);
			}
			else {
				// Game room requires password:
				SrvEnterRoomPassword packet = new SrvEnterRoomPassword();
				packet.gameRoomName = game.toString();
				
				connection.sendTCP(packet);
			}
		}
		else {
			// Game room is full:
			connection.sendTCP(new SrvGameFull());
		}
	}
	
	/**
	 * Validates data about an open game after a player joins the room.
	 * @param game validated game.
	 */
	private void validateGame(final Game game) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				// If the game is full - remove it from the open games list:
				if(game.isFull()) {
					openGames.remove(game);
				}
				
				// Updating game room list in the lobby:
				PctGameInfo gameInfo = new PctGameInfo();
				gameInfo.name = game.toString();
				gameInfo.mode = game.getGameMode().getModeName();
				gameInfo.players = game.getPlayersAmount();
				gameInfo.limit = game.getPlayersLimit();
				serverManager.getLobbyManager().updateGameRoomInfo(gameInfo);
			}
		});
	}
	
	public void resetMap(final Game game) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				// Removing game from the lists:
				activeGames.remove(game.hashCode());
				openGames.remove(game);
				serverManager.getLobbyManager().removeGameRoomInfo(game.toString());
				// Getting player list:
				ArrayList<Connection> players = new ArrayList<Connection>();
				players.addAll(game.getPlayers());
				// Removing players from the game:
				for(Connection player : players) {
					playersGameRooms.remove(player.toString().hashCode());
				}
				
				// Destroying the game:
				game.dispose();
				
				// Creating a new game object and adding it to the lists:
				Game newMapGame = new Game(game.toString(),game.getPassword(),
					MapInfo.RANDOM_CHANGING,game.getGameMode(),game.getMapIndex(),players.size());
				activeGames.put(newMapGame.hashCode(),newMapGame);
				if(!newMapGame.isPasswordProtected()) {
					openGames.add(newMapGame);
				}
				Log.debug("SRV: switched map on: "+newMapGame.toString()+
					", new limit: "+newMapGame.getPlayersLimit()+", new index: "+newMapGame.getMapIndex());
				
				// Adding players to the game:
				for(Connection player : players) {
					addPlayerToGameRoom(player, newMapGame, true, true);
				}
				
//				if(newMapGame.isFull()) {
//					// Removing game info from the lobby:
//					serverManager.getLobbyManager().removeGameRoomInfo(newMapGame.toString());
//				}
//				else {
//					// Updating game room list in the lobby:
//					PctGameInfo gameInfo = new PctGameInfo();
//					gameInfo.name = newMapGame.toString();
//					gameInfo.mode = newMapGame.getGameMode().getModeName();
//					gameInfo.players = newMapGame.getPlayersAmount();
//					gameInfo.limit = newMapGame.getPlayersLimit();
//					serverManager.getLobbyManager().updateGameRoomInfo(gameInfo);
//				}
			}
		});
	}
	
	/**
	 * Since the Box2D worlds require disposing, this manager has a "disposing" method.
	 * It's safe to use by other managers - the command itself is run by this manager's thread.
	 * Should be run before turning off the server.
	 */
	public void shutdown() {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				// Disposing Box2D worlds:
				for(Game game : activeGames.values()) {
					game.dispose();
				}
				// Clearing games lists:
				playersGameRooms.clear();
				openGames.clear();
				activeGames.clear();

				Log.info("SRV: destroyed Box2D worlds");
			}
		});
	}
	
	/**
	 * A singleton for a random number generator to avoid creating a new object each time.
	 * @author MJ
	 */
	private static enum RandomNumber {
		RANDOM;
		
		private Random random;
		private RandomNumber() {
			random = new Random();
		}
		
		/**
		 * @param max maximum value (exclusive).
		 * @return random value in range of 0 and maximum value.
		 */
		public int getRandom(int max) {
			return random.nextInt(max);
		}
		
//		/**
//		 * @param min minimum value.
//		 * @param max maximum value (exclusive).
//		 * @return random value in the given range.
//		 */
//		public int getRandom(int min,int max) {
//			return random.nextInt(max-min) + min;
//		}
	}
}
