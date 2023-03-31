package mj.konfigurats.logic.entities.projectiles;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.LogicUtils;
import mj.konfigurats.logic.physics.SpellUtils.Spell;

public class LightningBolt extends Projectile {

	public LightningBolt(int entityIndex, Body projectile, Player caster) {
		super(entityIndex, Spell.LIGHTNING_BOLT, projectile, caster);
		duration = 2.5f;
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
		victim.modifyHealth(caster, Spell.LIGHTNING_BOLT.getEfficiency(),
			true, true);
		// Calculating angle:
		final float angle = (float)LogicUtils.getAngle(this.projectileBody.getPosition(),
			victim.getPlayerBody().getPosition());
		
		// Pushing effect:
		victim.scheduleEvent(new ScheduledEvent(new Runnable() {
			@Override
			public void run() {
				// Pushing the player's body:
				victim.getPlayerBody().applyForceToCenter
					(6000f*MathUtils.cosDeg(angle),
					6000f*MathUtils.sinDeg(angle), true);
			}
		}, false, 0f));
	}

}
