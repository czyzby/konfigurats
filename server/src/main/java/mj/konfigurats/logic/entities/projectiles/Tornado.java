package mj.konfigurats.logic.entities.projectiles;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.LogicUtils;
import mj.konfigurats.logic.physics.SpellUtils.Spell;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;

public class Tornado extends Projectile {

	public Tornado(int entityIndex, Body projectile, Player caster) {
		super(entityIndex, Spell.TORNADO, projectile, caster);
		duration = 3f;
	}
	
	@Override
	public void update(float delta, Game game) {
		super.update(delta, game);
		
		// Getting random angle:
		float angle = MathUtils.random()*360f;
		// Applying force to the body in a random direction:
		projectileBody.applyForceToCenter(10000f*MathUtils.cosDeg(angle),
			10000f*MathUtils.sinDeg(angle), true);
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
		victim.modifyHealth(caster, Spell.TORNADO.getEfficiency(),
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
					(4500f*MathUtils.cosDeg(angle),
					4500f*MathUtils.sinDeg(angle), true);
			}
		}, false, 0f));
	}

}
