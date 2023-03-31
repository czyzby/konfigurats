package mj.konfigurats.network;


/**
 * Utility class containing game network packets. Do <b>not</b> try to initialize.
 * Srv packets are sent by the server, Clt are sent by clients, Pct are utility classes - they contain some informations for other packets and are never send individually.
 * Interfaces represent different groups of packets, usually used for different purposes and handled by different managers (and threads).
 * @author MJ
 */
public class GamePackets {
	private GamePackets() {}
	
	/**
	 * Game packets are used to manage game rooms and handle the game's logic.
	 */
	public static interface GamePacket {}

	// Logging packets:
	/**
	 * Sent by the client when he's trying to leave the room.
	 */
	public static class CltLeaveGame implements GamePacket {}
	
	/**
	 * Send by the server when it acknowledges that the players is trying to leave. 
	 */
	public static class SrvLeaveGame implements GamePacket {}
	
	// Game chat packets:
	/**
	 * A chat message sent by the client in a game room.
	 */
	public static class CltGameChatMessage implements GamePacket {
		public String message;
	}
	
	/**
	 * A chat message in game sent by a team member.
	 */
	public static class CltTeamMessage implements GamePacket {
		public String message;
	}
	
	/**
	 * Sent by the server, contains a game room chat message.
	 */
	public static class SrvGameChatMessage implements GamePacket {
		public String message;
	}
	
	/**
	 * Tells the client to add a username to the lobby users list.
	 */
	public static class SrvAddRoomUser implements GamePacket {
		public String username;
		public int teamIndex;
	}
	
	/**
	 * Tells the client to remove a username from the lobby users list.
	 */
	public static class SrvRemoveRoomUser implements GamePacket {
		public String username;
	}
	
	/**
	 * Tells the client to update scores of a player in his game room.
	 */
	public static class SrvScoresUpdate implements GamePacket {
		public String nickname;
		public int kills,deaths;
	}
	
	// Game logic packets:
	/**
	 * Send by the client when his battle manager is ready to process game packets.
	 */
	public static class CltGameInitiated implements GamePacket {}
	
	/**
	 * Sent when the player is trying to create a character and enter the game.
	 */
	public static class CltCreateCharacter implements GamePacket {
		public boolean isElite;
		public byte classIndex,fireSpell,waterSpell,earthSpell,airSpell;
	}
	
	/**
	 * Sent each time the player clicks on the map.
	 */
	public static class CltHandleClick implements GamePacket {
		public float x,y;
	}
	
	/**
	 * Sent each time the player is trying to cast a fire spell.
	 * The packet includes current mouse position.
	 */
	public static class CltHandleFireCast implements GamePacket {
		public float x,y;
	}
	
	/**
	 * Sent each time the player is trying to cast a water spell.
	 * The packet includes current mouse position.
	 */
	public static class CltHandleWaterCast implements GamePacket {
		public float x,y;
	}
	
	/**
	 * Sent each time the player is trying to cast an earth spell.
	 * The packet includes current mouse position.
	 */
	public static class CltHandleEarthCast implements GamePacket {
		public float x,y;
	}
	
	/**
	 * Sent each time the player is trying to cast an air spell.
	 * The packet includes current mouse position.
	 */
	public static class CltHandleAirCast implements GamePacket {
		public float x,y;
	}
	
	/**
	 * Sent when the client is trying to create a character with wrong values.
	 */
	public static class SrvCorruptedCreationData implements GamePacket {}
	
	/**
	 * Tells all players in the game room to create a new character.
	 */
	public static class SrvCreateCharacter implements GamePacket {
		public int characterIndex,teamIndex;
		public String playerName;
		public byte characterClass;
		public boolean isElite;
		public float x,y;
	}
	
	/**
	 * Sent by the server when the player is trying to create an elite character.
	 */
	public static class SrvPlayerNotElite implements GamePacket {}
	
	/**
	 * A single box2D world update. Contains room index, update index, current health
	 * of the player and informations about all entities on the battlefield.
	 */
	public static class SrvUpdateWorld implements GamePacket {
		// Final game's room index (game room's name hash code).
		public int gameRoomIndex;
		// Index of the update.
		public long updateIndex;
		// Current player's health.
		public float currentHealth,summonHealth;
		
		// Indexes of the characters on the arena:
		public int[] charactersIndexes;
		// Positions of the characters:
		public float[] charactersPositions;
		// Display data: 0. character's direction, 1. character's animation index.
		public byte[] charactereDisplayData;
		
		// Indexes of the projectiles on the arena:
		public int[] projectileIndexes;
		// Indexes of the projectiles' animations:
		public byte[] projectileAnimations;
		// Display data: 0-1. position, 2-3. linear velocity.
		public float[] projectileDisplayData;
	}
	
	/**
	 * Sent by the server to let the player know that he successfully
	 * cast a spell and now has to wait to recast it.
	 */
	public static class SrvSetSpellCooldown implements GamePacket {
		public byte spellType;
		public float cooldown;
	}
	
	/**
	 * Tells the client to display a SFX effect in the given position.
	 */
	public static class SrvDisplaySFX implements GamePacket {
		public int SFXIndex;
		public float x,y;
	}
	
	/**
	 * Tells the client to attach a SFX effect to a character for a given duration.
	 */
	public static class SrvAttachSFX implements GamePacket {
		public int SFXIndex,entityIndex;
		public float duration;
	}
	
	/**
	 * Tells the client to scale down a summon or a player and update his position.
	 */
	public static class SrvSetEntityFalling implements GamePacket {
		public int entityIndex;
		public float x,y;
	}
	
	/**
	 * Tells the client that the map has changed and he has to reset his battle manager.
	 */
	public static class SrvSwitchMap implements GamePacket {
		public String name;
		public int mapIndex,roomIndex;
	}
}