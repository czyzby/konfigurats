package mj.konfigurats.logic.entities.projectiles;

import com.badlogic.gdx.physics.box2d.Body;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.SpellUtils.SFXIndexes;
import mj.konfigurats.logic.physics.SpellUtils.Spell;
import mj.konfigurats.network.GamePackets.SrvAttachSFX;

public class Entangle extends Projectile {

	public Entangle(int entityIndex, Body projectile, Player caster) {
		super(entityIndex, Spell.ENTANGLE, projectile, caster);
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
		// Entangling player:
		victim.immobilizePlayer(true);
		victim.setLastDamageDealer(caster);
		
		// Adding packet with SFX:
		SrvAttachSFX packet = new SrvAttachSFX();
		packet.SFXIndex = SFXIndexes.ENTANGLE_BUFF.getSFXIndex();
		packet.entityIndex = victim.getEntityIndex();
		packet.duration = Spell.ENTANGLE.getEfficiency()*caster.getDamageModificator();
		victim.getGame().addSFXPacket(packet);
		
		// Scheduling curse check:
		victim.scheduleEvent(new ScheduledEvent(new Runnable() {
			@Override
			public void run() {
				// Unentangling player:
				victim.immobilizePlayer(false);
			}
		}, false, Spell.ENTANGLE.getEfficiency()*caster.getDamageModificator()));
		// Making sure the projectile will be removed:
		setTouched(true);
	}
}
