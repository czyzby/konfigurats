package mj.konfigurats.gui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import mj.konfigurats.Core;
import mj.konfigurats.gui.AbstractInterfaceScreen;
import mj.konfigurats.managers.InterfaceManager;
import mj.konfigurats.managers.NetworkManager;

public class LoadingScreen extends AbstractInterfaceScreen {
	// Control variables:
	private boolean loaded;
	// UI elements:
	private Label loadingLabel,connectingLabel;

	@Override
	public void create() {
		super.create();

		// Creating UI:
		Skin ui = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager().getSkin();
		window.getTitleLabel().setText("Loading...");

		loadingLabel = new Label("",ui);
		setLoadingLabel();
		window.add(loadingLabel).row();

		connectingLabel = new Label("Connecting...",ui);
		connectingLabel.addAction(Actions.alpha(0));
		window.add(connectingLabel);

		window.pack();
		window.setWidth(212);

		// Control variables:
		loaded = false;
	}

	@Override public void update(Object packet) {}

	@Override
	public void show() {
		InterfaceManager.centerWindow(window);
		super.show();
	}

	@Override
	public void render(float delta) {
		super.render(delta);

		if(!loaded) {
			setLoadingLabel();

			// If all assets are loaded:
			if(((Core)Gdx.app.getApplicationListener()).getInterfaceManager().updateAssetManager()) {
				loaded = true;
				setLoadingLabel();
				connectingLabel.addAction(Actions.fadeIn(0.3f));

				assignLoadedAssets();
				connectWithServer();
			}
		}
	}

	/**
	 * Sets the loading label's text according to the asset manager's progress.
	 */
	private void setLoadingLabel() {
		loadingLabel.setText("Loaded "+((Core)Gdx.app.getApplicationListener()).getInterfaceManager().getManagerProgress()+"% of assets.");
	}

	/**
	 * Schedules creation of screens, splitting spritesheets etc.
	 */
	private void assignLoadedAssets() {
		((Core)Gdx.app.getApplicationListener()).getNetworkManager()
			.executeOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				((Core)Gdx.app.getApplicationListener()).getInterfaceManager().createScreens();
			}
		});
	}

	private void connectWithServer() {
		new Dialog("Choose your server",((Core)Gdx.app.getApplicationListener())
			.getInterfaceManager().getSkin(),"dialog") {
			private final TextField serverAddress;

			{
				text("Enter server's address:");
				getContentTable().row();
				getContentTable().add(serverAddress =
					new TextField("127.0.0.1", ((Core)Gdx.app
					.getApplicationListener()).getInterfaceManager().getSkin()));

				button("Connect with server","y");
				getButtonTable().row();
				button("Use default address","n");
			}

			protected void result(Object object) {
				if(object.equals("y")) {
					NetworkManager.setServerAddress(serverAddress.getText());
				}
				else if(object.equals("n")) {
					NetworkManager.setServerAddress(NetworkManager.DEFAULT_IP);
				}

				// Scheduling actual connection with the server:
				((Core)Gdx.app.getApplicationListener()).getNetworkManager()
					.executeOnNetworkThread(new Runnable() {
					@Override
					public void run() {
						((Core)Gdx.app.getApplicationListener()).getNetworkManager().connect();
					}
				});
			};
		}.show(stage);


	}
}
