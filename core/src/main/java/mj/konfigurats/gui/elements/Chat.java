package mj.konfigurats.gui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import mj.konfigurats.Core;
import mj.konfigurats.game.utilities.ScoreSet;
import mj.konfigurats.network.GamePackets.CltGameChatMessage;
import mj.konfigurats.network.GamePackets.CltTeamMessage;
import mj.konfigurats.network.GamePackets.SrvScoresUpdate;
import mj.konfigurats.network.LobbyPackets.CltLobbyMessage;
import mj.konfigurats.network.LobbyPackets.CltPrivateMessage;

import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A table implementation that manages and displays a list of messages and users.
 * @author MJ
 */
public class Chat extends Table {
	private final static Pattern EMPTY_MESSAGE = Pattern.compile("\\s*"),
								PRIVATE_MESSAGE = Pattern.compile("/w \\w{2,10}\\s+[^\\s].*"),
								TEAM_MESSAGE = Pattern.compile("/[pt]\\s+[^\\s].*");
	// Default chat size:
	private final static int CHAT_SIZE = 128;
	// UI elements:
	private final Skin userInterface;
	// Chat table:
	private final Table chatTable;
	private final ScrollPane chatPane;
	// Users table:
	private final Table usersTable;
	// Chat controls:
	private final TextField chatTextField;
	private final TextButton chatSendButton;
	// Data lists:
	private final List<Label> chatLabels;
	private final List<String> chatMessages;
	private final Array<String> usersList;
	private final Map<String,ScoreSet> usersScores;
	// Chat's size:
	private final int capacity;
	// Control variables:
	private Matcher regexMatcher;
	private boolean shouldColorMessages;
	private int messagePadding,messageSpacing;
	private Type chatType;

	/**
	 * Creates a new chat with the default capacity.
	 * @param chatType type of the chat, depending on its purpose.
	 */
	public Chat(Type chatType) {
		this(CHAT_SIZE,chatType);
	}

	/**
	 * Creates a new chat.
	 * @param capacity maximum amount of messages on the chat.
	 * @param chatType type of the chat, depending on its purpose.
	 */
	public Chat(int capacity,Type chatType) {
		super();
		chatLabels = new ArrayList<Label>(capacity);
		this.capacity = capacity;
		this.chatType = chatType;

		// Getting default user interface skin:
		userInterface = ((Core)Gdx.app.getApplicationListener())
				.getInterfaceManager().getSkin();

		// Creating chat data lists:
		chatMessages = new LinkedList<String>();
		usersList = new Array<String>();
		// Creating users scores map:
		if(chatType == Type.GAME) {
			usersScores = new HashMap<String,ScoreSet>();
		}
		else {
			usersScores = null;
		}

		// Creating chat elements:
		// Chat pane:
		add(chatPane = new ScrollPane(chatTable = new Table() {
			{
				align(Align.top+Align.left);
			}
		},userInterface) {
			{
				setOverscroll(false, false);
				setScrollingDisabled(true, false);
			}
		}).space(4).expand().fill();

		// Users pane:
		switch(this.chatType) {
		case LOBBY:
			add(new ScrollPane(usersTable = new Table() {
				{
					align(Align.top+Align.left);
				}
			},userInterface) {
				{
					setOverscroll(false, false);
				}
			}).space(4).width(90).expandY().fillY().row();
			break;
		case GAME:
			add(new ScrollPane(usersTable = new Table() {
				{
					align(Align.top+Align.left);
				}
			},userInterface) {
				{
					setOverscroll(false, false);
					setFadeScrollBars(false);
				}
			}).space(4).width(148).expandY().fillY().row();
			break;
		default:
			// Shutting Eclipse up:
			usersTable = null;
		}

		// Chat messages text field:
		add(chatTextField = new TextField("",userInterface) {
			{
			setMaxLength(256);
			setOnlyFontChars(true);
			addListener(new InputListener() {
					@Override
					public boolean keyUp(InputEvent event,int keycode) {
						if(keycode == Keys.ENTER) {
							sendMessage();
							return true;
						}
						else return false;
					}
				});
			}
		}).space(4).expandX().fillX();

		// Sending button:
		add(chatSendButton = new TextButton("Send",userInterface) {
			{
				addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						sendMessage();
					}
				});
			}
		}).fillX().space(4);

		// Settings:
		messagePadding = 0;
		messageSpacing = 4;
		shouldColorMessages = true;
		chatType = Type.LOBBY;
	}

	@Override
	public void setSize(float width, float height) {
		super.setSize(width, height);
		invalidateHierarchy();
		setChatTableSize(chatTable.getWidth(),chatTable.getHeight());
	}

	/**
	 * Sets a new chat type. Depending on the type, messages will be send in different packets.
	 * @param chatType new chat type.
	 */
	public void setType(Type chatType) {
		this.chatType = chatType;
	}

	/**
	 * Sets a new size of the chat table. Can be used to refresh the table properties.
	 * @param width chat table width.
	 * @param height chat table height.
	 */
	private void setChatTableSize(float width, float height) {
		// Setting new cell properties for proper string wrapping:
		for(Cell<?> cell : chatTable.getCells()) {
			cell.width(width-12).left().pad(messagePadding).space(messageSpacing);
		}
		chatTable.invalidateHierarchy();
	}

	/**
	 * Sets the padding between messages. Default is 0.
	 * @param padding labels' padding.
	 */
	public void setLabelsPadding(int padding) {
		messagePadding = padding;
		setChatTableSize(chatTable.getWidth(),chatTable.getHeight());
	}

	/**
	 * Sets the spacing between messages. Default is 4.
	 * @param spacing labels' spacing.
	 */
	public void setLabelsSpacing(int spacing) {
		messageSpacing = spacing;
		setChatTableSize(chatTable.getWidth(),chatTable.getHeight());
	}

	/**
	 * @param shouldColorMessages true to color messages if they contain player's nickname. Default is true.
	 */
	public void setColoredMessages(boolean shouldColorMessages) {
		this.shouldColorMessages = shouldColorMessages;
	}

	/**
	 * Clears the whole chat.
	 */
	public void clearChat() {
		clearMessages();
		clearUsers();
		if(chatType == Type.GAME) {
			usersScores.clear();
		}
	}

	/**
	 * Should be run after the chat is shown after being active but invisible for some time.
	 * As most labels are permanently invisible, the chat is cleared, while the users list is refreshed.
	 */
	public void refreshHiddenChat() {
		clearMessages();
		refreshUsersList();
	}

	/**
	 * Refreshes the chat before showing it on the stage.
	 */
	public void refresh() {
		refreshUsersList();
		chatTextField.setText("");
	}

	/**
	 * Clears chat messages list, chat labels list and table children.
	 */
	private void clearMessages() {
		chatMessages.clear();
		chatLabels.clear();
		chatTable.clearChildren();
	}

	/**
	 * Clears chat users list and table.
	 */
	private void clearUsers() {
		usersTable.clearChildren();
		usersList.clear();
	}

	/**
	 * Adds a new message to the chat.
	 * @param message message's text.
	 */
	public void addMessage(String message) {
		// If chat is full - delete first message and add a new one. Update labels.
		if(isFull()) {
			chatMessages.remove(0);
			chatMessages.add(message);
			updateMessageLabels();
		}
		// If it isn't - add a new message and a new label.
		else {
			chatMessages.add(message);
			addNewMessageLabel(message,true);
		}
		// Scroll chat pane to the bottom:
		schedulePaneScrolling();
	}

	/**
	 * Sends a message to the chat using UDP. Takes the message's text from the chat text field.
	 */
	private void sendMessage() {
		// If the chat message isn't empty or full of spaces:
		regexMatcher = EMPTY_MESSAGE.matcher(chatTextField.getText());
		if(!regexMatcher.matches()) {
			// Special messages:
			if(chatTextField.getText().startsWith("/")) {
				switch(chatTextField.getText().charAt(1)) {
				case 't':
				case 'p':
					//Team message:
					if(chatType == Type.GAME) {
						regexMatcher = TEAM_MESSAGE.matcher(chatTextField.getText());
						if(regexMatcher.matches()) {
							CltTeamMessage packet = new CltTeamMessage();
							packet.message = chatTextField.getText().substring(3);
							((Core)Gdx.app.getApplicationListener()).getNetworkManager().sendUDP(packet);
						}
						chatTextField.setText("");
						return;
					}
					else {
						chatTextField.setText("");
						addMessage("Team messages available in game mode.");
						return;
					}
				case 'w':
					// Private message:
					regexMatcher = PRIVATE_MESSAGE.matcher(chatTextField.getText());
					if(regexMatcher.matches()) {
						CltPrivateMessage packet = new CltPrivateMessage();
						packet.target = chatTextField.getText().split("\\s+")[1];
						if(!packet.target.equals(((Core)Gdx.app.getApplicationListener())
							.getNetworkManager().getUsername())) {
							packet.message = chatTextField.getText()
								.substring(4+packet.target.length());
							((Core)Gdx.app.getApplicationListener())
								.getNetworkManager().sendUDP(packet);
						}
					}
					chatTextField.setText("");
					return;
				default:
					addMessage("Unknown command.");
					chatTextField.setText("");
					return;
				}
			}

			// Regular messages:
			switch(chatType) {
				case LOBBY: {
					CltLobbyMessage packet = new CltLobbyMessage();
					packet.message = chatTextField.getText();
					((Core)Gdx.app.getApplicationListener()).getNetworkManager().sendUDP(packet);
					break;
				}
				case GAME: {
					CltGameChatMessage packet = new CltGameChatMessage();
					packet.message = chatTextField.getText();
					((Core)Gdx.app.getApplicationListener()).getNetworkManager().sendUDP(packet);
					break;
				}
			}
		}
		chatTextField.setText("");
	}

	/**
	 * Updates scores for a chosen player.
	 * @param packet server packet with new scores.
	 */
	public void updateScores(SrvScoresUpdate packet) {
		if(usersScores.containsKey(packet.nickname)) {
			// Updating scores:
			usersScores.get(packet.nickname).setScores(packet.kills, packet.deaths);
		}
		else {
			// Creating scores object:
			usersScores.put(packet.nickname,new ScoreSet(packet.kills,packet.deaths));
		}

	}

	/**
	 * Adds a user to the chat list.
	 * @param nickname user's nickname.
	 */
	public void addUser(String nickname) {
		if(!usersList.contains(nickname,false)) {
			usersList.add(nickname);
			refreshUsersList();
		}
	}

	/**
	 * Adds a user to the game chat list.
	 * @param nickname user's nickname.
	 * @param teamIndex player's team.
	 */
	public void addUser(String nickname,int teamIndex) {
		if(!usersList.contains(nickname,false)) {
			usersList.add(nickname);
			// Adding score for the user:
			if(chatType == Type.GAME) {
				// If the score wasn't already received and processed:
				if(!usersScores.containsKey(nickname)) {
					usersScores.put(nickname,new ScoreSet(teamIndex));
				}
				else {
					usersScores.get(nickname).setTeamIndex(teamIndex);
				}

				// Showing game room message:
				addMessage(nickname+" entered the room.");
			}
			refreshUsersList();
		}
	}

	/**
	 * Removes a user from the chat list.
	 * @param nickname user's nickname.
	 */
	public void removeUser(String nickname) {
		if(usersList.contains(nickname,false)) {
			usersList.removeValue(nickname,false);
			if(chatType == Type.GAME) {
				// Removing player's score:
				usersScores.remove(nickname);

				// Showing game room message:
				addMessage(nickname+" left the room.");
			}
			refreshUsersList();
		}
	}

	/**
	 * Sorts the users list and adds their labels to the table.
	 */
	private void refreshUsersList() {
		// Preparing lobby users list:
		usersList.sort();
		// Clearing lobby users table:
		usersTable.clearChildren();

		for(String username : usersList) {
			if(username.equals(((Core)Gdx.app.getApplicationListener())
				.getNetworkManager().getUsername())) {
				if(chatType == Type.GAME) {
					switch(usersScores.get(username).getTeamIndex()) {
					case 0:
						usersTable.add(new Label(username,userInterface,"dark")).left();
						break;
					case 1:
						usersTable.add(new Label(username,userInterface,"title")).left();
						break;
					default:
						usersTable.add(new Label(username,userInterface,"title")).left();
						break;
					}
					// Showing score:
					usersTable.add(usersScores.get(username).getLabel()).padRight(10).right().expandX();
				}
				else {
					usersTable.add(new Label(username,userInterface,"title")).left();
				}

				usersTable.row();
			}
			else {
				// If it's another player:
				if(chatType == Type.GAME) {
					// Game chat:
					switch(usersScores.get(username).getTeamIndex()) {
					case 0:
						usersTable.add(new TextButton(username,userInterface,"team0") {
							{
								addListener(new ClickListener() {
									@Override
									public void clicked(InputEvent event, float x,float y) {
										if(!chatTextField.getText().contains(" "+getText()+": ") &&
											!chatTextField.getText().startsWith(getText()+": ")) {
											// If the chat text field doesn't have the clicked player's username - add it:
											chatTextField.setText(getText()+": "+chatTextField.getText());
											chatTextField.setCursorPosition(chatTextField.getText().length());
										}
										else {
											chatTextField.setText("/w "+getText()+" ");
											chatTextField.setCursorPosition(chatTextField.getText().length());
										}
									}
								});
							}
						}).left();
						break;
					case 1:
						usersTable.add(new TextButton(username,userInterface,"team1") {
							{
								addListener(new ClickListener() {
									@Override
									public void clicked(InputEvent event, float x,float y) {
										if(!chatTextField.getText().contains(" "+getText()+": ") &&
											!chatTextField.getText().startsWith(getText()+": ")) {
											// If the chat text field doesn't have the clicked player's username - add it:
											chatTextField.setText(getText()+": "+chatTextField.getText());
											chatTextField.setCursorPosition(chatTextField.getText().length());
										}
										else {
											chatTextField.setText("/w "+getText()+" ");
											chatTextField.setCursorPosition(chatTextField.getText().length());
										}
									}
								});
							}
						}).left();
						break;
					default:
						usersTable.add(new TextButton(username,userInterface,"text") {
							{
								addListener(new ClickListener() {
									@Override
									public void clicked(InputEvent event, float x,float y) {
										if(!chatTextField.getText().contains(" "+getText()+": ") &&
											!chatTextField.getText().startsWith(getText()+": ")) {
											// If the chat text field doesn't have the clicked player's username - add it:
											chatTextField.setText(getText()+": "+chatTextField.getText());
											chatTextField.setCursorPosition(chatTextField.getText().length());
										}
										else {
											chatTextField.setText("/w "+getText()+" ");
											chatTextField.setCursorPosition(chatTextField.getText().length());
										}
									}
								});
							}
						}).left();
						break;
					}

					// Showing score:
					usersTable.add(usersScores.get(username).getLabel()).padRight(10).right();
				}
				else {
					// Regular chat:
					usersTable.add(new TextButton(username,userInterface,"text") {
						{
							addListener(new ClickListener() {
								@Override
								public void clicked(InputEvent event, float x,float y) {
									if(!chatTextField.getText().contains(" "+getText()+": ") &&
										!chatTextField.getText().startsWith(getText()+": ")) {
										// If the chat text field doesn't have the clicked player's username - add it:
										chatTextField.setText(getText()+": "+chatTextField.getText());
										chatTextField.setCursorPosition(chatTextField.getText().length());
									}
									else {
										chatTextField.setText("/w "+getText()+" ");
										chatTextField.setCursorPosition(chatTextField.getText().length());
									}
								}
							});
						}
					}).left();
				}

				usersTable.row();
			}

		}
		usersTable.invalidateHierarchy();
	}

	/**
	 * @return true if the chat reached it's full capacity.
	 */
	public boolean isFull() {
		return chatMessages.size() == capacity;
	}

	/**
	 * @return true if there are no messages on the chat.
	 */
	public boolean isEmpty() {
		return chatMessages.size() == 0;
	}

	/**
	 * Adds a new label to the chat. Should be called only when the maximum capacity is not yet achieved.
	 * @param message message's text.
	 * @param invalidate true will invalidate table hierarchy. Should be skipped if there is more than one label being added at once.
	 */
	private void addNewMessageLabel(String message,boolean invalidate) {
		if(shouldColorMessages) {
			// Adding label to the labels list:
			if(message.startsWith(((Core)Gdx.app.getApplicationListener())
					.getNetworkManager().getUsername()+":")) {
					// If the message is sent by the player, it will be highlighted:
					chatLabels.add(createNewLabel(message, "title"));
				}
				else if(message.startsWith("To ") || message.startsWith("(Team)") ||
					message.startsWith("From ")) {
					// Private or team messages are a bit darker:
					chatLabels.add(createNewLabel(message, "medium"));
				}
				else if(message.contains(" "+((Core)Gdx.app.getApplicationListener())
					.getNetworkManager().getUsername()+": ")) {
					// If the message contains player's nickname, it will be colored:
					chatLabels.add(createNewLabel(message, "dark"));
				}
				else {
					// Regular message:
					chatLabels.add(createNewLabel(message, null));
				}
		}
		else {
			// Adding a regular message to the labels list:
			chatLabels.add(createNewLabel(message, null));
		}

		// Adding the new label to the table:
		chatTable.add(chatLabels.get(chatLabels.size()-1)).width(chatTable.getWidth()-12).left()
			.pad(messagePadding).space(messageSpacing).row();
		if(invalidate) {
			chatTable.invalidateHierarchy();
		}
	}

	/**
	 * Creates a new chat message label.
	 * @param message label's text.
	 * @param style label's style. Null for none.
	 * @return a new label.
	 */
	private Label createNewLabel(String message,String style) {
		return style != null // If a style is given:
		? new Label(message,((Core)Gdx.app.getApplicationListener())
			.getInterfaceManager().getSkin(),style) {
			{
				setWrap(true);
			}
		} // If it's not:
		: new Label(message,((Core)Gdx.app.getApplicationListener())
			.getInterfaceManager().getSkin()) {
			{
				setWrap(true);
			}
		};
	}

	/**
	 * Updates texts on the message labels.
	 */
	private void updateMessageLabels() {
		int index = 0;
		for(String message : chatMessages) {
			chatLabels.get(index++).setText(message);
		}
		chatTable.invalidateHierarchy();
	}

	/**
	 * Schedules chat pane scrolling on the thread manager's timer.
	 */
	private void schedulePaneScrolling() {
		((Core)Gdx.app.getApplicationListener()).getThreadManager().executeOnTimer(new TimerTask() {
			@Override
			public void run() {
				chatPane.setScrollPercentY(1);
			}
		},150);
	}

	/**
	 * @return current chat messages.
	 */
	public List<String> getChatMessages() {
		return chatMessages;
	}

	/**
	 * Deletes the current messages and sets new ones.
	 * @param newMessages a list of chat messages to be set.
	 * @return true for success. Returns false when the list is too big for the chat.
	 */
	public boolean setChatMessages(List<String> newMessages) {
		if(newMessages.size() <= capacity) {
			// If the lists sizes are equal, labels can be updated without the need to modify the table:
			if(newMessages.size() == chatMessages.size()) {
				chatMessages.clear();
				chatMessages.addAll(newMessages);
				updateMessageLabels();
			}
			// If the new messages list is greater, labels new labels can be added to the table without the need to remove the old ones:
			else if(newMessages.size() > chatMessages.size()) {
				if(chatMessages.isEmpty()) {
					chatMessages.addAll(newMessages);
					for(String message : chatMessages) {
						addNewMessageLabel(message, false);
					}
					chatTable.invalidateHierarchy();
				}
				else {
					chatMessages.clear();
					chatMessages.addAll(newMessages);
					// Adding empty labels to the table to make up for the smaller chat messages amount:
					while(chatLabels.size() < chatMessages.size()) {
						addNewMessageLabel(" ", false);
					}
					updateMessageLabels();
				}
			}
			// If the new messages list is smaller, some labels have to be removed. Good luck doing it without clearing the table.
			else {
				// Clearing whole chat:
				clearMessages();
				chatMessages.addAll(newMessages);
				// Adding new labels:
				for(String message : chatMessages) {
					addNewMessageLabel(message, false);
				}
				chatTable.invalidateHierarchy();
			}
			schedulePaneScrolling();
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * @return current users list.
	 */
	public Array<String> getUsersList() {
		return usersList;
	}

	/**
	 * @return current list of users' scores mapped with nicknames.
	 */
	public Map<String,ScoreSet> getUsersScores() {
		return usersScores;
	}

	/**
	 * Replaces current scores list with a new one.
	 * @param scores users' scores mapped with nicknames.
	 */
	public void setUsersScores(Map<String,ScoreSet> scores) {
		usersScores.clear();
		usersScores.putAll(scores);
	}

	/**
	 * Sets a new users list.
	 * @param usersList new users list.
	 */
	public void setUsersList(Array<String> usersList) {
		// Clearing and replacing current users list:
		this.usersList.clear();
		this.usersList.addAll(usersList);
		// Refreshing users table:
		refreshUsersList();
	}

	/**
	 * @return sending button.
	 */
	public TextButton getSendingButton() {
		return chatSendButton;
	}

	/**
	 * @return chat text field.
	 */
	public TextField getChatTextField() {
		return chatTextField;
	}

	/**
	 * Chat types. Depending on the type, network message packets will be different.
	 * @author MJ
	 */
	public static enum Type {
		LOBBY,GAME;
	}
}
