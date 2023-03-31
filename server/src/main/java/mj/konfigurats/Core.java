package mj.konfigurats;

import mj.konfigurats.server.ServerManager;


/**
 * Core application class containing main method. Do not initialize.
 * @author MJ
 */
public class Core {
	private Core() {}
	
	public static void main(String[] args) {
		// Initiating the server:
		ServerManager.SERVER.initiate();

		// Run before the application is closed:
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				ServerManager.SERVER.shutdown();
			}
		}));
	}
}
