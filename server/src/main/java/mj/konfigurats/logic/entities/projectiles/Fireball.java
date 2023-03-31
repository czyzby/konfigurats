package mj.konfigurats.logic.entities.projectiles;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.entities.ExplosionParticles;
import mj.konfigurats.logic.entities.ExplosionParticles.ExplosionType;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.SpellUtils.SFXIndexes;
import mj.konfigurats.logic.physics.SpellUtils.Spell;
import mj.konfigurats.network.GamePackets.SrvDisplaySFX;

import com.badlogic.gdx.physics.box2d.Body;

public class Fireball extends Projectile {
	public Fireball(int entityIndex, Body projectile, Player caster) {
		super(entityIndex, Spell.FIREBALL, projectile, caster);
		duration = 1f;
	}

	@Override
	public void destroy(Game game) {
		if(!isScheduledToRemove) {
			isScheduledToRemove = true;
			
			// Adding explosion:
			game.getExplosionParticles().add(new ExplosionParticles
				(game,caster,ExplosionType.FIREBALL,projectileBody.getPosition()));
			// Creating SFX packet:
			SrvDisplaySFX packet = new SrvDisplaySFX();
			packet.SFXIndex = SFXIndexes.FIREBALL_EXPLOSION.getSFXIndex();
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
