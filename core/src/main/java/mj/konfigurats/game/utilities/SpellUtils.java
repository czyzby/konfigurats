package mj.konfigurats.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import mj.konfigurats.Core;
import mj.konfigurats.game.entities.SFX.SFXType;

public class SpellUtils {
	private SpellUtils() {}
	private final static int PROJECTILE_SIZE = 64;

	private static TextureRegion[][] PROJECTILES;
	private static TextureRegion[][] SFX;

	/**
	 * Assigning projectile animations.
	 */
	public static void createProjectileAnimations() {
		// Getting projectile animation frames:
		PROJECTILES = TextureRegion.split(((Core)Gdx.app.getApplicationListener())
			.getInterfaceManager().getAsset("game/spells/projectiles.png",
			Texture.class),PROJECTILE_SIZE,PROJECTILE_SIZE);

		// Assigning projectiles' SFX:
		SFX = new TextureRegion[SFXType.values().length][];
		for(SFXType sfx : SFXType.values()) {
			SFX[sfx.getSFXIndex()] = TextureRegion.split(((Core)Gdx.app.getApplicationListener())
				.getInterfaceManager().getAsset("game/spells/"+sfx.getSFXName()+sfx.getSFXIndex()
				+".png",Texture.class),sfx.getWidth(),sfx.getHeight())[0];
		}
	}

	/**
	 * @param projectileIndex animation index of a projectile.
	 * @return projectile animation frames.
	 */
	public static TextureRegion[] getProjectileAnimationFrames(int projectileIndex) {
		return PROJECTILES[projectileIndex];
	}

	/**
	 * @param SFXIndex index of SFX's animation.
	 * @return SFX animation frames.
	 */
	public static TextureRegion[] getSFXAnimationFrames(int SFXIndex) {
		return SFX[SFXIndex];
	}

	/**
	 * Calculates the entity's angle based on its current velocity.
	 * @param xVelocity velocity on the x axis.
	 * @param yVelocity velocity on the y axis.
	 * @return entity's angle.
	 */
	public static float getAngle(float xVelocity,float yVelocity) {
		float sum = Math.abs(xVelocity)+Math.abs(yVelocity);
		return MathUtils.atan2(yVelocity/sum,xVelocity/sum)
			*MathUtils.radiansToDegrees;
	}

	/**
	 * Calculates distance between 2 points.
	 * @param x1 first point's x.
	 * @param y1 first point's y.
	 * @param x2 second point's x.
	 * @param y2 second point's y.
	 * @return
	 */
	public static float getDistance(float x1,float y1,float x2,float y2) {
		float deltaX = x1 - x2, deltaY = y2 - y2;
		return (float)Math.sqrt(deltaX*deltaX+deltaY*deltaY);
	}

	public static enum SpellType {
		FIRE(0),WATER(1),EARTH(2),AIR(3);

		private final int index;
		private SpellType(int index) {
			this.index = index;
		}

		/**
		 * @return spell type index.
		 */
		public int getIndex() {
			return index;
		}
	}

	/**
	 * Contains informations about all spells in the game.
	 * @author MJ
	 */
	public static enum Spells {
		/** Fire spells. */
		FIREBALL(0,0,"Fireball",
			"The caster throws a fireball,\nexploding after it hits an object.\nDamage: medium.\nRecast time: short."),
		METEOR(0,1,"Explosion",
			"The mage summons an explosion\nafter a short time delay.\nDamage: high.\nRecast time: long."),
		BLAZING_FEET(0,2,"Blazing Feet",
			"The target will leave a trail\nof fire behind as he moves.\nDamage: medium-high.\nRecast time: medium."),
		CURSE(0,3,"Curse",
			"A powerful spell, tormenting the\ntarget if he is not healed on time.\nDamage: extremely high.\nRecast time: extremely long."),
		MAGIC_MISSILE(0,4,"Magic Missile",
			"Creates a bolt of pure magic\nthat explodes at the chosen place.\nDamage: medium.\nRecast time: medium."),
		LIFE_STEAL(0,5,"Life Steal",
			"The mage focuses energy in a\ndeadly bolt that can steal health.\nDamage: low. Heals.\nRecast time: medium."),
		SWARM(0,6,"Swarm",
			"Allows the caster to summon\nan insect absorbing fire energy.\nSummons fire ant.\nRecast time: extremely long."),

		/** Water spells. */
		HEAL(1,0,"Heal",
			"Heals a chosen mage instantly.\nIs likely to remove curses.\nHeals.\nRecast time: very long."),
		FREEZE(1,1,"Freeze",
			"A nasty spell, freezing the target\nfor a short period of time.\nFreezes.\nRecast time: long."),
		ICE_BLOCK(1,2,"Ice Block",
			"Summons a heavy ice block, capable\nof reflecting projectiles.\nDamage: none.\nRecast time: very short."),
		SHIELD(1,3,"Shield",
				"Shields a mage, protecting him\nfrom damage, curses and swaps.\nShields.\nRecast time: very long."),
		PULSE(1,4,"Pulse",
			"Protects a chosen spellcaster\nwith a deadly magic wave.\nDamage: medium.\nRecast time: long."),
		CONFUSION(1,5,"Confusion",
			"Confuses an enemy, making him\nunable to control his body.\nConfuses.\nRecast time: long."),
		SILENCE(1,6,"Silence",
			"Silences the target, making him\nunable to cast spells.\nSilences.\nRecast time: long."),

		/** Earth spells. */
		QUAKE(2,0,"Quake",
			"Causes a small earthquake,\ndeadly around the caster.\nDamage: medium-high.\nRecast time: medium."),
		POISON(2,1,"Poison",
			"Gas cloud, poisoning and slowing\ntargets as it goes. Explodes.\nDamage: medium.\nRecast time: short."),
		ENTANGLE(2,2,"Entangle",
			"Entangles the target, making him\nunable to move for a while.\nImmobilizes.\nRecast time: long."),
		CURE(2,3,"Cure",
			"Cures a mage, allowing him to\nrecover. Can remove curses.\nHeals over time.\nRecast time: very long."),
		HOMING_ARROW(2,4,"Homing Arrow",
			"A sneaky magic bolt that\nfollows the chosen target.\nDamage: low.\nRecast time: medium."),
		SUMMON_BEAST(2,5,"Summon Beast",
			"Summons an enraged beast, striking\nits enemies with no mercy.\nSummons minotaur.\nRecast time: extremely long."),
		THORNS(2,6,"Thorns",
			"Shoots a bunch of sharp thorns,\ncapable of piercing through armors.\nDamage: medium-high.\nRecast time: medium."),

		/** Air spells. */
		HASTE(3,0,"Haste",
			"Boosts chosen mage's speed for\na short period of time.\nSpeeds up.\nRecast time: medium."),
		LEAP(3,1,"Leap",
			"Allows the caster to rapidly\nleap in the chosen direction.\nMovement spell.\nRecast time: short."),
		LIGHTNING_BOLT(3,2,"Lightning Bolt",
			"Shoots a devastating lightning\nbolt after a short delay.\nDamage: medium-high.\nRecast time: short."),
		TORNADO(3,3,"Tornado",
			"Starts a deadly tornado, moving\nin a random direction.\nDamage: low-high.\nRecast time: medium."),
		SWAP(3,4,"Swap",
			"Switches caster's position\nwith the chosen mage.\nMovement spell.\nRecast time: very long."),
		TAUNT(3,5,"Taunt",
			"Causes all nearby projectiles\nto follow the chosen mage.\nTaunts.\nRecast time: long."),
		LINK(3,6,"Link",
			"Links the target with his\nclosest opponent.\nLinks two mages.\nRecast time: medium-long.");

		private final int type,index;
		private final String name,description;
		private Spells(int type,int index,String name,String description) {
			this.type = type;
			this.index = index;
			this.name = name;
			this.description = description;
		}

		/**
		 * @return spell's type.
		 */
		public int getType() {
			return type;
		}

		/**
		 * @return spell's index in its type.
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @return description of the given spell.
		 */
		public String getDescription() {
			return description;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
