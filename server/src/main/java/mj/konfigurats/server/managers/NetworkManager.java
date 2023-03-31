package mj.konfigurats.server.managers;

import com.esotericsoftware.kryonet.Connection;

/**
 * A simple interface for a server manager.
 * @author MJ
 */
public interface NetworkManager {
	/**
	 * After the packet is allocated into one of the groups, network managers analyze its informations using this method.
	 * @param connection user's connection.
	 * @param packet packet sent by the user's client.
	 */
	public void analyze(Connection connection, Object packet);
	
	/**
	 * Run when the given user disconnects from the server.
	 * @param connection user's connection.
	 */
	public void disconnect(Connection connection);
}
