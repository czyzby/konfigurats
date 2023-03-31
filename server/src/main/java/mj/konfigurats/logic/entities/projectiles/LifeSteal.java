package mj.konfigurats.logic.entities.projectiles;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.SpellUtils.Spell;

import com.badlogic.gdx.physics.box2d.Body;

public class LifeSteal extends Projectile {

	public LifeSteal(int entityIndex, Body projectile, Player caster) {
		super(entityIndex, Spell.LIFE_STEAL, projectile, caster);
		duration = 2f;
	}

	@Override
	public void destroy(Game game) {
		if(!isScheduledToRemove) {
			isScheduledToRemove = true;
			
			// Removing projectile from the list:
			game.getProjectiles().remove(this);
			
			// Removing projectile's body:
			projectileBody.getWorld().destroyBody(projectileBody);
		}
	}

	@Override
	public void applyEffect(final Player victim) {
		// Lowering health:
		victim.modifyHealth(caster, Spell.LIFE_STEAL.getEfficiency(),
			true, true);
		
		// Healing caster:
		caster.modifyHealth(caster, -Spell.LIFE_STEAL.getEfficiency(),
			false, false);
		
		// "Destroying" projectile:
		setTouched(true);
	}
}
