package mj.konfigurats.gui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import mj.konfigurats.Core;
import mj.konfigurats.game.utilities.Maps.MapInfo;
import mj.konfigurats.gui.AbstractInterfaceScreen;
import mj.konfigurats.gui.elements.Chat;
import mj.konfigurats.gui.elements.Chat.Type;
import mj.konfigurats.managers.InterfaceManager;
import mj.konfigurats.managers.InterfaceManager.Screens;
import mj.konfigurats.network.LobbyPackets.*;

import java.util.Comparator;

public class LobbyScreen extends AbstractInterfaceScreen {
	// UI elements:
	private Skin userInterface;
	private Table activeGames;
	private Chat chat;
	// Control variables:
	private Array<PctGameInfo> activeGamesList;

	@Override
	public void create() {
		super.create();

		// Creating UI elements:
		userInterface = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager().getSkin();

		window.getTitleLabel().setText("Lobby");

		// Chat:
		window.add(chat = new Chat(Type.LOBBY)).expand().fill().pad(4);

		// A scroll pane which currently active games list:
		window.add(new Table() {
			{
				add(new Label("Active games:",userInterface)).space(2).row();
				add(new ScrollPane(activeGames = new Table() {
					{
						pad(2);
						align(Align.top+Align.left);
					}
				},userInterface) {
					{
						setOverscroll(false, false);
					}
				}).expand().fill();
			}
		}).width(128).pad(4).expandY().fillY().row();

		// General controls:
		window.add(new Table() {
			{
				// Allows to create a new game:
				add(new TextButton("Create",userInterface) {
					{
						addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x, float y) {
								createNewGame();
							}
						});
					}
				}).pad(4).width(80);

				// Allows to join a specific game or a player:
				add(new TextButton("Join",userInterface) {
					{
						addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x, float y) {
								joinGame();
							}
						});
					}
				}).pad(4).width(80);

				// Puts the player in a random game room:
				add(new TextButton("Play",userInterface) {
					{
						addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x, float y) {
								((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
									.displayConnectingDialog(true);
								((Core)Gdx.app.getApplicationListener()).getNetworkManager()
									.sendTCP(new CltJoinRandomGame());
							}
						});
					}
				}).pad(4).width(80);

				row();

				// Logs out:
				add(new TextButton("Log out",userInterface) {
					{
						addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x, float y) {
								((Core)Gdx.app.getApplicationListener()).getNetworkManager().
									sendTCP(new CltLobbyLogOut());
							}
						});
					}
				}).pad(4).width(80);

				// Shows settings:
				add(new TextButton("Settings",userInterface) {
					{
						addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x, float y) {
								// Showing settings dialog:
								((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
									.displaySettings(true);
							}
						});
					}
				}).pad(4).width(80);

				// Shows the ranking:
				add(new TextButton("Ranking",userInterface) {
					{
						addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x, float y) {
								((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
									.displayConnectingDialog(true);
								// Requesting ranking data:
								((Core)Gdx.app.getApplicationListener()).getNetworkManager()
									.sendTCP(new CltShowRanking());
							}
						});
					}
				}).pad(4).width(80);
			}
		}).colspan(2);

		// Creating lists of users and games:
		activeGamesList = new Array<PctGameInfo>();
	}

	@Override
	public void resize(int width, int height) {
		// Setting scroll panes sizes according to chat size:
		window.setSize(width-40, height-40);
		window.invalidateHierarchy();
		chat.setSize(chat.getWidth(),chat.getHeight());
		super.resize(width, height);
	}

	@Override
	public void show() {
		super.show();
		// Refreshing players and games lists to display them properly:
		refreshGamesList();
		chat.refresh();
		stage.setKeyboardFocus(chat.getChatTextField());
	}

	@Override
	public void hide(Screens screen) {
		// Clearing chat:
		chat.clearChat();
		// Clearing games list:
		activeGames.clearChildren();
		activeGamesList.clear();

		window.invalidateHierarchy();

		super.hide(screen);
	}

	@Override
	public void update(final Object packet) {
		// Chat and interface packets:
		if(packet instanceof SrvLobbyMessage) {
			// Got a new message: showing it on the chat:
			chat.addMessage(((SrvLobbyMessage)packet).message);
		}
		else if(packet instanceof SrvAddLobbyUser) {
			// Got a new username: adding it to the lobby users list:
			chat.addUser(((SrvAddLobbyUser)packet).username);
		}
		else if(packet instanceof SrvRemoveLobbyUser) {
			// A user left: removing him from the lobby users list:
			chat.removeUser(((SrvRemoveLobbyUser)packet).username);
		}
		else if(packet instanceof SrvUpdateGame) {
			// Information changed about a game room - updating:
			updateGameRoom((SrvUpdateGame)packet);
		}
		else if(packet instanceof SrvRemoveGame) {
			removeGameRoom(((SrvRemoveGame)packet).name);
		}
		else if(packet instanceof SrvLobbyLoggedOut) {
			// Player successfully logged out - going back to the menu and clearing player's username:
			((Core)Gdx.app.getApplicationListener()).getNetworkManager()
				.setUsername(null);
			hide(Screens.MENU);
		}
		// Game creation and management packets:
		else if(packet instanceof SrvStartGame) {
			// Player joined a game - showing game screen:
			((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
				.removeConnectingDialog();
			((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
				.initiateGame(((SrvStartGame)packet).name,((SrvStartGame)packet).roomIndex,
				((SrvStartGame)packet).mapIndex);
			hide(Screens.GAMES);
		}
		else if(packet instanceof SrvEnterRoomPassword) {
			((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
				.displayConnectingDialog(false);
			// Player tries to join a password-protected game room:
			new Dialog(((SrvEnterRoomPassword)packet).gameRoomName,userInterface,"dialog") {
				private final TextField passwordTextField;

				{
					padTop(InterfaceManager.DIALOG_PAD_TOP);

					text("The chosen room is");
					getContentTable().row();
					text("password protected.");
					getContentTable().row();
					text("Enter password to");
					getContentTable().row();
					text("join the room:");
					getContentTable().row();

					// Password textfield:
					getContentTable().add(passwordTextField = new TextField("",userInterface,"limited") {
						{
							setMaxLength(20);
							setOnlyFontChars(true);
						}
					}).row();

					button("Join");
				}

				@Override
				protected void result(Object object) {
					if(passwordTextField.getText().matches("\\s*")) {
						((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
							.displayError("Password cannot be empty.");
					}
					else {
						((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
							.displayConnectingDialog(true);
						// Ending packet with game room data:
						CltRoomPassword packet = new CltRoomPassword();
						packet.gameRoomName = this.getTitleLabel().getText().toString();
						packet.password = passwordTextField.getText();
						((Core)Gdx.app.getApplicationListener()).getNetworkManager()
							.sendTCP(packet);
					}
				}
			}.show(stage);
		}
		else if(packet instanceof SrvInvalidRoomPassword) {
			// Player enters wrong password for the game room:
			displayPacketError("Invalid game room password.");
		}
		else if(packet instanceof SrvGameAlreadyExists) {
			// Player tries to create a game with a taken name:
			displayPacketError("Your game room name is taken.");
		}
		else if(packet instanceof SrvGameFull) {
			// Player tries to enter a full room:
			displayPacketError("Selected game room is full.");
		}
		else if(packet instanceof SrvGameNotFound) {
			// Player tries to enter a room which doesn't exist:
			displayPacketError("Selected game room does not exist.");
		}
		else if(packet instanceof SrvPlayerNotFound) {
			// Player tries to join a player which isn't in a game room:
			displayPacketError("Selected player is not in a game room.");
		}
		else if(packet instanceof SrvNoGamesOpen) {
			// Player tries to join a random game, but there aren't any open games:
			displayPacketError("There are no open game rooms.","Try creating a new one.");
		}
		else if(packet instanceof SrvWrongMapIndex) {
			// Player tries to join a random game, but there aren't any open games:
			displayPacketError("Server received corrupted","map index. Try creating",
				"the room again. Sorry.");
		}
		else if(packet instanceof SrvRankingData) {
			((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
				.displayConnectingDialog(false);
			final SrvRankingData rankingData = (SrvRankingData)packet;
			new Dialog("Ranking",userInterface,"dialog") {
				private final Label[][] playerLabels;
				private final Label title;
				private RankingType rankingType;

				{
					padTop(InterfaceManager.DIALOG_PAD_TOP);

					// Showing player's score:
					getButtonTable().add(new Label("Your score: "+ rankingData.userKills
						+ "/" + rankingData.userDeaths,userInterface,"dark")).row();

					// Adding buttons capable of changing ranking type:
					getButtonTable().add(new Table() {
						{
							add(new TextButton("Best ratio", userInterface) {
								{
									addListener(new ClickListener() {
										@Override
										public void clicked(InputEvent event, float x, float y) {
											setRankingType(RankingType.RATIO);
										}
									});
								}
							}).width(96).pad(2);
							add(new TextButton("Most kills", userInterface) {
								{
									addListener(new ClickListener() {
										@Override
										public void clicked(InputEvent event, float x, float y) {
											setRankingType(RankingType.KILLS);
										}
									});
								}
							}).width(96).pad(2);
							add(new TextButton("Most deaths", userInterface) {
								{
									addListener(new ClickListener() {
										@Override
										public void clicked(InputEvent event, float x, float y) {
											setRankingType(RankingType.DEATHS);
										}
									});
								}
							}).width(96).pad(2);
						}
					}).row();

					getContentTable().add(title = new Label("Best ratio:",
						userInterface,"dark")).row();

					// Creating labels with players' data:
					playerLabels = new Label[2][rankingData.topRatio.length];
					getContentTable().add(new ScrollPane(new Table() {
						{
							int index = 0;
							for(String player : rankingData.topRatio) {
								String[] playerData = player.split(" ");
								playerLabels[0][index] = new Label(playerData[0],userInterface);
								playerLabels[1][index] = new Label(playerData[1],userInterface);
								add(playerLabels[0][index]).pad(2).expandX().left();
								add(playerLabels[1][index]).pad(2).right().row();
								index++;
							}
						}
					}, userInterface) {
						{
							setOverscroll(false, false);
						}
					}).expand().fill().height(96).row();

					getButtonTable().add(new Label("Ranking is refreshed every hour.",
						userInterface)).row();

					button("Back");
				}

				/**
				 * Changes the currently displayed ranking.
				 * @param rankingType type of the displayed ranking.
				 */
				private void setRankingType(final RankingType rankingType) {
					if(this.rankingType != rankingType) {
						this.rankingType = rankingType;

						getContentTable().addAction(Actions.sequence(Actions.fadeOut(0.2f),
						Actions.run(new Runnable() {
							@Override
							public void run() {
								String[] data;
								switch(rankingType) {
								case DEATHS:
									data = rankingData.topDeaths;
									title.setText("Most deaths:");
									break;
								case KILLS:
									data = rankingData.topKills;
									title.setText("Most kills:");
									break;
								case RATIO:
									data = rankingData.topRatio;
									title.setText("Best ratio:");
									break;
								default:
									data = null;
									break;
								}

								// Updating labels:
								int index = 0;
								for(String player : data) {
									String[] playerData = player.split(" ");
									playerLabels[0][index].setText(playerData[0]);
									playerLabels[1][index].setText(playerData[1]);
									index++;
								}
							}
						}),Actions.fadeIn(0.2f)));
					}
				}
			}.show(stage);
		}
	}

	private static enum RankingType {
		RATIO,KILLS,DEATHS;
	}

	/**
	 * Updates informations about a game room. Adds it to the list if it's new.
	 * @param packet informations about a game room.
	 */
	private void updateGameRoom(SrvUpdateGame packet) {
		// Checking if the game room is already on the list:
		for(PctGameInfo gameRoom : activeGamesList) {
			if(gameRoom.name.equals(packet.game.name)) {
				gameRoom.players = packet.game.players;
				refreshGamesList();
				return;
			}
		}

		// If the game room is new: adding it to the list:
		activeGamesList.add(packet.game);
		// Sorting games list:
		activeGamesList.sort(new Comparator<PctGameInfo>() {
			@Override
			public int compare(PctGameInfo game0, PctGameInfo game1) {
				return game0.name.compareTo(game1.name);
			}
		});
		refreshGamesList();
	}

	/**
	 * Deletes a game room from the list.
	 * @param name name of the room.
	 */
	private void removeGameRoom(String name) {
		for(PctGameInfo gameRoom : activeGamesList) {
			if(gameRoom.name.equals(name)) {
				activeGamesList.removeValue(gameRoom, true);
				refreshGamesList();
				return;
			}
		}
	}

	/**
	 * Sorts the games list and displays it on the games scroll pane.
	 */
	private void refreshGamesList() {
		// Clearing games list table:
		activeGames.clearChildren();

		for(final PctGameInfo game : activeGamesList) {
			if(game.limit != game.players) {
				// If the game is open:
				activeGames.add(new TextButton(game.name+"["+game.mode+"] "
					+game.players+"/"+game.limit,userInterface,"text") {
					{
						addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x,float y) {
								// Sends a packet with the game's name:
								((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
									.displayConnectingDialog(true);
								CltJoinGame packet = new CltJoinGame();
								packet.name = game.name;
								((Core)Gdx.app.getApplicationListener()).getNetworkManager()
									.sendTCP(packet);
							}
						});
					}
				}).left().row();
			}
			else {
				// If the game is full:
				activeGames.add(new Label(game.name+"["+game.mode+"] "+game.players+"/"+game.limit,userInterface,"bright")).left().row();
			}
		}
		activeGames.invalidateHierarchy();
	}

	/**
	 * Opens up a dialog which allows the player to create a new game.
	 */
	private void createNewGame() {
		new Dialog("Create new game",userInterface,"dialog-alt") {
			private Label gameModeInfo;
			private int chosenMap,gameMode;
			private final TextField nameTextField,passwordTextField;

			{
				// Default dialog padding:
				padTop(InterfaceManager.DIALOG_PAD_TOP);

				// Game room naming:
				text(new Label("Name your game room:",userInterface,"title"));
				getContentTable().row();
				getContentTable().add(nameTextField = new TextField("Arena",userInterface,"limited") {
					{
						setMaxLength(10);
						setOnlyFontChars(true);
					}
				}).row();

				// Game room password:
				text(new Label("Password (empty for none):",userInterface,"title"));
				getContentTable().row();
				getContentTable().add(passwordTextField = new TextField("",userInterface,"limited") {
					{
						setMaxLength(20);
						setOnlyFontChars(true);
					}
				}).row();

				// Map selection:
				text(new Label("Choose a map:",userInterface,"title"));
				getContentTable().row();
				getContentTable().add(new ScrollPane(new List<String>(userInterface) {
					{
						// Adding names of all maps:
						for(MapInfo mapInfo : MapInfo.values()) {
							getItems().add(mapInfo.toString());
						}
						setSelectedIndex(MapInfo.RANDOM.getIndex()+2);
						invalidateHierarchy();

						// Setting the clicked map as selected:
						addListener(new ClickListener() {
							public void clicked(InputEvent event, float x, float y) {
								chosenMap = getSelectedIndex()-2;
							};
						});
					}
				}, userInterface) {
					{
						setOverscroll(false,false);
						setFadeScrollBars(false);
					}
				}).height(96).width(200).row();

				// Game mode selection:
				text(new Label("Choose a game mode:",userInterface,"title"));
				getContentTable().row();
				getContentTable().add(new ScrollPane(new List<String>(userInterface) {
					{
						// Adding names of all maps:
						for(GameMode gameMode : GameMode.values()) {
							getItems().add(gameMode.toString());
						}
						setSelectedIndex(GameMode.STARNDARD.getIndex());
						invalidateHierarchy();

						// Setting the clicked map as selected:
						addListener(new ClickListener() {
							public void clicked(InputEvent event, float x, float y) {
								if(gameMode != getSelectedIndex()) {
									gameMode = getSelectedIndex();
									// Fading out game mode info, changing it and fading in:
									gameModeInfo.addAction(Actions.sequence(Actions.fadeOut(0.15f),
										Actions.run(new Runnable() {
										@Override
										public void run() {
											gameModeInfo.setText(GameMode.getGameMode
												(gameMode).getDescription());
										}
									}),Actions.fadeIn(0.15f)));
								}
							};
						});
					}
				}, userInterface) {
					{
						setOverscroll(false,false);
					}
				}).height(48).width(186).row();
				getContentTable().add(gameModeInfo = new Label(GameMode.STARNDARD.getDescription(),
					userInterface)).padLeft(32).padRight(32);

				// Buttons:
				button("Back","n");
				button("Create","y");

				// Default settings:
				chosenMap = -2;
				gameMode = 0;
			}

			// If the player clicked Create - send a packet.
			@Override
			protected void result(Object object) {
				if(((String)object).equals("y")) {
					((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
						.displayConnectingDialog(true);
					CltCreateGame packet = new CltCreateGame();
					packet.name = nameTextField.getText();
					packet.mapIndex = chosenMap;
					packet.gameMode = gameMode;
					// Setting room's password:
					if(!passwordTextField.getText().matches("\\s*")) {
						packet.password = passwordTextField.getText();
					}
					((Core)Gdx.app.getApplicationListener()).getNetworkManager().sendTCP(packet);
				}
			}
		}.show(stage);
	}

	/**
	 * Currently available game modes.
	 */
	public static enum GameMode {
		STARNDARD("Standard mode","Standard free-for-all.",0),
		TEAM("Team mode","Divides players into 2 teams.",1);

		private final String name,description;
		private final int index;
		private GameMode(String name, String description, int index) {
			this.index = index;
			this.name = name;
			this.description = description;
		}

		/**
		 * @return index of the game mode.
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @return game mode description.
		 */
		public String getDescription() {
			return description;
		}

		@Override
		public String toString() {
			return name;
		}

		/**
		 * Finds the game mode with the given index.
		 * @param index game mode's index.
		 * @return game mode connected with the index or null for invalid index.
		 */
		public static GameMode getGameMode(int index) {
			for(GameMode gameMode : GameMode.values()) {
				if(gameMode.getIndex() == index) {
					return gameMode;
				}
			}
			return null;
		}
	}

	/**
	 * Displays a dialog that allows the player to join a specific game or a user.
	 */
	private void joinGame() {
		new Dialog("Join a game",userInterface,"dialog") {
			private final TextField name;
			{
				padTop(InterfaceManager.DIALOG_PAD_TOP);

				text("Enter name of a game or a player:");
				getContentTable().row();
				getContentTable().add(name = new TextField("Name",userInterface,"limited") {
					{
						setMaxLength(20);
						setOnlyFontChars(true);
					}
				});
				button("Back","n");
				button("Join player","jp");
				button("Join game","jg");
			}

			@Override
			protected void result(Object object) {
				if(((String)object).equals("jp")) {
					// Player entered a nickname:
					((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
						.displayConnectingDialog(true);
					CltJoinPlayer packet = new CltJoinPlayer();
					packet.username = name.getText();
					((Core)Gdx.app.getApplicationListener()).getNetworkManager()
						.sendTCP(packet);
				}
				else if(((String)object).equals("jg")) {
					// Player entered a game's name:
					((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
						.displayConnectingDialog(true);
					CltJoinGame packet = new CltJoinGame();
					packet.name = name.getText();
					((Core)Gdx.app.getApplicationListener()).getNetworkManager()
						.sendTCP(packet);
				}
			};
		}.show(stage);
	}

	/**
	 * Hides connecting dialog and displays and error after receiving a server packet.
	 * @param errorMessage error's message.
	 */
	private void displayPacketError(String... errorMessage) {
		((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.displayConnectingDialog(false);
		((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.displayError(errorMessage);
	}
}
