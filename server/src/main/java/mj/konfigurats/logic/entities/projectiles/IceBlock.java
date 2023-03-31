package mj.konfigurats.logic.entities.projectiles;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.SpellUtils.Spell;

import com.badlogic.gdx.physics.box2d.Body;

public class IceBlock extends Projectile {
	
	public IceBlock(int entityIndex, Body projectile, Player caster) {
		super(entityIndex, Spell.ICE_BLOCK, projectile, caster);
		duration = Spell.ICE_BLOCK.getEfficiency()*caster.getDamageModificator();
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
		if(victim != null) {
			// Used by explosion particles. Lowers ice block durability:
			duration -= 0.3f*victim.getDamageModificator();
		}
		else {
			// Used by other bodies. Greatly lowers ice block durability:
			duration -= 5f;
		}
	}
}
