package mj.konfigurats.network;

/**
 * Utility class containing connection network packets. Do <b>not</b> try to initialize.
 * Srv packets are sent by the server, Clt are sent by clients, Pct are utility classes - they contain some informations for other packets and are never send individually.
 * Interfaces represent different groups of packets, usually used for different purposes and handled by different managers (and threads).
 * @author MJ
 */
public class ConnectionPackets {
	private ConnectionPackets() {}
	
	/**
	 * Connection packets are used to establish a connection with the server and let the users log in and out.
	 */
	public static interface ConnectionPacket {}
	
	// Logging packets:
	/**
	 * Contains data that the user entered to log in.
	 */
	public static class CltLogin implements ConnectionPacket {
		public String username,password;
	}
	
	/**
	 * Sent when a user successfully logs in.
	 */
	public static class SrvLogged implements ConnectionPacket {
		public String username;
	}
	
	/**
	 * Sent when a user tries to log in with an already logged account.
	 */
	public static class SrvAlreadyLogged implements ConnectionPacket {}
	
	/**
	 * Sent when a user enters an username that doesn't exist in the database.
	 */
	public static class SrvUsernameInvalid implements ConnectionPacket {}
	
	/**
	 * Sent when a user enters a wrong password for the given username.
	 */
	public static class SrvPasswordInvalid implements ConnectionPacket {}
	
	// Registration packets:	
	/**
	 * Contains data that the user entered to register a new account.
	 */
	public static class CltRegister implements ConnectionPacket {
		public String username,password;
	}
	
	/**
	 * Sent when a user successfully registers an account.
	 */
	public static class SrvRegistered implements ConnectionPacket {}
	
	/**
	 * Sent when a user tries to register an account with an already taken username.
	 */
	public static class SrvUsernameTaken implements ConnectionPacket {}
	
	/**
	 * Sent when the server receives data that don't match the requirements for
	 * a username or a password.
	 */
	public static class SrvCorruptedData implements ConnectionPacket {}
}
