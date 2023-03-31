package mj.konfigurats;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import mj.konfigurats.game.utilities.Maps;
import mj.konfigurats.managers.InterfaceManager;
import mj.konfigurats.managers.NetworkManager;
import mj.konfigurats.managers.ThreadManager;

public class Core implements ApplicationListener {
	// Managers:
	private InterfaceManager interfaceManager;
	private NetworkManager networkManager;
	private ThreadManager threadManager;

	@Override
	public void create() {
		// Setting up clearing color:
		Gdx.gl.glClearColor(0, 0, 0, 1);

		// Creating managers:
		interfaceManager = new InterfaceManager();
		threadManager = new ThreadManager();
		networkManager = new NetworkManager();

		// Creating screen objects and setting up the first screen:
		interfaceManager.loadAssets();
		interfaceManager.initiateScreens();
	}

	@Override
	public void resize(int width, int height) {
		interfaceManager.resize(width, height);
	}

	@Override
	public void dispose() {
		// Managers disposing:
		if(interfaceManager != null) {
			interfaceManager.dispose();
		}
		if(networkManager != null) {
			networkManager.dispose();
		}
		if(threadManager != null) {
			threadManager.dispose();
		}

		// Game resources disposing:
		Maps.dispose();
	}

	@Override
	public void render() {
		interfaceManager.render(Gdx.graphics.getDeltaTime());
	}

	@Override
	public void pause() {
		//TODO
	}

	@Override
	public void resume() {
		//TODO
	}

	/**
	 * @return current interface manager.
	 */
	public InterfaceManager getInterfaceManager() {
		return interfaceManager;
	}

	/**
	 * @return current thread manager.
	 */
	public NetworkManager getNetworkManager() {
		return networkManager;
	}

	/**
	 * @return current thread manager.
	 */
	public ThreadManager getThreadManager() {
		return threadManager;
	}

	/**
	 * Contains version IDs.
	 * @author MJ
	 */
	public static enum Version {
		CURRENT("0.9.9b");

		private final String version;
		private Version(String version) {
			this.version = version;
		}

		@Override
		public String toString() {
			return version;
		}
	}
}
