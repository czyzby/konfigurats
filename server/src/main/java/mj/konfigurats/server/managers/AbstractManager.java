package mj.konfigurats.server.managers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mj.konfigurats.server.ServerManager;

import com.esotericsoftware.kryonet.Connection;

/**
 * Basic network manager implementation. Contains a reference to the server manager and creates a separate manager thread.
 * @author MJ
 */
public abstract class AbstractManager implements NetworkManager {
	protected final ExecutorService managerThread;
	protected final ServerManager serverManager;
	
	public AbstractManager(ServerManager serverManager) {
		this.managerThread = Executors.newSingleThreadExecutor();
		this.serverManager = serverManager;
	}

	@Override
	public void analyze(final Connection connection,final Object packet) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				analyzePacket(connection, packet);
			}
		});
	}
	
	@Override
	public void disconnect(final Connection connection) {
		managerThread.execute(new Runnable() {
			@Override
			public void run() {
				disconnectUser(connection);
			}
		});
	}
	
	/**
	 * Run by the manager's thread, this method should contain actual analysis of the packet.
	 * @param connection user's connection.
	 * @param packet packet sent by the user's client.
	 */
	protected abstract void analyzePacket(Connection connection, Object packet);
	
	/**
	 * Run by the manager's thread, this method should actually disconnect the user.
	 * @param connection user's connection.
	 * @return true if the player was logged by the manager.
	 */
	protected abstract boolean disconnectUser(Connection connection);
}
