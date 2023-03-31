package mj.konfigurats.logic;

import com.esotericsoftware.kryonet.Connection;

/**
 * Contains temporary data about a player that entered a game room.
 * @author MJ
 */
public class GameRoomUser {
	private final Connection userConnection;
	private final int index,teamIndex;
	private int kills,deaths;
	
	public GameRoomUser(Connection userConnection,int teamIndex) {
		this.userConnection = userConnection;
		kills = deaths = 0;
		index = userConnection.getID();
		this.teamIndex = teamIndex;
	}
	
	/**
	 * @return team index of the player or negative number.
	 */
	public int getTeamIndex() {
		return teamIndex;
	}
	
	/**
	 * @param packet will be sent through the player's connection.
	 */
	public void sendTCP(Object packet) {
		userConnection.sendTCP(packet);
	}
	
	/**
	 * @param packet will be sent through the player's connection.
	 */
	public void sendUDP(Object packet) {
		userConnection.sendUDP(packet);
	}
	
	/**
	 * @return user's KryoNet connection.
	 */
	public Connection getUserConnection() {
		return userConnection;
	}
	
	/**
	 * @return current kills amount.
	 */
	public int getKills() {
		return kills;
	}
	
	/**
	 * @return current deaths amount.
	 */
	public int getDeaths() {
		return deaths;
	}
	
	@Override
	public int hashCode() {
		return index;
	}
	
	/**
	 * Increments kills amount.
	 */
	public void addKill() {
		kills++;
	}
	
	/**
	 * Increments deaths amount.
	 */
	public void addDeath() {
		deaths++;
	}
	
	@Override
	public String toString() {
		return userConnection.toString();
	}
}
