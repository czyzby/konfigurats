package mj.konfigurats.server;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mj.konfigurats.logic.maps.Maps;
import mj.konfigurats.network.ConnectionPackets.CltLogin;
import mj.konfigurats.network.ConnectionPackets.CltRegister;
import mj.konfigurats.network.ConnectionPackets.ConnectionPacket;
import mj.konfigurats.network.ConnectionPackets.SrvAlreadyLogged;
import mj.konfigurats.network.ConnectionPackets.SrvCorruptedData;
import mj.konfigurats.network.ConnectionPackets.SrvLogged;
import mj.konfigurats.network.ConnectionPackets.SrvPasswordInvalid;
import mj.konfigurats.network.ConnectionPackets.SrvRegistered;
import mj.konfigurats.network.ConnectionPackets.SrvUsernameInvalid;
import mj.konfigurats.network.ConnectionPackets.SrvUsernameTaken;
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
import mj.konfigurats.network.GamePackets.GamePacket;
import mj.konfigurats.network.GamePackets.SrvAddRoomUser;
import mj.konfigurats.network.GamePackets.SrvAttachSFX;
import mj.konfigurats.network.GamePackets.SrvCorruptedCreationData;
import mj.konfigurats.network.GamePackets.SrvCreateCharacter;
import mj.konfigurats.network.GamePackets.SrvDisplaySFX;
import mj.konfigurats.network.GamePackets.SrvGameChatMessage;
import mj.konfigurats.network.GamePackets.SrvLeaveGame;
import mj.konfigurats.network.GamePackets.SrvPlayerNotElite;
import mj.konfigurats.network.GamePackets.SrvRemoveRoomUser;
import mj.konfigurats.network.GamePackets.SrvScoresUpdate;
import mj.konfigurats.network.GamePackets.SrvSetEntityFalling;
import mj.konfigurats.network.GamePackets.SrvSetSpellCooldown;
import mj.konfigurats.network.GamePackets.SrvSwitchMap;
import mj.konfigurats.network.GamePackets.SrvUpdateWorld;
import mj.konfigurats.network.LobbyPackets.CltCreateGame;
import mj.konfigurats.network.LobbyPackets.CltJoinGame;
import mj.konfigurats.network.LobbyPackets.CltJoinPlayer;
import mj.konfigurats.network.LobbyPackets.CltJoinRandomGame;
import mj.konfigurats.network.LobbyPackets.CltLobbyLogOut;
import mj.konfigurats.network.LobbyPackets.CltLobbyMessage;
import mj.konfigurats.network.LobbyPackets.CltPrivateMessage;
import mj.konfigurats.network.LobbyPackets.CltRoomPassword;
import mj.konfigurats.network.LobbyPackets.CltShowRanking;
import mj.konfigurats.network.LobbyPackets.LobbyPacket;
import mj.konfigurats.network.LobbyPackets.PctGameInfo;
import mj.konfigurats.network.LobbyPackets.SrvAddLobbyUser;
import mj.konfigurats.network.LobbyPackets.SrvEnterRoomPassword;
import mj.konfigurats.network.LobbyPackets.SrvGameAlreadyExists;
import mj.konfigurats.network.LobbyPackets.SrvGameFull;
import mj.konfigurats.network.LobbyPackets.SrvGameNotFound;
import mj.konfigurats.network.LobbyPackets.SrvInvalidRoomPassword;
import mj.konfigurats.network.LobbyPackets.SrvLobbyLoggedOut;
import mj.konfigurats.network.LobbyPackets.SrvLobbyMessage;
import mj.konfigurats.network.LobbyPackets.SrvNoGamesOpen;
import mj.konfigurats.network.LobbyPackets.SrvPlayerNotFound;
import mj.konfigurats.network.LobbyPackets.SrvRankingData;
import mj.konfigurats.network.LobbyPackets.SrvRemoveGame;
import mj.konfigurats.network.LobbyPackets.SrvRemoveLobbyUser;
import mj.konfigurats.network.LobbyPackets.SrvStartGame;
import mj.konfigurats.network.LobbyPackets.SrvUpdateGame;
import mj.konfigurats.network.LobbyPackets.SrvWrongMapIndex;
import mj.konfigurats.network.Ports;
import mj.konfigurats.server.managers.ConnectionManager;
import mj.konfigurats.server.managers.GamesManager;
import mj.konfigurats.server.managers.LobbyManager;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

/**
 * A singleton which manages a KryoNet server.
 * @author MJ
 */
public enum ServerManager {
	SERVER;
	
	// Server manager threads:
	private final ExecutorService listenerThread,analyzerThread;
	// KryoNet server:
	private final Server server;
	// Network managers:
	private final ConnectionManager connectionManager;
	private final LobbyManager lobbyManager;
	private final GamesManager gamesManager;
	
	private ServerManager() {
		// Logging settings:
		Log.DEBUG();
		
		// Loading game maps:
		Log.info("SRV: attempting to load resources...");
		Maps.createMaps();
		Log.info("SRV: maps loaded");
		
		// Initializing threads and managers:
		Log.info("SRV: attempting to start the server...");
		listenerThread = Executors.newSingleThreadExecutor();
		analyzerThread = Executors.newSingleThreadExecutor();
		server = new Server();
		lobbyManager = new LobbyManager(this);
		connectionManager = new ConnectionManager(this);
		gamesManager = new GamesManager(this);
		
		// Server settings:
		registerPackets();
		server.addListener(new ServerListener(this));
		
		// Binding ports:
		try {
			Log.info("SRV: binding ports...");
			server.bind(Ports.TCP.get(),Ports.UDP.get());
		}
		catch (IOException e) {
			Log.error("SRV: unable to bind the ports: "
				+e.getLocalizedMessage());
			System.exit(1);
		}
		finally {
			Log.info("SRV: server created");
		}
	}
	
	/**
	 * Starts the server on the listener thread.
	 */
	public void initiate() {
		listenerThread.execute(new Runnable() {
			@Override
			public void run() {
				server.start();
				Log.info("SRV: server initiated");
			}
		});
	}
	
	/**
	 * Registers the packets that the server is expected to receive and send.
	 */
	private final void registerPackets() {
		// General packets:
		server.getKryo().register(byte[].class);
		server.getKryo().register(float[].class);
		server.getKryo().register(int[].class);
		server.getKryo().register(String[].class);
		
		// Connection packets group:
		server.getKryo().register(ConnectionPacket.class);
		// Logging packets:
		server.getKryo().register(CltLogin.class);
		server.getKryo().register(SrvLogged.class);
		server.getKryo().register(SrvAlreadyLogged.class);
		server.getKryo().register(SrvUsernameInvalid.class);
		server.getKryo().register(SrvPasswordInvalid.class);
		// Registration packets:
		server.getKryo().register(CltRegister.class);
		server.getKryo().register(SrvRegistered.class);
		server.getKryo().register(SrvUsernameTaken.class);
		server.getKryo().register(SrvCorruptedData.class);
		
		// Lobby packets group:
		server.getKryo().register(LobbyPacket.class);
		// Logging packets:
		server.getKryo().register(CltLobbyLogOut.class);
		server.getKryo().register(SrvLobbyLoggedOut.class);
		// Chat packets:
		server.getKryo().register(CltLobbyMessage.class);
		server.getKryo().register(CltPrivateMessage.class);
		server.getKryo().register(SrvLobbyMessage.class);
		server.getKryo().register(SrvAddLobbyUser.class);
		server.getKryo().register(SrvRemoveLobbyUser.class);
		// Game rooms management packets:
		server.getKryo().register(PctGameInfo.class);
		server.getKryo().register(CltCreateGame.class);
		server.getKryo().register(CltJoinRandomGame.class);
		server.getKryo().register(CltJoinGame.class);
		server.getKryo().register(CltJoinPlayer.class);
		server.getKryo().register(CltRoomPassword.class);
		server.getKryo().register(SrvUpdateGame.class);
		server.getKryo().register(SrvRemoveGame.class);
		server.getKryo().register(SrvStartGame.class);
		server.getKryo().register(SrvEnterRoomPassword.class);
		server.getKryo().register(SrvInvalidRoomPassword.class);
		server.getKryo().register(SrvGameAlreadyExists.class);
		server.getKryo().register(SrvGameFull.class);
		server.getKryo().register(SrvGameNotFound.class);
		server.getKryo().register(SrvPlayerNotFound.class);
		server.getKryo().register(SrvNoGamesOpen.class);
		server.getKryo().register(SrvWrongMapIndex.class);
		// Ranking packets:
		server.getKryo().register(CltShowRanking.class);
		server.getKryo().register(SrvRankingData.class);
		
		// Game packets group:
		server.getKryo().register(GamePacket.class);
		// Logging packets:
		server.getKryo().register(CltLeaveGame.class);
		server.getKryo().register(SrvLeaveGame.class);
		// Game chat packets:
		server.getKryo().register(CltGameChatMessage.class);
		server.getKryo().register(CltTeamMessage.class);
		server.getKryo().register(SrvGameChatMessage.class);
		server.getKryo().register(SrvAddRoomUser.class);
		server.getKryo().register(SrvRemoveRoomUser.class);
		server.getKryo().register(SrvScoresUpdate.class);
		// Game logic packets:
		server.getKryo().register(CltGameInitiated.class);
		server.getKryo().register(CltCreateCharacter.class);
		server.getKryo().register(CltHandleClick.class);
		server.getKryo().register(CltHandleFireCast.class);
		server.getKryo().register(CltHandleWaterCast.class);
		server.getKryo().register(CltHandleEarthCast.class);
		server.getKryo().register(CltHandleAirCast.class);
		server.getKryo().register(SrvCorruptedCreationData.class);
		server.getKryo().register(SrvCreateCharacter.class);
		server.getKryo().register(SrvPlayerNotElite.class);
		server.getKryo().register(SrvUpdateWorld.class);
		server.getKryo().register(SrvSetSpellCooldown.class);
		server.getKryo().register(SrvDisplaySFX.class);
		server.getKryo().register(SrvAttachSFX.class);
		server.getKryo().register(SrvSetEntityFalling.class);
		server.getKryo().register(SrvSwitchMap.class);
	}
	
	/**
	 * Run by the listener when a user disconnects from the server.
	 * @param connection user's connection.
	 */
	public void disconnect(final Connection connection) {
		analyzerThread.execute(new Runnable() {
			@Override
			public void run() {
				connectionManager.disconnect(connection);
				lobbyManager.disconnect(connection);
				gamesManager.disconnect(connection);
				
				Log.info("ID"+connection.getID()+": user disconnected");
			}
		});
	}
	
	/**
	 * Run by the listener when the server receives a packet.
	 * The packet is analyzed by the analyzer thread and handled by one of the managers, depending on its type.
	 * @param connection user's connection.
	 * @param packet packet sent by the user's client.
	 */
	public void analyze(final Connection connection,final Object packet) {
		analyzerThread.execute(new Runnable() {
			@Override
			public void run() {
				if(packet instanceof ConnectionPacket) {
					connectionManager.analyze(connection, packet);
				}
				else if(packet instanceof LobbyPacket) {
					lobbyManager.analyze(connection, packet);
				}
				else if(packet instanceof GamePacket) {
					gamesManager.analyze(connection, packet);
				}
			}
		});
	}
	
	/**
	 * Turns off the server. Run before the application is closed.
	 */
	public void shutdown() {
		Log.info("SRV: attempting to stop the server...");
		listenerThread.execute(new Runnable() {
			@Override
			public void run() {
				// Closing the KryoNet server:
				if(server != null) {
					server.close();
					Log.info("SRV: closed KryoNet server");
				}
			}
		});
		// Disposing of the Box2D worlds, managed by the games manager:
		if(gamesManager != null) {
			gamesManager.shutdown();
		}
		// Closing database connection:
		if(connectionManager != null) {
			connectionManager.shutdown();
		}
	}
	
	/**
	 * @return current connection manager.
	 */
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}
	
	/**
	 * @return current lobby manager.
	 */
	public LobbyManager getLobbyManager() {
		return lobbyManager;
	}
	
	/**
	 * @return current games manager.
	 */
	public GamesManager getGamesManager() {
		return gamesManager;
	}
}
