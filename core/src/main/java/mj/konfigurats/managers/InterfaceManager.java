package mj.konfigurats.managers;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import mj.konfigurats.game.entities.Player.PlayerAnimationType;
import mj.konfigurats.game.entities.Player.PlayerClass;
import mj.konfigurats.game.entities.SFX.SFXType;
import mj.konfigurats.game.utilities.Maps;
import mj.konfigurats.game.utilities.PlayerUtils;
import mj.konfigurats.game.utilities.SpellUtils;
import mj.konfigurats.gui.GameScreen;
import mj.konfigurats.gui.GameSettings;
import mj.konfigurats.gui.MusicUtilities;
import mj.konfigurats.gui.SoundUtilities;
import mj.konfigurats.gui.screens.GamesScreen;
import mj.konfigurats.gui.screens.LoadingScreen;
import mj.konfigurats.gui.screens.LobbyScreen;
import mj.konfigurats.gui.screens.MenuScreen;
import mj.konfigurats.network.ConnectionPackets.ConnectionPacket;
import mj.konfigurats.network.GamePackets.GamePacket;
import mj.konfigurats.network.LobbyPackets.LobbyPacket;

/**
 * Manages assets and screens. Contains informations and resources for the graphical interface.
 * @author MJ
 */
public class InterfaceManager {
	// Managers:
	private final AssetManager assetManager;

	// Screens:
	private GameScreen currentScreen;
	private LoadingScreen loadingScreen;
	private MenuScreen menuScreen;
	private LobbyScreen lobbyScreen;
	private GamesScreen gamesScreen;

	// UI elements:
	private final Skin userInterface;
	private final SpriteBatch stageBatch;
	public final static float DIALOG_PAD_TOP = 48,GAME_BGS_AMOUNT = 4;
	private Dialog connectingDialog,settingsDialog;

	public InterfaceManager() {
		stageBatch = new SpriteBatch();
		assetManager = new AssetManager();
		assetManager.setLoader(TiledMap.class, new TmxMapLoader());

		// Setting up GUI skin:
		loadCrucialAssets();
		userInterface = new Skin(Gdx.files.internal("global/images/ui.json"),
			assetManager.get("global/images/ui.pack",TextureAtlas.class));

		// Preparing platform specific interface changes:
		if(Gdx.app.getType() == ApplicationType.Android) {
			// Full mouse control being the only available mode:
			GameSettings.MOUSE_CONTROL = true;
		}
	}

	/**
	 * Resizes the current screen.
	 * @param width new screen width.
	 * @param height new screen height.
	 */
	public void resize(int width,int height) {
		if(currentScreen != null) {
			currentScreen.resize(width, height);
		}
	}

	/**
	 * Renders the screen.
	 * @param delta time that passed since the last rendering.
	 */
	public void render(float delta) {
		if(currentScreen != null) {
			currentScreen.render(delta);
		}
	}

	/**
	 * Destroys all objects that need disposing.
	 */
	public void dispose() {
		// Screens disposing:
		if(loadingScreen != null) {
			loadingScreen.dispose();
		}
		if(menuScreen != null) {
			menuScreen.dispose();
		}
		if(lobbyScreen != null) {
			lobbyScreen.dispose();
		}
		if(gamesScreen != null) {
			gamesScreen.dispose();
		}

		// Assets disposing:
		if(assetManager != null) {
			assetManager.dispose();
		}
		if(userInterface != null) {
			userInterface.dispose();
		}
		SoundUtilities.dispose();

		// LibGDX heavy objects disposing:
		if(stageBatch != null) {
			stageBatch.dispose();
		}
	}

	/**
	 * Updates adequate screen's logic according to a received packet.
	 * @param packet network packet.
	 */
	public void update(Object packet) {
		if(packet instanceof ConnectionPacket) {
			menuScreen.update(packet);
		}
		else if(packet instanceof LobbyPacket) {
			lobbyScreen.update(packet);
		}
		else if(packet instanceof GamePacket) {
			gamesScreen.update(packet);
		}
	}

	// Asset manager methods:
	/**
	 * Loads the most important assets, needed to show the starting screen.
	 */
	private final void loadCrucialAssets() {
		// User interface:
		assetManager.load("global/images/ui.pack",TextureAtlas.class);
		// Background:
		assetManager.load("global/images/bg-interface.png",Texture.class);
		// Interface sounds:
		assetManager.load("global/sounds/mouse-press.ogg",Sound.class);
		assetManager.load("global/sounds/mouse-release.ogg",Sound.class);
		// Making sure the assets will be loaded before continuing:
		assetManager.finishLoading();
	}

	/**
	 * Loads most of the game assets.
	 */
	public void loadAssets() {
		// Loading maps:
		Maps.loadMaps(assetManager);

		// Loading font:
		assetManager.load("global/fonts/courier.fnt",BitmapFont.class);

		// Loading sprites:
		for(PlayerClass playerClass : PlayerClass.values()) {
			for(int i=0; i<PlayerAnimationType.values().length; i++) {
				assetManager.load("game/sprites/"+playerClass.getIndex()+"-"+i+".png",Texture.class);
				// Elite sprite:
				if(!playerClass.isSummon()) {
					assetManager.load("game/sprites/"+playerClass.getEliteIndex()+"-"+i+".png",Texture.class);
				}
			}
		}

		// Loading spell images:
		assetManager.load("game/spells/projectiles.png",Texture.class);
		for(SFXType sfx : SFXType.values()) {
			assetManager.load("game/spells/"+sfx.getSFXName()
				+sfx.getSFXIndex()+".png",Texture.class);
		}

		// Loading game interface images:
		assetManager.load("global/images/logo.png",Texture.class);
		assetManager.load("game/interface/spell-select.png",Texture.class);
		assetManager.load("game/interface/avatars.pack",TextureAtlas.class);
		assetManager.load("game/interface/healthbar.pack",TextureAtlas.class);
		assetManager.load("game/interface/spell-icons-small.pack",TextureAtlas.class);
		assetManager.load("game/interface/spell-icons-medium.pack",TextureAtlas.class);
		assetManager.load("game/interface/spell-icons-big.pack",TextureAtlas.class);

		// Loading game backgrounds:
		for(int i=0; i < GAME_BGS_AMOUNT; i++) {
			assetManager.load("game/interface/bg-"+i+".png",Texture.class);
		}

		// Loading game sounds:
		for(int i=0; i < SoundUtilities.GAME_SOUNDS_AMOUNT; i++) {
			assetManager.load("game/sounds/"+i+".mp3",Sound.class);
		}

		// Loading music:
		assetManager.load("global/sounds/menu.mp3",Music.class);
		for(int i=0; i < MusicUtilities.BATTLE_THEMES_AMOUNT; i++) {
			assetManager.load("game/music/battle"+i+".mp3",Music.class);
		}
	}

	/**
	 * Updates the asset manager.
	 * @return true if the asset manager loaded all resources.
	 */
	public boolean updateAssetManager() {
		return assetManager.update();
	}

	/**
	 * @return manager loading progress (in %).
	 */
	public int getManagerProgress() {
		return (int)(assetManager.getProgress()*100);
	}

	/**
	 * Method used to access manager's resources.
	 * @param path path to the resource.
	 * @param type class of the resource.
	 * @return an object of the chosen class created  with the asset located at the given path.
	 */
	public <T> T getAsset(String path,Class<T> type) {
		return assetManager.get(path,type);
	}

	// Interface methods:
	/**
	 * Creates screen objects and sets up the loading screen.
	 */
	public void initiateScreens() {
		loadingScreen = new LoadingScreen();
		menuScreen = new MenuScreen();
		lobbyScreen = new LobbyScreen();
		gamesScreen = new GamesScreen();

		// Assigning sounds:
		SoundUtilities.prepareInterfaceSounds();

		// Showing the first screen:
		loadingScreen.create();
		showScreen(Screens.LOADING);
	}

	/**
	 * Creates all screens and prepares loaded game's assets.
	 * Should be called after asset manager loads all assets.
	 */
	public void createScreens() {
		// Adding game images that are parts of the UI:
		userInterface.addRegions(assetManager.get("game/interface/avatars.pack",
			TextureAtlas.class));
		for(String iconSize : new String[] {"small", "medium", "big"}) {
			userInterface.addRegions(assetManager.get("game/interface/spell-icons-"
				+iconSize+".pack",TextureAtlas.class));
		}
		userInterface.addRegions(assetManager.get("game/interface/healthbar.pack",
			TextureAtlas.class));

		// Assigning maps:
		Maps.createMaps();

		// Assigning sprites:
		SpellUtils.createProjectileAnimations();
		PlayerUtils.createPlayerAnimations();

		// Assigning sounds:
		SoundUtilities.prepareGameSounds();
		MusicUtilities.prepareMusic();

		// Creating screens:
		menuScreen.create();
		lobbyScreen.create();
		gamesScreen.create();
	}

	/**
	 * @return currently displayed screen.
	 */
	public GameScreen getCurrentScreen() {
		return currentScreen;
	}

	/**
	 * @return current skin which contains GUI elements.
	 */
	public Skin getSkin() {
		return userInterface;
	}

	/**
	 * @return current sprite batch for stages. There should be only one such object, so make sure to use this one.
	 */
	public Batch getStageBatch() {
		return stageBatch;
	}

	/**
	 * Shows the selected screen.
	 * @param screen will be shown. EXIT closes the application.
	 */
	public void showScreen(Screens screen) {
		switch(screen) {
			case LOADING:
				setScreen(loadingScreen);
				break;
			case MENU:
				MusicUtilities.startMenuMusic();
				setScreen(menuScreen);
				break;
			case LOBBY:
				MusicUtilities.startMenuMusic();
				setScreen(lobbyScreen);
				break;
			case GAMES:
				// Games screen doesn't have scrolled background, so no need for setScreen.
				currentScreen = gamesScreen;
				MusicUtilities.startGameMusic();
				break;
			case EXIT: {
				currentScreen = null;
				Gdx.app.exit();
				return;
			}
		}

		if(currentScreen != null) {
			currentScreen.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
			currentScreen.show();
		}
	}

	/**
	 * Sets a new current screen. A helper function for showScreen.
	 * @param screen new current screen.
	 */
	private final void setScreen(GameScreen screen) {
		// Setting background offset got from the previous screen:
		if(currentScreen != null) {
			screen.setBackgroundOffset(currentScreen.getBackgroundOffset());
		}
		// Setting the new screen as current:
		currentScreen = screen;
	}

	/**
	 * Displays an error in a separate dialog window.
	 * @param messages message lines. Start with "#" or "$" to highlight.
	 */
	public void displayError(String... messages) {
		displayDialog("Error",messages);
	}

	/**
	 * Displays a message in a separate dialog window.
	 * @param title dialog's title.
	 * @param messages message lines. Start with "#" or "$" to highlight.
	 */
	public void displayDialog(String title,final String... messages) {
		if(currentScreen != null)
		{
			new Dialog(title,userInterface,"dialog") {
				{
					// Default dialog padding:
					padTop(DIALOG_PAD_TOP);

					// Adding each message line as a label:
					for(String message : messages) {
						// Highlighted line:
						if(message.startsWith("#")) {
							text(new Label(message.substring(1),userInterface,"title"));
						}
						// Highlighted line:
						else if(message.startsWith("$")) {
							text(new Label(message.substring(1),userInterface,"dark"));
						}
						// Regular line:
						else {
							text(message);
						}
						getContentTable().row();
					}
					button("Back");

					// If the message is unusually long, create a scroll pane:
					if(messages.length >= 5) {
						// Clearing dialog table:
						clearChildren();
						// Adding content table in a scroll pane:
						add(new ScrollPane(getContentTable(),userInterface) {
							{
								setOverscroll(false, false);
								setFadeScrollBars(false);
							}
						}).height(128).width(324).row();
						// Adding button table:
						add(getButtonTable());
					}
				}
			}.show(currentScreen.getStage());
		}
	}

	/**
	 * Displays a dialog that lets the user to answer a simple yes-no question.
	 * @param title dialog's title
	 * @param yesAction action run after the "yes" answer.
	 * @param noAction action run after the "no" answer.
	 * @param messages dialog's message lines. Start with "#" or "$" to highlight.
	 */
	public void displayDialog(String title,final Runnable yesAction,
		final Runnable noAction,final String... messages) {
		if(currentScreen != null) {
			new Dialog(title,userInterface,"dialog-alt") {
				private final Runnable yes,no;

				{
					// Default dialog padding:
					padTop(DIALOG_PAD_TOP);

					// Setting actions:
					yes = yesAction;
					no = noAction;

					// Displaying messages:
					for(String message : messages) {
						if(message.startsWith("#")) {
							text(new Label(message.substring(1),userInterface,"title"));
						}
						else if(message.startsWith("$")) {
							text(new Label(message.substring(1),userInterface,"bright"));
						}
						else {
							text(message);
						}
						getContentTable().row();
					}
					button("No","n");
					button("Yes","y");
				}

				@Override
				public Dialog button(String text,final Object object) {
					// Modified button adding function to unify the buttons width:
					getButtonTable().add(new TextButton(text,userInterface) {
						{
							setObject(this, object);
						}
					}).width(64).pad(4);
					return this;
				}

				@Override
				protected void result(Object object) {
					// Running "yes" option:
					if(((String)object).equals("y")) {
						if(yes != null) {
							yes.run();
						}
					} // Running "no" option:
					else {
						if(no != null) {
							no.run();
						}
					}
				};
			}.show(currentScreen.getStage());
		}
	}

	/**
	 * Used to show and hide settings dialog on the current window.
	 * @param show true to show, false to hide.
	 * False immediately removes the dialog, without fading out - use only when necessary.
	 */
	public void displaySettings(boolean show) {
		if(show) {
			if(currentScreen != null) {
				settingsDialog = new Dialog("Settings",userInterface,"dialog-alt") {
					{
						padTop(DIALOG_PAD_TOP);

						// Adding titles:
						text(new Label("Sound preferences:",userInterface,"dark"));
						text(new Label("Game preferences:",userInterface,"dark"));
						getContentTable().row();

						// Sound preferences:
						getContentTable().add(new Table() {
							{
								// Sounds:
								add(new Label("Sounds:",userInterface,"title")).space(2).row();
								add(new Slider(0f,1f,0.01f,false,userInterface) {
									{
										setValue(GameSettings.SOUNDS_VOLUME);

										// Changing the user's sounds volume setting:
										addListener(new ClickListener() {
											@Override
											public void touchDragged(InputEvent event, float x,
													float y, int pointer) {
												GameSettings.SOUNDS_VOLUME = getValue();
											}
										});
									}
								}).row();
								add(new CheckBox("Sound ON", userInterface) {
									{
										setChecked(GameSettings.SOUNDS_ON);

										// Changing user's sounds setting:
										addListener(new ClickListener() {
											@Override
											public void clicked(InputEvent event,float x, float y) {
												GameSettings.SOUNDS_ON = isChecked();
											}
										});
									}
								}).row();

								// Music:
								add(new Label("Music:",userInterface,"title")).space(2).row();
								add(new Slider(0f,1f,0.01f,false,userInterface) {
									{
										setValue(GameSettings.MUSIC_VOLUME);

										// Changing the user's music volume setting:
										addListener(new ClickListener() {
											@Override
											public void touchDragged(InputEvent event, float x,
													float y, int pointer) {
												GameSettings.MUSIC_VOLUME = getValue();
												MusicUtilities.validateMusicVolume();
											}
										});
									}
								}).row();
								add(new CheckBox("Music ON", userInterface) {
									{
										setChecked(GameSettings.MUSIC_ON);

										// Changing user's sounds setting:
										addListener(new ClickListener() {
											@Override
											public void clicked(InputEvent event,float x, float y) {
												GameSettings.MUSIC_ON = isChecked();
												MusicUtilities.validateMusic();
											}
										});
									}
								}).row();
							}
						}).pad(4).top();

						// Game preferences:
						getContentTable().add(new ScrollPane(new Table() {
							{
								// Total mouse control:
								if(Gdx.app.getType() == ApplicationType.Desktop) {
									add(new CheckBox("Fullscreen", userInterface) {
										{
											setChecked(GameSettings.FULLSCREEN);

											// Switching to full/regular screen:
											addListener(new ClickListener() {
												@Override
												public void clicked(InputEvent event,float x, float y) {
													GameSettings.switchToFullScreen(isChecked());
													centerWindow(settingsDialog);
												}
											});
										}
									}).left().space(2).row();

									add(new CheckBox("Mouse control",userInterface) {
											{
												setChecked(GameSettings.MOUSE_CONTROL);

												// Changing the current controls:
												addListener(new ClickListener() {
													@Override
													public void clicked(InputEvent event,float x, float y) {
														GameSettings.MOUSE_CONTROL = isChecked();
													}
												});
											}
										}).left().space(2).row();
									add(new Label("Turn on to play\nentirely with\nmouse.",
										userInterface,"title")).left().space(2).row();
								}

								add(new CheckBox("V-Sync", userInterface) {
									{
										setChecked(GameSettings.VSYNC);

										// Switching to full/regular screen:
										addListener(new ClickListener() {
											@Override
											public void clicked(InputEvent event,float x, float y) {
												GameSettings.VSYNC = isChecked();
												Gdx.graphics.setVSync(GameSettings.VSYNC);
											}
										});
									}
								}).left().space(2).row();
								add(new Label("Turn off to increase\nperformance.",
									userInterface,"title")).left().space(2).row();

								// Hiding game management window's chat:
								add(new CheckBox("Chat in character\ncreation",userInterface) {
										{
											setChecked(GameSettings.CHAT_IN_GAME_MANAGEMENT);
											addListener(new ClickListener() {
												@Override
												public void clicked(InputEvent event,float x, float y) {
													// Showing or hiding game management chat:
													gamesScreen.showGameManagementChat(isChecked());
												}
											});
										}
									}).left().space(2).row();
								add(new Label(Gdx.app.getType() == ApplicationType.Desktop ?
									"On Android, chat\nmight make the\ncharacter "
									+"management\nwindow too big." : "Turn off if the\n"
									+"character management\nwindow is too big.",
									userInterface,"title")).left().space(2).row();

								// Hiding game interface:
								add(new CheckBox("Show full game\ninterface",userInterface) {
										{
											setChecked(GameSettings.SHOW_GAME_INTERFACE);
											addListener(new ClickListener() {
												@Override
												public void clicked(InputEvent event,float x, float y) {
													// Showing or hiding full game interface:
													gamesScreen.showFullGameInterface(isChecked());
												}
											});
										}
									}).left().space(2).row();
								add(new Label("When turned off,\nthe game interface\nwill have "
									+ "only\nthe health bar\nand spell icons.",
									userInterface,"title")).left().space(2).row();
							}
						},userInterface) {
							{
								setOverscroll(false, false);
							}
						}).height(128).width(190).pad(4);

						button("Back");
					}
				}.show(currentScreen.getStage());
			}
		}
		else {
			if(settingsDialog != null && settingsDialog.isVisible()) {
				settingsDialog.clearActions();
				settingsDialog.remove();
			}
		}
	}

	/**
	 * @return a new dialog with a message about connecting to the server. May return null if the current screen is empty.
	 */
	public void displayConnectingDialog(boolean show) {
		if(show) {
			if(currentScreen != null) {
				if(!(connectingDialog != null && connectingDialog.isVisible())) {
					connectingDialog = new Dialog("Connecting...",userInterface,"dialog") {
						{
							padTop(DIALOG_PAD_TOP);
							text("Please wait.");
						}
					}.show(currentScreen.getStage());
				}
			}
		}
		else {
			if(connectingDialog != null && connectingDialog.isVisible()) {
				connectingDialog.clearActions();
				connectingDialog.hide();
			}
		}
	}

	/**
	 * Immediately removes the connecting dialog from the stage.
	 */
	public void removeConnectingDialog() {
		if(connectingDialog != null) {
			connectingDialog.clearActions();
			connectingDialog.remove();
		}
	}

	/**
	 * Triggered by the network thread when the client loses connection.
	 * Displays a dialog to let the player know that he's being reconnected to the server
	 * or hides the reconnecting dialog when the player manages to connect to the server.
	 * @param show true to display, false to hide.
	 */
	public void displayReconnectingDialog(boolean show) {
		if(show) {
			if(currentScreen != null) {
				// Clearing previous connecting dialog:
				if(connectingDialog != null && connectingDialog.isVisible()) {
					removeConnectingDialog();
				}
				// Showing reconnecting dialog:
				connectingDialog = new Dialog("Connecting...",userInterface,"dialog-alt") {
					{
						padTop(DIALOG_PAD_TOP);
						text("Attempting to reconnect.");
						getContentTable().row();
						text("Please wait.");
					}
				}.show(currentScreen.getStage());
			}
		}
		else {
			if(connectingDialog != null && connectingDialog.isVisible()) {
				connectingDialog.clearActions();
				connectingDialog.hide();
			}
		}
	}

	/**
	 * Centers window according to the current screen size.
	 * @param window will be centered.
	 */
	public static void centerWindow(Window window) {
		window.setPosition((int)(Gdx.graphics.getWidth()/2-window.getWidth()/2),
			(int)(Gdx.graphics.getHeight()/2-window.getHeight()/2));
	}

	/**
	 * Contains names of all game's screens.
	 * @author MJ
	 */
	public static enum Screens {
		LOADING,MENU,LOBBY,GAMES,EXIT
	}

	// Game methods:

	/**
	 * Initiates a new game.
	 * @param gameName will be set as the current game name.
	 */
	public void initiateGame(String gameName,int gameIndex,int mapIndex) {
		gamesScreen.initiateGame(gameName,gameIndex,mapIndex);
	}

	/**
	 * @return current game name.
	 */
	public String getGameName() {
		return gamesScreen.getGameName();
	}

	/**
	 * @return current battle manager.
	 */
	public BattleManager getBattleManager() {
		return gamesScreen.getBattleManager();
	}
}
