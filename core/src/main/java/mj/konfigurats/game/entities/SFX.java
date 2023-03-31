package mj.konfigurats.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import mj.konfigurats.Core;
import mj.konfigurats.game.utilities.SpellUtils;
import mj.konfigurats.gui.SoundUtilities;

/**
 * Entity for a single special effect.
 * @author MJ
 */
public class SFX extends AbstractEntity {
	private final static float SHAKE_THRESHOLD = 128f;

	private Player owner;
	private SFXType type;
	private float duration,xOffset,yOffset;
	private boolean isNotFinished;
	private TextureRegion[] animationFrames;

	/**
	 * Constructor used for simple animations linked with the player
	 * that cannot be removed (or, rather, there's no reason to).
	 * These are usually short indicators of a spell cast or damage.
	 * @param type animation's type.
	 * @param owner the player linked with the animation.
	 */
	public SFX(SFXType type,Player owner,float duration) {
		this(type, new Vector2(owner.getX(),owner.getY()+1f),duration);
		this.owner = owner;
		owner.addSFX(this);
	}

	/**
	 * Constructor used for an animation with a static position.
	 * These don't have an unique entity index, as there's no reason
	 * to remove them. Usually these are explosions etc.
	 * @param type animation's type.
	 * @param position animation's static position.
	 */
	public SFX(SFXType type,Vector2 position,float duration) {
		super(0, position);
		this.type = type;
		isNotFinished = true;

		// Getting animation and frames length info:
		animationLength = type.getAnimationLength();
		if(duration <= 0f) {
			this.duration = type.getDuration();
		}
		else {
			this.duration = duration;
		}
		// Calculating image offsets:
		xOffset = -(((float)type.getWidth())/2f);
		yOffset = -(((float)type.getHeight())/2f);
		// Getting animation frames:
		animationFrames = SpellUtils.getSFXAnimationFrames
			(type.getSFXIndex());

		// Shaking camera if it's an explosion:
		if(type.shakesCamera()) {
			Player userAvatar = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
				.getBattleManager().getUserCharacter();
			if(userAvatar != null) {
				if(SpellUtils.getDistance(position.x, position.y,
					userAvatar.getX(), userAvatar.getY()) <= SHAKE_THRESHOLD)
					((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
						.getBattleManager().shakeCamera();
			}
		}

		// Playing SFX sound:
		if(type.playsSound()) {
			SoundUtilities.playGameSound(type.getSFXIndex(), position);
		}
	}
	@Override
	public void render(SpriteBatch spriteBatch, float delta) {
		if(isNotFinished) {
			super.render(spriteBatch, delta);

			// Checking if the SFX is finished:
			duration -= delta;
			if(duration <= 0f) {
				isNotFinished = false;
				if(owner != null) {
					owner.removeSFX(this);
				}
				return;
			}

			// Setting current frame:
			while(timePassed >= animationLength) {
				timePassed -= animationLength;
				currentFrame++;
				currentFrame %= type.getFramesAmount();
			}

			spriteBatch.draw(animationFrames[currentFrame],x+xOffset,y+yOffset);
		}
	}

	@Override
	public void setUpdated(boolean updated) {
		// We don't let no game threads screw with our animations!
	}

	@Override
	public boolean wasUpdated() {
		return isNotFinished;
	}

	/**
	 * When the SFX is attached to a player, it should always be displayed
	 * behind or in front of the player. This is the offset that corrects
	 * its y position.
	 * @return y offset that should be added to the SFX x position.
	 */
	public float getYOffset() {
		return type.isDisplayedBehindPlayer() ? 1f : -1f;
	}

	/**
	 * Sets the SFX position without the need to use a Vector2.
	 * @param x position's x.
	 * @param y position's y.
	 */
	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * List of current SFX animations.
	 * @author MJ
	 */
	public static enum SFXType {
		/** Fire. */
		FIREBALL_CAST(0,"fire",4,64,64,4f/9f,1f/9f,false,false,true),
		FIREBALL_EXPLOSION(1,"fire",12,256,128,1f,1f/12f,false,true,true),
		METEOR_WARNING(2,"fire",20,168,148,2f,1f/24f,false,false,true),
		METEOR_EXPLOSION(3,"fire",6,256,128,6f/15f,1f/15f,false,true,true),
		BLAZING_FEET_BUFF(4,"fire",4,64,64,4f/15f,1f/15f,true,false,true),
		BLAZING_FEET_EFFECT(5,"fire",32,98,172,3.05f,3.05f/32f,false,false,false),
		CURSE_CAST(6,"fire",4,64,64,4f/9f,1f/9f,false,false,true),
		CURSE_BUFF(7,"fire",4,64,64,4f/15f,1f/15f,true,false,true),
		MAGIC_MISSILE_EXPLOSION(26,"fire",16,128,128,1f,1f/16f,false,true,true),
		/** Water. */
		HEAL_BUFF(8,"water",6,64,64,6f/9f,1f/9f,false,false,true),
		FREEZE_CAST(9,"water",4,64,64,4f/9f,1f/9f,false,false,true),
		FREEZE_BUFF(10,"water",4,64,94,4f,1f,false,false,true),
		ICE_BLOCK_CAST(11,"water",4,64,64,4f/9f,1f/9f,false,false,true),
		ICE_BLOCK_EXPLOSION(12,"water",4,64,64,4f/15f,1f/15f,false,false,true),
		SHIELD_BUFF(16,"water",4,128,128,4f/125f,1f/15f,true,false,true),
		PULSE_EXPLOSION(27,"water",8,162,162,8f/16f,1f/16f,false,false,true),
		CONFUSION_BUFF(29,"water",4,64,64,4f/15f,1f/15f,true,false,true),
		SILENCE_BUFF(31,"water",4,64,64,4f/15f,1f/15f,true,false,true),
		/** Earth. */
		QUAKE_EXPLOSION(15,"earth",6,256,128,6f/12f,1f/12f,false,true,true),
		POISON_CAST(13,"earth",4,64,64,4f/9f,1f/9f,false,false,true),
		POISON_EXPLOSION(14,"earth",12,256,128,12f/12f,1f/12f,false,false,true),
		ENTANGLE_CAST(17,"earth",4,64,64,4f/9f,1f/9f,false,false,true),
		ENTANGLE_BUFF(18,"earth",2,48,47,1f,1f,false,false,true),
		CURE_BUFF(19,"earth",4,64,64,4f/15f,1f/15f,true,false,true),
		/** Air. */
		HASTE_BUFF(20,"air",4,64,64,4f/15f,1f/15f,true,false,true),
		LEAP_CAST(21,"air",10,128,128,10f/12f,1f/12f,false,false,true),
		LIGHTNING_CAST(22,"air",20,56,65,0.8f,0.8f/20f,false,false,true),
		LIGHTNING_EXPLOSION(23,"air",4,64,64,4f/9f,1f/9f,false,false,true),
		TORNADO_CAST(24,"air",4,64,64,4f/9f,1f/9f,false,false,true),
		SWAP_CAST(28,"air",22,98,340,1.5f,1.5f/22,false,false,true),
		TAUNT_BUFF(30,"air",6,64,64,6f/9f,1f/9f,false,false,true),
		/** Other. */
		SPAWN(25,"spawn",19,50,47,1f,1f/19f,false,false,true),
		TELEPORT(32,"spawn",32,80,67,Float.MAX_VALUE,1f/50f,false,false,false),
		EMPTY(33,"spawn",1,1,1,0f,0f,false,false,false);

		private final int SFXIndex,framesAmount,width,height;
		private final String SFXName;
		private final float duration,animationLength;
		private final boolean displayedBehindPlayer,shakesCamera,playsSound;
		private SFXType(int SFXIndex,String SFXName,int framesAmount,
			int width,int height,float duration,float animationLength,
			boolean displayedBehindPlayer, boolean shakesCamera, boolean playsSound) {
			this.SFXIndex = SFXIndex;
			this.SFXName = SFXName;
			this.framesAmount = framesAmount;
			this.animationLength = animationLength;
			this.duration = duration;
			this.width = width;
			this.height = height;
			this.displayedBehindPlayer = displayedBehindPlayer;
			this.shakesCamera = shakesCamera;
			this.playsSound = playsSound;
		}

		/**
		 * @return animation's index.
		 */
		public int getSFXIndex() {
			return SFXIndex;
		}

		/**
		 * @return animation's image name.
		 */
		public String getSFXName() {
			return SFXName;
		}

		/**
		 * @return amount of frames in the current animation.
		 */
		public int getFramesAmount() {
			return framesAmount;
		}

		/**
		 * @return width of a single frame.
		 */
		public int getWidth() {
			return width;
		}

		/**
		 * @return height of a single frame.
		 */
		public int getHeight() {
			return height;
		}

		/**
		 * @return total animation length in seconds.
		 */
		public float getDuration() {
			return duration;
		}

		/**
		 * @return length of a single frame in seconds.
		 */
		public float getAnimationLength() {
			return animationLength;
		}

		/**
		 * @return true if the animation is usually attached to a player
		 * and has to be displayed behind him.
		 */
		public boolean isDisplayedBehindPlayer() {
			return displayedBehindPlayer;
		}

		/**
		 * @return if returns true, the SFX on creation - if it is close enough
		 * to the player - should shake the camera.
		 */
		public boolean shakesCamera() {
			return shakesCamera;
		}

		/**
		 * @return true if there's a sound effect attached to the SFX.
		 */
		public boolean playsSound() {
			return playsSound;
		}

		/**
		 * Finds SFX type for the given index.
		 * @param index SFX index.
		 * @return SFX type, null for invalid index.
		 */
		public static SFXType getSFXType(int index) {
			for(SFXType sfx : SFXType.values()) {
				if(sfx.getSFXIndex() == index) {
					return sfx;
				}
			}
			return null;
		}
	}
}
