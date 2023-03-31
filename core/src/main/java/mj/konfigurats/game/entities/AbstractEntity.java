package mj.konfigurats.game.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

/**
 * A simple entity implementation. Handles assigning index, setting
 * positions and info about the updates. Default render implementation
 * increases time passed by the delta. Class' hash code is the entity
 * index - as long as the entity actually receives an unique index
 * from the server, it's safe to hash.
 * @author MJ
 */
public abstract class AbstractEntity implements Entity {
	// Entity index given by the server:
	private final int entityIndex;
	// Control variables:
	protected float x,y,timePassed,animationLength;
	protected boolean wasUpdated;
	protected int currentFrame;
	
	public AbstractEntity(int entityIndex,Vector2 initialPosition) {
		this.entityIndex = entityIndex;
		this.x = initialPosition.x;
		this.y = initialPosition.y;
		
		// Setting default animation data:
		currentFrame = 0;
		timePassed = 0f;
		animationLength = 1f/9f;
	}

	@Override
	public int getEntityIndex() {
		return entityIndex;
	}

	@Override
	public void updatePosition(Vector2 newPosition) {
		this.x = newPosition.x;
		this.y = newPosition.y;
	}

	@Override
	public void setUpdated(boolean updated) {
		wasUpdated = updated;
	}

	@Override
	public boolean wasUpdated() {
		return wasUpdated;
	}
	
	@Override
	public void render(SpriteBatch spriteBatch, float delta) {
		// Updating time passed since the last update by the delta:
		timePassed += delta;
	}
	
	@Override
	public int hashCode() {
		return entityIndex;
	}
	
	@Override
	public float getY() {
		return y;
	}
	
	@Override
	public int compareTo(Entity entity) {
		return Float.compare(entity.getY(), this.y);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Entity) {
			return ((Entity)obj).getEntityIndex() == entityIndex;
		}
		else return false;
	}
}
