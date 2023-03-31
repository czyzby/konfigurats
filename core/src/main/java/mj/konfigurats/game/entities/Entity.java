package mj.konfigurats.game.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

/**
 * Interface of a single game entity. Should be implemented by every
 * object shown on the game screen, so it could be updated and drawn.
 * @author MJ
 */
public interface Entity extends Comparable<Entity> {
	/**
	 * @return index of the entity selected by the server.
	 * For most entities (players, projectiles, buffs SFX) the index is
	 * unique, but some never need to be identified and will return 0.
	 */
	public int getEntityIndex();
	
	/**
	 * Sets the current position of the entity's sprite.
	 * Takes a Vector2 to simplify conversion of Box2D coordinates.
	 * @param newPosition new position of the sprite on the map.
	 */
	public void updatePosition(Vector2 newPosition);
	
	/**
	 * During updating the game world, this method is used
	 * to mark the sprite as updated or to reset it's update status.
	 * @param updated true to set updated, false to reset.
	 */
	public void setUpdated(boolean updated);
	
	/**
	 * @return true if the entity was mentioned in a world update.
	 * Usually called right after processing the update packet
	 * to see if the entity should be removed or not.
	 */
	public boolean wasUpdated();
	
	/**
	 * Draws the entity on the screen at its last updated position.
	 * @param spriteBatch the sprite will be drawn using this batch.
	 * @param delta time passes since the last update.
	 */
	public void render(SpriteBatch spriteBatch, float delta);
	
	/**
	 * Needed to sort the entities' sprites.
	 * @return current Y position.
	 */
	public float getY();
}
