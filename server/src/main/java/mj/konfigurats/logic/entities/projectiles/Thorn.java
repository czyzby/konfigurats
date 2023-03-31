package mj.konfigurats.logic.entities.projectiles;

import com.badlogic.gdx.physics.box2d.Body;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.SpellUtils.Spell;

public class Thorn extends Projectile {
	public Thorn(int entityIndex, Body projectile, Player caster) {
		super(entityIndex, Spell.THORNS, projectile, caster);
		duration = 0.4f;
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
		victim.modifyHealth(caster, Spell.THORNS.getEfficiency(),
			true, true);
		
		// "Destroying" projectile:
		setTouched(true);
	}
}
