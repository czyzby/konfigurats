package mj.konfigurats;

import mj.konfigurats.server.ServerManager;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class HeadlessCore {
	public static void main(String[] args) {
		new HeadlessApplication(new ApplicationAdapter()  {
			@Override
			public void create() {
				// Initiating game server:
				ServerManager.SERVER.initiate();
				
				// Run before the application is closed:
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					@Override
					public void run() {
						ServerManager.SERVER.shutdown();
					}
				}));
			}
		},new HeadlessApplicationConfiguration() {
			{
				// Making sure the application won't try to render:
				this.updatesPerSecond = -1;
			}
		});
	}
}
