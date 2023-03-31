package mj.konfigurats.logic.entities;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.physics.LogicUtils;
import mj.konfigurats.logic.physics.SpellUtils.Spell;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Class maintaining a single summon.
 * @author MJ
 */
public class Summon extends Player {
	private final Player caster;
	private final SummonType type;
	private float cooldown,actionCooldown;
	private Player target;
	private boolean wasTeleported;
	
	public Summon(int entityIndex, Game game, PlayerClass playerClass,
		SummonType type, Vector2 position, Player caster,
		float initialHealth,byte initialDirection) {
		super(entityIndex, null, caster.getPlayerName()+playerClass.toString(),game,
			playerClass, false, position, caster.getTeamIndex());
		
		// Setting final classification data:
		this.type = type;
		this.caster = caster;
		// Assigning summon to the caster:
		caster.setSummon(this);
		
		// Setting initial data:
		currentHealth = initialHealth;
		recalculateHealthPercent();
		setDirection(initialDirection);
		actionCooldown = type.getActionDelay();
	}
	
	/**
	 * @return creature's summoner.
	 */
	public Player getCaster() {
		return caster;
	}

	@Override
	public void update(float delta, Game game) {
		updateScheduledEvents(delta);
		checkIfInLava();
		updateMovement(delta);
		
		// Checking if the summon has been recently moved:
		if(wasTeleported) {
			wasTeleported = false;
			// Changing target:
			target = null;
		}
		
		if(!isDead) {
			// If summon is "tired":
			if(cooldown > 0f) {
				cooldown -= delta;
			}
			else {
				// Looking for a target:
				if(target == null) {
					target = getGame().getNearestEnemy(this);
					// There are no enemies, so let's not look for them with every update:
					if(target == null) {
						cooldown = 0.75f;
					}
				}
				// Chasing target:
				else {
					if(target.isDead()) {
						// Target is killed - looking for another one.
						target = null;
						cancelDestination();
					}
					else {
						// Target is alive - chasing the hell out of him:
						actionCooldown -= delta;
						
						if(actionCooldown <= 0f) {
							actionCooldown = type.getActionDelay();
							
							switch(type) {
							case MINOTAUR:
								// Cannot kill the target for 3 seconds?
								// Let's find another!
								target = getGame().getNearestEnemy(this);
								break;
							case FIRE_ANT:
								// Cannot get the target for a long time...
								// Nothing solves it better than a fireball.
								if(!isParalyzed()) {
									if(isConfused()) {
										type.getCastedSpell().cast(getGame().getUniqueEntityIndex(),
											getGame(), this, getGame().getCircleShape(),
											LogicUtils.getInvertedPosition(this,target
											.getPlayerBody().getPosition()));
									}
									else {
										type.getCastedSpell().cast(getGame().getUniqueEntityIndex(),
											getGame(), this, getGame().getCircleShape(),
											target.getPlayerBody().getPosition());
									}
								}
								// Finding another target:
								target = getGame().getNearestEnemy(this);
								break;
							}
						}
						
						if(!isImmobilized()) {
							if(isConfused()) {
								// Setting inverted destination:
								setDestination(LogicUtils.getInvertedPosition
									(this, target.getPlayerBody().getPosition()));
							}
							else {
								// Setting actual target's position:
								setDestination(target.getPlayerBody().getPosition());
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public void setConfused(boolean confused) {
		// Works as super.setConfused(), overrides to avoid double checks.
		if(confused) {
			isConfused++;
		}
		else  {
			if(isConfused()) {
				isConfused--;
				
				// When the confusion ends, summon will usually end up far
				// away from its target, so we want to find a new one:
				target = null;
			}
		}
	}
	
	@Override
	public void immobilizePlayer(boolean immobilize) {
		super.immobilizePlayer(immobilize);
		target = null;
	}
	
	@Override
	public void setTeleported(boolean teleported) {
		this.wasTeleported = teleported;
	}
	
	/**
	 * Usually triggered by collision with a player. Summon casts
	 * a spell and tries to attack the target.
	 * @param target summon's target.
	 */
	public void attack(final Player target) {
		// If summon is not "tired" and not trying to kill an ally:
		if(cooldown <= 0f && !isParalyzed() && target != null
			&& target != this && target != caster && // Not from the same team:
			(target.getTeamIndex() < 0 || target.getTeamIndex() != getTeamIndex())) {
			// Setting cooldown:
			cooldown = type.getCooldown();
			// Canceling movement, resetting target:
			cancelDestination();
			this.target = null;
			actionCooldown = type.getActionDelay();
			
			switch(type) {
			case MINOTAUR:
				// Setting direction to face the player:
				setDirection(LogicUtils.getPlayerAngle(getPlayerBody()
					.getPosition(), target.getPlayerBody().getPosition()));
				// Slight push away from the player:
				float angle = (float)LogicUtils.getAngle(target.getPlayerBody()
					.getPosition(), getPlayerBody().getPosition());
				getPlayerBody().applyForceToCenter(10000f*MathUtils.cosDeg(angle),
					10000f*MathUtils.sinDeg(angle), true);
				// Scheduling attack:
				scheduleEvent(new ScheduledEvent(new Runnable() {
					@Override
					public void run() {
						type.getCastedSpell().cast(0, getGame(),
							Summon.this, null, null);
					}
				},false,type.getAttackDelay()));
				break;
			case FIRE_ANT:
				if(!isImmobilized()) {
					// Jumping away from the player:
					Vector2 leapPosition = target.getPlayerBody().getPosition();
					leapPosition.sub(getPlayerBody().getPosition());
					leapPosition.set(-leapPosition.x,-leapPosition.y);
					leapPosition.add(getPlayerBody().getPosition());
					// Casting leap:
					type.getUtilitySpell().cast(0, getGame(), this, null, leapPosition);
				}
				// Scheduling attack:
				scheduleEvent(new ScheduledEvent(new Runnable() {
					@Override
					public void run() {
						if(isConfused()) {
							type.getCastedSpell().cast(getGame().getUniqueEntityIndex(),
								getGame(), Summon.this, getGame().getCircleShape(),
								LogicUtils.getInvertedPosition(Summon.this,target
								.getPlayerBody().getPosition()));
						}
						else {
							type.getCastedSpell().cast(getGame().getUniqueEntityIndex(),
								getGame(), Summon.this, getGame().getCircleShape(),
								target.getPlayerBody().getPosition());
						}
					}
				},false,type.getAttackDelay()));
				break;
			}
		}
	}
	
	public void avoidBlock(final Projectile block) {
		switch(type) {
		// Minotaur is trying to destroy the block, while losing his target.
		case MINOTAUR:
			if(cooldown <= 0f && !isParalyzed()) {
				// Setting lowered cooldown:
				cooldown = type.getCooldown()/2f;
				// Canceling movement, resetting target:
				cancelDestination();
				actionCooldown = type.getActionDelay();
				this.target = null;
				// Setting direction to face the block:
				setDirection(LogicUtils.getPlayerAngle(getPlayerBody()
					.getPosition(), block.getProjectileBody().getPosition()));
				// Scheduling attack:
				scheduleEvent(new ScheduledEvent(new Runnable() {
					@Override
					public void run() {
						type.getCastedSpell().cast(0, getGame(),
							Summon.this, null, null);
						// Greatly damaging the block (quake is not enough):
						block.applyEffect(null);
					}
				},false,type.getAttackDelay()));
			}
			break;
		case FIRE_ANT:
			if(!isImmobilized()) {
				Vector2 leapPosition = block.getProjectileBody().getPosition();
				leapPosition.sub(getPlayerBody().getPosition());
				leapPosition.set(-leapPosition.x,-leapPosition.y);
				leapPosition.add(getPlayerBody().getPosition());
				// Casting leap:
				type.getUtilitySpell().cast(0, getGame(), this, null, leapPosition);
				// Waiting...
				cooldown = type.getCooldown()/3f;
				cancelDestination();
				// Changing target:
				target = null;
			}
			break;
		}
	}
	
	@Override
	protected void checkIfInLava() {
		if(inLava()) {
			switch(type) {
			case MINOTAUR:
				modifyHealth(null, -0.1f, false, true);
				break;
			case FIRE_ANT:
				modifyHealth(null, 0.15f, false, true);
				break;
			}
		}
	}
	
	/**
	 * Called by warning objects to trigger summon's AI when trying to avoid the void.
	 */
	public void avoidVoid(Vector2 voidPosition) {
		switch(type) {
		case MINOTAUR:
			// If is active and not pushed:
			if(cooldown <= 0f && !isImmobilized()
				&& getPlayerBody().getLinearVelocity().x < Player.PUSHBACK_THRESHOLD
				&& getPlayerBody().getLinearVelocity().y < Player.PUSHBACK_THRESHOLD) {
				// Slight push away from the void:
				float angle = (float)LogicUtils.getAngle(voidPosition,
					getPlayerBody().getPosition());
				getPlayerBody().applyForceToCenter(10000f*MathUtils.cosDeg(angle),
					10000f*MathUtils.sinDeg(angle), true);
				// Slowing down:
				getPlayerBody().applyForceToCenter(-5000f*getPlayerBody()
					.getLinearVelocity().x,-5000f*getPlayerBody()
					.getLinearVelocity().y, true);
				// Waiting...
				cooldown = type.getCooldown()/3f;
				cancelDestination();
				// Changing target:
				target = null;
			}
			break;
		case FIRE_ANT:
			if(!isImmobilized()) {
				// Slight push away from the void:
				float angle = (float)LogicUtils.getAngle(voidPosition,
					getPlayerBody().getPosition());
				getPlayerBody().applyForceToCenter(10000f*MathUtils.cosDeg(angle),
					10000f*MathUtils.sinDeg(angle), true);
//				// Slowing down: - too crazy with the leaps
//				getPlayerBody().applyForceToCenter(-3000f*getPlayerBody()
//					.getLinearVelocity().x,-3000f*getPlayerBody()
//					.getLinearVelocity().y, true);
				// Casting leap:
				voidPosition.sub(getPlayerBody().getPosition());
				voidPosition.set(-voidPosition.x,-voidPosition.y);
				voidPosition.add(getPlayerBody().getPosition());
				type.getUtilitySpell().cast(0, getGame(), this, null, voidPosition);
				if(cooldown <= 0f) {
					// Waiting...
					cooldown = type.getCooldown()/3f;
					cancelDestination();
					// Changing target:
					target = null;
				}
			}
			break;
		}
	}
	
	@Override
	public void applySpellCooldown(byte spellType, float cooldown) {
		// Does nothing.
	}
	
	@Override
	protected void setDead() {
		currentHealth = 0f;
		isDead = true;
		cancelDestination();
		
		// Telling the game to remove the summon:
		getGame().getEntitiesToRemove().add(this);
		// Removing caster's summon:
		if(caster.getSummon() == this) {
			caster.setSummon(null);
		}
	}
	
	/**
	 * Kills the summon instantly. Usually used when the player is trying
	 * to summon another creature.
	 */
	public void killSummon() {
		scheduleEvent(new ScheduledEvent(new Runnable() {
			@Override
			public void run() {
				setDead();
			}
		}, false, 0.1f));
	}
	
	public static enum SummonType {
		MINOTAUR(2f,0.2f,3f,Spell.QUAKE,null),
		FIRE_ANT(1.5f,0.45f,6f,Spell.MAGIC_MISSILE,Spell.LEAP);
		
		private final float cooldown,attackDelay,actionDelay;
		private final Spell castedSpell,utilitySpell;
		private SummonType(float cooldown,float attackDelay, float actionDelay,
			Spell castedSpell,Spell utilitySpell) {
			this.cooldown = cooldown;
			this.attackDelay = attackDelay;
			this.actionDelay = actionDelay;
			this.castedSpell = castedSpell;
			this.utilitySpell = utilitySpell;
		}
		
		/**
		 * @return time spent doing nothing after casting summon's spell.
		 */
		public float getCooldown() {
			return cooldown;
		}
		
		/**
		 * @return delay before casting a spell after finding a target.
		 */
		public float getAttackDelay() {
			return attackDelay;
		}
		
		/**
		 * @return time spent chasing the target before the summon does something.
		 */
		public float getActionDelay() {
			return actionDelay;
		}
		
		/**
		 * @return spell caster by the summon.
		 */
		public Spell getCastedSpell() {
			return castedSpell;
		}
		
		/**
		 * @return summon's utility spell. Might be null.
		 */
		public Spell getUtilitySpell() {
			return utilitySpell;
		}
	}
}
