package mj.konfigurats.logic.physics;

import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.entities.ExplosionParticles;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.entities.Summon;
import mj.konfigurats.logic.entities.Teleport;
import mj.konfigurats.logic.physics.BodyInformation.BodyBehavior;
import mj.konfigurats.network.GamePackets.SrvSetEntityFalling;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;

/**
 * Utility class for some logic calculations. Do not initialize.
 * @author MJ
 */
public class LogicUtils {
	private LogicUtils() {}
	
	/** Possible player directions. */
	public static final byte W=0, NW=1, N=2, NE=3, E=4, SE=5, S=6, SW=7;
	
	/**
	 * Calculates the angle between 2 points.
	 * @param x1 first point's x.
	 * @param y1 first point's y.
	 * @param x2 second point's x.
	 * @param y2 second point's y.
	 * @return angle between the points.
	 */
	public static double getAngle(float x1,float y1,float x2,float y2) {
		if(x2-x1 == 0) {
			if(y2 > y1) {
				return 90;
			}
			else {
				return 270;
			}
		}
		
		if(y2 == y1) {
			if(x2 < x1) {
				return 180;
			}
			else {
				return 0;
			}
		}
		
		if(y2-y1 == 0) {
			if(x2 > x1) {
				return 0;
			}
			else {
				return 180;
			}
		}
		
		double angle = Math.toDegrees(Math.atan((y2-y1)/(x2-x1)));
		if(angle < 0) {
			angle+=180;
		}
		if(y2 < y1) {
			angle+=180;
		}
		
		return angle;
	}
	
	/**
	 * Calculates the angle between 2 points.
	 * @param point1 first point.
	 * @param point2 second point.
	 * @return angle between the points.
	 */
	public static double getAngle(Vector2 point1, Vector2 point2) {
		if(point2.x-point1.x == 0) {
			if(point2.y > point1.y) {
				return 90;
			}
			else {
				return 270;
			}
		}
		
		if(point2.y == point1.y) {
			if(point2.x < point1.x) {
				return 180;
			}
			else {
				return 0;
			}
		}
		
		double angle = Math.toDegrees(Math.atan((point2.y-point1.y)
			/(point2.x-point1.x)));
		if(angle < 0) {
			angle+=180;
		}
		if(point2.y < point1.y) {
			angle+=180;
		}
		
		return angle;
	}
	
	public static Vector2 getSinCos(Vector2 velocity) {
		float sum = Math.abs(velocity.x) + Math.abs(velocity.y);
		System.out.print(sum);
		return velocity.set(velocity.x/sum, velocity.y/sum);
	}

	/**
	 * Sets the character's direction according to the angle he's facing.
	 * @param currentPosition character's position.
	 * @param destination character's destination.
	 * @return new character's direction, <0,7>. -1 for a calculation error.
	 */
	public static byte getPlayerAngle(Vector2 currentPosition, Vector2 destination) {
		double angle = getAngle(currentPosition,destination);
		
		if(angle > 337.5d || angle <= 22.5d) {
			return E;
		}
		if(angle > 22.5d && angle <= 67.5d) {
			return NE;
		}
		if(angle > 67.5d && angle <= 112.5d) {
			return N;
		}
		if(angle > 112.5d && angle <= 157.5d) {
			return NW;
		}
		if(angle > 157.5d && angle <= 202.5d) {
			return W;
		}
		if(angle > 202.5d && angle <= 247.5d) {
			return SW;
		}
		if(angle > 247.5d && angle <= 292.5d) {
			return S;
		}
		if(angle > 292.5d && angle <= 337.5d) {
			return SE;
		}
		
		return -1;
	}
	
	/**
	 * Sets the character's direction according to the angle he's facing.
	 * @param angle player's angle.
	 * @return new character's direction, <0,7>. -1 for invalid angle.
	 */
	public static byte getPlayerAngle(float angle) {
		if(angle > 337.5d || angle <= 22.5d) {
			return E;
		}
		if(angle > 22.5d && angle <= 67.5d) {
			return NE;
		}
		if(angle > 67.5d && angle <= 112.5d) {
			return N;
		}
		if(angle > 112.5d && angle <= 157.5d) {
			return NW;
		}
		if(angle > 157.5d && angle <= 202.5d) {
			return W;
		}
		if(angle > 202.5d && angle <= 247.5d) {
			return SW;
		}
		if(angle > 247.5d && angle <= 292.5d) {
			return S;
		}
		if(angle > 292.5d && angle <= 337.5d) {
			return SE;
		}
		return -1;
	}
	
	/**
	 * Sets the character's direction according to the angle he's facing.
	 * @param currentPosition character's position.
	 * @param x facing location's x.
	 * @param y facing location's y.
	 * @return new character's direction, <0,7>. -1 for a calculation error.
	 */
	public static byte getPlayerAngle(Vector2 currentPosition, float x, float y) {
		double angle = getAngle(currentPosition.x,currentPosition.y,x,y);
		
		if(angle > 337.5d || angle <= 22.5d) {
			return E;
		}
		if(angle > 22.5d && angle <= 67.5d) {
			return NE;
		}
		if(angle > 67.5d && angle <= 112.5d) {
			return N;
		}
		if(angle > 112.5d && angle <= 157.5d) {
			return NW;
		}
		if(angle > 157.5d && angle <= 202.5d) {
			return W;
		}
		if(angle > 202.5d && angle <= 247.5d) {
			return SW;
		}
		if(angle > 247.5d && angle <= 292.5d) {
			return S;
		}
		if(angle > 292.5d && angle <= 337.5d) {
			return SE;
		}
		
		return -1;
	}
	
	/**
	 * Returns distance indicator - it can be used to measure which
	 * object is the closest to a certain point, but it doesn't calculate
	 * the actual distance between the points. Use getDistance instead.
	 * @param pointA first point.
	 * @param pointB second point.
	 * @return distance indicator between the points (distance^2).
	 */
	public static float getDistanceIndicator(Vector2 pointA, Vector2 pointB) {
		float deltaX = pointA.x - pointB.x, deltaY = pointA.y - pointB.y;
		return deltaX*deltaX+deltaY*deltaY;
	}
	
	/**
	 * Inverts a position relatively to a character. As opposed to the method
	 * with similar name from Game class (taking x and y), this method takes
	 * a Vector2 and is usually used by the game's logic rather than input handling.
	 * @param character character used to calculate relative position.
	 * @param position position to be inverted.
	 * @return inverted position.
	 */
	public static Vector2 getInvertedPosition(Player character,Vector2 position) {
		// Getting relative position:
		position.set(position.x - character.getPlayerBody().getPosition().x,
			position.y - character.getPlayerBody().getPosition().y);
		// Inverting relative position and returning actual position:
		return position.set(-position.x + character.getPlayerBody().getPosition().x,
			-position.y + character.getPlayerBody().getPosition().y);
	}
	
	/**
	 * Returns the distance between two points.
	 * @param pointA first point.
	 * @param pointB second point.
	 * @return actual distance, as opposed to getDistanceIndicator method.
	 */
	public static float getDistance(Vector2 pointA, Vector2 pointB) {
		float deltaX = pointA.x - pointB.x, deltaY = pointA.y - pointB.y;
		return (float)Math.sqrt(deltaX*deltaX+deltaY*deltaY);
	}
	
	public static ContactListener createContactListener() {
		return new ContactListener() {
			@Override
			public void beginContact(Contact contact) {
				// Checking collisions. Since we won't want to write a 100 cases,
				// we check two separate variants:
				checkCollisions(contact.getFixtureA(), contact.getFixtureB());
				checkCollisions(contact.getFixtureB(), contact.getFixtureA());
			}
			
			/**
			 * Used to check collisions between 2 fixtures.
			 */
			private void checkCollisions(Fixture fixtureA, Fixture fixtureB) {
				// Player or a summon hits...
				if(fixtureA.getUserData() == BodyBehavior.PLAYER ||
					fixtureA.getUserData() == BodyBehavior.SUMMON) {
					// Map bounds:
					if(fixtureB.getUserData() == BodyBehavior.BOUNDS) {
						((Player)(fixtureA.getBody()
							.getUserData())).cancelDestination();
					}
					// Regular projectile:
					else if(fixtureB.getUserData() == BodyBehavior.PROJECTILE ||
							fixtureB.getUserData() == BodyBehavior.SEEKING_PROJECTILE ||
							fixtureB.getUserData() == BodyBehavior.SENSOR_PROJECTILE ||
							fixtureB.getUserData() == BodyBehavior.BOUNCING_PROJECTILE) {
						// Projectile will (likely) hurt the player and may disappear:
						((Projectile)fixtureB.getBody().getUserData())
							.applyEffect((Player)fixtureA.getBody().getUserData());
					}
					// Explosion particle:
					else if(fixtureB.getUserData() == BodyBehavior.EXPLOSION_PARTICLE) {
						// Particle will (usually) damage the player:
						((ExplosionParticles)fixtureB.getBody().getUserData())
							.dealDamage((Player)fixtureA.getBody().getUserData(),
							fixtureB.getBody());
					}
					// A summon:
					else if(fixtureB.getUserData() == BodyBehavior.SUMMON) {
						// Summon might attack the player:
						((Summon)fixtureB.getBody().getUserData())
							.attack(((Player)fixtureA.getBody().getUserData()));
					}
					// Lava:
					else if(fixtureB.getUserData() == BodyBehavior.LAVA) {
						// Dealing damage:
						((Player)fixtureA.getBody().getUserData()).enterLava(true);
					}
					// Void:
					else if(fixtureB.getUserData() == BodyBehavior.VOID) {
						// Kills the player:
						Player fallingEntity = (Player)fixtureA.getBody().getUserData();
						fallingEntity.modifyHealth(null,-1000f, false, false);
						// Scheduling packet with falling player info:
						SrvSetEntityFalling packet = new SrvSetEntityFalling();
						packet.entityIndex = fallingEntity.getEntityIndex();
						packet.x = fallingEntity.getPlayerBody().getPosition().x;
						packet.y = fallingEntity.getPlayerBody().getPosition().y;
						fallingEntity.getGame().addSFXPacket(packet);
					}
				}
				// Exploding projectile hits...
				else if(fixtureA.getUserData() == BodyBehavior.EXPLODING_PROJECTILE) {
					// Projectile will explode on anything but void:
					if(fixtureB.getUserData() != BodyBehavior.VOID &&
						fixtureB.getUserData() != BodyBehavior.WARNING &&
						fixtureB.getUserData() != BodyBehavior.LAVA &&
						fixtureB.getUserData() != BodyBehavior.TELEPORT) {
						((Projectile)fixtureA.getBody().getUserData())
							.setTouched(true);
					}
				}
				// Seeking projectile hits...
				else if(fixtureA.getUserData() == BodyBehavior.SEEKING_PROJECTILE) {
					// Explosion particle, another seeking projectile or a block:
					if(fixtureB.getUserData() == BodyBehavior.EXPLOSION_PARTICLE ||
						fixtureB.getUserData() == BodyBehavior.SEEKING_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.CRACKING_PROJECTILE) {
						// Destroying projectile:
						((Projectile)fixtureA.getBody().getUserData())
							.setTouched(true);
					}
				}
				// Sensor projectile hits...
				else if(fixtureA.getUserData() == BodyBehavior.SENSOR_PROJECTILE) {
					// A wall:
					if(fixtureB.getUserData() == BodyBehavior.BOUNDS) {
						((Projectile)fixtureA.getBody().getUserData())
							.setTouched(true);
					}
				}
				// Cracking projectile hits...
				else if(fixtureA.getUserData() == BodyBehavior.CRACKING_PROJECTILE) {
					// A projectile:
					if(fixtureB.getUserData() == BodyBehavior.BOUNCING_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.SEEKING_PROJECTILE) {
						// Greatly damaging cracking projectile:
						((Projectile)fixtureA.getBody().getUserData())
							.applyEffect(null);
					}
					// Explosion particle:
					else if(fixtureB.getUserData() == BodyBehavior.EXPLOSION_PARTICLE) {
						// Lowering cracking projectile's duration:
						((ExplosionParticles)fixtureB.getBody().getUserData())
							.dealDamage((Projectile)fixtureA.getBody().getUserData());
					}
					// A summon:
					else if(fixtureB.getUserData() == BodyBehavior.SUMMON) {
						// Applying summon's AI to avoid the block:
						((Summon)fixtureB.getBody().getUserData())
							.avoidBlock((Projectile)fixtureA.getBody().getUserData());
					}
					// Void or lava:
					else if(fixtureB.getUserData() == BodyBehavior.VOID ||
							fixtureB.getUserData() == BodyBehavior.LAVA) {
						// Destroying projectile:
						((Projectile)fixtureA.getBody().getUserData()).setTouched(true);
					}
				}
				// Teleportation:
				else if(fixtureA.getUserData() == BodyBehavior.TELEPORT) {
					if(fixtureB.getUserData() == BodyBehavior.PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.SEEKING_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.SENSOR_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.BOUNCING_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.EXPLODING_PROJECTILE) {
						// Teleporting a projectile:
						final Teleport teleport = (Teleport)fixtureA.getBody().getUserData();
						final Projectile projectile = (Projectile)fixtureB.getBody().getUserData();
						teleport.getGame().scheduleEvent(new ScheduledEvent(new Runnable() {
							@Override
							public void run() {
								teleport.getGame().teleport(projectile,teleport);
							}
						}, false, 0f));
					}
					else if(fixtureB.getUserData() == BodyBehavior.SUMMON ||
						fixtureB.getUserData() == BodyBehavior.PLAYER) {
						// Teleporting a player:
						final Teleport teleport = (Teleport)fixtureA.getBody().getUserData();
						final Player player = (Player)fixtureB.getBody().getUserData();
						teleport.getGame().scheduleEvent(new ScheduledEvent(new Runnable() {
							@Override
							public void run() {
								teleport.getGame().teleport(player,teleport);
							}
						}, false, 0f));
					}
				}
				// Trying to warn a summon - there's void ahead!
				else if(fixtureA.getUserData() == BodyBehavior.WARNING) {
					if(fixtureB.getUserData() == BodyBehavior.SUMMON) {
						((Summon)fixtureB.getBody().getUserData())
							.avoidVoid(fixtureA.getBody().getPosition());
					}
				}
				// Something (or someone) got out of the map bounds:
				else if(fixtureA.getUserData() == BodyBehavior.OUTER_BOUNDS) {
					// Destroying: projectile:
					if(fixtureB.getUserData() == BodyBehavior.PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.SEEKING_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.SENSOR_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.BOUNCING_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.EXPLODING_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.CRACKING_PROJECTILE) {
						((Projectile)fixtureB.getBody().getUserData()).setTouched(true);
					}
					// Killing summon:
					else if(fixtureB.getUserData() == BodyBehavior.SUMMON) {
						((Summon)fixtureB.getBody().getUserData()).killSummon();
					}
					// Killing player:
					else if(fixtureB.getUserData() == BodyBehavior.PLAYER) {
						((Player)fixtureB.getBody().getUserData()).modifyHealth(null,
							-1000f, false, false);
					}
				}
			}

			@Override
			public void endContact(Contact contact) {
				checkEndingCollisions(contact.getFixtureA(), contact.getFixtureB());
				checkEndingCollisions(contact.getFixtureB(), contact.getFixtureA());
			}
			
			/**
			 * Checks if something should be done with the ending collisions.
			 * @param fixtureA first fixture of the collision.
			 * @param fixtureB second fixture of the collision.
			 */
			private void checkEndingCollisions(Fixture fixtureA, Fixture fixtureB) {
				//If a player gets out of lava:
				if(fixtureA.getUserData() == BodyBehavior.LAVA) {
					if(fixtureB.getUserData() == BodyBehavior.PLAYER ||
						fixtureB.getUserData() == BodyBehavior.SUMMON) {
						((Player)fixtureB.getBody().getUserData()).enterLava(false);
					}
				}
				// If a player or projectile gets out of teleport:
				else if(fixtureA.getUserData() == BodyBehavior.TELEPORT) {
					if(fixtureB.getUserData() == BodyBehavior.PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.SEEKING_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.SENSOR_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.BOUNCING_PROJECTILE ||
						fixtureB.getUserData() == BodyBehavior.CRACKING_PROJECTILE) {
						// Resetting last touched teleport:
						((Projectile)fixtureB.getBody().getUserData())
							.resetLastUsedTeleport(((Teleport)fixtureA.getBody()
							.getUserData()).getEntityIndex());
						
					}
					else if(fixtureB.getUserData() == BodyBehavior.SUMMON ||
						fixtureB.getUserData() == BodyBehavior.PLAYER) {
						((Teleport)fixtureA.getBody().getUserData()).getGame()
							.teleport((Player)fixtureB.getBody().getUserData(),
							(Teleport)fixtureA.getBody().getUserData());
						// Resetting last touched teleport:
						((Player)fixtureB.getBody().getUserData())
							.resetLastUsedTeleport(((Teleport)fixtureA.getBody()
							.getUserData()).getEntityIndex());
					}
				}
			}
			
			@Override
			public void preSolve(Contact contact, Manifold oldManifold) {}
			@Override
			public void postSolve(Contact contact, ContactImpulse impulse) {}
		};
	}
}
