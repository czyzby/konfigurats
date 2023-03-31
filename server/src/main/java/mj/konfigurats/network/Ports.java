package mj.konfigurats.network;

/**
 * Contains ports.
 * @author MJ
 */
public enum Ports {
	TCP(42666),UDP(43666);
	
	private final int port;
	private Ports(int port) { this.port = port; }
	
	/**
	 * @return port number.
	 */
	public int get()
	{
		return port;
	}
}