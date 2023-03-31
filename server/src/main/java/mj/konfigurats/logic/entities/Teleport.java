package mj.konfigurats.logic.entities;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.physics.BodyInformation;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public class Teleport implements Entity {
	private final int teleportIndex;
	private final Game game;
	private final Body teleportBody;
	private final Vector2 position;
	
	public Teleport(int teleportIndex,Vector2 position,Game game) {
		this.teleportIndex = teleportIndex;
		this.position = new Vector2(position);
		this.game = game;
		// Creating teleport body:
		teleportBody = BodyInformation.TELEPORT.createNewBody(game.getBox2DWorld(),
			game.getCircleShape(), this, this.position.x, this.position.y);
	}

	@Override
	public void update(float delta, Game game) {
		// Never updated.
	}

	@Override
	public int getEntityIndex() {
		// This is actually a teleport index, NOT an entity index - there could be
		// other entities with same indexes as teleport data is not send in the packets.
		return teleportIndex;
	}
	
	/**
	 * @return the game that the teleport belongs to.
	 */
	public Game getGame() {
		return game;
	}
	
	/**
	 * @return position of the teleport.
	 */
	public Vector2 getPosition() {
		return position;
	}

	@Override
	public void destroy(Game game) {
		// Generally not used, since teleports cannot be destroyed.
		teleportBody.getWorld().destroyBody(teleportBody);
	}
}
