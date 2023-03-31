package mj.konfigurats.logic.entities;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.entities.projectiles.Poison;
import mj.konfigurats.logic.physics.LogicUtils;
import mj.konfigurats.logic.physics.BodyInformation.BodyBehavior;
import mj.konfigurats.logic.physics.SpellUtils.Spell;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Array;

/**
 * Contains an array of particles, which are shot in all directions
 * when an explosion occurs.
 * @author MJ
 */
public class ExplosionParticles implements Entity {
	private final Array<Body> particles;
	private final ExplosionType type;
	private final Player caster;
	private float duration;
	
	/**
	 * Creates a new set of explosion particles.
	 * @param game game room.
	 * @param caster player that cast the original spell.
	 * @param type type of the explosion, depending on the spell projectile.
	 * @param position initial position of the explosion blast.
	 * @param isSensor if true, particles will detect contacts, but do not
	 * cause collisions.
	 */
	public ExplosionParticles(Game game, Player caster,
		ExplosionType type,Vector2 position) {
		this.type = type;
		this.caster = caster;
		this.duration = type.getDuration();
		particles = new Array<Body>();
		
		// Creating particle's body definition:
		BodyDef bodyDefinition = new BodyDef();
		bodyDefinition.type = BodyType.DynamicBody;
		bodyDefinition.bullet = true;
		bodyDefinition.position.set(position);
		bodyDefinition.fixedRotation = true;
		
		// Creating particle's fixture definition:
		game.getCircleShape().setRadius(type.getRadius());
		FixtureDef fixtureDefinition = new FixtureDef();
		fixtureDefinition.shape = game.getCircleShape();
		fixtureDefinition.isSensor = type.isSensor();
		fixtureDefinition.density = type.getDensity();
		fixtureDefinition.restitution = 1f;
		fixtureDefinition.filter.categoryBits = 0x0010;
		fixtureDefinition.filter.groupIndex = -0x0010;
		
		for(float angle=0; angle<360; angle += type.getAngle()) {
			// Adjusting position if the explosion doesn't start at the center:
			if(type.getOffset() != 0f) {
				bodyDefinition.position.set(position.x+type.getOffset()
					*MathUtils.cosDeg(angle),position.y+type.getOffset()
					*MathUtils.sinDeg(angle));
			}
			
			// Creating particle's body and setting it's data:
			Body particle = game.getBox2DWorld()
				.createBody(bodyDefinition);
			particle.setUserData(this);
			
			// Creating particle's fixture:
			particle.createFixture(fixtureDefinition)
				.setUserData(BodyBehavior.EXPLOSION_PARTICLE);
			
			// Applying explosion's force:
			particle.setTransform(particle.getPosition(), angle);
			particle.applyForceToCenter(type.getForce()*MathUtils.cosDeg(angle),
				type.getForce()*MathUtils.sinDeg(angle),true);
			
			// Adding particle to the list:
			particles.add(particle);
		}
	}

	@Override
	public void update(float delta, Game game) {
		duration -= delta;
		if(duration <= 0f) {
			game.getEntitiesToRemove().add(this);
		}
	}

	@Override
	public int getEntityIndex() {
		// Particles are never send to the client.
		return 0;
	}

	@Override
	public void destroy(Game game) {
		// Removing particle's bodies:
		for(Body particle : particles) {
			particle.getWorld().destroyBody(particle);
		}
		
		// Removing explosion from the game list:
		game.getExplosionParticles().remove(this);
	}
	
	/**
	 * Deals damage or applies other explosion's effects to the victim.
	 * @param victim player that collided with a particle.
	 */
	public void dealDamage(final Player victim,Body particle) {
		switch(type) {
		case POISON_PRIMARY:
		case POISON_SECONDARY:
			// Slowing down:
			victim.modifySpeed(Poison.SPEED_MODIFICATOR);
			// Applying poison:
			victim.scheduleEvent(new ScheduledEvent(new Runnable() {
				@Override
				public void run() {
					// Lowering health:
					victim.modifyHealth(caster, type.getDamage(),false,false);
					// Regaining speed:
					victim.modifySpeed(Poison.SPEED_RECOVERY);
				}
			}, true, 8f));
			break;
		case PULSE:
			if(caster.getEntityIndex() != victim.getEntityIndex()) {
				// Dealing damage if the target is not the caster:
				victim.modifyHealth(caster,type.getDamage(),true,true);
			}
			// Simulating explosion push:
			victim.getPlayerBody().applyForceToCenter(6000f*MathUtils.cosDeg
				(particle.getAngle())*caster.getDamageModificator(),
				6000f*MathUtils.sinDeg(particle.getAngle()*caster
				.getDamageModificator()),true);
			break;
		case BLAZING_FEET_FLAME:
			if(!victim.isShielded() && !victim.isBlazing()) {
				float touchAngle = (float)LogicUtils.getAngle(particle.getPosition(),
					victim.getPlayerBody().getPosition());
				// Simulating explosion push:
				victim.getPlayerBody().applyForceToCenter(10000f*MathUtils.cosDeg(touchAngle),
					10000f*MathUtils.sinDeg(touchAngle),true);
				// Dealing damage:
				victim.modifyHealth(caster,type.getDamage(),true,true);
			}
			break;
		case QUAKE:
			// Quake cannot hurt the caster:
			if(caster.getEntityIndex() == victim.getEntityIndex()) {
				break;
			}
		default:
			// Dealing damage:
			victim.modifyHealth(caster,type.getDamage(),true,true);
			break;
		}
	}
	
	/**
	 * Deals damage to a cracking projectile (ice block etc.)
	 * @param crackingProjectile projectile's object.
	 */
	public void dealDamage(Projectile crackingProjectile) {
		switch(type) {
		case POISON_PRIMARY:
		case POISON_SECONDARY:
			break;
		default:
			// Dealing damage to cracking block:
			crackingProjectile.applyEffect(caster);
			break;
		}
	}
	
	/**
	 * Contains all types of explosions available in the game.
	 * @author MJ
	 */
	public static enum ExplosionType {
		FIREBALL(Spell.FIREBALL.getEfficiency(),3f/20f,15f,1000f,10000f,0f,0.1f,false),
		METEOR_PRIMARY(Spell.METEOR.getEfficiency(),2f/20f,60f,1250f,12500f,0.4f,0.1f,true),
		METEOR_SECONDARY(Spell.METEOR.getEfficiency(),3f/20f,12f,1250f,12500f,0.9f,0.1f,false),
		BLAZING_FEET_FLAME(Spell.BLAZING_FEET.getEfficiency(),3f,360f,10f,0f,0f,0.3f,true),
		MAGIC_MISSILE(Spell.MAGIC_MISSILE.getEfficiency(),2f/20f,15f,1000f,10250f,0.15f,0.1f,false),
		POISON_PRIMARY(Spell.POISON.getEfficiency(),6f/20f,60f,800f,15000f,0.1f,0.25f,true),
		POISON_SECONDARY(Spell.POISON.getEfficiency(),10f/20f,15f,800f,15000f,1f,0.25f,true),
		PULSE(Spell.PULSE.getEfficiency(),9f/20f,12f,800f,10500f,1f,0.2f,true),
		QUAKE(Spell.QUAKE.getEfficiency(),2f/20f,10f,800f,8000f,1f,0.1f,false);
		
		private final float damage,duration,angle,density,force,offset,radius;
		private final boolean isSensor;
		private ExplosionType(float damage, float duration,float angle,
			float density,float force,float offset,float radius,boolean isSensor) {
			this.damage = damage;
			this.duration = duration;
			this.angle = angle;
			this.density = density;
			this.force = force;
			this.offset = offset;
			this.radius = radius;
			this.isSensor = isSensor;
		}
		
		/**
		 * @return the angle separating each particle.
		 * 360/angle is the particle amount.
		 */
		public float getAngle() {
			return angle;
		}
		
		/**
		 * @return damage dealt by each particle.
		 */
		public float getDamage() {
			return damage;
		}
		
		/**
		 * @return how long the particles last.
		 */
		public float getDuration() {
			return duration;
		}
		
		/**
		 * @return density of a single particle.
		 */
		public float getDensity() {
			return density;
		}
		
		/**
		 * @return force applied to each particle.
		 */
		public float getForce() {
			return force;
		}
		
		/**
		 * @return particle creation offset from the original position.
		 */
		public float getOffset() {
			return offset;
		}
		
		/**
		 * @return a single particle radius.
		 */
		public float getRadius() {
			return radius;
		}
		
		/**
		 * @return true is the particles never collide.
		 */
		public boolean isSensor() {
			return isSensor;
		}
	}
}
