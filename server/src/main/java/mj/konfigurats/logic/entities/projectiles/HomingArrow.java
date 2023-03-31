package mj.konfigurats.logic.entities.projectiles;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.LogicUtils;
import mj.konfigurats.logic.physics.SpellUtils.Spell;

public class HomingArrow extends Projectile {
	private Player target;
	public HomingArrow(int entityIndex, Body projectile, Player caster, Player target) {
		super(entityIndex, Spell.HOMING_ARROW, projectile, caster);
		duration = 2.5f;
		this.target = target;
	}
	
	@Override
	public void setCaster(Player caster, Player target) {
		super.setCaster(caster, target);
		
		// Changing target:
		if(target != null) {
			this.target = target;
		}
	}
	
	@Override
	public void update(float delta, Game game) {
		super.update(delta, game);
		
		// Applying force:
		if(target != null && !target.isDead() && target.getPlayerBody().getPosition() != null) {
			// "Stopping" projectile:
			if(!projectileBody.getLinearVelocity().equals(Vector2.Zero)) {
				projectileBody.applyForceToCenter(200f *
					-projectileBody.getLinearVelocity().x,200f *
					-projectileBody.getLinearVelocity().y, true);
			}
			
			// Calculating angle:
			float angle = (float)LogicUtils.getAngle(projectileBody.getPosition(),
				target.getPlayerBody().getPosition());
			projectileBody.applyForceToCenter(1300f*MathUtils.cosDeg(angle),
				1300f*MathUtils.sinDeg(angle), true);
		}
		else {
			// Target is dead or otherwise lost, destroying missile:
			setTouched(true);
		}
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
		victim.modifyHealth(caster, Spell.HOMING_ARROW.getEfficiency(),
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
					(7000f*MathUtils.cosDeg(angle),
					7000f*MathUtils.sinDeg(angle), true);
			}
		}, false, 0f));
		
		// "Destroying" projectile:
		setTouched(true);
	}
}
