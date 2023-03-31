package mj.konfigurats.gui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import mj.konfigurats.Core;
import mj.konfigurats.game.entities.Player.PlayerClass;
import mj.konfigurats.game.utilities.SpellUtils.SpellType;
import mj.konfigurats.game.utilities.SpellUtils.Spells;
import mj.konfigurats.gui.AbstractScreen;
import mj.konfigurats.gui.GameSettings;
import mj.konfigurats.gui.SoundUtilities;
import mj.konfigurats.gui.elements.Chat;
import mj.konfigurats.gui.elements.Chat.Type;
import mj.konfigurats.gui.elements.HealthBar;
import mj.konfigurats.managers.BattleManager;
import mj.konfigurats.managers.InterfaceManager;
import mj.konfigurats.managers.InterfaceManager.Screens;
import mj.konfigurats.network.GamePackets.*;

public class GamesScreen extends AbstractScreen {
	// UI elements:
	private Skin userInterface;
	private TiledDrawable background;
	// Game interface elements:
	private Table gameInterface,spellSelectionTable;
	private Label instructionLabel;
	private Chat interfaceChat;
	private HealthBar healthBar,summonHealthBar;
	private Image[] spellIcons,spellSelection;
	// Game management window elements:
	private Window gameManagement;
	private Chat managementChat;

	// Game management:
	private BattleManager battleManager;
	private String gameName;
	private SpellType selectedElement;

	@Override
	public void create() {
		// Assigning the default UI skin:
		userInterface = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager().getSkin();

		// Creating game interface, be shown during playing:
		createGameInterface();

		// Creating game management window, shown before playing:
		createGameManagementWindow();

		// Returns keyboard focus to the game interface if the player clicks outside the chat:
		stage.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				// Playing click sounds:
				if(gameManagement.isVisible()) {
					SoundUtilities.playMousePressSound();
					return true;
				}
				else if(gameInterface.isVisible()) {
					// Set focus on the game interface:
					if(stage.hit(x, y, true) != interfaceChat.getChatTextField()
						&& stage.hit(x, y, true) != interfaceChat.getSendingButton()) {
						stage.setKeyboardFocus(gameInterface);

						// Handle game click:
						if(GameSettings.MOUSE_CONTROL) {
							// Full mouse control:
							if(spellSelectionTable.getX() > x || spellSelectionTable.getRight() < x
								|| spellSelectionTable.getY() > y || spellSelectionTable.getTop() < y) {
								// Not clicking the spell interface:
								if(selectedElement == null) {
									// No spell selected - regular movement:
									battleManager.handleClick(x, stage.getHeight()-y);
								}
								else {
									// A spell selected - casting spell:
									spellSelection[selectedElement.getIndex()]
										.addAction(Actions.fadeOut(0.25f));
									battleManager.handleSpellCast(selectedElement);
									selectedElement = null;
								}
							}
						}
						else {
							// Regular movement:
							battleManager.handleClick(x, stage.getHeight()-y);
						}
					}
					return true;
				}
				return false;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y,
					int pointer, int button) {
				// Playing click sounds:
				if(gameManagement.isVisible()) {
					SoundUtilities.playMouseReleaseSound();
				}
			}
		});

		// Default settings:
		gameInterface.addAction(Actions.alpha(0));
		gameInterface.setVisible(false);
	}

	private final void createGameInterface() {
		// Creating game interface:
		stage.addActor(gameInterface = new Table() {
			{
				this.setFillParent(true);

				// Adding spell icons background:
				add(new Image(new NinePatch(userInterface.getRegion
					("icon-background"),3,3,3,3))).width(6+48*4).
					height(6+48).row();
				// Right interface - spell icons:
				spellIcons = new Image[SpellType.values().length];
				add(new Table() {
					{
						for(SpellType spellType : SpellType.values()) {
							add(spellIcons[spellType.getIndex()] =
								new Image());
						}
					}
				}).padTop(-(6+48)).row();
				// Spell selection icons:
				spellSelection = new Image[SpellType.values().length];
				add(spellSelectionTable = new Table() {
					{
						for(final SpellType spellType : SpellType.values()) {
							add(spellSelection[spellType.getIndex()] =
								new Image(((Core)Gdx.app.getApplicationListener())
								.getInterfaceManager().getAsset
								("game/interface/spell-select.png",Texture.class)) {
								{
									addAction(Actions.alpha(0));
									addListener(new ClickListener() {
										@Override
										public void clicked(InputEvent event,
											float x, float y) {
											if(GameSettings.MOUSE_CONTROL) {
												if(selectedElement != null) {
													spellSelection[selectedElement.getIndex()]
														.addAction(Actions.fadeOut(0.25f));
													if(selectedElement == spellType) {
														selectedElement = null;
														return;
													}
												}
												selectedElement = spellType;
												addAction(Actions.fadeIn(0.25f));
											}
										}
									});
								}
							});
						}
					}
				}).padTop(-(6+48)).row();

				// Top left label - game instructions:
				add(instructionLabel = new Label("MOUSE: move.\n1 - 4: cast.\nENTER: chat.\nESCAPE:quit.",
					userInterface,"bright")).expandX().left().top().padTop(-56).row();

				// Bottom middle interface - summon health bar:
				add(summonHealthBar = new HealthBar()).expandY().bottom().row();
				// Summon health bar will fade out if the summon is dead:
				summonHealthBar.setFadingOutOnZero(true);

				// Bottom middle interface - health bar:
				add(healthBar = new HealthBar()).bottom().pad(4).row();

				// Bottom - chat:
				add(interfaceChat = new Chat(Type.GAME)).height(128).expandX().fillX().bottom();

				// Keyboard listener (handles game's logic):
				addListener(new InputListener() {
					@Override
					public boolean keyUp(InputEvent event, int keycode) {
						switch(keycode) {
						case Keys.ESCAPE:
							// User hits escape - let him leave the game room:
							((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
								.displayDialog("Exit", new Runnable() {
								@Override
								public void run() {
									((Core)Gdx.app.getApplicationListener()).getNetworkManager()
										.sendTCP(new CltLeaveGame());
								}
							}, new Runnable() {
								@Override
								public void run() {
									Timer.schedule(new Task() {
										@Override
										public void run() {
											stage.setKeyboardFocus(gameInterface);
										}
									}, 0.5f);
								}
							}, "Do you really want to leave?");
							return true;
						case Keys.NUM_1:
							// User tries to cast fire spell:
							if(!GameSettings.MOUSE_CONTROL) {
								if(stage.getKeyboardFocus() == gameInterface) {
									battleManager.handleSpellCast(SpellType.FIRE);
								}
								return true;
							}
							return false;
						case Keys.NUM_2:
							// User tries to cast water spell:
							if(!GameSettings.MOUSE_CONTROL) {
								if(stage.getKeyboardFocus() == gameInterface) {
									battleManager.handleSpellCast(SpellType.WATER);
								}
								return true;
							}
							return false;
						case Keys.NUM_3:
							// User tries to cast earth spell:
							if(!GameSettings.MOUSE_CONTROL) {
								if(stage.getKeyboardFocus() == gameInterface) {
									battleManager.handleSpellCast(SpellType.EARTH);
								}
								return true;
							}
							return false;
						case Keys.NUM_4:
							// User tries to cast air spell:
							if(!GameSettings.MOUSE_CONTROL) {
								if(stage.getKeyboardFocus() == gameInterface) {
									battleManager.handleSpellCast(SpellType.AIR);
								}
								return true;
							}
							return false;
						case Keys.ENTER:
							// User tries to write a message - focusing chat text field:
							stage.setKeyboardFocus(interfaceChat.getChatTextField());
							return true;
						case Keys.F11:
							// Going full screen:
							GameSettings.switchToFullScreen(!GameSettings.FULLSCREEN);
							InterfaceManager.centerWindow(gameManagement);
							return true;
						default:
							return false;
						}
					}
				});
			}
		});
	}

	/**
	 * Creates the game management window that pretty much handles everything
	 * connected with the character creation.
	 */
	private final void createGameManagementWindow() {
		// Game management window:
		stage.addActor(gameManagement = new Window("Enter the arena",
			userInterface,"dialog") {
			// Character creation data:
			private PlayerClass currentClass;
			private Spells[] spells;
			private CheckBox eliteCharacter;

			// Characters icons table elements:
			private Table classesTable,charactersTable;
			private Image[] buttonShadows,classButtons;
			private ScrollPane charactersScrollPane;

			{
				// Default dialog padding:
				padTop(InterfaceManager.DIALOG_PAD_TOP);

				// Setting the default class and spells:
				currentClass = PlayerClass.LICH;
				spells = new Spells[SpellType.values().length];
				spells[SpellType.FIRE.getIndex()] = Spells.FIREBALL;
				spells[SpellType.WATER.getIndex()] = Spells.HEAL;
				spells[SpellType.EARTH.getIndex()] = Spells.QUAKE;
				spells[SpellType.AIR.getIndex()] = Spells.HASTE;

				add(new Label("Choose your mage:",userInterface,"title")).padTop(-4).row();

				// Table with all UI elements that concern character classes:
				add(classesTable = new Table() {
					{
						// Character classes icons:
						add(new Table() {
							{
								// Buttons' highlights:
								buttonShadows = new Image[PlayerClass.values().length
								    -PlayerClass.getSummonsAmount()];
								add(new Table() {
									{
										// Adding an image with the shadow of each class icon:
										for(PlayerClass playerClass : PlayerClass.values()) {
											if(!playerClass.isSummon()) {
												add(buttonShadows[playerClass.getIndex()] =
													new Image(userInterface.getDrawable(playerClass.getIndex()+"s")));
												if(playerClass != currentClass) {
													buttonShadows[playerClass.getIndex()].addAction(Actions.alpha(0));
												}
											}
										}
									}
								}).row();

								// Player classes' buttons table:
								classButtons = new Image[PlayerClass.values().length
								    -PlayerClass.getSummonsAmount()];
								add(new Table() {
									{
										// Adding a button for each of the classes:
										for(final PlayerClass playerClass : PlayerClass.values()) {
											if(!playerClass.isSummon()) {
												add(classButtons[playerClass.getIndex()] =
													new Image(userInterface.getDrawable(playerClass.getIndex()+"a")) {
													{
														addListener(new ClickListener() {
															@Override
															public void enter(InputEvent event,float x, float y, int pointer, Actor fromActor) {
																if(playerClass != currentClass) {
																	// Highlighting class icon shadow:
																	buttonShadows[playerClass.getIndex()].clearActions();
																	buttonShadows[playerClass.getIndex()].addAction(Actions.fadeIn(0.2f));
																}
															}

															@Override
															public void exit(InputEvent event,float x, float y, int pointer, Actor toActor) {
																if(playerClass != currentClass) {
																	// Canceling highlight of class icon shadow:
																	buttonShadows[playerClass.getIndex()].clearActions();
																	buttonShadows[playerClass.getIndex()].addAction(Actions.fadeOut(0.2f));
																}
															}

															@Override
															public void clicked(InputEvent event, float x, float y) {
																// If the user selects a different class, updating info:
																if(playerClass != currentClass) {
																	updateClassInfo(playerClass);
																}
															};
														});
													}
												});
											}
										}
									}
								}).padTop(-64).row();
							}
						}).expand().fill().row();

						// Classes information panel:
						add(charactersScrollPane = new ScrollPane(charactersTable = new Table(),userInterface) {
							{
								setOverscroll(false, false);
							}
						}).height(92).expandX().fillX().pad(2).row();

						updateClassDescription(false);
					}
				}).expand().fill().row();

				// Elite character checkbox:
				add(eliteCharacter = new CheckBox("Show elite classes",userInterface) {
					{
						// Default - standard mode:
						setChecked(false);
						addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x,float y) {
								// Changing the current type of displayed classes:
								switchToEliteCharacters(isChecked());
							}
						});
					}
				}).row();

				// Buttons table:
				add(new Table() {
					{
						add(new TextButton("Leave",userInterface) {
							{
								addListener(new ClickListener() {
									@Override
									public void clicked(InputEvent event, float x,float y) {
										// Leaving to the lobby:
										((Core)Gdx.app.getApplicationListener()).getNetworkManager()
											.sendTCP(new CltLeaveGame());
									}
								});
							}
						}).width(84).pad(2);
						add(new TextButton("Choose spells",userInterface) {
							{
								addListener(new ClickListener() {
									@Override
									public void clicked(InputEvent event, float x,float y) {
										// Showing spells management dialog:
										showSpellsManagement();
									}
								});
							}
						}).expandX().fillX().pad(2);
						add(new TextButton("Settings",userInterface) {
							{
								addListener(new ClickListener() {
									@Override
									public void clicked(InputEvent event, float x,float y) {
										// Showing settings window:
										((Core)Gdx.app.getApplicationListener())
											.getInterfaceManager().displaySettings(true);
									}
								});
							}
						}).width(84).pad(2);
					}
				}).expandX().fillX().row();

				// Allows the user to enter the game:
				add(new TextButton("Enter the arena!",userInterface) {
					{
						addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x,float y) {
								// Creating the character. Showing connecting dialog:
								((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
									.displayConnectingDialog(true);

								// Updating game interface:
								updateCurrentSpells(spells);

								// Preparing server packet:
								CltCreateCharacter packet = new CltCreateCharacter();
								// Checking if the character is in elite mode:
								packet.isElite = eliteCharacter.isChecked();
								// Assigning class:
								packet.classIndex = (byte)currentClass.getIndex();
								// Assigning spells:
								packet.fireSpell = (byte)spells[SpellType.FIRE.getIndex()].getIndex();
								packet.waterSpell = (byte)spells[SpellType.WATER.getIndex()].getIndex();
								packet.earthSpell = (byte)spells[SpellType.EARTH.getIndex()].getIndex();
								packet.airSpell = (byte)spells[SpellType.AIR.getIndex()].getIndex();
								// Sending the packet:
								((Core)Gdx.app.getApplicationListener()).getNetworkManager()
									.sendTCP(packet);
							}
						});
					}
				}).expandX().fillX().pad(2).row();

				// Chat:
				add(managementChat = new Chat(Type.GAME)).height(96).width(364).pad(4);

				pack();
			}

			/**
			 * Switches icons and informations to match the chosen character type.
			 * @param elite true for elite characters, false for standard.
			 */
			private void switchToEliteCharacters(final boolean elite) {
				// Clearing actions on the whole classes information table:
				classesTable.clearActions();

				classesTable.addAction(Actions.sequence(Actions.fadeOut(0.2f),
					Actions.run(new Runnable() {
						@Override
						public void run() {
							// If character type switches to elite:
							if(elite) {
								// Setting elite class icons:
								for(PlayerClass playerClass : PlayerClass.values()) {
									if(!playerClass.isSummon()) {
										buttonShadows[playerClass.getIndex()].setDrawable
											(userInterface.getDrawable(playerClass.getEliteIndex()+"s"));
										classButtons[playerClass.getIndex()].setDrawable
											(userInterface.getDrawable(playerClass.getEliteIndex()+"a"));
									}
								}
							}
							// If character type switches to standard:
							else {
								// Setting standard class icons:
								for(PlayerClass playerClass : PlayerClass.values()) {
									if(!playerClass.isSummon()) {
										buttonShadows[playerClass.getIndex()].setDrawable
											(userInterface.getDrawable(playerClass.getIndex()+"s"));
										classButtons[playerClass.getIndex()].setDrawable
											(userInterface.getDrawable(playerClass.getIndex()+"a"));
									}
								}
							}
							// Updating class description:
							updateClassDescription(elite);
						}
					}),
				Actions.fadeIn(0.2f)));
			}

			/**
			 * Used to change the displayed info to match a different class.
			 * @param playerClass will display info about this class.
			 */
			private void updateClassInfo(PlayerClass playerClass) {
				// Fading out the current class' shadow.
				buttonShadows[currentClass.getIndex()].clearActions();
				buttonShadows[currentClass.getIndex()].addAction(Actions.fadeOut(0.2f));

				// Setting new selected class:
				currentClass = playerClass;

				// Updating characters information table:
				charactersTable.clearActions();
				charactersTable.addAction(Actions.sequence(Actions.fadeOut(0.1f),
					Actions.run(new Runnable() {
						@Override
						public void run() {
							updateClassDescription(eliteCharacter.isChecked());
						}
					}),// Fading in:
				Actions.fadeIn(0.1f)));
			}

			/**
			 * Updates current class description, without adding any actions to the characters table.
			 * @param elite true for elite descriptions, false for standard.
			 */
			private void updateClassDescription(boolean elite) {
				// Changing class description:
				charactersTable.clearChildren();

				String[] description;
				// Elite character:
				if(elite) {
					// Showing elite class name:
					charactersTable.add(new Label(currentClass.getEliteName(),userInterface,"dark")).row();
					// Setting elite class description:
					description = currentClass.getEliteDescription().split("\n");
				}
				else {
					// Showing class name:
					charactersTable.add(new Label(currentClass.toString(),userInterface,"dark")).row();
					// Setting class description:
					description = currentClass.getDescription().split("\n");
				}

				// Showing class description:
				for(String descriptionLine : description) {
					charactersTable.add(new Label(descriptionLine,userInterface)).row();
				}
				// Showing class statistics:
				String[] statistics = currentClass.getStatictics().split("\n");
				for(String statisticsLine : statistics) {
					charactersTable.add(new Label(statisticsLine,userInterface,"title")).row();
				}
				// Updating table and setting scroll pane:
				charactersTable.invalidateHierarchy();
				charactersScrollPane.setScrollPercentY(0);
			}

			/**
			 * Shows a dialog that allows the user to manage his spells.
			 */
			private void showSpellsManagement() {
				new Dialog("Choose your spells",userInterface,"dialog-alt") {
					private Table spellInformation;
					private Image[] chosenSpellsIcons;
					private Spells viewedSpell;

					{
						// Adding spells background:
						getContentTable().add(new Image(new NinePatch(
							userInterface.getRegion("icon-background"),
							3,3,3,3))).width(6+32*7).height(6+32*4).row();

						// Adding spells buttons:
						getContentTable().add(new Table() {
							{
								for(final Spells spell : Spells.values()) {
									add(new Image(userInterface
										.getDrawable("s"+spell.getType()+"-"+spell.getIndex())) {
										{
											addListener(new ClickListener() {
												@Override
												public void clicked(InputEvent event, float x,float y) {
													// If user clicks on the icon, update spell description:
													updateSpellInfo(spell);
												}
											});
										}
									});
									// Making sure the icons are not in one row...
									if(spell.getIndex() == 6) {
										row();
									}
								}
							}
						}).padTop(-(13+32*4)).row();

						// Adding spell information table:
						getContentTable().add(spellInformation = new Table() {
							{
								// Setting default information (for chosen fire spell):
								String[] description = spells[SpellType.FIRE.getIndex()]
									.getDescription().split("\n");
								add(new Label(spells[SpellType.FIRE.getIndex()].toString(),
									userInterface,"dark")).pad(2).row();
								for(String descriptionLine : description) {
									add(new Label(descriptionLine,userInterface)).row();
								}
							}
						}).pad(2).expand().fill().row();
						// Setting default viewed spell:
						viewedSpell = spells[SpellType.FIRE.getIndex()];

						// Adding chosen spells title:
						getContentTable().add(new Label("Chosen spells:",userInterface,"title")).row();

						// Adding chosen spells background:
						getContentTable().add(new Image(new NinePatch(
							userInterface.getRegion("icon-background"),
							3,3,3,3))).padLeft(12).padRight(12).width(6+64*4).height(6+64).row();

						// Adding previously chosen spell icons:
						chosenSpellsIcons = new Image[SpellType.values().length];
						// Getting currently chosen spells from the management window:
						getContentTable().add(new Table() {
							{
								for(final Spells spell : spells) {
									add(chosenSpellsIcons[spell.getType()] =
										new Image(userInterface.getDrawable("b"+spell.getType()
										+"-"+spell.getIndex())) {
										{
											addListener(new ClickListener() {
												@Override
												public void clicked(InputEvent event, float x,float y) {
													// If user clicks on the icon, update spell description:
													updateSpellInfo(spells[spell.getType()]);
												}
											});
										}
									});
								}
							}
						}).padTop(-(18+64));

						button("Save");
					}

					/**
					 * Displaying description of the chosen spell.
					 * @param spell spell chosen by the user.
					 */
					private void updateSpellInfo(final Spells spell) {
						if(spell != viewedSpell) {
							// Setting currently viewed spell:
							viewedSpell = spell;

							// Clearing actions for interface elements we're about to change:
							spellInformation.clearActions();
							chosenSpellsIcons[spell.getType()].clearActions();
							// Adding actions for the spell information table:
							spellInformation.addAction(Actions.sequence(
								Actions.fadeOut(0.1f),Actions.run(new Runnable() {
									@Override
									public void run() {
										// Clearing spell information table after fading out:
										spellInformation.clearChildren();

										// Setting description for the chosen spell:
										String[] description = spell.getDescription().split("\n");
										spellInformation.add(new Label(spell.toString(),
											userInterface,"dark")).pad(2).row();
										for(String descriptionLine : description) {
											spellInformation.add(new Label(descriptionLine,
												userInterface)).row();
										}
									}
								}), // Fading in:
							Actions.fadeIn(0.1f)));

							// Checking if the spell actually changed for the given magic school:
							if(spell != spells[spell.getType()]) {
								// Setting the new spell:
								spells[spell.getType()] = spell;

								// Adding actions for the spell icon:
								chosenSpellsIcons[spell.getType()].addAction(Actions.sequence(
									Actions.fadeOut(0.25f),Actions.run(new Runnable() {
										@Override
										public void run() {
											// Changing the icon when the image is transparent:
											chosenSpellsIcons[spell.getType()].setDrawable(
												userInterface.getDrawable("b"+spell.getType()
												+"-"+spell.getIndex()));
										}
									}), // Fading in:
								Actions.fadeIn(0.25f)));
							}
						}
					}
				}.show(stage);
			}
		});
	}

	@Override
	public void show() {
		// Getting a random background:
		background = new TiledDrawable(new TextureRegion(((Core)
			Gdx.app.getApplicationListener()).getInterfaceManager().
			getAsset("game/interface/bg-"+(int)MathUtils.random
			(InterfaceManager.GAME_BGS_AMOUNT)+".png",Texture.class)));

		// Refreshing chats:
		interfaceChat.refresh();
		managementChat.refresh();

		super.show();

		// Setting keyboard focus:
		stage.setKeyboardFocus(managementChat.getChatTextField());
	}

	@Override
	public void hide(final Screens screen) {
		// Hiding this screen needs a few additional methods, so it has its own implementation.
		Gdx.input.setInputProcessor(null);

		// Clearing all actions of the actors:
		for(Actor actor : stage.getActors()) {
			actor.clearActions();
		}
		// Fading the screen out:
		stage.addAction(Actions.sequence(Actions.fadeOut(0.5f),Actions.run(new Runnable() {
			@Override
			public void run() {
				// Hiding game interface:
				if(gameInterface.isVisible()) {
					gameInterface.addAction(Actions.alpha(0));
					gameInterface.setVisible(false);
					gameManagement.addAction(Actions.alpha(1));
					gameManagement.setVisible(true);
				}
				interfaceChat.clearChat();
				managementChat.clearChat();

				// Showing another screen:
				((Core)Gdx.app.getApplicationListener()).getInterfaceManager().showScreen(screen);
			}
		})));
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		// Resizing user interface:
		gameInterface.setSize(stage.getWidth(), stage.getHeight());
		gameInterface.invalidateHierarchy();
		interfaceChat.setSize(interfaceChat.getWidth(),interfaceChat.getHeight());
		if(gameManagement.isVisible()) {
			InterfaceManager.centerWindow(gameManagement);
		}
		// Resizing Box2D world:
		if(battleManager != null) {
			battleManager.setSize(width, height);
		}
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// Drawing background:
		if(background != null) {
			stage.getBatch().begin();
				stage.getBatch().disableBlending();
				background.draw(stage.getBatch(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				stage.getBatch().enableBlending();
			stage.getBatch().end();
		}

		// Drawing game:
		if(battleManager != null) {
			battleManager.render(delta);
		}

		// Drawing stage:
		super.render(delta);
	}

	@Override
	public void update(final Object packet) {
		if(packet instanceof SrvLeaveGame) {
			// Player left the room: show lobby screen:
			hide(Screens.LOBBY);
		}
		else if(packet instanceof SrvGameChatMessage) {
			// Someone sends a chat message - show it:
			if(gameInterface.isVisible()) {
				interfaceChat.addMessage(((SrvGameChatMessage)packet).message);
			}
			else {
				managementChat.addMessage(((SrvGameChatMessage)packet).message);
			}
		}
		else if(packet instanceof SrvAddRoomUser) {
			// Someone joins the room - show his nickname on the chat list:
			if(gameInterface.isVisible()) {
				interfaceChat.addUser(((SrvAddRoomUser)packet).username,
					((SrvAddRoomUser)packet).teamIndex);
			}
			else {
				managementChat.addUser(((SrvAddRoomUser)packet).username,
					((SrvAddRoomUser)packet).teamIndex);
			}
		}
		else if(packet instanceof SrvRemoveRoomUser) {
			// Someone leave the room - delete his nickname from the chat list:
			if(gameInterface.isVisible()) {
				interfaceChat.removeUser(((SrvRemoveRoomUser)packet).username);
			}
			else {
				managementChat.removeUser(((SrvRemoveRoomUser)packet).username);
			}
		}
		else if(packet instanceof SrvScoresUpdate) {
			if(gameInterface.isVisible()) {
				interfaceChat.updateScores((SrvScoresUpdate)packet);
			}
			else {
				managementChat.updateScores((SrvScoresUpdate)packet);
			}
		}

		// Game logic packets:
		else if(packet instanceof SrvUpdateWorld) {
			// Updating game logic:
			if(battleManager != null) {
				battleManager.update((SrvUpdateWorld)packet);
				// Updating the health bar:
				if(Float.compare(healthBar.getCurrentHealth(),
					((SrvUpdateWorld)packet).currentHealth) != 0) {
					healthBar.setCurrentHealth(((SrvUpdateWorld)packet).currentHealth);
				}
				// Updating summon health bar:
				if(Float.compare(summonHealthBar.getCurrentHealth(),
					((SrvUpdateWorld)packet).summonHealth) != 0) {
					summonHealthBar.setCurrentHealth(((SrvUpdateWorld)packet).summonHealth);
				}
			}
		}
		else if(packet instanceof SrvPlayerNotElite) {
			// Player tried to create elite character without the status:
			((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
				.displayConnectingDialog(false);
			((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
				.displayError("You need the elite", "status to create", "this character.");
		}
		else if(packet instanceof SrvCreateCharacter) {
			// Someone has created a character - updating world:
			if(battleManager != null) {
				battleManager.update((SrvCreateCharacter)packet);
				if(((SrvCreateCharacter)packet).playerName.equals(((Core)Gdx.app.
					getApplicationListener()).getNetworkManager().getUsername())) {
					// The player has just created his character: hiding management window:
					((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
						.displayConnectingDialog(false);
					showGameManagement(false);
				}
			}
		}
		else if(packet instanceof SrvSetSpellCooldown) {
			SrvSetSpellCooldown spellInfo = (SrvSetSpellCooldown)packet;
			// Hiding the spell icon:
			spellIcons[spellInfo.spellType].clearActions();
			// Fading out the icon and starting to fade it in:
			spellIcons[spellInfo.spellType].addAction
				(Actions.sequence(Actions.alpha(0f),Actions
				.alpha(0.5f,spellInfo.cooldown),Actions.repeat
				(2, Actions.sequence(Actions.fadeOut(0.15f),
				Actions.fadeIn(0.15f)))));
		}
		else if(packet instanceof SrvDisplaySFX) {
			// Trying to display a SFX:
			if(battleManager != null) {
				battleManager.displaySFX((SrvDisplaySFX)packet);
			}
		}
		else if(packet instanceof SrvAttachSFX) {
			// Trying to attach SFX to a player:
			if(battleManager != null) {
				battleManager.displaySFX((SrvAttachSFX)packet);
			}
		}
		else if(packet instanceof SrvSetEntityFalling) {
			// Setting an entity falling:
			if(battleManager != null) {
				battleManager.setEntityFalling((SrvSetEntityFalling)packet);
			}
		}
		else if(packet instanceof SrvSwitchMap) {
			// Changing map - clearing chats:
			interfaceChat.clearChat();
			managementChat.clearChat();
			// Showing game management:
			if(gameInterface.isVisible()) {
				showGameInterface(false);
			}
			// Preparing new battle manager:
			initiateGame(((SrvSwitchMap)packet).name,
				((SrvSwitchMap)packet).roomIndex,
				((SrvSwitchMap)packet).mapIndex);
		}
	}

	@Override
	public void dispose() {
		super.dispose();

		if(battleManager != null) {
			battleManager.dispose();
		}
	}

	/**
	 * @return current game name.
	 */
	public String getGameName() {
		return gameName;
	}

	/**
	 * Initiates a new game.
	 * @param gameName game's room name.
	 */
	public void initiateGame(String gameName,int gameIndex,int mapIndex) {
		if(battleManager != null) {
			battleManager.dispose();
		}
		battleManager = new BattleManager(gameIndex,mapIndex);
		setGameName(gameName);
	}

	/**
	 * @param gameName new game name.
	 */
	public void setGameName(String gameName) {
		this.gameName = gameName;
		gameManagement.getTitleLabel().setText("Enter The "+gameName);
	}

	/**
	 * Shows or hides the chat in the game management window.
	 * @param show true to show, false to hide.
	 */
	public void showGameManagementChat(boolean show) {
		if(gameManagement != null) {
			// Setting the chat (in)visible:
			managementChat.setVisible(show);
			if(show) {
				// Resizing the chat cell:
				gameManagement.getCell(managementChat).height(96)
					.width(364).pad(4);
				managementChat.refreshHiddenChat();
			}
			else {
				// Resizing the chat cell to 0:
				gameManagement.getCell(managementChat).size(0).pad(0).space(0);
			}

			GameSettings.CHAT_IN_GAME_MANAGEMENT = show;
			// Refreshing game management window:
			gameManagement.invalidateHierarchy();
			gameManagement.pack();
			InterfaceManager.centerWindow(gameManagement);
		}
	}

	/**
	 * Shows or hides game instructions and interface chat, leaving only game HUD.
	 * @param show true to show, false to hide.
	 */
	public void showFullGameInterface(boolean show) {
		if(gameInterface != null) {
			// Setting the game interface elements (in)visible:
			interfaceChat.setVisible(show);
			instructionLabel.setVisible(show);

			if(show) {
				// Resizing interface chat cell:
				gameInterface.getCell(interfaceChat).height(96).expandX().fillX();
				interfaceChat.refreshHiddenChat();
			}
			else {
				// Resizing interface chat cell to 0:
				gameInterface.getCell(interfaceChat).size(0).pad(0).space(0);
			} // Instruction label's cell is not resized - setting it invisible is enough.

			GameSettings.SHOW_GAME_INTERFACE = show;
			// Refreshing game interface table:
			gameInterface.invalidateHierarchy();
		}
	}

	/**
	 * Used to refresh the spell icons each time the user enters the game.
	 * @param spells spell list.
	 */
	private void updateCurrentSpells(Spells[] spells) {
		// Setting current spell icons:
		for(Spells spell : spells) {
			spellIcons[spell.getType()].setDrawable(userInterface
				.getDrawable("m"+spell.getType()+"-"+spell.getIndex()));
		}
	}

	/**
	 * Hides game interface and shows game management window.
	 * Should be triggered when player's character dies.
	 */
	public void hideGameInterface() {
		showGameInterface(false);
	}

	/**
	 * @param show true to show game management window;
	 * false to hide game management window and show game interface.
	 */
	private void showGameManagement(boolean show) {
		gameManagement.clearActions();
		if(show) {
			gameManagement.setVisible(true);
			stage.setKeyboardFocus(managementChat.getChatTextField());
			gameManagement.addAction(Actions.sequence(Actions.fadeIn(0.2f),
				Actions.run(new Runnable() {
					@Override
					public void run() {
						managementChat.setChatMessages(interfaceChat.getChatMessages());
					}
				})));
		}
		else {
			gameManagement.addAction(Actions.sequence(Actions.fadeOut(0.2f),
				Actions.run(new Runnable() {
					@Override
					public void run() {
						gameManagement.addAction(Actions.alpha(0));
						gameManagement.setVisible(false);
						// Synchronizing chats:
						interfaceChat.setUsersScores(managementChat.getUsersScores());
						interfaceChat.setUsersList(managementChat.getUsersList());
						interfaceChat.getChatTextField()
							.setText(managementChat.getChatTextField().getText());
						interfaceChat.getChatTextField().setCursorPosition
							(managementChat.getChatTextField().getText().length());
						showGameInterface(true);
					}
				})));
		}
	}

	/**
	 * @param show true to show game interface;
	 * false to hide game interface and show game management window.
	 */
	private void showGameInterface(boolean show) {
		gameInterface.clearActions();
		if(show) {
			gameInterface.setVisible(true);
			gameInterface.addAction(Actions.sequence(Actions.fadeIn(0.2f),
				Actions.run(new Runnable() {
					@Override
					public void run() {
						// Making sure the spell icons are visible:
						for(Image spellIcon : spellIcons) {
							spellIcon.clearActions();
							spellIcon.addAction(Actions.alpha(1f));
						}

						stage.setKeyboardFocus(gameInterface);
						interfaceChat.setChatMessages(managementChat.getChatMessages());
					}
				})));
		}
		else {
			gameInterface.addAction(Actions.sequence(Actions.fadeOut(0.2f),
				Actions.run(new Runnable() {
					@Override
					public void run() {
						gameInterface.addAction(Actions.alpha(0));
						gameInterface.setVisible(false);
						// Synchronizing chats:
						managementChat.setUsersScores(interfaceChat.getUsersScores());
						managementChat.setUsersList(interfaceChat.getUsersList());
						managementChat.getChatTextField()
							.setText(interfaceChat.getChatTextField().getText());
						managementChat.getChatTextField().setCursorPosition
							(interfaceChat.getChatTextField().getText().length());
						showGameManagement(true);
					}
				})));
		}
	}

	/**
	 * @return current battle manager. Might be null.
	 */
	public BattleManager getBattleManager() {
		return battleManager;
	}
}
