package mj.konfigurats.managers;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.minlog.Log;
import mj.konfigurats.Core;
import mj.konfigurats.managers.InterfaceManager.Screens;
import mj.konfigurats.network.ClientListener;
import mj.konfigurats.network.ConnectionPackets.*;
import mj.konfigurats.network.GamePackets.*;
import mj.konfigurats.network.LobbyPackets.*;
import mj.konfigurats.network.Ports;

import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkManager {
	public final static String
		LOCALHOST = "localhost",
		DEFAULT_IP = "85.193.224.223";
	private static String SERVER_IP = DEFAULT_IP;

	private final ExecutorService listenerThread;
	private final Client client;
	// User data:
	private String username; // Contains player's current server username.
	// Control variables:
	private boolean isActive; // Prevents the client from reconnecting when the user closes the application.

	public NetworkManager() {
		Log.INFO();
		listenerThread = Executors.newSingleThreadExecutor();
		client = new Client();
		isActive = true;

		// Setting up the client:
		client.addListener(new ClientListener(this));
		registerPackets();
	}

	/**
	 * Sets the address that the game is trying to connect.
	 * @param address server's address.
	 */
	public static void setServerAddress(String address) {
		SERVER_IP = address;
	}

	/**
	 * Registers all packets that the client is expected to send and receive.
	 */
	private final void registerPackets() {
		// General packets:
		client.getKryo().register(byte[].class);
		client.getKryo().register(float[].class);
		client.getKryo().register(int[].class);
		client.getKryo().register(String[].class);

		// Connection packets group:
		client.getKryo().register(ConnectionPacket.class);
		// Logging packets:
		client.getKryo().register(CltLogin.class);
		client.getKryo().register(SrvLogged.class);
		client.getKryo().register(SrvAlreadyLogged.class);
		client.getKryo().register(SrvUsernameInvalid.class);
		client.getKryo().register(SrvPasswordInvalid.class);
		// Registration packets:
		client.getKryo().register(CltRegister.class);
		client.getKryo().register(SrvRegistered.class);
		client.getKryo().register(SrvUsernameTaken.class);
		client.getKryo().register(SrvCorruptedData.class);

		// Lobby packets group:
		client.getKryo().register(LobbyPacket.class);
		// Logging packets:
		client.getKryo().register(CltLobbyLogOut.class);
		client.getKryo().register(SrvLobbyLoggedOut.class);
		// Chat packets:
		client.getKryo().register(CltLobbyMessage.class);
		client.getKryo().register(CltPrivateMessage.class);
		client.getKryo().register(SrvLobbyMessage.class);
		client.getKryo().register(SrvAddLobbyUser.class);
		client.getKryo().register(SrvRemoveLobbyUser.class);
		// Game rooms management packets:
		client.getKryo().register(PctGameInfo.class);
		client.getKryo().register(CltCreateGame.class);
		client.getKryo().register(CltJoinRandomGame.class);
		client.getKryo().register(CltJoinGame.class);
		client.getKryo().register(CltJoinPlayer.class);
		client.getKryo().register(CltRoomPassword.class);
		client.getKryo().register(SrvUpdateGame.class);
		client.getKryo().register(SrvRemoveGame.class);
		client.getKryo().register(SrvStartGame.class);
		client.getKryo().register(SrvEnterRoomPassword.class);
		client.getKryo().register(SrvInvalidRoomPassword.class);
		client.getKryo().register(SrvGameAlreadyExists.class);
		client.getKryo().register(SrvGameFull.class);
		client.getKryo().register(SrvGameNotFound.class);
		client.getKryo().register(SrvPlayerNotFound.class);
		client.getKryo().register(SrvNoGamesOpen.class);
		client.getKryo().register(SrvWrongMapIndex.class);
		// Ranking packets:
		client.getKryo().register(CltShowRanking.class);
		client.getKryo().register(SrvRankingData.class);

		// Game packets group:
		client.getKryo().register(GamePacket.class);
		// Logging packets:
		client.getKryo().register(CltLeaveGame.class);
		client.getKryo().register(SrvLeaveGame.class);
		// Game chat packets:
		client.getKryo().register(CltGameChatMessage.class);
		client.getKryo().register(CltTeamMessage.class);
		client.getKryo().register(SrvGameChatMessage.class);
		client.getKryo().register(SrvAddRoomUser.class);
		client.getKryo().register(SrvRemoveRoomUser.class);
		client.getKryo().register(SrvScoresUpdate.class);
		// Game logic packets:
		client.getKryo().register(CltGameInitiated.class);
		client.getKryo().register(CltCreateCharacter.class);
		client.getKryo().register(CltHandleClick.class);
		client.getKryo().register(CltHandleFireCast.class);
		client.getKryo().register(CltHandleWaterCast.class);
		client.getKryo().register(CltHandleEarthCast.class);
		client.getKryo().register(CltHandleAirCast.class);
		client.getKryo().register(SrvCorruptedCreationData.class);
		client.getKryo().register(SrvCreateCharacter.class);
		client.getKryo().register(SrvPlayerNotElite.class);
		client.getKryo().register(SrvUpdateWorld.class);
		client.getKryo().register(SrvSetSpellCooldown.class);
		client.getKryo().register(SrvDisplaySFX.class);
		client.getKryo().register(SrvAttachSFX.class);
		client.getKryo().register(SrvSetEntityFalling.class);
		client.getKryo().register(SrvSwitchMap.class);
	}

	/**
	 * Tries to connect to the game server every 2 seconds.
	 */
	public void connect() {
		listenerThread.execute(new Runnable() {
			@Override
			public void run() {
				client.start();
				try {
					if(!client.isConnected()) {
						client.connect(50000,SERVER_IP,Ports.TCP.get(),Ports.UDP.get());
					}
				}
				catch (IOException e) {
					Log.warn("Unable to connect.");
				}
			}
		});
		((Core)Gdx.app.getApplicationListener()).getThreadManager()
			.executeOnTimer(new TimerTask() {
			@Override
			public void run() {
				if(!client.isConnected()) {
					connect();
				}
			}
		},2000);
	}

	/**
	 * Shuts listener thread and network client down.
	 */
	public void dispose() {
		if(listenerThread != null) {
			listenerThread.shutdownNow();
		}
		if(client != null) {
			isActive = false;
			client.stop();
		}
	}

	/**
	 * Schedules a runnable for the network thread.
	 * @param runnable the runnable to be executed.
	 */
	public void executeOnNetworkThread(Runnable runnable) {
		listenerThread.execute(runnable);
	}

	/**
	 * Sends a packet through TCP. Use for every important packet, crucial to
	 * game logic or connecting to the server.
	 * @param packet information sent to the server.
	 */
	public void sendTCP(final Object packet) {
		listenerThread.execute(new Runnable() {
			@Override
			public void run() {
				client.sendTCP(packet);
			}
		});
	}

	/**
	 * Sends a packet through UDP. Use <b>only</b> if the packet can be lost
	 * without doing any/much damage.
	 * @param packet non-crucial information sent to the server.
	 */
	public void sendUDP(final Object packet) {
		listenerThread.execute(new Runnable() {
			@Override
			public void run() {
				client.sendUDP(packet);
			}
		});
	}

	/**
	 * Analyzes received packet.
	 * @param packet network packet.
	 */
	public void analyze(final Object packet) {
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				((Core)Gdx.app.getApplicationListener())
					.getInterfaceManager().update(packet);
			}
		});
	}

	/**
	 * Run when the user connects to the server. Sets up the menu screen.
	 */
	public void connected() {
		// Canceling scheduled connections:
		((Core)Gdx.app.getApplicationListener()).getThreadManager()
			.cancelTimerTasks();
		// Hiding reconnecting dialog (if present):
		((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.displayReconnectingDialog(false);
		// Displaying menu screen:
		((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getCurrentScreen().hide(Screens.MENU);
	}

	/**
	 * Attempts to reconnect to the server after the connection is lost.
	 */
	public void reconnect() {
		if(isActive) {
			((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
				.displayConnectingDialog(false);
			((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
				.displayReconnectingDialog(true);
			listenerThread.execute(new Runnable() {
				@Override
				public void run() {
					connect();
				}
			});
		}
	}

	/**
	 * @return current player's username (null if unlogged).
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the current nickname of the player for his current session.
	 * @param username new nickname.
	 */
	public void setUsername(String username) {
		this.username = username;
	}
}
