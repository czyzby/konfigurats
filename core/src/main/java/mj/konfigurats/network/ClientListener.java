package mj.konfigurats.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import mj.konfigurats.managers.NetworkManager;

/**
 * A simple network client listener.
 * @author MJ
 */
public class ClientListener extends Listener {
	private final NetworkManager networkManager;

	public ClientListener(NetworkManager networkManager) {
		this.networkManager = networkManager;
	}
	@Override
	public void connected(Connection connection) {
		networkManager.connected();
	}

	@Override
	public void received(Connection connection,final Object packet) {
		networkManager.analyze(packet);
	}

	@Override
	public void disconnected(Connection arg0) {
		networkManager.reconnect();
	}
}
