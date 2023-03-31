package mj.konfigurats.logic.physics;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.entities.ExplosionParticles;
import mj.konfigurats.logic.entities.ExplosionParticles.ExplosionType;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Player.PlayerClass;
import mj.konfigurats.logic.entities.Player.PlayerState;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.entities.Summon;
import mj.konfigurats.logic.entities.Summon.SummonType;
import mj.konfigurats.logic.entities.projectiles.Curse;
import mj.konfigurats.logic.entities.projectiles.Entangle;
import mj.konfigurats.logic.entities.projectiles.Fireball;
import mj.konfigurats.logic.entities.projectiles.Freeze;
import mj.konfigurats.logic.entities.projectiles.HomingArrow;
import mj.konfigurats.logic.entities.projectiles.IceBlock;
import mj.konfigurats.logic.entities.projectiles.LifeSteal;
import mj.konfigurats.logic.entities.projectiles.LightningBolt;
import mj.konfigurats.logic.entities.projectiles.MagicMissile;
import mj.konfigurats.logic.entities.projectiles.Poison;
import mj.konfigurats.logic.entities.projectiles.Thorn;
import mj.konfigurats.logic.entities.projectiles.Tornado;
import mj.konfigurats.logic.physics.BodyInformation.BodyBehavior;
import mj.konfigurats.network.GamePackets.SrvAttachSFX;
import mj.konfigurats.network.GamePackets.SrvCreateCharacter;
import mj.konfigurats.network.GamePackets.SrvDisplaySFX;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Shape;
import com.esotericsoftware.kryonet.Connection;

/**
 * Utility class for spells management.
 * @author MJ
 */
public class SpellUtils {
	private SpellUtils() {}
	
	/**
	 * Contains all spell types.
	 * @author MJ
	 */
	public static enum SpellType {
		FIRE((byte)0) {
			@Override
			public Spell getSpell(byte index) {
				switch(index) {
				case 0:
					return Spell.FIREBALL;
				case 1:
					return Spell.METEOR;
				case 2:
					return Spell.BLAZING_FEET;
				case 3:
					return Spell.CURSE;
				case 4:
					return Spell.MAGIC_MISSILE;
				case 5:
					return Spell.LIFE_STEAL;
				case 6:
					return Spell.SWARM;
				default:
					return null;
				}
			}
		},
		WATER((byte)1) {
			@Override
			public Spell getSpell(byte index) {
				switch(index) {
				case 0:
					return Spell.HEAL;
				case 1:
					return Spell.FREEZE;
				case 2:
					return Spell.ICE_BLOCK;
				case 3:
					return Spell.SHIELD;
				case 4:
					return Spell.PULSE;
				case 5:
					return Spell.CONFUSION;
				case 6:
					return Spell.SILENCE;
				default:
					return null;
				}
			}
		},
		EARTH((byte)2) {
			@Override
			public Spell getSpell(byte index) {
				switch(index) {
				case 0:
					return Spell.QUAKE;
				case 1:
					return Spell.POISON;
				case 2:
					return Spell.ENTANGLE;
				case 3:
					return Spell.CURE;
				case 4:
					return Spell.HOMING_ARROW;
				case 5:
					return Spell.SUMMON_BEAST;
				case 6:
					return Spell.THORNS;
				default:
					return null;
				}
			}
		},
		AIR((byte)3) {
			@Override
			public Spell getSpell(byte index) {
				switch(index) {
				case 0:
					return Spell.HASTE;
				case 1:
					return Spell.LEAP;
				case 2:
					return Spell.LIGHTNING_BOLT;
				case 3:
					return Spell.TORNADO;
				case 4:
					return Spell.SWAP;
				case 5:
					return Spell.TAUNT;
				case 6:
					return Spell.LINK;
				default:
					return null;
				}
			}
		};
		
		private final byte index;
		private SpellType(byte index) {
			this.index = index;
		}
		
		/**
		 * @return spell type index.
		 */
		public byte getIndex() {
			return index;
		}
		
		/**
		 * @param index spell type index.
		 * @return spell type with the given index. Null for invalid index.
		 */
		public static SpellType getSpellType(byte index) {
			for(SpellType spellType : SpellType.values()) {
				if(spellType.getIndex() == index) {
					return spellType;
				}
			}
			return null;
		}
		
		/**
		 * @param index spell index in the given type.
		 * @return spell with the given index. Null for invalid index.
		 */
		public abstract Spell getSpell(byte index);
	}
	
	/**
	 * Contains all spells.
	 * @author MJ
	 */
	public static enum Spell {
		/** Fire spells. */
		FIREBALL(SpellType.FIRE.getIndex(),0,(byte)0,
			PlayerState.CAST1,-4f,7f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
					Shape shape,Vector2 position) {
				// Calculating angle and its cos&sin:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position), cos = MathUtils.cosDeg(angle),
					sin = MathUtils.sinDeg(angle);
				
				// Setting actual spell position:
				position.set(caster.getPlayerBody().getPosition().x + cos,
					caster.getPlayerBody().getPosition().y + sin);
				
				// Creating body:
				Body projectile = game.getBox2DWorld().createBody
					(getDefaultProjectileBodyDef(position));
				
				// Creating fixture definition:
				shape.setRadius(0.25f);
				FixtureDef fixtureDefinition = new FixtureDef();
				fixtureDefinition.shape = shape;
				fixtureDefinition.density = 150f;
				fixtureDefinition.restitution = 0.9f;
				
				// Creating fixture:
				projectile.createFixture(fixtureDefinition)
					.setUserData(BodyBehavior.EXPLODING_PROJECTILE);
				
				// Applying force to the projectile:
				projectile.applyLinearImpulse(position.set(450f*cos,450f*sin),
					projectile.getWorldCenter(),true);
				
				// Creating a projectile object, so that it could be updated:
				projectile.setUserData(new Fireball
					(entityIndex,projectile,caster));
				
				// Adding projectile to the list:
				game.getProjectiles().add((Projectile)projectile.getUserData());
			
				validateSpellCast(caster, angle);
			}
		}, METEOR(SpellType.FIRE.getIndex(),1,(byte)-1,
			PlayerState.CAST2,-5f,10f) {
			@Override
			public void cast(int entityIndex,final Game game,final Player caster,
					Shape shape,Vector2 position) {
				// Adding packet with SFX warning:
				SrvDisplaySFX packet = new SrvDisplaySFX();
				packet.SFXIndex = SFXIndexes.METEOR_WARNING.getSFXIndex();
				packet.x = position.x;
				packet.y = position.y;
				game.addSFXPacket(packet);
				
				final Vector2 spellCastPostion = new Vector2(position);
				
				// Scheduling explosion:
				game.scheduleEvent(new ScheduledEvent(new Runnable() {
					@Override
					public void run() {
						game.getExplosionParticles().add(new ExplosionParticles
							(game,caster,ExplosionType.METEOR_PRIMARY,spellCastPostion));
						game.getExplosionParticles().add(new ExplosionParticles
							(game,caster,ExplosionType.METEOR_SECONDARY,spellCastPostion));
						// Adding packet with SFX explosion:
						SrvDisplaySFX packet = new SrvDisplaySFX();
						packet.SFXIndex = SFXIndexes.METEOR_EXPLOSION.getSFXIndex();
						packet.x = spellCastPostion.x;
						packet.y = spellCastPostion.y;
						game.addSFXPacket(packet);
					}
				}, false, 1.85f));
				
				validateSpellCast(caster, (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position));
			}
		}, BLAZING_FEET(SpellType.FIRE.getIndex(),2,(byte)-1,
			PlayerState.CAST2,-7.5f,9.5f) {
			@Override
			public void cast(int entityIndex,final Game game, Player caster,
					Shape shape,Vector2 position) {
				// Finding target nearest to the cursor:
				final Player target = game.getNearestPlayer(position);
				
				if(target != null) {
					// Applying blazing buff:
					target.setBlazing(true);
					
					// Calculating flames creation frequency:
					final float creationTime,
						speedModification = 750f*caster.getDamageModificator();
					if(target.getSpeed() >= 5000) {
						creationTime = 0.1f;
					}
					else {
						creationTime = 0.6f - (target.getSpeed()/1000f)*0.1f;
					}
					
					// Applying haste:
					target.modifySpeed(speedModification);
					
					// Creating flames:
					target.scheduleEvent(new ScheduledEvent(new Runnable() {
						private float timePassed;
						@Override
						public void run() {
							timePassed += Game.UPDATE_TIME;
							
							while(timePassed >= creationTime) {
								timePassed -= creationTime;
								
								if(target.isMoving()) {
									// Creating flame:
									game.getExplosionParticles().add(new ExplosionParticles
										(game, target, ExplosionType.BLAZING_FEET_FLAME,
										target.getPlayerBody().getPosition()));
									
									// Adding packet with SFX flame:
									SrvDisplaySFX packet = new SrvDisplaySFX();
									packet.SFXIndex = SFXIndexes.BLAZING_FEET_FLAME.getSFXIndex();
									packet.x = target.getPlayerBody().getPosition().x;
									packet.y = target.getPlayerBody().getPosition().y+0.2f;
									game.addSFXPacket(packet);
								}
							}
						}
					}, true, 5f*caster.getDamageModificator()));
					
					// Scheduling buff removal:
					target.scheduleEvent(new ScheduledEvent(new Runnable() {
						@Override
						public void run() {
							// Removing buff:
							target.setBlazing(false);
							target.modifySpeed(-speedModification);
						}
					// Buff removal is a bit longer than the effect so that you can escape your flames:
					}, false, 5.25f*caster.getDamageModificator()));
					
					// Adding packet with SFX:
					SrvAttachSFX packet = new SrvAttachSFX();
					packet.SFXIndex = SFXIndexes.BLAZING_FEET_BUFF.getSFXIndex();
					packet.entityIndex = target.getEntityIndex();
					packet.duration =  5f*caster.getDamageModificator();
					game.addSFXPacket(packet);
				}
				
				validateSpellCast(caster);
			}
		}, CURSE(SpellType.FIRE.getIndex(),3,(byte)1,
			PlayerState.CAST1,-50f,25f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
					Shape shape,Vector2 position) {
				// Calculating angle and its cos&sin:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position), cos = MathUtils.cosDeg(angle),
					sin = MathUtils.sinDeg(angle);
				// Setting actual spell position:
				position.set(caster.getPlayerBody().getPosition().x + cos,
					caster.getPlayerBody().getPosition().y + sin);
				
				// Creating body:
				Body projectile = game.getBox2DWorld().createBody
					(getDefaultProjectileBodyDef(position));
				
				// Creating fixture definition:
				shape.setRadius(0.25f);
				FixtureDef fixtureDefinition = new FixtureDef();
				fixtureDefinition.shape = shape;
				fixtureDefinition.density = 50f;
				fixtureDefinition.restitution = 0.5f;
				
				// Creating fixture:
				projectile.createFixture(fixtureDefinition)
					.setUserData(BodyBehavior.PROJECTILE);
				
				// Applying force to the projectile:
				projectile.applyLinearImpulse(position.set(30f*cos,30f*sin),
					projectile.getWorldCenter(),true);
				
				// Creating a projectile object, so that it could be updated:
				projectile.setUserData(new Curse(entityIndex,projectile,caster));

				// Adding projectile to the list:
				game.getProjectiles().add((Projectile)projectile.getUserData());
			
				validateSpellCast(caster, angle);
			}
		}, MAGIC_MISSILE(SpellType.FIRE.getIndex(),4,(byte)8,
			PlayerState.CAST1,-3.25f,8f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
				Shape shape, Vector2 position) {
				// Calculating angle and its cos&sin:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position),
					// Randomizing angle:
					modifiedAngle = (angle + MathUtils.random()*80f
					- MathUtils.random()*80f)%360,
					cos = MathUtils.cosDeg(modifiedAngle),
					sin = MathUtils.sinDeg(modifiedAngle);
				
				// Setting actual spell position:
				Vector2 missileDestination = new Vector2(position);
				position.set(caster.getPlayerBody().getPosition().x + cos,
					caster.getPlayerBody().getPosition().y + sin);
				
				// Creating body:
				Body projectile = game.getBox2DWorld().createBody
					(getDefaultProjectileBodyDef(position));
				
				// Creating fixture definition:
				shape.setRadius(0.25f);
				FixtureDef fixtureDefinition = new FixtureDef();
				fixtureDefinition.shape = shape;
				fixtureDefinition.density = 150f;
				fixtureDefinition.restitution = 0.9f;
				
				// Creating fixture:
				projectile.createFixture(fixtureDefinition)
					.setUserData(BodyBehavior.EXPLODING_PROJECTILE);
				
				// Applying force to the projectile:
				projectile.applyLinearImpulse(position.set(650f*cos,650f*sin),
					projectile.getWorldCenter(),true);
				
				// Creating a projectile object, so that it could be updated:
				projectile.setUserData(new MagicMissile
					(entityIndex,projectile,caster,missileDestination));
				
				// Adding projectile to the list:
				game.getProjectiles().add((Projectile)projectile.getUserData());
			
				validateSpellCast(caster, angle);
			}
		}, LIFE_STEAL(SpellType.FIRE.getIndex(),5,(byte)10,
			PlayerState.CAST1,-8f,9f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
				Shape shape, Vector2 position) {
				// Calculating angle and its cos&sin:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position), cos = MathUtils.cosDeg(angle),
					sin = MathUtils.sinDeg(angle);
				// Setting actual spell position:
				position.set(caster.getPlayerBody().getPosition().x + cos,
					caster.getPlayerBody().getPosition().y + sin);
				
				// Creating body:
				Body projectile = game.getBox2DWorld().createBody
					(getDefaultProjectileBodyDef(position));
				
				// Creating fixture definition:
				shape.setRadius(0.25f);
				FixtureDef fixtureDefinition = new FixtureDef();
				fixtureDefinition.shape = shape;
				fixtureDefinition.density = 200f;
				fixtureDefinition.restitution = 0.6f;
				
				// Creating fixture:
				projectile.createFixture(fixtureDefinition)
					.setUserData(BodyBehavior.PROJECTILE);
				
				// Applying force to the projectile:
				projectile.applyLinearImpulse(position.set(400f*cos,400f*sin),
					projectile.getWorldCenter(),true);
				
				// Creating a projectile object, so that it could be updated:
				projectile.setUserData(new LifeSteal(entityIndex,projectile,caster));
				
				// Adding projectile to the list:
				game.getProjectiles().add((Projectile)projectile.getUserData());
				
				validateSpellCast(caster, angle);
			}
		}, SWARM(SpellType.FIRE.getIndex(),6,(byte)-1,
			PlayerState.CAST2,35f,60f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
				Shape shape, Vector2 position) {
				// Removing old summon:
				if(caster.hasSummon()) {
					caster.getSummon().killSummon();
				}
				
				// Calculating angle:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position);
				// Setting actual spell position:
				position.set(caster.getPlayerBody().getPosition().x + 1.5f*MathUtils.cosDeg(angle),
					caster.getPlayerBody().getPosition().y + 1.5f*MathUtils.sinDeg(angle));
				
				// Summoning fire ant:
				Summon fireAnt = new Summon(entityIndex,game,PlayerClass.FIRE_ANT,
					SummonType.FIRE_ANT,position,caster,this.getEfficiency()
					*caster.getDamageModificator(),LogicUtils.getPlayerAngle(angle));
				
				// Adding the summon to the game's characters list:
				game.getCharacters().add(fireAnt);
				
				// Telling all players that a new character has been summoned:
				SrvCreateCharacter characterPacket = new SrvCreateCharacter();
				characterPacket.characterIndex = fireAnt.getEntityIndex();
				characterPacket.teamIndex = fireAnt.getTeamIndex();
				characterPacket.playerName = fireAnt.getPlayerName();
				characterPacket.characterClass = fireAnt.getPlayerClass().getIndex();
				characterPacket.isElite = false; 
				characterPacket.x = fireAnt.getPlayerBody().getPosition().x;
				characterPacket.y = fireAnt.getPlayerBody().getPosition().y;
				for(Connection connection : game.getPlayers()) {
					connection.sendTCP(characterPacket);
				}
				
				validateSpellCast(caster, angle);
			}
		},
		
		/** Water spells. */
		HEAL(SpellType.WATER.getIndex(),0,(byte)-1,
			PlayerState.CAST2,15f,22f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
					Shape shape,Vector2 position) {
				// Finding target nearest to the cursor:
				Player target = game.getNearestPlayer(position);
				
				if(target != null) {
					// Adding health:
					target.modifyHealth(caster, this.getEfficiency(), false, false);
					// Trying to remove a curse:
					target.healCurse(0.65f*caster.getDamageModificator());
					// Adding packet with SFX healing:
					SrvAttachSFX packet = new SrvAttachSFX();
					packet.SFXIndex = SFXIndexes.HEAL_BUFF.getSFXIndex();
					packet.entityIndex = target.getEntityIndex();
					packet.duration = -1f;
					game.addSFXPacket(packet);
				}
				
				validateSpellCast(caster);
			}
		}, FREEZE(SpellType.WATER.getIndex(),1,(byte)2,
			PlayerState.CAST1,3f,16f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
					Shape shape,Vector2 position) {
				// Calculating angle and its cos&sin:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position), cos = MathUtils.cosDeg(angle),
					sin = MathUtils.sinDeg(angle);
				// Setting actual spell position:
				position.set(caster.getPlayerBody().getPosition().x + cos,
					caster.getPlayerBody().getPosition().y + sin);
				
				// Creating body:
				Body projectile = game.getBox2DWorld().createBody
					(getDefaultProjectileBodyDef(position));
				
				// Creating fixture definition:
				shape.setRadius(0.25f);
				FixtureDef fixtureDefinition = new FixtureDef();
				fixtureDefinition.shape = shape;
				fixtureDefinition.density = 150f;
				fixtureDefinition.restitution = 0.4f;
				
				// Creating fixture:
				projectile.createFixture(fixtureDefinition)
					.setUserData(BodyBehavior.PROJECTILE);
				
				// Applying force to the projectile:
				projectile.applyLinearImpulse(position.set(160f*cos,160f*sin),
					projectile.getWorldCenter(),true);
				
				// Creating a projectile object, so that it could be updated:
				projectile.setUserData(new Freeze(entityIndex,projectile,caster));
				
				// Adding projectile to the list:
				game.getProjectiles().add((Projectile)projectile.getUserData());
				
				validateSpellCast(caster, angle);
			}
		}, ICE_BLOCK(SpellType.WATER.getIndex(),2,(byte)3,
			PlayerState.CAST1,12f,6f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
					Shape shape,Vector2 position) {
				// Calculating angle and its cos&sin:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position), cos = MathUtils.cosDeg(angle),
					sin = MathUtils.sinDeg(angle);
				
				// Setting actual spell position:
				position.set(caster.getPlayerBody().getPosition().x + 2f*cos,
					caster.getPlayerBody().getPosition().y + 2f*sin);
				
				// Creating body:
				Body projectile = game.getBox2DWorld().createBody
					(getDefaultProjectileBodyDef(position));
				
				// Creating fixture definition:
				shape.setRadius(1.1f);
				FixtureDef fixtureDefinition = new FixtureDef();
				fixtureDefinition.shape = shape;
				fixtureDefinition.density = 6000f;
				fixtureDefinition.restitution = 0.6f;
				
				// Creating fixture:
				projectile.createFixture(fixtureDefinition)
					.setUserData(BodyBehavior.CRACKING_PROJECTILE);
				
				// Creating a projectile object, so that it could be updated:
				projectile.setUserData(new IceBlock(entityIndex,projectile,caster));
				
				// Adding projectile to the list:
				game.getProjectiles().add((Projectile)projectile.getUserData());
			
				validateSpellCast(caster, angle);
			}
		}, SHIELD(SpellType.WATER.getIndex(),3,(byte)-1,
				PlayerState.CAST2,5f,14f) {
				@Override
				public void cast(int entityIndex, Game game,final Player caster,
						Shape shape,Vector2 position) {
					// Finding target nearest to the cursor:
					final Player target = game.getNearestPlayer(position);
					
					if(target != null) {
						// Adding shield:
						target.setShielded(true);
						// Adding packet with SFX buff:
						SrvAttachSFX packet = new SrvAttachSFX();
						packet.SFXIndex = SFXIndexes.SHIELD_BUFF.getSFXIndex();
						packet.entityIndex = target.getEntityIndex();
						packet.duration = this.getEfficiency()*caster.getDamageModificator();
						game.addSFXPacket(packet);
						
						// Removing shield:
						target.scheduleEvent(new ScheduledEvent(new Runnable() {
							@Override
							public void run() {
								target.setShielded(false);
							}
						}, false, this.getEfficiency()*caster.getDamageModificator()));
					}
					
					validateSpellCast(caster);
				}
		}, PULSE(SpellType.WATER.getIndex(),4,(byte)-1,
			PlayerState.CAST1,-4f,10f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
				Shape shape, Vector2 position) {
				// Finding target nearest to the cursor:
				final Player target = game.getNearestPlayer(position);
				
				if(target != null) {
					// Creating pulse around the target:
					game.getExplosionParticles().add(new ExplosionParticles
						(game,caster,ExplosionType.PULSE,
						target.getPlayerBody().getPosition()));
					
					// Adding packet with SFX explosion:
					SrvDisplaySFX packet = new SrvDisplaySFX();
					packet.SFXIndex = SFXIndexes.PULSE_EXPLOSION.getSFXIndex();
					packet.x = target.getPlayerBody().getPosition().x;
					packet.y = target.getPlayerBody().getPosition().y;
					game.addSFXPacket(packet);
				}
				
				validateSpellCast(caster);
			}
		}, CONFUSION(SpellType.WATER.getIndex(),5,(byte)-1,
			PlayerState.CAST2,4f,14f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
				Shape shape, Vector2 position) {
				// Finding target nearest to the cursor:
				final Player target = game.getNearestPlayer(position);
				
				if(target != null) {
					// Applying confusion:
					target.setConfused(true);
					target.setLastDamageDealer(caster);
					// Adding packet with SFX buff:
					SrvAttachSFX packet = new SrvAttachSFX();
					packet.SFXIndex = SFXIndexes.CONFUSION_BUFF.getSFXIndex();
					packet.entityIndex = target.getEntityIndex();
					packet.duration = this.getEfficiency()*caster.getDamageModificator();
					game.addSFXPacket(packet);
					
					// Removing confusion:
					target.scheduleEvent(new ScheduledEvent(new Runnable() {
						@Override
						public void run() {
							target.setConfused(false);
						}
					}, false, this.getEfficiency()*caster.getDamageModificator()));
				}
				validateSpellCast(caster);
			}
		}, SILENCE(SpellType.WATER.getIndex(),6,(byte)-1,
			PlayerState.CAST2,4f,16f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
				Shape shape, Vector2 position) {
				// Finding target nearest to the cursor:
				final Player target = game.getNearestPlayer(position);
				
				if(target != null) {
					// Applying effect:
					target.paralyzePlayer(true);
					target.setLastDamageDealer(caster);
					// Adding packet with SFX buff:
					SrvAttachSFX packet = new SrvAttachSFX();
					packet.SFXIndex = SFXIndexes.SILENCE_BUFF.getSFXIndex();
					packet.entityIndex = target.getEntityIndex();
					packet.duration = this.getEfficiency()*caster.getDamageModificator();
					game.addSFXPacket(packet);
					
					// Removing effect:
					target.scheduleEvent(new ScheduledEvent(new Runnable() {
						@Override
						public void run() {
							target.paralyzePlayer(false);
						}
					}, false, this.getEfficiency()*caster.getDamageModificator()));
				}
				validateSpellCast(caster);
			}
		},
		
		/** Earth spells. */
		QUAKE(SpellType.EARTH.getIndex(),0,(byte)-1,
			PlayerState.CAST2,-3.75f,9f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
					Shape shape,Vector2 position) {
				// Creating earthquake:
				game.getExplosionParticles().add(new ExplosionParticles
					(game,caster,ExplosionType.QUAKE,
					caster.getPlayerBody().getPosition()));
				// Adding packet with SFX explosion:
				SrvDisplaySFX packet = new SrvDisplaySFX();
				packet.SFXIndex = SFXIndexes.QUAKE_EXPLOSION.getSFXIndex();
				packet.x = caster.getPlayerBody().getPosition().x;
				packet.y = caster.getPlayerBody().getPosition().y;
				game.addSFXPacket(packet);
				
				validateSpellCast(caster);
			}
		}, POISON(SpellType.EARTH.getIndex(),1,(byte)4,
				PlayerState.CAST1,-(1f/60f),7.5f) {
				@Override
				public void cast(int entityIndex, Game game, Player caster,
					Shape shape,Vector2 position) {
				// Calculating angle and its cos&sin:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position), cos = MathUtils.cosDeg(angle),
					sin = MathUtils.sinDeg(angle);
				
				// Setting destination:
				Vector2 poisonDestination = new Vector2(position);
				// Setting actual spell position:
				position.set(caster.getPlayerBody().getPosition().x + cos,
					caster.getPlayerBody().getPosition().y + sin);
				
				// Creating body:
				Body projectile = game.getBox2DWorld().createBody
					(getDefaultProjectileBodyDef(position));
				
				// Creating fixture definition:
				shape.setRadius(0.25f);
				FixtureDef fixtureDefinition = new FixtureDef();
				fixtureDefinition.shape = shape;
				fixtureDefinition.density = 150f;
				fixtureDefinition.isSensor = true;
				
				// Creating fixture:
				projectile.createFixture(fixtureDefinition)
					.setUserData(BodyBehavior.SENSOR_PROJECTILE);
				
				// Applying force to the projectile:
				projectile.applyLinearImpulse(position.set(100f*cos,100f*sin),
					projectile.getWorldCenter(),true);
				
				// Creating a projectile object, so that it could be updated:
				projectile.setUserData(new Poison
					(entityIndex,projectile,caster,poisonDestination));

				// Adding projectile to the list:
				game.getProjectiles().add((Projectile)projectile.getUserData());
				
				validateSpellCast(caster, angle);
			}
		}, ENTANGLE(SpellType.EARTH.getIndex(),2,(byte)5,
			PlayerState.CAST1,4.5f,15f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
					Shape shape,Vector2 position) {
				// Calculating angle and its cos&sin:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position), cos = MathUtils.cosDeg(angle),
					sin = MathUtils.sinDeg(angle);
				// Setting actual spell position:
				position.set(caster.getPlayerBody().getPosition().x + cos,
					caster.getPlayerBody().getPosition().y + sin);
				
				// Creating body:
				Body projectile = game.getBox2DWorld().createBody
					(getDefaultProjectileBodyDef(position));
				
				// Creating fixture definition:
				shape.setRadius(0.25f);
				FixtureDef fixtureDefinition = new FixtureDef();
				fixtureDefinition.shape = shape;
				fixtureDefinition.density = 200f;
				fixtureDefinition.restitution = 0.6f;
				
				// Creating fixture:
				projectile.createFixture(fixtureDefinition)
					.setUserData(BodyBehavior.PROJECTILE);
				
				// Applying force to the projectile:
				projectile.applyLinearImpulse(position.set(400f*cos,400f*sin),
					projectile.getWorldCenter(),true);
				
				// Creating a projectile object, so that it could be updated:
				projectile.setUserData(new Entangle(entityIndex,projectile,caster));
				
				// Adding projectile to the list:
				game.getProjectiles().add((Projectile)projectile.getUserData());
				
				validateSpellCast(caster, angle);
			}
		}, CURE(SpellType.EARTH.getIndex(),3,(byte)-1,
			PlayerState.CAST2,1/6f,18.5f) {
			@Override
			public void cast(int entityIndex, Game game,final Player caster,
					Shape shape,Vector2 position) {
				// Finding target nearest to the cursor:
				final Player target = game.getNearestPlayer(position);
				
				if(target != null) {
					// Applying healing buff:
					target.scheduleEvent(new ScheduledEvent(new Runnable() {
						@Override
						public void run() {
							// Adding health:
							target.modifyHealth(caster, Spell.CURE.getEfficiency(),
								false, false);
						}
					}, true, 6f*caster.getDamageModificator()));
					// Trying to remove a curse:
					target.healCurse(0.6f*caster.getDamageModificator());
					
					// Adding packet with SFX healing:
					SrvAttachSFX packet = new SrvAttachSFX();
					packet.SFXIndex = SFXIndexes.CURE_BUFF.getSFXIndex();
					packet.entityIndex = target.getEntityIndex();
					packet.duration = 6f*caster.getDamageModificator();
					game.addSFXPacket(packet);
				}
				
				validateSpellCast(caster);
			}
		}, HOMING_ARROW(SpellType.EARTH.getIndex(),4,(byte)9,
			PlayerState.CAST1,-10f,9.5f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
				Shape shape, Vector2 position) {
				// Finding target nearest to the cursor:
				Player target = game.getNearestPlayer(position);
				
				if(target != null) {
					// Calculating angle and its cos&sin:
					float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
						.getPosition(), position), cos = MathUtils.cosDeg(angle),
						sin = MathUtils.sinDeg(angle);
					// Setting actual spell position:
					position.set(caster.getPlayerBody().getPosition().x + cos,
						caster.getPlayerBody().getPosition().y + sin);
					
					// Creating body:
					Body projectile = game.getBox2DWorld().createBody
						(getDefaultProjectileBodyDef(position));
					
					// Creating fixture definition:
					shape.setRadius(0.25f);
					FixtureDef fixtureDefinition = new FixtureDef();
					fixtureDefinition.shape = shape;
					fixtureDefinition.density = 200f;
					fixtureDefinition.restitution = 0.6f;
					
					// Creating fixture:
					projectile.createFixture(fixtureDefinition)
						.setUserData(BodyBehavior.SEEKING_PROJECTILE);
					
					// Creating a projectile object, so that it could be updated:
					projectile.setUserData(new HomingArrow(entityIndex,
							projectile,caster,target));
					
					// Adding projectile to the list:
					game.getProjectiles().add((Projectile)projectile.getUserData());
					
					validateSpellCast(caster, angle);
				}
				else {
					validateSpellCast(caster);
				}
			}
		}, SUMMON_BEAST(SpellType.EARTH.getIndex(),5,(byte)-1,
			PlayerState.CAST2,55f,60f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
				Shape shape, Vector2 position) {
				// Removing old summon:
				if(caster.hasSummon()) {
					caster.getSummon().killSummon();
				}
				
				// Calculating angle:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position);
				// Setting actual spell position:
				position.set(caster.getPlayerBody().getPosition().x + 1.5f*MathUtils.cosDeg(angle),
					caster.getPlayerBody().getPosition().y + 1.5f*MathUtils.sinDeg(angle));
				
				// Summoning minotaur:
				Summon minotaur = new Summon(entityIndex,game,PlayerClass.MINOTAUR,
					SummonType.MINOTAUR,position,caster,this.getEfficiency()
					*caster.getDamageModificator(),LogicUtils.getPlayerAngle(angle));
				
				// Adding the summon to the game's characters list:
				game.getCharacters().add(minotaur);
				
				// Telling all players that a new character has been summoned:
				SrvCreateCharacter characterPacket = new SrvCreateCharacter();
				characterPacket.characterIndex = minotaur.getEntityIndex();
				characterPacket.teamIndex = minotaur.getTeamIndex();
				characterPacket.playerName = minotaur.getPlayerName();
				characterPacket.characterClass = minotaur.getPlayerClass().getIndex();
				characterPacket.isElite = false; 
				characterPacket.x = minotaur.getPlayerBody().getPosition().x;
				characterPacket.y = minotaur.getPlayerBody().getPosition().y;
				for(Connection connection : game.getPlayers()) {
					connection.sendTCP(characterPacket);
				}
				
				validateSpellCast(caster, angle);
			}
		}, THORNS(SpellType.EARTH.getIndex(),2,(byte)11,
				PlayerState.CAST1,-5f,8.5f) {
				@Override
				public void cast(int entityIndex, Game game, Player caster,
						Shape shape,Vector2 position) {
					// Calculating angle and its cos&sin:
					float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
						.getPosition(), position) - 60f, cos, sin;
					
					for(int i=0; i<7; i++) {
						cos = MathUtils.cosDeg(angle+i*15f);
						sin = MathUtils.sinDeg(angle+i*15f);
						
						// Setting actual spell position:
						position.set(caster.getPlayerBody().getPosition().x
							+ cos,caster.getPlayerBody().getPosition().y + sin);
						
						// Creating body:
						Body projectile = game.getBox2DWorld().createBody
							(getDefaultProjectileBodyDef(position));
						
						// Creating fixture definition:
						shape.setRadius(0.1f);
						FixtureDef fixtureDefinition = new FixtureDef();
						fixtureDefinition.shape = shape;
						fixtureDefinition.density = 200f;
						fixtureDefinition.restitution = 0.6f;
						
						// Creating fixture:
						projectile.createFixture(fixtureDefinition)
							.setUserData(BodyBehavior.SEEKING_PROJECTILE);
						
						// Applying force to the projectile:
						projectile.applyLinearImpulse(position.set(150f*cos,150f*sin),
							projectile.getWorldCenter(),true);
						
						// Creating a projectile object, so that it could be updated:
						projectile.setUserData(new Thorn(game
							.getUniqueEntityIndex(),projectile,caster));
						
						// Adding projectile to the list:
						game.getProjectiles().add((Projectile)projectile.getUserData());
					}
					
					validateSpellCast(caster, angle+60f);
				}
			},
		
		/** Air spells. */
		HASTE(SpellType.AIR.getIndex(),0,(byte)-1,
			PlayerState.CAST2,2000f,8f) {
			@Override
			public void cast(int entityIndex, Game game,final Player caster,
					Shape shape,Vector2 position) {
				// Finding target nearest to the cursor:
				final Player target = game.getNearestPlayer(position);
				
				if(target != null) {
					// Calculating speed bonus:
					final float speedBonus = this.getEfficiency()*caster.getDamageModificator();
					
					// Applying speed buff:
					target.modifySpeed(speedBonus);
	
					// Adding packet with SFX healing:
					SrvAttachSFX packet = new SrvAttachSFX();
					packet.SFXIndex = SFXIndexes.HASTE_BUFF.getSFXIndex();
					packet.entityIndex = target.getEntityIndex();
					packet.duration = 4.5f*caster.getDamageModificator();
					game.addSFXPacket(packet);
					
					target.scheduleEvent(new ScheduledEvent(new Runnable() {
						@Override
						public void run() {
							// Lowering speed:
							target.modifySpeed(-speedBonus);
						}
					}, false, 4.5f*caster.getDamageModificator()));
				}
				
				validateSpellCast(caster);
			}
		}, LEAP(SpellType.AIR.getIndex(),1,(byte)-1,
			PlayerState.CAST2,30000f,7.5f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
					Shape shape,Vector2 position) {
				// Calculating angle and its cos&sin:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position);
				// Applying force impulse:
				caster.getPlayerBody().applyForceToCenter
					(MathUtils.cosDeg(angle)*this.getEfficiency()*caster.getDamageModificator(),
					MathUtils.sinDeg(angle)*this.getEfficiency()*caster.getDamageModificator(), true);

				// Adding packet with SFX leap:
				SrvDisplaySFX packet = new SrvDisplaySFX();
				packet.SFXIndex = SFXIndexes.LEAP_CAST.getSFXIndex();
				packet.x = caster.getPlayerBody().getPosition().x;
				packet.y = caster.getPlayerBody().getPosition().y;
				game.addSFXPacket(packet);
				
				validateSpellCast(caster, angle);
			}
		}, LIGHTNING_BOLT(SpellType.AIR.getIndex(),2,(byte)6,
			PlayerState.CAST1,-17.5f,7.5f) {
			@Override
			public void cast(final int entityIndex,final Game game,
					final Player caster,final Shape shape,Vector2 position) {
				// Calculating angle and its cos&sin:
				final float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
					.getPosition(), position), cos = MathUtils.cosDeg(angle),
					sin = MathUtils.sinDeg(angle);
				// Setting actual spell position:
				final Vector2 spellCastPosition = new Vector2(caster.getPlayerBody().getPosition().x + cos,
					caster.getPlayerBody().getPosition().y + sin);
				
				// Adding packet with SFX cast:
				SrvDisplaySFX packet = new SrvDisplaySFX();
				packet.SFXIndex = SFXIndexes.LIGHTNING_CAST.getSFXIndex();
				packet.x = spellCastPosition.x;
				packet.y = spellCastPosition.y;
				game.addSFXPacket(packet);
				
				// Scheduling actual lighting cast:
				game.scheduleEvent(new ScheduledEvent(new Runnable() {
					@Override
					public void run() {
						// Creating body:
						Body projectile = game.getBox2DWorld().createBody
							(getDefaultProjectileBodyDef(spellCastPosition));
						
						// Creating fixture definition:
						shape.setRadius(0.3f);
						FixtureDef fixtureDefinition = new FixtureDef();
						fixtureDefinition.shape = shape;
						fixtureDefinition.density = 150f;
						fixtureDefinition.restitution = 1f;
						
						// Creating fixture:
						projectile.createFixture(fixtureDefinition)
							.setUserData(BodyBehavior.BOUNCING_PROJECTILE);
						
						// Applying force to the projectile:
						projectile.applyLinearImpulse(spellCastPosition.set(1200f*cos,1200f*sin),
							projectile.getWorldCenter(),true);
						
						// Creating a projectile object, so that it could be updated:
						projectile.setUserData(new LightningBolt(entityIndex,projectile,caster));

						// Adding projectile to the list:
						game.getProjectiles().add((Projectile)projectile.getUserData());
					}
				}, false, 0.8f));
				
				validateSpellCast(caster, angle);
			}
		}, TORNADO(SpellType.AIR.getIndex(),3,(byte)7,
			PlayerState.CAST2,-7f,9.5f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
					Shape shape,Vector2 position) {
				// Calculating angle and its cos&sin:
				float angle = (float)LogicUtils.getAngle(caster.getPlayerBody()
						.getPosition(), position);
				
				// Creating projectile body:
				Body projectile = game.getBox2DWorld().createBody
					(getDefaultProjectileBodyDef(position));
				
				// Creating fixture definition:
				shape.setRadius(0.4f);
				FixtureDef fixtureDefinition = new FixtureDef();
				fixtureDefinition.shape = shape;
				fixtureDefinition.density = 400f;
				fixtureDefinition.restitution = 0.3f;
				
				// Creating fixture:
				projectile.createFixture(fixtureDefinition)
					.setUserData(BodyBehavior.BOUNCING_PROJECTILE);
				
				// Creating a projectile object, so that it could be updated:
				projectile.setUserData(new Tornado(entityIndex,projectile,caster));
	
				// Adding projectile to the list:
				game.getProjectiles().add((Projectile)projectile.getUserData());
				
				validateSpellCast(caster, angle);
			}
		}, SWAP(SpellType.AIR.getIndex(),4,(byte)8,
			PlayerState.CAST2,0f,18f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
				Shape shape, Vector2 position) {
				// Finding player nearest to the cursor:
				Player target = game.getNearestPlayer(position);
				target.setLastDamageDealer(caster);
				// If target exists, switching places:
				if(target != null && target != caster) {
					// If target is not shielded:
					if(!target.isShielded()) {
						// Saving caster position:
						position.set(caster.getPlayerBody().getPosition());
						// Setting target's position for the caster:
						caster.getPlayerBody().setTransform
							(target.getPlayerBody().getPosition(),
							caster.getPlayerBody().getAngle());
						// Setting caster's position for the target:
						target.getPlayerBody().setTransform(position,
							target.getPlayerBody().getAngle());
						// Cancelling destinations for both characters:
						caster.cancelDestination();
						caster.setTeleported(true);
						target.cancelDestination();
						target.setTeleported(true);
						
						// Adding packet with SFX cast for both players:
						SrvDisplaySFX casterPacket = new SrvDisplaySFX();
						casterPacket.SFXIndex = SFXIndexes.SWAP_CAST.getSFXIndex();
						casterPacket.x = caster.getPlayerBody().getPosition().x;
						casterPacket.y = caster.getPlayerBody().getPosition().y;
						game.addSFXPacket(casterPacket);
						SrvDisplaySFX targetPacket = new SrvDisplaySFX();
						targetPacket.SFXIndex = SFXIndexes.SWAP_CAST.getSFXIndex();
						targetPacket.x = target.getPlayerBody().getPosition().x;
						targetPacket.y = target.getPlayerBody().getPosition().y;
						game.addSFXPacket(targetPacket);
					}
				}
				
				validateSpellCast(caster);
			}
		}, TAUNT(SpellType.AIR.getIndex(),5,(byte)-1,
			PlayerState.CAST2,11f,12.5f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
				Shape shape, Vector2 position) {
				// Finding player nearest to the cursor:
				Player target = game.getNearestPlayer(position);
				
				// If target exists, changing velocity of projectiles around him:
				if(target != null) {
					// Calculating radius:
					float radius = this.getEfficiency() * caster.getDamageModificator(),
							totalVelocity,angle;
					// Checking if any projectiles are around the player:
					for(Projectile projectile : game.getProjectiles()) {
						if(LogicUtils.getDistance(target.getPlayerBody()
							.getPosition(), projectile.getProjectileBody()
							.getPosition()) <= radius) {
							// Changing projectile's caster:
							projectile.setCaster(caster,target);			
							// Getting projectile velocity:
							totalVelocity = Math.abs(projectile.getProjectileBody()
								.getLinearVelocity().x) + Math.abs(projectile.getProjectileBody()
								.getLinearVelocity().y);
							// Getting angle between the target and projectile:
							angle = (float)LogicUtils.getAngle(projectile.getProjectileBody()
								.getPosition(),target.getPlayerBody().getPosition());
							// Setting projectile's velocity:
							projectile.getProjectileBody().setLinearVelocity
								(totalVelocity*MathUtils.cosDeg(angle),
								totalVelocity*MathUtils.sinDeg(angle));
						}
					}
					
					// Adding packet with SFX:
					SrvAttachSFX packet = new SrvAttachSFX();
					packet.SFXIndex = SFXIndexes.TAUNT_BUFF.getSFXIndex();
					packet.entityIndex = target.getEntityIndex();
					packet.duration = -1f;
					game.addSFXPacket(packet);
				}
				
				validateSpellCast(caster);
			}
		}, LINK(SpellType.AIR.getIndex(),6,(byte)-1,
			PlayerState.CAST2,20000f,11.5f) {
			@Override
			public void cast(int entityIndex, Game game, Player caster,
				Shape shape, Vector2 position) {
				// Finding player nearest to the cursor:
				Player firstTarget = game.getNearestPlayer(position);
				
				// If target exists...
				if(firstTarget != null) {
					// Finding player closest to the target:
					Player secondTarget = game.getNearestPlayer(firstTarget);
					
					// If second target exists, pushing them into each other:
					if(secondTarget != null && secondTarget != firstTarget) {
						// Calculating angle and its cos&sin:
						float angle = (float)LogicUtils.getAngle(firstTarget
							.getPlayerBody().getPosition(),secondTarget
							.getPlayerBody().getPosition());
						
						// Applying force impulses:
						firstTarget.setLastDamageDealer(caster);
						firstTarget.getPlayerBody().applyForceToCenter
							(MathUtils.cosDeg(angle)*this.getEfficiency()*caster.getDamageModificator(),
							MathUtils.sinDeg(angle)*this.getEfficiency()*caster.getDamageModificator(), true);
						angle+=180f;
						secondTarget.setLastDamageDealer(caster);
						secondTarget.getPlayerBody().applyForceToCenter
							(MathUtils.cosDeg(angle)*this.getEfficiency()*caster.getDamageModificator(),
							MathUtils.sinDeg(angle)*this.getEfficiency()*caster.getDamageModificator(), true);
						
						// Adding packet with SFX:
						SrvDisplaySFX firstPacket = new SrvDisplaySFX();
						firstPacket.SFXIndex = SFXIndexes.LEAP_CAST.getSFXIndex();
						firstPacket.x = firstTarget.getPlayerBody().getPosition().x;
						firstPacket.y = firstTarget.getPlayerBody().getPosition().y;
						game.addSFXPacket(firstPacket);
						
						SrvDisplaySFX secondPacket = new SrvDisplaySFX();
						secondPacket.SFXIndex = SFXIndexes.LEAP_CAST.getSFXIndex();
						secondPacket.x = secondTarget.getPlayerBody().getPosition().x;
						secondPacket.y = secondTarget.getPlayerBody().getPosition().y;
						game.addSFXPacket(secondPacket);
					}
				}
				
				validateSpellCast(caster);
			}
		},;
		
		private final byte type;
		private final int index;
		private final byte projectileIndex;
		private final PlayerState characterAnimation;
		private final float efficiency,cooldown;
		private Spell(byte type,int index,byte projectileIndex,
			PlayerState characterAnimation,float efficiency,float cooldown) {
			this.type = type;
			this.index = index;
			this.projectileIndex = projectileIndex;
			this.characterAnimation = characterAnimation;
			this.efficiency = efficiency;
			this.cooldown = cooldown;
		}
		
		/**
		 * @return spell's projectile's animation index. -1 for none.
		 */
		public byte getProjectileIndex() {
			return projectileIndex;
		}
		
		/**
		 * @return spell's type.
		 */
		public byte getType() {
			return type;
		}
		
		/**
		 * @return spell's index in its type.
		 */
		public int getIndex() {
			return index;
		}
		
		/**
		 * @return spell's damage, duration etc.
		 */
		public float getEfficiency() {
			return efficiency;
		}
		
		/**
		 * @return player animation type shown after casting the spell.
		 */
		public PlayerState getCharacterAnimation() {
			return characterAnimation;
		}
		
		/**
		 * @return spell's recast time.
		 */
		public float getCooldown() {
			return cooldown;
		}
		
		/**
		 * Changes player's animation and adds spell cooldown.
		 * @param caster player who cast the spell.
		 */
		protected void validateSpellCast(Player caster) {
			// Changing player's animation:
			caster.setPlayerState(this.getCharacterAnimation());
			// Setting spell's cooldown:
			caster.applySpellCooldown(this.getType(), this.getCooldown());
		}
		
		/**
		 * Changes player's animation, faces him in the casting direction
		 * and adds spell cooldown.
		 * @param caster player who cast the spell.
		 * @param angle angle between the player and the cast.
		 */
		protected void validateSpellCast(Player caster,float angle) {
			// Changing player's animation:
			caster.setPlayerState(this.getCharacterAnimation());
			// Facing the caster in a new direction:
			caster.setDirection(LogicUtils.getPlayerAngle(angle));
			// Setting spell's cooldown:
			caster.applySpellCooldown(this.getType(), this.getCooldown());
		}
		
		/**
		 * Casts the chosen spell.
		 * @param entityIndex entity's index.
		 * @param world Box2D world.
		 * @param caster spell's caster.
		 * @param position spell's initial position.
		 * @return if the spell fires a projectile, the method will return it. If not - null.
		 */
		public abstract void cast(int entityIndex,Game game,Player caster,
				Shape shape,Vector2 position);
		
		/**
		 * Returns a definition of a dynamic body with fixed rotation.
		 * @param position position of the body.
		 * @return body definition.
		 */
		private static BodyDef getDefaultProjectileBodyDef(Vector2 position) {
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.DynamicBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(position);
			return bodyDefinition;
		}
	}
	
	/**
	 * Contains indexes of all SFX displayed by the client that need to be
	 * triggered by the server.
	 * @author MJ
	 */
	public static enum SFXIndexes {
		FIREBALL_EXPLOSION(1),
		METEOR_WARNING(2),
		METEOR_EXPLOSION(3),
		BLAZING_FEET_BUFF(4),
		BLAZING_FEET_FLAME(5),
		CURSE_BUFF(7),
		HEAL_BUFF(8),
		FREEZE_BUFF(10),
		QUAKE_EXPLOSION(15),
		SHIELD_BUFF(16),
		ENTANGLE_BUFF(18),
		CURE_BUFF(19),
		HASTE_BUFF(20),
		LEAP_CAST(21),
		LIGHTNING_CAST(22),
		MAGIC_MISSILE_EXPLOSION(26),
		PULSE_EXPLOSION(27),
		SWAP_CAST(28),
		CONFUSION_BUFF(29),
		TAUNT_BUFF(30),
		SILENCE_BUFF(31);
		
		private final int SFXIndex;
		private SFXIndexes(int SFXIndex) {
			this.SFXIndex = SFXIndex;
		}
		
		/**
		 * @return index of the special effect.
		 */
		public int getSFXIndex() {
			return SFXIndex;
		}
	}
}
