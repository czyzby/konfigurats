package mj.konfigurats.logic.entities;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.physics.SpellUtils.Spell;

import com.badlogic.gdx.physics.box2d.Body;

public abstract class Projectile implements Entity {
	private final int entityIndex;
	private final byte projectileIndex;
	protected final Body projectileBody;
	protected Player caster;
	protected float duration;
	protected boolean isScheduledToRemove,touched;
	private int lastUsedTeleport;
	
	public Projectile(int entityIndex, Spell spell,
		Body projectile, Player caster) {
		this.entityIndex = entityIndex;
		this.projectileBody = projectile;
		this.caster = caster;
		this.projectileIndex = spell.getProjectileIndex();
		isScheduledToRemove = false;
		touched = false;
		lastUsedTeleport = -1;
	}
	
	/**
	 * Changes the caster of the projectile. Should be triggered only by
	 * other spells like Taunt.
	 * @param caster new caster of the spell.
	 * @param target if the spell seeks a player or tries to reach a destination,
	 * its goal will be changed according to the target.
	 */
	public void setCaster(Player caster,Player target) {
		if(caster != null) {
			this.caster = caster;
		}
	}

	@Override
	public void update(float delta,Game game) {
		duration -= delta;
		if(duration <= 0f || touched) {
			game.getEntitiesToRemove().add(this);
		}
	}

	@Override
	public int getEntityIndex() {
		return entityIndex;
	}
	
	/**
	 * @param touched if true, projectile will explode.
	 */
	public void setTouched(boolean touched) {
		this.touched = touched;
	}
	
	/**
	 * @return projectile's animation index.
	 */
	public byte getProjectileIndex() {
		return projectileIndex;
	}
	
	/**
	 * If the projectile's type is BodyBehavior.PROJECTILE,
	 * it will be destroyed after the first contact with a player
	 * and should override this function to apply the effect.
	 * @param victim player shot by the projectile.
	 */
	public abstract void applyEffect(Player victim);

	/**
	 * @return spell's caster.
	 */
	public Player getCaster() {
		return caster;
	}
	
	/**
	 * @return index of the last used teleport or -1 for none.
	 */
	public int getLastUsedTeleportIndex() {
		return lastUsedTeleport;
	}
	
	/**
	 * @param teleportIndex will be set as the index of the last used teleport.
	 */
	public void setLastUsedTeleport(int teleportIndex) {
		this.lastUsedTeleport = teleportIndex;
	}
	
	/**
	 * Sets last used teleport index to -1, allowing the projectile to be teleported by
	 * all teleports.
	 * @param teleportIndex index of the teleport that ends contact with the projectile.
	 */
	public void resetLastUsedTeleport(int teleportIndex) {
		if(teleportIndex == lastUsedTeleport) {
			lastUsedTeleport = -1;
		}
	}
	
	/**
	 * @return projectile's Box2D body.
	 */
	public Body getProjectileBody() {
		return projectileBody;
	}
}
