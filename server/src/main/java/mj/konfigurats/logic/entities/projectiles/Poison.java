package mj.konfigurats.logic.entities.projectiles;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.entities.ExplosionParticles;
import mj.konfigurats.logic.entities.ExplosionParticles.ExplosionType;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.SpellUtils.Spell;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public class Poison extends Projectile {
	public static final float
		SPEED_MODIFICATOR = -24f,
		SPEED_RECOVERY = 0.15f,
		PROJECTILE_SPEED_MOD = SPEED_MODIFICATOR*5f,
		PROJECTILE_SPEED_REC = SPEED_RECOVERY*5f;
		
	private Vector2 destination;
	
	public Poison(int entityIndex, Body projectile, Player caster, Vector2 destination) {
		super(entityIndex, Spell.POISON, projectile, caster);
		duration = 2f;
		this.destination = destination;
	}
	
	@Override
	public void update(float delta, Game game) {
		super.update(delta, game);
		
		// Checking if poison reached its destination:
		if(projectileBody.getFixtureList().get(0).testPoint(destination)) {
			// Making the projectile explode:
			setTouched(true);
		}
	}

	@Override
	public void destroy(Game game) {
		if(!isScheduledToRemove) {
			isScheduledToRemove = true;
			
			// Adding explosion:
			game.getExplosionParticles().add(new ExplosionParticles
				(game,caster,ExplosionType.POISON_PRIMARY,projectileBody.getPosition()));
			game.getExplosionParticles().add(new ExplosionParticles
				(game,caster,ExplosionType.POISON_SECONDARY,projectileBody.getPosition()));
			
			// Removing projectile from the list:
			game.getProjectiles().remove(this);
			
			// Removing projectile's body:
			projectileBody.getWorld().destroyBody(projectileBody);
		}
	}

	@Override
	public void applyEffect(final Player victim) {
		// Slowing down:
		victim.modifySpeed(PROJECTILE_SPEED_MOD);
		// Applying poison:
		victim.scheduleEvent(new ScheduledEvent(new Runnable() {
			@Override
			public void run() {
				// Lowering health:
				victim.modifyHealth(caster, Spell.POISON.getEfficiency()*4,false,false);
				// Regaining speed:
				victim.modifySpeed(PROJECTILE_SPEED_REC);
			}
		}, true, 8f));
	}

}
