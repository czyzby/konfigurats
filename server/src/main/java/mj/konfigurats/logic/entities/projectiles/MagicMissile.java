package mj.konfigurats.logic.entities.projectiles;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.entities.ExplosionParticles;
import mj.konfigurats.logic.entities.ExplosionParticles.ExplosionType;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.LogicUtils;
import mj.konfigurats.logic.physics.SpellUtils.SFXIndexes;
import mj.konfigurats.logic.physics.SpellUtils.Spell;
import mj.konfigurats.network.GamePackets.SrvDisplaySFX;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public class MagicMissile extends Projectile {
	private final Vector2 destination;
	
	public MagicMissile(int entityIndex, Body projectile, Player caster,Vector2 destination) {
		super(entityIndex, Spell.MAGIC_MISSILE, projectile, caster);
		duration = 2.5f;
		this.destination = destination;
	}
	
	@Override
	public void setCaster(Player caster, Player target) {
		super.setCaster(caster, target);
		
		// Changing destination:
		if(target != null) {
			destination.set(target.getPlayerBody().getPosition());
		}
	}
	
	@Override
	public void update(float delta, Game game) {
		super.update(delta, game);
		
		// Checking if missile reached its destination:
		if(projectileBody.getFixtureList().get(0).testPoint(destination)) {
			// Making the projectile explode:
			setTouched(true);
		}
		// Applying force:
		else {
			// "Stopping" projectile:
			if(!projectileBody.getLinearVelocity().equals(Vector2.Zero)) {
				projectileBody.applyForceToCenter(300f *
					-projectileBody.getLinearVelocity().x,300f *
					-projectileBody.getLinearVelocity().y, true);
			}
			
			// Calculating angle:
			float angle = (float)LogicUtils.getAngle(projectileBody.getPosition(),
				destination);
			projectileBody.applyForceToCenter(3000f*MathUtils.cosDeg(angle),
				3000f*MathUtils.sinDeg(angle), true);
		}
	}

	@Override
	public void destroy(Game game) {
		if(!isScheduledToRemove) {
			isScheduledToRemove = true;
			
			// Adding explosion:
			game.getExplosionParticles().add(new ExplosionParticles
				(game,caster,ExplosionType.MAGIC_MISSILE,projectileBody.getPosition()));
			// Creating SFX packet:
			SrvDisplaySFX packet = new SrvDisplaySFX();
			packet.SFXIndex = SFXIndexes.MAGIC_MISSILE_EXPLOSION.getSFXIndex();
			packet.x = projectileBody.getPosition().x;
			packet.y = projectileBody.getPosition().y;
			game.addSFXPacket(packet);
						
			// Removing projectile from the list:
			game.getProjectiles().remove(this);
			
			// Removing projectile's body:
			projectileBody.getWorld().destroyBody(projectileBody);
		}
	}

	@Override
	public void applyEffect(Player victim) {
		// None.
	}
}
