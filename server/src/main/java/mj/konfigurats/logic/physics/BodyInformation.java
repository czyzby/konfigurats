package mj.konfigurats.logic.physics;

import mj.konfigurats.logic.entities.Entity;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

/**
 * Contains informations about characters and map objects.
 * Other entities (like spells) got they information stored elsewhere.
 * @author MJ
 */
public enum BodyInformation {
	BOUNDS(BodyBehavior.BOUNDS) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating body definition:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.StaticBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			// Creating fixture definition:
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.restitution = 0.75f;
			fixtureDefinition.friction = 0.25f;
			
			// Creating obstacle's Box2D body:
			world.createBody(bodyDefinition).createFixture(fixtureDefinition)
				.setUserData(BodyBehavior.BOUNDS);
			
			// Since game manager doesn't need a reference to the bounds body, returning null:
			return null;
		}
	},
	TELEPORT(BodyBehavior.TELEPORT) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating body definition:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.StaticBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			// Creating fixture definition:
			shape.setRadius(1f);
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.isSensor = true;
			
			// Creating obstacle's Box2D body:
			Body teleport = world.createBody(bodyDefinition);
			teleport.setUserData(owner);
			teleport.createFixture(fixtureDefinition)
				.setUserData(BodyBehavior.TELEPORT);
			
			return teleport;
		}
	},
	LAVA(BodyBehavior.LAVA) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating body definition:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.StaticBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			// Creating fixture definition:
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.isSensor = true;
			
			// Creating obstacle's Box2D body:
			world.createBody(bodyDefinition).createFixture(fixtureDefinition)
				.setUserData(BodyBehavior.LAVA);
			
			// Since game manager doesn't need a reference to the lava body, returning null:
			return null;
		}
	},
	VOID(BodyBehavior.VOID) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating body definition:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.StaticBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			// Creating fixture definition:
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.isSensor = true;
			
			// Creating obstacle's Box2D body:
			world.createBody(bodyDefinition).createFixture(fixtureDefinition)
				.setUserData(BodyBehavior.VOID);
			
			// Since game manager doesn't need a reference to the lava body, returning null:
			return null;
		}
	},
	WARNING(BodyBehavior.WARNING) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating body definition:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.StaticBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			// Creating fixture definition:
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.isSensor = true;
			
			// Creating obstacle's Box2D body:
			world.createBody(bodyDefinition).createFixture(fixtureDefinition)
				.setUserData(BodyBehavior.WARNING);
			
			// Since game manager doesn't need a reference to the lava body, returning null:
			return null;
		}
	},
	OUTER_BOUNDS(BodyBehavior.OUTER_BOUNDS) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating body definition:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.StaticBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			// Creating fixture definition:
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.isSensor = true;
			
			// Creating obstacle's Box2D body:
			world.createBody(bodyDefinition).createFixture(fixtureDefinition)
				.setUserData(BodyBehavior.OUTER_BOUNDS);
			
			// Since game manager doesn't need a reference to the lava body, returning null:
			return null;
		}
	},
	LICH(BodyBehavior.PLAYER) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating Box2D body:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.DynamicBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			Body body = world.createBody(bodyDefinition);
			
			// Creating player's fixture:
			shape.setRadius(0.5f);
			
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.density = 100;
			fixtureDefinition.restitution = 0.1f;
			fixtureDefinition.friction = 0.3f;
			
			body.createFixture(fixtureDefinition).setUserData
				(BodyBehavior.PLAYER);
			
			// Setting the owner of the body as the player data for the object:
			body.setUserData(owner);
			
			return body;
		}
	},
	
	GOBLIN(BodyBehavior.PLAYER) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating Box2D body:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.DynamicBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			Body body = world.createBody(bodyDefinition);
			
			// Creating player's fixture:
			shape.setRadius(0.45f);
			
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.density = 80;
			fixtureDefinition.restitution = 0.25f;
			fixtureDefinition.friction = 0.25f;
			
			body.createFixture(fixtureDefinition).setUserData
				(BodyBehavior.PLAYER);
			
			// Setting the owner of the body as the player data for the object:
			body.setUserData(owner);
			
			return body;
		}
	},
	
	WITCH(BodyBehavior.PLAYER) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating Box2D body:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.DynamicBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			Body body = world.createBody(bodyDefinition);
			
			// Creating player's fixture:
			shape.setRadius(0.475f);
			
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.density = 120;
			fixtureDefinition.restitution = 0.1f;
			fixtureDefinition.friction = 0.2f;
			
			body.createFixture(fixtureDefinition).setUserData
				(BodyBehavior.PLAYER);
			
			// Setting the owner of the body as the player data for the object:
			body.setUserData(owner);
			
			return body;
		}
	},
	
	JUGGERNAUT(BodyBehavior.PLAYER) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating Box2D body:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.DynamicBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			Body body = world.createBody(bodyDefinition);
			
			// Creating player's fixture:
			shape.setRadius(0.525f);
			
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.density = 140;
			fixtureDefinition.restitution = 0.075f;
			fixtureDefinition.friction = 0.4f;
			
			body.createFixture(fixtureDefinition).setUserData
				(BodyBehavior.PLAYER);
			
			// Setting the owner of the body as the player data for the object:
			body.setUserData(owner);
			
			return body;
		}
	},
	
	MINOTAUR(BodyBehavior.SUMMON) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating Box2D body:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.DynamicBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			Body body = world.createBody(bodyDefinition);
			
			// Creating player's fixture:
			shape.setRadius(0.65f);
			
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.density = 140;
			fixtureDefinition.restitution = 0.075f;
			fixtureDefinition.friction = 0.4f;
			
			body.createFixture(fixtureDefinition).setUserData
				(BodyBehavior.SUMMON);
			
			// Setting the owner of the body as the player data for the object:
			body.setUserData(owner);
			
			return body;
		}
	},
	
	FIRE_ANT(BodyBehavior.SUMMON) {
		@Override
		public Body createNewBody(World world,Shape shape,Entity owner,float x,float y) {
			// Creating Box2D body:
			BodyDef bodyDefinition = new BodyDef();
			bodyDefinition.type = BodyType.DynamicBody;
			bodyDefinition.fixedRotation = true;
			bodyDefinition.position.set(x,y);
			
			Body body = world.createBody(bodyDefinition);
			
			// Creating player's fixture:
			shape.setRadius(0.5f);
			
			FixtureDef fixtureDefinition = new FixtureDef();
			fixtureDefinition.shape = shape;
			fixtureDefinition.density = 100;
			fixtureDefinition.restitution = 0.2f;
			fixtureDefinition.friction = 0.2f;
			
			body.createFixture(fixtureDefinition).setUserData
				(BodyBehavior.SUMMON);
			
			// Setting the owner of the body as the player data for the object:
			body.setUserData(owner);
			
			return body;
		}
	};
	
	private final BodyBehavior bodyType;
	private BodyInformation(BodyBehavior bodyType) {
		this.bodyType = bodyType;
	}
	
	public BodyBehavior getBodyBehavior() {
		return bodyType;
	}
	
	/**
	 * Creates a Box2D body at the given position. If the object doesn't require
	 * additional data (like map properties), the body will have a fixture.
	 * Other objects - like map obstacles - will return bodies without fixtures.
	 * @param world Box2D world.
	 * @param x x position.
	 * @param y y position.
	 * @return the created body.
	 */
	public abstract Body createNewBody(World world,Shape shape,Entity owner,float x,float y);

	/**
	 * Contains all types of bodies' behavior. Used by contact listener.
	 * @author MJ
	 */
	public static enum BodyBehavior {
		/** Body belongs to a player. */
		PLAYER,
		/** Body belongs to a summon. */
		SUMMON,
		/** Body disappears after contact with a player or after some time. */
		PROJECTILE,
		/** Body disappears after contact with a player, explosion or block. */
		SEEKING_PROJECTILE,
		/** Body disappears after after some time. */
		BOUNCING_PROJECTILE,
		/** Body disappears after some time and does not collide. */
		SENSOR_PROJECTILE,
		/** Body disappears after any contact. */
		EXPLODING_PROJECTILE,
		/** Body gets damaged by projectiles and explosions. */
		CRACKING_PROJECTILE,
		/** Body is a map obstacle. */
		OBSTACLE,
		/** Body is an explosion particle and disappears after a short time. */
		EXPLOSION_PARTICLE,
		/** Body teleports projectiles and players.*/
		TELEPORT,
		/** Body belongs to a warning for the summon AI. */
		WARNING,
		/** Body belongs to the map bounds. */
		BOUNDS,
		/** Body outside the actual map, destroys everything. */
		OUTER_BOUNDS,
		/** Body belongs to the map void, characters die when they touch void. */
		VOID,
		/** Body belongs to a laval "obstacle". */
		LAVA;
	}
}


