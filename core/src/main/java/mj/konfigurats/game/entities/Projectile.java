package mj.konfigurats.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import mj.konfigurats.Core;
import mj.konfigurats.game.entities.SFX.SFXType;
import mj.konfigurats.game.utilities.SpellUtils;

public class Projectile extends AbstractEntity {
	public final static int PROJECTILE_FRAMES_AMOUNT = 8,
							PROJECTILE_IMAGE_SIZE = 64;

	// Animation:
	private TextureRegion[] animationFrames;
	// Control variables:
	private final ProjectileType type;
	private float angle;

	/**
	 * Creates a new projectile.
	 * @param entityIndex projectile's unique entity index.
	 * @param type projectile's animation type.
	 * @param initialPosition projectile's initial position.
	 * @param initialAngle projectile's initial angle. <0,360)
	 * If the projectile is never rotated, this value will be ignored.
	 */
	public Projectile(int entityIndex,ProjectileType type,
		Vector2 initialPosition,float initialAngle) {
		super(entityIndex,initialPosition);
		this.type = type;
		this.angle = initialAngle;

		// Getting images:
		animationFrames = SpellUtils.getProjectileAnimationFrames
			(type.getAnimationIndex());

		// Displaying creation SFX:
		((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getBattleManager().addSFX(new SFX(type.getCreationSFX(),initialPosition,-1f));
	}

	@Override
	public void render(SpriteBatch spriteBatch, float delta) {
		super.render(spriteBatch, delta);

		// Updating animations frame:
		while(timePassed >= animationLength) {
			timePassed -= animationLength;
			currentFrame++;
			currentFrame %= PROJECTILE_FRAMES_AMOUNT;
		}

		// If the projectile requires rotating - apply the effect:
		if(type.isRotating()) {
			spriteBatch.draw(animationFrames[currentFrame],
				x+type.getXOffset(),y+type.getYOffset(),
				-type.getXOffset(),-type.getYOffset(),
				PROJECTILE_IMAGE_SIZE,PROJECTILE_IMAGE_SIZE,
				1f,1f,angle);
		}
		// If it doesn't, just draw it:
		else {
			spriteBatch.draw(animationFrames[currentFrame],
				x+type.getXOffset(),y+type.getYOffset());
		}
	}

	/**
	 * @return current projectile's angle.
	 */
	public float getAngle() {
		return angle;
	}

	/**
	 * Sets a new projectile's angle.
	 * @param angle <0,360).
	 */
	public void setAngle(float angle) {
		this.angle = MathUtils.floor(angle);
	}

	/**
	 * Displays SFX effect after projectile is removed.
	 */
	public void displaySFX() {
		// Adding destruction SFX:
		((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getBattleManager().addSFX(new SFX(type.getDestructionSFX(),new Vector2(x,y),-1f));
	}

	/**
	 * List of all current projectiles.
	 * @author MJ
	 */
	public static enum ProjectileType {
		/** Fire spells. */
		FIREBALL(0,true,-48f,-32f,SFXType.FIREBALL_CAST,SFXType.FIREBALL_CAST),
		CURSE(1,true,-48f,-32f,SFXType.CURSE_CAST,SFXType.CURSE_CAST),
		MAGIC_MISSILE(8,true,-40f,-32f,SFXType.FIREBALL_CAST,SFXType.EMPTY),
		LIFE_STEAL(10,true,-40f,-32f,SFXType.CURSE_CAST,SFXType.CURSE_CAST),
		/** Water spells. */
		FREEZE(2,true,-40f,-32f,SFXType.FREEZE_CAST,SFXType.FREEZE_CAST),
		ICE_BLOCK(3,false,-32f,-20f,SFXType.ICE_BLOCK_CAST,SFXType.ICE_BLOCK_EXPLOSION),
		/** Earth spells. */
		POISON(4,true,-48,-32,SFXType.POISON_CAST,SFXType.POISON_EXPLOSION),
		ENTANGLE(5,true,-48f,-32f,SFXType.ENTANGLE_CAST,SFXType.ENTANGLE_CAST),
		HOMING_ARROW(9,true,-40f,-32f,SFXType.ENTANGLE_CAST,SFXType.ENTANGLE_CAST),
		THORNS(11,true,-32f,-32f,SFXType.EMPTY,SFXType.EMPTY), //TODO?
		/** Air spells. */
		LIGHTNING_BOLT(6,true,-48f,-32f,SFXType.EMPTY,SFXType.LIGHTNING_EXPLOSION),
		TORNADO(7,false,-32f,-16f,SFXType.TORNADO_CAST,SFXType.TORNADO_CAST);

		private final int animationIndex;
		private final float xOffset,yOffset;
		private final boolean isRotating;
		private final SFXType creationSFX,destructionSFX;
		private ProjectileType(int animationIndex, boolean isRotating,
			float xOffset,float yOffset,SFXType creationSFX, SFXType destructionSFX) {
			this.animationIndex = animationIndex;
			this.isRotating = isRotating;
			this.xOffset = xOffset;
			this.yOffset = yOffset;
			this.creationSFX = creationSFX;
			this.destructionSFX = destructionSFX;
		}

		/**
		 * @return animation's index.
		 */
		public int getAnimationIndex() {
			return animationIndex;
		}

		/**
		 * @return true if the animation should be rotated.
		 */
		public boolean isRotating() {
			return isRotating;
		}

		/**
		 * @return projectile's animation x offset needed to match the Box2D body.
		 */
		public float getXOffset() {
			return xOffset;
		}
		/**
		 * @return projectile's animation y offset needed to match the Box2D body.
		 */
		public float getYOffset() {
			return yOffset;
		}

		/**
		 * @return SFX type displayed when the projectile is created.
		 */
		public SFXType getCreationSFX() {
			return creationSFX;
		}

		/**
		 * @return SFX type displayed when the projectile is destroyed.
		 */
		public SFXType getDestructionSFX() {
			return destructionSFX;
		}

		/**
		 * Finds a projectile type connected with the given index.
		 * @param index projectile's animation index.
		 * @return projectile's type.
		 */
		public static ProjectileType getProjectileType(int index) {
			for(ProjectileType projectileType : ProjectileType.values()) {
				if(projectileType.getAnimationIndex() == index) {
					return projectileType;
				}
			}
			return null;
		}
	}
}
