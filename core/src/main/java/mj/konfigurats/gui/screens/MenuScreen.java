package mj.konfigurats.gui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import mj.konfigurats.Core;
import mj.konfigurats.gui.AbstractInterfaceScreen;
import mj.konfigurats.managers.InterfaceManager.Screens;
import mj.konfigurats.network.ConnectionPackets.*;

public class MenuScreen extends AbstractInterfaceScreen {
	// UI elements:
	private TextField username,password;

	@Override
	public void create() {
		super.create();

		// Creating UI:
		Skin ui = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager().getSkin();

		window.getTitleLabel().setText("Main menu");

		// Game logo:
		window.add(new Image(((Core)Gdx.app.getApplicationListener())
			.getInterfaceManager().getAsset("global/images/logo.png",
			Texture.class))).colspan(2).row();
		// Username text field:
		window.add(username = new TextField("Username",ui,"limited") {
			{
				setMaxLength(10);
				setFocusTraversal(true);
				setOnlyFontChars(true);

				addListener(new InputListener() {
					@Override
					public boolean keyUp(InputEvent event,int keycode) {
						if(keycode == Keys.ENTER) {
							next(true);
							return true;
						}
						else return false;
					}
				});
			}
		}).colspan(2).width(128).pad(4).row();
		// Password text field:
		window.add(password = new TextField("Password",ui,"limited") {
			{
				setPasswordCharacter('-');
				setPasswordMode(true);
				setMaxLength(20);
				setFocusTraversal(true);
				setOnlyFontChars(true);

				addListener(new InputListener() {
					@Override
					public boolean keyUp(InputEvent event,int keycode) {
						if(keycode==Keys.ENTER) {
							logIn(false);
							return true;
						}
						else return false;
					}
				});
			}
		}).colspan(2).width(128).pad(4).row();

		// Show password checkbox:
		window.add(new CheckBox("Show password",ui) {
			{
				setChecked(false);

				addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						password.setPasswordMode(!isChecked());
					}
				});
			}
		}).colspan(2).pad(2).row();

		// Logging button:
		window.add(new TextButton("Log in",ui) {
			{
				addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						logIn(false);
					}
				});
			}
		}).width(80).right().pad(4);
		// Registration button:
		window.add(new TextButton("Register",ui) {
			{
				addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						logIn(true);
					}
				});
			}
		}).width(80).left().pad(4).row();
		// Settings button:
		window.add(new TextButton("Settings",ui) {
			{
				addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
							.displaySettings(true);
					}
				});
			}
		}).width(80).right().pad(4);
		// Credits button:
		window.add(new TextButton("Credits",ui) {
			{
				addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						// Showing a simple credits dialog:
						((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
							.displayDialog("Credits", "#Programming by MJ.",
							"User interface (poorly) made by MJ.",
							"Most of other assets from:", "#OpenGameArt.org.",
							"Music by Alexandr Zhelanov.","#Graphics from the FLARE project.",
							"(Mainly made by Clint Bellanger.)","Spell icons by J. W. Bjerk.",
							"Sounds by: Michel Baradari,","Paolo D'Emilio and Iwan Gabovitch.",
							"Most backgrounds by Reiner Prokein.",
							"",
							"Have patience - it's my first game!",
							"$Thank you for playing.");
					}
				});
			}
		}).width(80).left().pad(4).row();
		// Exit button:
		window.add(new TextButton("Exit",ui) {
			{
				addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						hide(Screens.EXIT);
					}
				});
			}
		}).colspan(2).width(80).pad(4);

		window.pack();
	}

	@Override
	public void show() {
		super.show();

		// Resetting textfields:
		username.setCursorPosition(username.getText().length());
		password.setCursorPosition(password.getText().length());
		password.selectAll();
		// Focusing username text field:
		stage.setKeyboardFocus(username);
	}

	@Override
	public void update(Object packet) {
		if(packet instanceof ConnectionPacket) {
			// Packets sent from the menu screen usually show the connecting
			// dialog, so any response lets the client hide the dialog:
			((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
				.displayConnectingDialog(false);

			if(packet instanceof SrvLogged) {
				// Logged in successfully:
				((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
					.removeConnectingDialog();
				((Core)Gdx.app.getApplicationListener()).getNetworkManager()
					.setUsername(((SrvLogged)packet).username);
				hide(Screens.LOBBY);
			}
			else if(packet instanceof SrvRegistered) {
				// Registered successfully - displaying logging prompt:
				((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
					.displayDialog("Success", new Runnable() {
						@Override
						public void run() {
							logIn(false);
						}
					}, null, "Account successfully registered.","Would you like to log in now?");
			}
			else if(packet instanceof SrvAlreadyLogged) {
				// Account already logged in.
				((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
					.displayError("Account already logged.","If it is yours, please","contact support.");
			}
			else if(packet instanceof SrvUsernameInvalid) {
				// Username doesn't exist.
				((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
					.displayError("Username unknown.");
			}
			else if(packet instanceof SrvPasswordInvalid) {
				// Wrong password.
				((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
					.displayError("Password invalid.");
			}
			else if(packet instanceof SrvUsernameTaken) {
				// Username already used.
				((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
					.displayError("Username already taken.");
			}
			else if(packet instanceof SrvCorruptedData) {
				// Registration unsuccessful.
				((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
					.displayError("Data corrupted.","Please try to log in again.");
			}
		}
	}

	/**
	 * Takes input from the text fields and sends it to the server to log user in or register a new account.
	 * @param register false to log in, true to register.
	 */
	private void logIn(boolean register) {
		// At least 2 characters in nickname:
		if(username.getText().length()>1) {
			// No spaces (the only disallowed character from the limited font):
			if(!username.getText().contains(" ")) {
				// Doesn't consist of dashes:
				if(!username.getText().matches("-+")) {
					// At least 8 characters in password:
					if(password.getText().length()>7) {
						// No spaces in password:
						if(!password.getText().contains(" ")) {
							// Data valid.
							ConnectionPacket packet;
							if(register) {
								// Registering a new user:
								packet = new CltRegister();
								((CltRegister)packet).username = username.getText();
								((CltRegister)packet).password = password.getText();
							}
							else {
								// Logging in:
								packet = new CltLogin();
								((CltLogin)packet).username = username.getText();
								((CltLogin)packet).password = password.getText();
							}
							// Displaying connecting dialog and sending the packet:
							((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
								.displayConnectingDialog(true);
							((Core)Gdx.app.getApplicationListener()).getNetworkManager()
								.sendTCP(packet);
						}
						else ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
							.displayError("Password contains space(s).");
					}
					else ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
						.displayError("Password too short.");
				}
				else((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
					.displayError("Username should contain","at least one digit","or character.");
			}
			else((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
				.displayError("Username contains space(s).");
		}
		else ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.displayError("Username too short.");
	}
}
