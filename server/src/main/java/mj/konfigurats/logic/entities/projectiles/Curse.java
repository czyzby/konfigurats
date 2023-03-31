package mj.konfigurats.logic.entities.projectiles;

import com.badlogic.gdx.physics.box2d.Body;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.physics.SpellUtils.SFXIndexes;
import mj.konfigurats.logic.physics.SpellUtils.Spell;
import mj.konfigurats.network.GamePackets.SrvAttachSFX;

public class Curse extends Projectile {
	public Curse(int entityIndex, Body projectile, Player caster) {
		super(entityIndex, Spell.CURSE, projectile, caster);
		duration = 3.5f;
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
		//  If player isn't shielded:
		if(!victim.isShielded()) {
			// Cursing the player:
			victim.setCursed(true);
			
			// Adding packet with SFX:
			SrvAttachSFX packet = new SrvAttachSFX();
			packet.SFXIndex = SFXIndexes.CURSE_BUFF.getSFXIndex();
			packet.entityIndex = victim.getEntityIndex();
			packet.duration = 45f;
			victim.getGame().addSFXPacket(packet);
			
			// Scheduling curse check:
			victim.scheduleEvent(new ScheduledEvent(new Runnable() {
				@Override
				public void run() {
					// If curse was not removed:
					if(victim.isCursed()) {
						victim.modifyHealth(caster, Spell.CURSE.getEfficiency()
							*caster.getDamageModificator(),true,false);
						victim.setCursed(false);
					}
				}
			}, false, 45f));
		}
		// Making sure the projectile will be removed:
		setTouched(true);
	}
}
