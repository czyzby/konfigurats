package mj.konfigurats.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import mj.konfigurats.Core;
import mj.konfigurats.game.entities.Entity;
import mj.konfigurats.game.entities.Player;
import mj.konfigurats.game.entities.Player.PlayerAnimationType;
import mj.konfigurats.game.entities.Player.PlayerClass;
import mj.konfigurats.game.entities.Projectile;
import mj.konfigurats.game.entities.Projectile.ProjectileType;
import mj.konfigurats.game.entities.SFX;
import mj.konfigurats.game.entities.SFX.SFXType;
import mj.konfigurats.game.utilities.Maps.MapInfo;
import mj.konfigurats.game.utilities.SpellUtils;
import mj.konfigurats.game.utilities.SpellUtils.SpellType;
import mj.konfigurats.gui.screens.GamesScreen;
import mj.konfigurats.network.GamePackets.*;

import java.util.HashMap;

/**
 * A manager for a single arena.
 * @author MJ
 */
public class BattleManager {
	// Tiled map:
	private TiledMap map;
	private IsometricTiledMapRenderer mapRenderer;
	private OrthographicCamera mapCamera;

	// Entities:
	private Player userCharacter;
	private HashMap<Integer,Entity> entities;
	private Array<Entity> entityList,entitiesToRemove;
	private Array<Player> playersToRemove;

	// Control variables:
	public final static float CAMERA_RATIO=24;
	private final int roomIndex;
	private final float yOffset;
	private float cameraShakeX,cameraShakeY;
	private long updateIndex;

	// Temporary values:
	private Vector2 tempVector2;
	private Vector3 tempVector3;

	public BattleManager(int roomIndex,int mapIndex) {
		// Assigning variables:
		this.roomIndex = roomIndex;
		map = MapInfo.getTiledMap(mapIndex);

		// Creating tiled map utility objects:
		mapCamera = new OrthographicCamera(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		mapRenderer = new IsometricTiledMapRenderer(this.map);
		entities = new HashMap<Integer,Entity>();
		entityList = new Array<Entity>();
		entitiesToRemove = new Array<Entity>();
		playersToRemove = new Array<Player>();

		// Initializing temporary values:
		tempVector2 = new Vector2();
		tempVector3 = new Vector3();
		updateIndex = Long.MIN_VALUE;

		// Calculating map offset:
		yOffset = map.getProperties().get("tileheight",int.class)/2;

		// Reading map objects:
		for(MapObject mapObject : map.getLayers().get("objects").getObjects()) {
			if(mapObject.getProperties().containsKey("TELEPORT")) {
				// Adding teleport SFX:
				entityList.add(new SFX(SFXType.TELEPORT,tiledToMapCoords(((RectangleMapObject)mapObject)
					.getRectangle().getPosition(tempVector2).add(-34f,60f)),-1f));
			}
		}

		// Setting up map camera:
		mapCamera.position.set((map.getProperties().get("width",int.class)/2)
			*map.getProperties().get("tilewidth",int.class),yOffset,0);
		mapCamera.update();

		// Sending packet to the server:
		((Core)Gdx.app.getApplicationListener()).getNetworkManager()
			.sendTCP(new CltGameInitiated());
	}

	/**
	 * Disposes of the map renderer.
	 */
	public void dispose() {
		mapRenderer.dispose();
	}

	/**
	 * Updates sprites positions, schedules animations.
	 * @param packet server's packet.
	 */
	public void update(SrvUpdateWorld packet) {
		// Making sure the packet is from the right room:
		if(packet.gameRoomIndex == roomIndex) {
			// Making sure the packet is up-to-date:
			if(packet.updateIndex > updateIndex) {
				// Setting the new update index:
				updateIndex = packet.updateIndex;

				// Setting entities data:
				for(int i=0; i<packet.charactersIndexes.length; i++) {
					if(entities.containsKey(packet.charactersIndexes[i])) {
						Player player = (Player)entities.get(packet.charactersIndexes[i]);
						// Setting position:
						player.updatePosition(box2DToMapCoords(packet.charactersPositions[i*2],
							packet.charactersPositions[i*2+1]));
						// Setting direction:
						player.setDirection(packet.charactereDisplayData[i*2]);
						// Setting animation state:
						player.setAnimation(PlayerAnimationType.getAnimationType
							(packet.charactereDisplayData[i*2+1]));
						// Making sure the entity is checked:
						player.setUpdated(true);
					}
				}

				// Setting projectiles data:
				for(int i=0; i<packet.projectileIndexes.length; i++) {
					// Projectile exists:
					if(entities.containsKey(packet.projectileIndexes[i])) {
						Projectile projectile = (Projectile)
							entities.get(packet.projectileIndexes[i]);
						// Setting position:
						projectile.updatePosition(box2DToMapCoords(packet.projectileDisplayData[i*4],
							packet.projectileDisplayData[i*4+1]));
						// Setting angle:
						projectile.setAngle(SpellUtils.getAngle(packet.projectileDisplayData[i*4+2],
							packet.projectileDisplayData[i*4+3]));
						// Making sure the entity is checked:
						projectile.setUpdated(true);
					}
					// Projectile doesn't exist:
					else {
						Projectile projectile = new Projectile(packet.projectileIndexes[i],
							ProjectileType.getProjectileType(packet.projectileAnimations[i]),
							box2DToMapCoords(packet.projectileDisplayData[i*4],
							packet.projectileDisplayData[i*4+1]),SpellUtils.getAngle(packet.projectileDisplayData[i*4+2],
							packet.projectileDisplayData[i*4+3]));
						projectile.setUpdated(true);
						entities.put(projectile.getEntityIndex(), projectile);
						entityList.add(projectile);
					}
				}

				// Checking if all entities have been updated:
				for(Entity entity : entityList) {
					// Was updated - resetting status:
					if(!entity.wasUpdated()) {
						entitiesToRemove.add(entity);
					}
					entity.setUpdated(false);
				}
				// Removing no longer existent entities:
				for(Entity entity : entitiesToRemove) {
					if(entity instanceof Player) {
						// We're removing characters that have been
						// mentioned before to prevent deleting
						// new characters due to receiving old packets.
						if(((Player)entity).wasAlreadyUpdated()) {
							((Player)entity).setDead();
						}
					}
					else {
						// Removing entity from the lists:
						entityList.removeValue(entity,true);
						// Displaying projectile's destruction SFX:
						if(entity instanceof Projectile) {
							entities.remove(entity.getEntityIndex());
							((Projectile)entity).displaySFX();
						}
					}
				}
				entitiesToRemove.clear();

				// Sorting sprites:
				entityList.sort();
			}
		}
	}

	/**
	 * Updates entities list by creating a new character.
	 * @param packet character's informations.
	 */
	public void update(SrvCreateCharacter packet) {
		if(!entities.containsKey(packet.characterIndex)) {
			// Creating a new character and adding him to the entities list:
			Player player = new Player(packet.characterIndex,packet.teamIndex,
				packet.playerName,packet.isElite,PlayerClass.getPlayerClass
				(packet.characterClass),box2DToMapCoords(packet.x, packet.y));
			entities.put(player.getEntityIndex(), player);
			entityList.add(player);

			// If the player is the owner of the character, focus him:
			if(packet.playerName.equals(((Core)Gdx.app.getApplicationListener())
				.getNetworkManager().getUsername())) {
				userCharacter = player;
			}
		}
	}

	/**
	 * If an entity dies by falling into the void, its sprite is scaled.
	 * @param packet contains info about a falling entity.
	 */
	public void setEntityFalling(SrvSetEntityFalling packet) {
		if(entities.containsKey(packet.entityIndex)) {
			((Player)entities.get(packet.entityIndex)).setFalling();
			entities.get(packet.entityIndex).updatePosition
				(box2DToMapCoords(packet.x,packet.y));
		}
	}

	/**
	 * @return list of current players scheduled to be removed.
	 */
	public Array<Player> getPlayersToRemove() {
		return playersToRemove;
	}

	/**
	 * @return current player's character or null if none.
	 */
	public Player getUserCharacter() {
		return userCharacter;
	}

	/**
	 * Adding a SFX effect to the entity list, so it can be sorted
	 * and properly displayed along with players and projectiles.
	 * @param sfx SFX effect.
	 */
	public void addSFX(SFX sfx) {
		entityList.add(sfx);
		entityList.sort();
	}

	/**
	 * Displays a special effect requested by the server.
	 * @param sfxPacket SFX data.
	 */
	public void displaySFX(SrvDisplaySFX sfxPacket) {
		// Getting SFX type:
		SFXType sfx = SFXType.getSFXType(sfxPacket.SFXIndex);
		// If SFX exists, display it:
		if(sfx != null) {
			addSFX(new SFX(sfx,box2DToMapCoords(sfxPacket.x, sfxPacket.y),-1f));
		}
	}

	/**
	 * Attaches a special effect requested by the server to a character.
	 * @param sfxPacket SFX data.
	 */
	public void displaySFX(SrvAttachSFX sfxPacket) {
		// Getting SFX type:
		SFXType sfx = SFXType.getSFXType(sfxPacket.SFXIndex);
		// If SFX exists:
		if(sfx != null) {
			Entity player = entities.get(sfxPacket.entityIndex);
			// If player exists - attach SFX to him:
			if(player != null) {
				addSFX(new SFX(sfx,(Player)player,sfxPacket.duration));
			}
		}
	}

	/**
	 * Schedules camera shake.
	 */
	public void shakeCamera() {
		// Setting random camera shakes:
		cameraShakeX += getRandomCameraShake();
		cameraShakeY += getRandomCameraShake();

		// Scheduling random camera shakes:
		Timer.schedule(new Task() {
			@Override
			public void run() {
				cameraShakeX += getRandomCameraShake();
				cameraShakeY += getRandomCameraShake();
			}
		}, 0.05f, 0.05f, 3);
	}

	/**
	 * @return random (negative or positive) value
	 * that represents the strength of a camera shake.
	 */
	private float getRandomCameraShake() {
		float shake = MathUtils.random()*7f+2f;
		if(MathUtils.randomBoolean()) {
			shake = -shake;
		}
		return shake;
	}

	/**
	 * Validates the camera shake, trying to get it to 0.
	 * @param delta time past since the last update.
	 */
	private void validateCameraShake(float delta) {
		// Validating X shake:
		if(cameraShakeX > 0f) {
			cameraShakeX -= delta*60;
			if(cameraShakeX <= 0f) {
				cameraShakeX = 0f;
			}
		}
		else if(cameraShakeX <0f) {
			cameraShakeX += delta*60;
			if(cameraShakeX >= 0f) {
				cameraShakeX = 0f;
			}
		}
		// Validating Y shake:
		if(cameraShakeY > 0f) {
			cameraShakeY -= delta*60;
			if(cameraShakeY <= 0f) {
				cameraShakeY = 0f;
			}
		}
		else if(cameraShakeY <0f) {
			cameraShakeY += delta*60;
			if(cameraShakeY >= 0f) {
				cameraShakeY = 0f;
			}
		}
	}

	/**
	 * Updates the cameras and draws map with its sprites.
	 * @param delta time passed since the last rendering.
	 */
	public void render(float delta) {
		// Removing dead players:
		for(Player player : playersToRemove) {
			// Removing player from entity lists.
			entityList.removeValue(player,true);
			// Removing player's SFX effects:
			for(SFX sfx : player.getSFXEffects()) {
				entityList.removeValue(sfx, true);
			}
			entities.remove(player.getEntityIndex());
			if(player.equals(userCharacter)) {
				userCharacter = null;
				// Hiding game interface:
				((GamesScreen)((Core)Gdx.app.getApplicationListener())
					.getInterfaceManager().getCurrentScreen()).hideGameInterface();
			}
		}
		playersToRemove.clear();

		// Setting up the camera:
		if(userCharacter != null) {
			validateCameraShake(delta);

			if(!mapCamera.position.equals(tempVector3.set(userCharacter.getX()
				+cameraShakeX,userCharacter.getY()+cameraShakeY, 0))) {
				mapCamera.position.set(tempVector3);
				mapCamera.update();
			}
		}

		// Drawing the arena:
		mapRenderer.setView(mapCamera);
		mapRenderer.getBatch().begin();
			// Rendering background:
			mapRenderer.renderTileLayer((TiledMapTileLayer)map
				.getLayers().get("background"));
			// Rendering sprites:
			for(Entity entity : entityList) {
				entity.render((SpriteBatch)mapRenderer
					.getBatch(), delta);
			}
			// Rendering foreground:
			mapRenderer.renderTileLayer((TiledMapTileLayer)map
				.getLayers().get("foreground"));

		mapRenderer.getBatch().end();
	}

	/**
	 * Should be called when the screen is resized.
	 * @param width new screen width.
	 * @param height new screen height.
	 */
	public void setSize(int width, int height) {
		mapCamera.viewportWidth = width;
		mapCamera.viewportHeight = height;
		mapCamera.update();
	}


	/**
	 * Calculates the Box2D coordinates and informs the server about the click.
	 * @param x clicked position's x.
	 * @param y clicked position's y.
	 */
	public void handleClick(float x,float y) {
		// Getting Box2D coordinates:
		mapCamera.unproject(tempVector3.set(x,y,0));
		tempVector2 = mapToBox2DCoords(tempVector3.x, tempVector3.y);

		// Sending packet to the server:
		CltHandleClick packet = new CltHandleClick();
		packet.x = tempVector2.x;
		packet.y = tempVector2.y;
		((Core)Gdx.app.getApplicationListener()).getNetworkManager()
			.sendUDP(packet);
	}

	/**
	 * Calculates the Box2D coordinates and informs the server about the spell cast.
	 */
	public void handleSpellCast(SpellType spellType) {
		// Getting Box2D coordinates:
		mapCamera.unproject(tempVector3.set(Gdx.input.getX(),Gdx.input.getY(),0));
		tempVector2 = mapToBox2DCoords(tempVector3.x, tempVector3.y);

		// Preparing packet with the casting data:
		GamePacket packet = null;
		switch(spellType) {
			case FIRE:
				packet = new CltHandleFireCast();
				((CltHandleFireCast)packet).x = tempVector2.x;
				((CltHandleFireCast)packet).y = tempVector2.y;
				break;
			case WATER:
				packet = new CltHandleWaterCast();
				((CltHandleWaterCast)packet).x = tempVector2.x;
				((CltHandleWaterCast)packet).y = tempVector2.y;
				break;
			case EARTH:
				packet = new CltHandleEarthCast();
				((CltHandleEarthCast)packet).x = tempVector2.x;
				((CltHandleEarthCast)packet).y = tempVector2.y;
				break;
			case AIR:
				packet = new CltHandleAirCast();
				((CltHandleAirCast)packet).x = tempVector2.x;
				((CltHandleAirCast)packet).y = tempVector2.y;
				break;
		}

		// Sending packet to the server:
		((Core)Gdx.app.getApplicationListener()).getNetworkManager()
			.sendUDP(packet);
	}

	/**
	 * @param x Box2D x position.
	 * @param y Box2D y position.
	 * @return tiled map coordinates.
	 */
	private Vector2 box2DToMapCoords(float x,float y) {
		return tempVector2.set(x*CAMERA_RATIO,y*CAMERA_RATIO+yOffset);
	}

	/**
	 * @param x tiled map x position.
	 * @param y tiled map y position.
	 * @return box2D coordinates.
	 */
	private Vector2 mapToBox2DCoords(float x,float y) {
		return tempVector2.set(x/CAMERA_RATIO,(y-yOffset)/CAMERA_RATIO);
	}

	/**
	 * Converts TMX (Tiled editor map format) coordinates into actual map coordinates.
	 * Should be used to read the map objects.
	 * @param tiledCoords TMX coordinates.
	 * @return map coordinates.
	 */
	private Vector2 tiledToMapCoords(Vector2 tiledCoords) {
		return tempVector2.set(tiledCoords.x + tiledCoords.y,
			-(tiledCoords.x - tiledCoords.y)/2f);
	}
}
