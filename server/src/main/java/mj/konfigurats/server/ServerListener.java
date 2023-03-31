package mj.konfigurats.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

/**
 * A simple listener. Logs new connections and tells the server manager to handle packets and disconnections.
 * @author MJ
 */
public class ServerListener extends Listener {
	private final ServerManager server;
	
	public ServerListener(ServerManager server) {
		super();
		this.server = server;
	}
	
	@Override
	public void connected(Connection connection) {
		Log.info("ID"+connection.getID()+": user connected with IP: "+connection.getRemoteAddressTCP().getAddress().getHostAddress());
	}
	
	@Override
	public void disconnected(Connection connection) {
		server.disconnect(connection);
	}
	
	@Override
	public void received(Connection connection, Object packet) {
		server.analyze(connection, packet);
	}
}
