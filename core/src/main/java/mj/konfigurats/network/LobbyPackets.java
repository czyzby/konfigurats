package mj.konfigurats.network;

/**
 * Utility class containing lobby network packets. Do <b>not</b> try to initialize.
 * Srv packets are sent by the server, Clt are sent by clients, Pct are utility classes - they contain some informations for other packets and are never send individually.
 * Interfaces represent different groups of packets, usually used for different purposes and handled by different managers (and threads).
 * @author MJ
 */
public class LobbyPackets {
	private LobbyPackets() {}
	
	/**
	 * Lobby packets are used to manage game rooms and handle the lobby chat.
	 */
	public static interface LobbyPacket {}
	
	// Logging packets:
	/**
	 * Sent by the client the user is trying to log out in the lobby.
	 */
	public static class CltLobbyLogOut implements LobbyPacket {}
	
	/**
	 * Sent by the server when the user is successfully logger out.
	 */
	public static class SrvLobbyLoggedOut implements LobbyPacket {}
	
	// Lobby chat packets:
	/**
	 * A single message sent by an user.
	 */
	public static class CltLobbyMessage implements LobbyPacket {
		public String message;
	}
	
	/**
	 * A single private message sent by an user to a user.
	 */
	public static class CltPrivateMessage implements LobbyPacket {
		public String message,target;
	}
	
	/**
	 * A message that is to be displayed on the user's lobby chat.
	 */
	public static class SrvLobbyMessage implements LobbyPacket {
		public String message;
	}
	
	/**
	 * Tells the client to add a username to the lobby users list.
	 */
	public static class SrvAddLobbyUser implements LobbyPacket {
		public String username;
	}
	
	/**
	 * Tells the client to remove a username from the lobby users list.
	 */
	public static class SrvRemoveLobbyUser implements LobbyPacket {
		public String username;
	}
	
	// Game rooms management packets:
	/**
	 * Contains name and amount of players of a single room.
	 */
	public static class PctGameInfo implements LobbyPacket {
		public String name;
		public char mode;
		public int players,limit;
	}
	
	/**
	 * Player's request to create a new game room.
	 */
	public static class CltCreateGame implements LobbyPacket {
		public String name,password;
		public int mapIndex,gameMode;
	}
	
	/**
	 * Sent by the player's client after trying to join a random game.
	 */
	public static class CltJoinRandomGame implements LobbyPacket {}
	
	/**
	 * Sent by the player's client after trying to join a specific game.
	 */
	public static class CltJoinGame implements LobbyPacket {
		public String name;
	}
	
	/**
	 * Sent by the player's client after trying to join a specific player.
	 */
	public static class CltJoinPlayer implements LobbyPacket {
		public String username;
	}
	
	/**
	 * Sent by the client when the player is trying to join a password protected room.
	 */
	public static class CltRoomPassword implements LobbyPacket {
		public String gameRoomName, password;
	}
	
	/**
	 * Contains informations about a single game.
	 */
	public static class SrvUpdateGame implements LobbyPacket {
		public PctGameInfo game;
	}
	
	/**
	 * Sent by the server to tell the clients to remove a game from their lists.
	 */
	public static class SrvRemoveGame implements LobbyPacket {
		public String name;
	}
	
	/**
	 * Sent by the server to make the player join a specific game.
	 */
	public static class SrvStartGame implements LobbyPacket {
		public String name;
		public int mapIndex,roomIndex;
	}
	
	/**
	 * Sent by the server when the player is trying to join a room that requires
	 * a password.
	 */
	public static class SrvEnterRoomPassword implements LobbyPacket {
		public String gameRoomName;
	}
	
	/**
	 * Sent by the server when the player enters wrong room password.
	 */
	public static class SrvInvalidRoomPassword implements LobbyPacket {}
	
	/**
	 * Sent to the client when the player is trying to create a room with a name that is taken.
	 */
	public static class SrvGameAlreadyExists implements LobbyPacket {}
	
	/**
	 * Sent to the client when the room he has chosen is full.
	 */
	public static class SrvGameFull implements LobbyPacket {}
	
	/**
	 * Sent to the client when the room he has chosen doesn't exist.
	 */
	public static class SrvGameNotFound implements LobbyPacket {}
	
	/**
	 * Sent to the client when the player he's trying to join doesn't exist or isn't playing.
	 */
	public static class SrvPlayerNotFound implements LobbyPacket {}
	
	/**
	 * Sent by the server if there are no open game rooms.
	 */
	public static class SrvNoGamesOpen implements LobbyPacket {}
	
	/**
	 * Sent by the server when it receives a corrupted map index.
	 */
	public static class SrvWrongMapIndex implements LobbyPacket {}
	
	// Ranking packets:
	/**
	 * Sent by the client when the user tries to access ranking.
	 */
	public static class CltShowRanking implements LobbyPacket {}
	
	/**
	 * Contains data of the top players.
	 */
	public static class SrvRankingData implements LobbyPacket {
		public String[] topKills,topDeaths,topRatio;
		public int userKills,userDeaths;
	}
}