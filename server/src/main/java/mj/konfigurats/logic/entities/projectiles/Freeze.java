package mj.konfigurats.logic.entities.projectiles;

import com.badlogic.gdx.physics.box2d.Body;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.SpellUtils.SFXIndexes;
import mj.konfigurats.logic.physics.SpellUtils.Spell;
import mj.konfigurats.network.GamePackets.SrvAttachSFX;

public class Freeze extends Projectile {

	public Freeze(int entityIndex, Body projectile, Player caster) {
		super(entityIndex, Spell.FREEZE, projectile, caster);
		duration = 3f;
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
		// Freezing player:
		victim.freezePlayer(true);
		victim.setLastDamageDealer(caster);
		
		// Adding packet with SFX:
		SrvAttachSFX packet = new SrvAttachSFX();
		packet.SFXIndex = SFXIndexes.FREEZE_BUFF.getSFXIndex();
		packet.entityIndex = victim.getEntityIndex();
		packet.duration = Spell.FREEZE.getEfficiency()*caster.getDamageModificator();
		victim.getGame().addSFXPacket(packet);
		
		// Scheduling curse check:
		victim.scheduleEvent(new ScheduledEvent(new Runnable() {
			@Override
			public void run() {
				// Unfreezing player:
				victim.freezePlayer(false);
			}
		}, false, Spell.FREEZE.getEfficiency()*caster.getDamageModificator()));
		// Making sure the projectile will be removed:
		setTouched(true);
	}
}
