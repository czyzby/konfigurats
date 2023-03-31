package mj.konfigurats.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import mj.konfigurats.Core;
import mj.konfigurats.game.entities.SFX.SFXType;
import mj.konfigurats.game.utilities.PlayerUtils;

public class Player extends AbstractEntity {
	public final static float SPRITE_SIZE = 128f,
							  PLAYER_X_OFFSET = -64f,
							  PLAYER_Y_OFFSET = -32f,
							  FONT_Y_OFFSET = -8;

	// Player's data:
	private final String nickname;
	private final PlayerClass playerClass;
	private final boolean isElite;
	private boolean isDead,displayingCorpse,wasAlreadyUpdated;
	// Player's images:
	private final TextureRegion[][][] animationFrames;
	private final Array<SFX> SFXEffects;
	// Player's nickname font:
	private final BitmapFont font;
	private final float fontOffset;
	private final Color fontColor;
	// Control variables:
	private PlayerAnimationType animationType,scheduledType;
	private boolean goingForward,isFirstLoop,isFalling;
	private int direction;
	private float scale;
	private GlyphLayout glyphLayout = new GlyphLayout();

	/**
	 * Creates a new player on the battlefield.
	 * @param entityIndex player's entity index chosen by the server.
	 * @param nickname username of the player in control of the character.
	 * @param isElite true to display elite sprite, false for regular.
	 * @param playerClass character's class.
	 * @param initialPosition initial position of the player's sprite.
	 */
	public Player(int entityIndex, int teamIndex, String nickname, boolean isElite,
			PlayerClass playerClass, Vector2 initialPosition) {
		super(entityIndex, initialPosition);

		// Assigning player's data:
		this.nickname = nickname;
		this.isElite = isElite;
		this.playerClass = playerClass;
		this.isDead = this.displayingCorpse = this.wasAlreadyUpdated = false;

		// Getting animation frames:
		if(this.isElite) {
			animationFrames = PlayerUtils.getPlayerAnimationSet
				(this.playerClass.getEliteIndex());
		}
		else {
			animationFrames = PlayerUtils.getPlayerAnimationSet
				(this.playerClass.getIndex());
		}
		// Setting animation data:
		changeAnimationType(PlayerAnimationType.STANCE);
		setDirection(5);
		scale = 1f;
		isFalling = false;

		// Setting up the nickname's font:
		font = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getAsset("global/fonts/courier.fnt", BitmapFont.class);
		this.glyphLayout.setText(font, this.nickname);

		fontOffset = -(glyphLayout.width/2f);
		if(teamIndex < 0) {
			// Standard mode:
			if(this.nickname.equals(((Core)Gdx.app.getApplicationListener())
				.getNetworkManager().getUsername()) || this.nickname.startsWith
				(((Core)Gdx.app.getApplicationListener()).getNetworkManager()
				.getUsername()+"'")) {
				// If the player is the owner of the character - highlight:
				fontColor = new Color(0.5f,0f,0f,1f);
			}
			else {
				// If not, assign regular color:
				fontColor = new Color(0.4f,0.4f,0.3f,1f);
			}
		}
		else {
			//Team mode:
			if(teamIndex == 0) {
				// Team 1:
				if(this.nickname.equals(((Core)Gdx.app.getApplicationListener())
					.getNetworkManager().getUsername()) || this.nickname.startsWith
					(((Core)Gdx.app.getApplicationListener()).getNetworkManager()
					.getUsername()+"'")) {
					// If the player is the owner of the character - highlight:
					fontColor = new Color(0.5f,0f,0f,1f);
				}
				else {
					fontColor = new Color(0.7f,0f,0.1f,1f);
				}
			}
			else {
				// Team 2:
				if(this.nickname.equals(((Core)Gdx.app.getApplicationListener())
					.getNetworkManager().getUsername()) || this.nickname.startsWith
					(((Core)Gdx.app.getApplicationListener()).getNetworkManager()
					.getUsername()+"'s")) {
					// If the player is the owner of the character - highlight:
					fontColor = new Color(0.55f,0.45f,0f,1f);
				}
				else {
					fontColor = new Color(0.8f,0.65f,0f,1f);
				}
			}
		}

		// Creating SFX array and adding spawning SFX:
		SFXEffects = new Array<SFX>();
		((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getBattleManager().addSFX(new SFX(SFXType.SPAWN,this,-1f));
	}

	/**
	 * @param sfx attaches this SFX to the character.
	 */
	public void addSFX(SFX sfx) {
		SFXEffects.add(sfx);
	}

	/**
	 * @param sfx removes this SFX from the character's SFX list.
	 */
	public void removeSFX(SFX sfx) {
		SFXEffects.removeValue(sfx, true);
	}

	/**
	 * @return list of SFX currently attached to the player.
	 */
	public Array<SFX> getSFXEffects() {
		return SFXEffects;
	}

	@Override
	public void updatePosition(Vector2 newPosition) {
		super.updatePosition(newPosition);

		if(!wasAlreadyUpdated) {
			wasAlreadyUpdated = true;
		}

		for(SFX sfx : SFXEffects) {
			sfx.setPosition(newPosition.x, newPosition.y+sfx.getYOffset());
		}
	}

	/**
	 * @return true if the character was mentioned in at least one packet.
	 */
	public boolean wasAlreadyUpdated() {
		return wasAlreadyUpdated;
	}

	/**
	 * When the character is absent in the packet, it means he died.
	 * By setting the player as dead, this object will start to render
	 * dying animations and eventually remove itself from the player list.
	 */
	public void setDead() {
		isDead = true;
	}

	/**
	 * When the player is dying due to falling into the void, his sprite will be
	 * scaled down.
	 */
	public void setFalling() {
		isFalling = true;
	}

	/**
	 * @param direction new player's direction. <0,7>
	 */
	public void setDirection(int direction) {
		this.direction = direction;
	}

	/**
	 * @return current x position.
	 */
	public float getX() {
		return x;
	}

	/**
	 * Sets the current animation.
	 * @param animationType animation type. Use this class' enum.
	 */
	public void setAnimation(PlayerAnimationType animationType) {
		if(this.animationType != animationType) {
			// Animation is played once - checking priority:
			if(animationType.isPlayedOnce()) {
				if(animationType.getPriority() >= this.animationType.getPriority()) {
					changeAnimationType(animationType);
				}
			}
			// Animation is constant - trying to ignore priority:
			else {
				// Current animation is not constant: cannot ignore priority:
				if(this.animationType.isPlayedOnce()) {
					// Current animation has lower priority:
					if(animationType.getPriority() >= this.animationType.getPriority()) {
						changeAnimationType(animationType);
					}
					// Current animation has higher priority:
					else {
						// Scheduling the next animation:
						scheduledType = animationType;
					}
				}
				else {
					// Current animation is constant: ignoring priority:
					changeAnimationType(animationType);
				}
			}
		}
	}

	private void changeAnimationType(PlayerAnimationType animationType) {
		// Switching from "constant" animation to a one-time-display:
		if(this.animationType != null && !this.animationType.isPlayedOnce()) {
			// Scheduling the return of the last constant animation:
			scheduledType = this.animationType;
		}
		// Setting new animation type:
		this.animationType = animationType;
		// Resetting control variables:
		timePassed = currentFrame = 0;
		goingForward = true;
		isFirstLoop = true;
	}

	@Override
	public void render(SpriteBatch spriteBatch, float delta) {
		super.render(spriteBatch, delta);

		validateScale(delta);
		validateCurrentFrame();

		// If current frame exists:
		if(animationFrames[animationType.getIndex()][direction]
			[currentFrame] != null) {
			// Drawing player's sprite's frame:
			spriteBatch.draw(animationFrames[animationType.getIndex()]
				[direction][currentFrame],x+PLAYER_X_OFFSET*scale,
				y+PLAYER_Y_OFFSET*scale,SPRITE_SIZE*scale,SPRITE_SIZE*scale);
			// Drawing player's nickname:
			if(font != null) {
				font.setColor(fontColor);
				font.draw(spriteBatch, nickname, x+fontOffset, y+FONT_Y_OFFSET);
			}
		}
	}

	/**
	 * Changing sprite's size if the player is falling.
	 * @param delta time past since the last update.
	 */
	private void validateScale(float delta) {
		if(isFalling) {
			if(scale > 0f) {
				// Changing scale:
				scale -= delta/2f;
				if(scale <= 0f) {
					scale = 0f;
				}
			}
		}
	}

	private void validateCurrentFrame() {
		// Player did not appear in the packet - displaying death animation:
		if(isDead) {
			// Death animation is already set:
			if(displayingCorpse) {
				// Changing death animation frames:
				if(currentFrame != animationType.getFramesAmount()-1) {
					while(timePassed >= animationLength) {
						timePassed -= animationLength;
						currentFrame++;
						if(currentFrame == animationType.getFramesAmount()-1) {
							// A lag occurred and the game is trying to set too many frames:
							break;
						}
					}
				}
				// Final frame:
				else {
					// Displaying corpse for about 20 frames:
					if(timePassed >= animationLength*20) {
						// Removing player from the list:
						((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
							.getBattleManager().getPlayersToRemove().add(this);
						// If the player didn't fall into void...
						if(!isFalling) {
							//...displaying SFX:
							((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
								.getBattleManager().addSFX(new SFX(SFXType.SPAWN,new Vector2(x,y),-1f));
						}
					}
				}
			}
			// Setting death animation:
			else {
				displayingCorpse = true;
				changeAnimationType(PlayerAnimationType.DEATH);
			}
		}
		// Player appeared in the packet - displaying current animation:
		else {
			// Changing animation frames:
			while(timePassed >= animationLength) {
				timePassed -= animationLength;

				// Looping (yoyo) animation:
				if(animationType.isLooping()) {
					if(goingForward) {
						// Going forward:
						currentFrame++;

						// Last frame - going backwards:
						if(currentFrame == animationType.getFramesAmount()-1) {
							goingForward = false;
						}

						// If the animation is played once and it finished - switching to scheduled type:
						if(animationType.isPlayedOnce && !isFirstLoop && currentFrame == 1) {
							changeAnimationType(scheduledType);
						}

						isFirstLoop = false;
					}
					else {
						// Going backwards:
						currentFrame--;

						// First frame - going forward:
						if(currentFrame == 0) {
							goingForward = true;
						}
					}
				}
				else {
					// Regular animation:
					currentFrame = (currentFrame+1)%animationType.getFramesAmount();

					// Animation is played once and it finished - switching animation to scheduled:
					if(animationType.isPlayedOnce && currentFrame == 0) {
						setAnimation(scheduledType);
					}
				}
			}
		}
	}

	/**
	 * Contains all current players' characters' types.
	 * @author MJ
	 */
	public static enum PlayerClass {
		LICH(0,"Lich","Ancient Lich",false,
			"Cursed to live an eternal life,\nthe liches had many years to perfect\ntheir art of magic. In pursuit of\nunimaginable power, these dreadful mages\nbecame victims of their own dark rituals.",
			"Ancient liches have (un)lived\nenough ages to gather powerful artifacts\nthat aid their magical abilities.\nMore dangerous than ever, these mages\nare thirsty for challange and... blood.",
			"Resistance: low. Damage: very high.\nRecast time: long. Speed: low.\nMass: low."),
		GOBLIN(1,"Goblin Shaman","Hobgoblin Witchdoctor",false,
			"While it may seem unbelievable that\nthese little creatures are capable of\nfighting mighty warlocks, goblin shamans\nare known to unleach fearsome\nnature's hidden powers.",
			"Stronger and smarter than their\ngreen counterparts, hobgoblins are\namong the most powerful shamans\nthat have entered the arena.\nDo not underestimate them!",
			"Resistance: very low. Damage: low.\nRecast time: very short. Speed: very high.\nMass: very low."),
		WITCH(2,"Witch","Sorceress",false,
			"Have you heard about their curses?\nWell, now you surely will. Sinister\nand envious, witches contact demons\nand evil spirits to aid them in battle\nand to charm their enemies.",
			"Sorceresses are chosen by their\nsisters to rule their secret covens.\nDeadly and charming, they will bring\na slow, painful death to all that\ndare to oppose them.",
			"Resistance: medium. Damage: medium.\nRecast time: short. Speed: high.\nMass: medium."),
		JUGGERNAUT(3,"Juggernaut","Elite Juggernaut",false,
			"Studying both science and magic,\njuggernauts enter battles wearing their\nresistant plate armors. Ready to kill\nanyone standing in their way,\nthey are indeed formidable opponents.",
			"The most successful juggernauts\ncall themselves the \"elite\" of their kind.\nUsing modern armors and impenetrable\nshields, they are nearly impossible\nto kill by a mere mortal.",
			"Resistance: extremely high. Damage: low.\nRecast time: long. Speed: medium.\nMass: high."),
		MINOTAUR(8,"Minotaur","",true,"","",""),
		FIRE_ANT(9,"Fire Ant","",true,"","","");

		private final static int SUMMONS_AMOUNT = 2;
		private final int index;
		private final boolean isSummon;
		private final String name, eliteName, description, eliteDescription, statictics;
		private PlayerClass(int index,String name,String eliteName, boolean isSummon,
				String description,String eliteDescription,String statictics) {
			this.index = index;
			this.name = name;
			this.eliteName = eliteName;
			this.isSummon = isSummon;
			this.description = description;
			this.eliteDescription = eliteDescription;
			this.statictics = statictics;
		}

		/**
		 * @return amount of summons among the classes.
		 */
		public static int getSummonsAmount() {
			return SUMMONS_AMOUNT;
		}

		/**
		 * @return index connected with the chosen class.
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @return false if the character is a regular class and can be
		 * chosen by a player.
		 */
		public boolean isSummon() {
			return isSummon;
		}

		/**
		 * @return index of the elite sprite.
		 */
		public int getEliteIndex() {
			return index+PlayerClass.values().length-PlayerClass.getSummonsAmount();
		}

		/**
		 * @return name of the elite class variant.
		 */
		public String getEliteName() {
			return eliteName;
		}

		/**
		 * @return description of the elite class variant.
		 */
		public String getEliteDescription() {
			return eliteDescription;
		}

		/**
		 * Finds a class connected with the given index.
		 * @param index class' index.
		 * @return player class. Null for corrupted index.
		 */
		public static PlayerClass getPlayerClass(int index) {
			for(PlayerClass playerClass : PlayerClass.values()) {
				if(playerClass.getIndex() == index) {
					return playerClass;
				}
			}
			return null;
		}

		/**
		 * @return the description of the class.
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * @return the description of default class' statistics.
		 */
		public String getStatictics() {
			return statictics;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * Some constants linked to players' sprites' animations.
	 * @author MJ
	 */
	public static enum PlayerAnimationType {
		/** Stance animation. **/
		STANCE(0,4,true,false,0),
		/** Walking animation. **/
		WALK(1,8,false,false,2),
		/** First casting animation. **/
		CAST1(2,4,true,true,2),
		/** Second casting animation. **/
		CAST2(3,4,true,true,2),
		/** Blocking animation. **/
		BLOCK(4,2,true,true,1),
		/** Dying animation. **/
		DEATH(5,6,false,true,3);

		private final int index,framesAmount,priority;
		private final boolean isLooping,isPlayedOnce;
		private PlayerAnimationType(int index,int framesAmount,
				boolean isLooping, boolean isPlayedOnce,int priority) {
			this.index = index;
			this.framesAmount = framesAmount;
			this.isLooping = isLooping;
			this.isPlayedOnce = isPlayedOnce;
			this.priority = priority;
		}

		/**
		 * @return animation's index.
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @return constant's value.
		 */
		public int getFramesAmount() {
			return framesAmount;
		}

		/**
		 * @return true if the animation should be looped.
		 */
		public boolean isLooping() {
			return isLooping;
		}

		/**
		 * @return true if the animation is played only once when selected.
		 */
		public boolean isPlayedOnce() {
			return isPlayedOnce;
		}

		/**
		 * @return animation's  priority. If the new animation's priority
		 * is higher than the current player's animation priority,
		 * new animation will replace the current.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Finds animation connected with the given index.
		 * @param index animation index.
		 * @return animation type. Null for invalid index.
		 */
		public static PlayerAnimationType getAnimationType(int index) {
			for(PlayerAnimationType animationType : PlayerAnimationType.values()) {
				if(animationType.getIndex() == index) {
					return animationType;
				}
			}
			return null;
		}
	}
}
