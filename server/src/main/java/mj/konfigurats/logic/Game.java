package mj.konfigurats.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import mj.konfigurats.logic.entities.Entity;
import mj.konfigurats.logic.entities.ExplosionParticles;
import mj.konfigurats.logic.entities.Player;
import mj.konfigurats.logic.entities.Player.PlayerClass;
import mj.konfigurats.logic.entities.Projectile;
import mj.konfigurats.logic.entities.Summon;
import mj.konfigurats.logic.entities.Teleport;
import mj.konfigurats.logic.maps.Maps;
import mj.konfigurats.logic.maps.Maps.MapInfo;
import mj.konfigurats.logic.physics.BodyInformation;
import mj.konfigurats.logic.physics.LogicUtils;
import mj.konfigurats.logic.physics.SpellUtils.SFXIndexes;
import mj.konfigurats.logic.physics.SpellUtils.Spell;
import mj.konfigurats.logic.physics.SpellUtils.SpellType;
import mj.konfigurats.network.GamePackets.CltCreateCharacter;
import mj.konfigurats.network.GamePackets.GamePacket;
import mj.konfigurats.network.GamePackets.SrvCorruptedCreationData;
import mj.konfigurats.network.GamePackets.SrvCreateCharacter;
import mj.konfigurats.network.GamePackets.SrvDisplaySFX;
import mj.konfigurats.network.GamePackets.SrvGameChatMessage;
import mj.konfigurats.network.GamePackets.SrvPlayerNotElite;
import mj.konfigurats.network.GamePackets.SrvScoresUpdate;
import mj.konfigurats.network.GamePackets.SrvUpdateWorld;
import mj.konfigurats.server.ServerManager;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;

/**
 * A single game room. Contains players and maintains game logic.
 * @author MJ
 */
public class Game {
	// Room informations:
	private final String roomName,password;
	private final boolean isPasswordProtected;
	private final HashMap<Connection,Player> players;
	private final HashMap<Connection,GameRoomUser> usersInfo;
	private final List<List<GameRoomUser>> teams;
	private final int limit,roomIndex,mapIndex;
	private final GameMode gameMode;
	private final Timer gameThread;
	
	// Box2D variables:
	private final World box2DWorld;
	private final List<Player> characters;
	private final List<Projectile> projectiles;
	private final List<Entity> entitiesToRemove;
	private final List<ExplosionParticles> explosionParticles;
	private final List<ScheduledEvent> eventsQueue;
	private final List<GamePacket> SFXpackets;
	private CircleShape circleShape;
	
	// Temporary values:
	private Vector2 tempVector2,timerVector2;
	
	// Static control variables:
	public final static float UPDATE_TIME=1/20f;
	private final static long UPDATE_MILIS=50;
	public final static float CAMERA_RATIO=24;
	
	// Control variables:
	private float mapHeight;
	private AtomicLong updateIndex;
	private AtomicInteger entityIndex;
	private Array<Vector2> spawnPoints;
	private Array<Teleport> teleports;
	private int teleportsAmount;
	
	public Game(String roomName,String password,
		MapInfo mapInfo,GameMode gameMode,int previousMap, int playersAmount) {
		boolean changing = false;
		try {
			// Assigning room data:
			this.roomName = roomName;
			this.password = password;
			this.players = new HashMap<Connection,Player>();
			this.usersInfo = new HashMap<Connection,GameRoomUser>();
			// Getting proper map info:
			switch(mapInfo) {
			case RANDOM:
				// Permanent random map:
				mapInfo = Maps.getRandomMap();
				break;
			case RANDOM_CHANGING:
				// Switching random map:
				if(previousMap < 0) {
					mapInfo = Maps.getRandomMap();
				}
				else {
					mapInfo = Maps.getRandomMap(previousMap, playersAmount);
				}
				changing = true;
				break;
			default:
				break;
			}
			this.limit = mapInfo.getLimit();
			this.mapIndex = mapInfo.getIndex();
			this.roomIndex = roomName.hashCode();
			this.gameMode = gameMode;
			
			if(this.password != null) {
				isPasswordProtected = true;
			}
			else {
				isPasswordProtected = false;
			}
			
			switch(gameMode) {
			case TEAM:
				// Creating team arrays:
				teams = new ArrayList<List<GameRoomUser>>(2);
				teams.add(new ArrayList<GameRoomUser>((mapInfo.getLimit()+1)/2));
				teams.add(new ArrayList<GameRoomUser>((mapInfo.getLimit()+1)/2));
				break;
			default:
				teams = null;
				break;
			}
			
			// Initiating control variables:
			tempVector2 = new Vector2();
			timerVector2 = new Vector2();
			updateIndex = new AtomicLong(Long.MIN_VALUE);
			entityIndex = new AtomicInteger(Integer.MIN_VALUE);
			
			// Creating Box2D world:
			box2DWorld = new World(Vector2.Zero,true);
			box2DWorld.setContactListener(LogicUtils.createContactListener());
			characters = new ArrayList<Player>(mapInfo.getLimit()*2);
			projectiles = new ArrayList<Projectile>(mapInfo.getLimit()*4);
			entitiesToRemove = new LinkedList<Entity>();
			explosionParticles = new ArrayList<ExplosionParticles>(mapInfo.getLimit()*2);
			eventsQueue = new LinkedList<ScheduledEvent>();
			SFXpackets = new LinkedList<GamePacket>();
			
			// Creating Box2D heavy objects:
			circleShape = new CircleShape();
			
			// Creating Box2D bodies from the TMX map objects:
			parseMapToBox2D(mapInfo.getLinkedMap());
		}
		finally {
			// Setting up the game thread:
			gameThread = new Timer();
			gameThread.schedule(new TimerTask() { //scheduleAtFixedRate
				// Updating the game logic:
				@Override
				public void run() {
					updateWorld();
				}
			},UPDATE_MILIS,UPDATE_MILIS);
			
			// Scheduling map change:
			if(changing) {
				gameThread.schedule(new TimerTask() {
					@Override
					public void run() {
						for(GameRoomUser roomUser : usersInfo.values()) {
							ServerManager.SERVER.getConnectionManager()
								.updatePlayerScore(roomUser);
						}
						ServerManager.SERVER.getGamesManager().resetMap(Game.this);
					}
				}, 300000);
			}
		}
	}
	
	/**
	 * Schedules a single event on the queue.
	 * @param event scheduled event.
	 */
	public void scheduleEvent(ScheduledEvent event) {
		eventsQueue.add(event);
	}
	
	/**
	 * @return true if the room requires a password to be entered.
	 */
	public boolean isPasswordProtected() {
		return isPasswordProtected;
	}
	
	/**
	 * @return game room's password or null for none.
	 */
	public String getPassword() {
		return password;
	}
	
	/**
	 * Does a single world update.
	 */
	private void updateWorld() {
		box2DWorld.step(UPDATE_TIME, 6, 2);
		
		// Updating scheduled events:
		Iterator<ScheduledEvent> iterator = eventsQueue.iterator();
		while(iterator.hasNext()) {
			ScheduledEvent event = iterator.next();
			if(event.update(UPDATE_TIME)) {
				iterator.remove();
			}
		}

		// Updating players:
		for(Entity entity : characters) {
			entity.update(UPDATE_TIME,Game.this);
		}
		// Updating projectiles:
		for(Entity entity : projectiles) {
			entity.update(UPDATE_TIME,Game.this);
		}
		// Updating explosions:
		for(Entity entity : explosionParticles) {
			entity.update(UPDATE_TIME,Game.this);
		}
		
		// Removing destroyed entities:
		for(Entity entity : entitiesToRemove) {
			entity.destroy(Game.this);
		}
		entitiesToRemove.clear();
		
		// Sending update packets:
		sendUpdatePackets();
		
		// Refreshing characters' states:
		for(Player character : characters) {
			character.refreshState();
		}
	}
	
	/**
	 * @return list of current characters.
	 */
	public List<Player> getCharacters() {
		return characters;
	}
	
	/**
	 * @return list of entities scheduled to remove.
	 */
	public List<Entity> getEntitiesToRemove() {
		return entitiesToRemove;
	}
	
	/**
	 * @return list of currently active projectiles.
	 */
	public List<Projectile> getProjectiles() {
		return projectiles;
	}
	
	/**
	 * @return list of lists (sic) of explosion parcticles.
	 */
	public List<ExplosionParticles> getExplosionParticles() {
		return explosionParticles;
	}
	
	/**
	 * @return current game mode.
	 */
	public GameMode getGameMode() {
		return gameMode;
	}
	
	/**
	 * @return room's Box2D world.
	 */
	public World getBox2DWorld() {
		return box2DWorld;
	}
	
	/**
	 * @return default Box2D circle shape.
	 */
	public CircleShape getCircleShape() {
		return circleShape;
	}
	
	/**
	 * @return unique entity index.
	 */
	public int getUniqueEntityIndex() {
		return entityIndex.getAndIncrement();
	}
	
	/**
	 * Removes player entity linked with the user.
	 * @param player user's connection.
	 */
	public void removeCharacterReference(Connection player) {
		if(player != null) {
			if(players.containsKey(player)) {
				players.put(player, null);
			}
		}
	}
	
	/**
	 * Schedules sending of a SFX info game packet.
	 * @param sfxPacket SrvAttachSFX or SrvDisplaySFX packet.
	 */
	public void addSFXPacket(GamePacket sfxPacket) {
		SFXpackets.add(sfxPacket);
	}
	
	/**
	 * Adds map's objects to the Box2D world.
	 * @param map parsed map.
	 */
	private final void parseMapToBox2D(TiledMap map) {
		// Setting up map properties:
		mapHeight = map.getProperties().get("height",Integer.class)*
				map.getProperties().get("tileheight",Integer.class);
		
		// Creating arrays:
		spawnPoints = new Array<Vector2>();
		teleports = new Array<Teleport>();
		int teleportIndex = 0;
		
		// Reading objects:
		for(MapObject mapObject : map.getLayers().get("objects").getObjects()) {
			// Rectangle objects:
			if(mapObject instanceof RectangleMapObject) {
				if(mapObject.getProperties().containsKey("SPAWN")) {
					// The object is a spawn point. Add to the spawn list:
					spawnPoints.add(new Vector2(tiledToBox2DCoords(getActualTiledPosition
						(((RectangleMapObject) mapObject).getRectangle()
						.getPosition(tempVector2)))));
				}
				else if(mapObject.getProperties().containsKey("TELEPORT")) {
					// The object is a teleport:
					teleports.add(new Teleport(teleportIndex++, tiledToBox2DCoords
						(getActualTiledPosition(((RectangleMapObject) mapObject)
						.getRectangle().getPosition(tempVector2))),this));
				}
				else  {
					// Getting rectangle's bounds:
					Rectangle tempRect = ((RectangleMapObject)mapObject).getRectangle();
					
					Vector2[] vertices = new Vector2[5];
					tempVector2 = getActualTiledPosition(tempRect.getPosition(tempVector2));
					vertices[0] = new Vector2(tiledToBox2DCoords(tempVector2));
					
					tempVector2 = getActualTiledPosition(tempRect.getPosition(tempVector2));
					tempVector2.add(0, -tempRect.height);
					vertices[1] = new Vector2(tiledToBox2DCoords(tempVector2));
					
					tempVector2 = getActualTiledPosition(tempRect.getPosition(tempVector2));
					tempVector2.add(tempRect.width, -tempRect.height);
					vertices[2] = new Vector2(tiledToBox2DCoords(tempVector2));
					
					tempVector2 = getActualTiledPosition(tempRect.getPosition(tempVector2));
					tempVector2.add(tempRect.width, 0);
					vertices[3] = new Vector2(tiledToBox2DCoords(tempVector2));
					
					vertices[4] = new Vector2(vertices[0]);
					
					if(mapObject.getProperties().containsKey("LAVA")) {
						PolygonShape polygonShape = new PolygonShape();
						polygonShape.set(vertices);
						
						BodyInformation.LAVA.createNewBody(box2DWorld,
							polygonShape,null,0,0);
						
						polygonShape.dispose();
					}
					else {
//							Doesn't work for some weird C++ reasons: some chain
//							shapes cannot be created. Should be fixed some day
//							// Getting actual object position:
//							Vector2 objectPosition = new Vector2();
//							rectangle.getPosition(tempVector2);
//							objectPosition.set(getActualTiledPosition(tempVector2));
//
//							// Setting object points:
//							Vector2[] vertices = new Vector2[5];
//							vertices[0] = new Vector2(tiledToBox2DCoords(objectPosition));
//							vertices[1] = new Vector2(tiledToBox2DCoords(objectPosition.x,
//								objectPosition.y-rectangle.height));
//							vertices[2] = new Vector2(tiledToBox2DCoords(objectPosition.x
//								+ rectangle.width, objectPosition.y-rectangle.height));
//							vertices[3] = new Vector2(tiledToBox2DCoords(objectPosition.x
//								+ rectangle.width, objectPosition.y-rectangle.height));
//							vertices[4] = new Vector2(vertices[0]);
						
						ChainShape chainShape = new ChainShape();
						chainShape.createChain(vertices);
						
						if(mapObject.getProperties().containsKey("BOUNDS")) {
							// Creating obstacle body:
							BodyInformation.BOUNDS.createNewBody(box2DWorld,
								chainShape,null,0,0);
						}
						else if(mapObject.getProperties().containsKey("WARN")) {
							// The object is an edge to the void:
							BodyInformation.WARNING.createNewBody(box2DWorld,
								chainShape,null,0,0);
						}
						else if(mapObject.getProperties().containsKey("VOID")) {
							// The object is an edge to the void:
							BodyInformation.VOID.createNewBody(box2DWorld,
								chainShape,null,0,0);
						}
						else if(mapObject.getProperties().containsKey("OUTER")) {
							// The object protects from summoning and casting spells outside of map bounds:
							BodyInformation.OUTER_BOUNDS.createNewBody(box2DWorld,
								chainShape,null,0,0);
						}
						
						// Destroying chain shape:
						chainShape.dispose();
					}
				}
			}
			else if(mapObject instanceof PolylineMapObject) {
				// The object is an obstacle. Getting polyline:
				Polyline polyline = ((PolylineMapObject)mapObject).getPolyline();
				
				// Setting vertices:
				Vector2[] vertices = new Vector2[polyline.getVertices().length/2];
				for(int i=0; i<vertices.length; i++) {
					vertices[i] = new Vector2(tiledToBox2DCoords(polyline.getVertices()[i*2],
						-polyline.getVertices()[i*2+1]));
					// Minus y because of different TMX coords.
				}
				
				// Creating chain shape:
				ChainShape chainShape = new ChainShape();
				chainShape.createChain(vertices);
				
				// Getting object position:
				tempVector2 = tiledToBox2DCoords(getActualTiledPosition
					(tempVector2.set(mapObject.getProperties().get("x",float.class),
					mapObject.getProperties().get("y",float.class))));
				
				if(mapObject.getProperties().containsKey("BOUNDS")) {
					// Creating obstacle body:
					BodyInformation.BOUNDS.createNewBody(box2DWorld,chainShape,null,
						tempVector2.x,tempVector2.y);
				}
				else if(mapObject.getProperties().containsKey("WARN")) {
					// The object is an edge to the void:
					BodyInformation.WARNING.createNewBody(box2DWorld,chainShape,null,
						tempVector2.x,tempVector2.y);
				}
				else if(mapObject.getProperties().containsKey("VOID")) {
					// The object is an edge to the void:
					BodyInformation.VOID.createNewBody(box2DWorld,chainShape,null,
						tempVector2.x,tempVector2.y);
				}
				else if(mapObject.getProperties().containsKey("OUTER")) {
					// The object protects from summoning and casting spells outside of map bounds:
					BodyInformation.OUTER_BOUNDS.createNewBody(box2DWorld,chainShape,null,
						tempVector2.x,tempVector2.y);
				}
				
				// Destroying chain shape:
				chainShape.dispose();
			}
		}
		// Setting total amount of teleports:
		teleportsAmount = teleportIndex;
	}
	
	/**
	 * @return game's timer.
	 */
	public Timer getGameThread() {
		return gameThread;
	}
	
	/**
	 * Sends packets with the Box2D body positions to all players.
	 */
	private void sendUpdatePackets() {
		// Creating update packet:
		SrvUpdateWorld updatePacket = new SrvUpdateWorld();
		updatePacket.gameRoomIndex = roomIndex;
		updatePacket.updateIndex = updateIndex.getAndIncrement();
		// Characters informations:
		updatePacket.charactersIndexes = new int[characters.size()];
		updatePacket.charactersPositions = new float[characters.size()*2];
		updatePacket.charactereDisplayData = new byte[characters.size()*2];
		// Projectiles informations:
		updatePacket.projectileIndexes = new int[projectiles.size()];
		updatePacket.projectileAnimations = new byte[projectiles.size()];
		updatePacket.projectileDisplayData = new float[projectiles.size()*4];
		
		// Setting characters informations:
		int i = 0;
		for(Player character : characters) {
			// Index:
			updatePacket.charactersIndexes[i] = character.getEntityIndex();
			// Position:
			timerVector2.set(character.getPlayerBody().getPosition());
			updatePacket.charactersPositions[i*2] = timerVector2.x;
			updatePacket.charactersPositions[i*2+1] = timerVector2.y;
			// Direction:
			updatePacket.charactereDisplayData[i*2] = character.getDirection();
			// Animation:
			updatePacket.charactereDisplayData[i*2+1] = character.getState().getIndex();
			i++;
		}
		
		// Setting projectiles informations:
		i = 0;
		for(Projectile projectile : projectiles) {
			// Index:
			updatePacket.projectileIndexes[i] = projectile.getEntityIndex();
			// Animation:
			updatePacket.projectileAnimations[i] = projectile.getProjectileIndex();
			// Position:
			timerVector2.set(projectile.getProjectileBody().getPosition());
			updatePacket.projectileDisplayData[i*4] = timerVector2.x;
			updatePacket.projectileDisplayData[i*4+1] = timerVector2.y;
			// Velocity:
			timerVector2.set(projectile.getProjectileBody().getLinearVelocity());
			updatePacket.projectileDisplayData[i*4+2] = timerVector2.x;
			updatePacket.projectileDisplayData[i*4+3] = timerVector2.y;
			i++;
		}
		
		// Sending packets to all players:
		for(Entry<Connection,Player> player : players.entrySet()) {
			// Setting info about current health:
			if(player.getValue() != null) {
				updatePacket.currentHealth = player.getValue().getCurrentHealthPercent();
				if(player.getValue().hasSummon()) {
					updatePacket.summonHealth = player.getValue()
						.getSummon().getCurrentHealthPercent();
				}
				else {
					updatePacket.summonHealth = 0f;
				}
			}
			else {
				updatePacket.currentHealth = 0f;
				updatePacket.summonHealth = 0f;
			}
			player.getKey().sendUDP(updatePacket);
			
			// Sending info about special effects of spells:
			for(GamePacket SFXpacket : SFXpackets) {
				player.getKey().sendUDP(SFXpacket);
			}
		}
		SFXpackets.clear();
	}
	
	/**
	 * Creates a character after player decides to do so. Run by the
	 * game thread.
	 * @param player user's connection.
	 * @param packet packet with user's character data.
	 */
	public void createCharacter(final Connection player,final CltCreateCharacter packet) {
		gameThread.schedule(new TimerTask() {
			@Override
			public void run() {
				if(usersInfo.containsKey(player) && players.containsKey(player)
					&& players.get(player) == null) {
					// Player is trying to create an elite character:
					if(packet.isElite) {
						if(!ServerManager.SERVER.getConnectionManager().isElite(player.toString())) {
							player.sendTCP(new SrvPlayerNotElite());
							return;
						}
					}
					
					// Getting player's class:
					PlayerClass playerClass = PlayerClass.getPlayerClass(packet.classIndex);
					if(playerClass != null && !playerClass.isSummon()) {
						// Getting player's spells:
						Spell fireSpell = SpellType.FIRE.getSpell(packet.fireSpell),
							waterSpell = SpellType.WATER.getSpell(packet.waterSpell),
							earthSpell = SpellType.EARTH.getSpell(packet.earthSpell),
							airSpell = SpellType.AIR.getSpell(packet.airSpell);
						
						if(fireSpell != null && waterSpell != null && earthSpell != null && airSpell != null) {
							
							
							// Creating a new player character:
							Player playerCharacter = new Player(entityIndex.getAndIncrement(),
								player,null,Game.this,playerClass,packet.isElite,
								spawnPoints.random(),usersInfo.get(player).getTeamIndex(),
								fireSpell,waterSpell,earthSpell,airSpell);
							
							// Adding the character to the game's lists:
							players.put(player,playerCharacter);
							characters.add(playerCharacter);
							
							// Telling all players that a new character has been created:
							SrvCreateCharacter characterPacket = new SrvCreateCharacter();
							characterPacket.characterIndex = playerCharacter.getEntityIndex();
							characterPacket.teamIndex = playerCharacter.getTeamIndex();
							characterPacket.playerName = playerCharacter.getPlayerName();
							characterPacket.characterClass = playerCharacter.getPlayerClass().getIndex();
							characterPacket.isElite = playerCharacter.isElite(); 
							characterPacket.x = playerCharacter.getPlayerBody().getPosition().x;
							characterPacket.y = playerCharacter.getPlayerBody().getPosition().y;
							for(Connection connection : players.keySet()) {
								connection.sendTCP(characterPacket);
							}
						}
						else {
							// Corrupted spell index:
							player.sendTCP(new SrvCorruptedCreationData());
						}
					}
					else {
						// Corrupted class index:
						player.sendTCP(new SrvCorruptedCreationData());
					}
				}
			}
		}, 0);
	}
	
	/**
	 * Inverts player's input position.
	 * @param player player's character.
	 * @param x input position x.
	 * @param y input position y.
	 * @return inverted position.
	 */
	public Vector2 getInvertedPosition(Player player,float x,float y) {
		// Getting relative position:
		x -= player.getPlayerBody().getPosition().x;
		y -= player.getPlayerBody().getPosition().y;
		// Inverting relative position:
		y = -y;
		x = -x;
		// Getting actual position:
		x += player.getPlayerBody().getPosition().x;
		y += player.getPlayerBody().getPosition().y;
		return tempVector2.set(x,y);
	}
	
	/**
	 * Moves the player's character to the selected position.
	 * @param player user's connection.
	 * @param x x position of the tiled map camera.
	 * @param y y position of the tiled map camera.
	 */
	public void handleClick(final Connection player,final float x,final float y) {
		gameThread.schedule(new TimerTask() {
			@Override
			public void run() {
				Player character = players.get(player);
				if(character != null) {
					if(character.isConfused()) {
						// Inverted destination:
						character.setDestination(getInvertedPosition(character, x, y));
					}
					else {
						// Proper destination:
						character.setDestination(tempVector2.set(x,y));
					}
				}
			}
		}, 0);
	}
	
	/**
	 * Lets the player cast his fire spell.
	 * @param player user's connection.
	 * @param x his current x mouse position.
	 * @param y his current y mouse position.
	 */
	public void handleFireCast(final Connection player,final float x,final float y) {
		gameThread.schedule(new TimerTask() {
			@Override
			public void run() {
				Player caster = players.get(player);
				// Checking if player actually has a character:
				if(caster == null) {
					return;
				}
				
				if(caster != null && caster.canCastSpell(SpellType.FIRE.getIndex())) {
					caster.cancelDestination();
					
					// Getting spell position:
					if(caster.isConfused()) {
						getInvertedPosition(caster, x, y);
					}
					else {
						tempVector2.set(x,y);
					}
					
					switch(caster.getSpell(SpellType.FIRE)) {
					case FIREBALL:
						// Adding fireball projectile:
						Spell.FIREBALL.cast(entityIndex.getAndIncrement(),
							Game.this, caster, circleShape, tempVector2);
						break;
					case METEOR:
						// Scheduling meteor fall:
						Spell.METEOR.cast(0, Game.this, caster, null, tempVector2);
						break;
					case BLAZING_FEET:
						// Adding blazing feet status:
						Spell.BLAZING_FEET.cast(0, Game.this, caster, null, tempVector2);
						break;
					case CURSE:
						// Adding curse projectile:
						Spell.CURSE.cast(entityIndex.getAndIncrement(),
							Game.this, caster, circleShape, tempVector2);
						break;
					case MAGIC_MISSILE:
						// Adding magic missile projectile:
						Spell.MAGIC_MISSILE.cast(entityIndex.getAndIncrement(),
							Game.this, caster, circleShape, tempVector2);
						break;
					case LIFE_STEAL:
						// Adding life steal projectile:
						Spell.LIFE_STEAL.cast(entityIndex.getAndIncrement(),
							Game.this, caster, circleShape, tempVector2);
						break;
					case SWARM:
						// Summoning fire ant:
						Spell.SWARM.cast(entityIndex.getAndIncrement(), Game.this,
							caster, null, tempVector2);
						break;
					default:
						break;
					}
				}
			}
		},0);
	}
	
	/**
	 * Lets the player cast his water spell.
	 * @param player user's connection.
	 * @param x his current x mouse position.
	 * @param y his current y mouse position.
	 */
	public void handleWaterCast(final Connection player,final float x,final float y) {
		gameThread.schedule(new TimerTask() {
			@Override
			public void run() {
				Player caster = players.get(player);
				// Checking if player actually has a character:
				if(caster == null) {
					return;
				}
				
				if(caster != null && caster.canCastSpell(SpellType.WATER.getIndex())) {
					caster.cancelDestination();
					
					// Getting spell position:
					if(caster.isConfused()) {
						getInvertedPosition(caster, x, y);
					}
					else {
						tempVector2.set(x,y);
					}
					
					switch(caster.getSpell(SpellType.WATER)) {
					case HEAL:
						// Healing the player:
						Spell.HEAL.cast(0, Game.this, caster, null, tempVector2);
						break;
					case FREEZE:
						// Adding freezing projectile:
						Spell.FREEZE.cast(entityIndex.getAndIncrement(),
							Game.this, caster, circleShape, tempVector2);
						break;
					case ICE_BLOCK:
						// Adding ice block projectile:
						Spell.ICE_BLOCK.cast(entityIndex.getAndIncrement(),
							Game.this, caster, circleShape, tempVector2);
						break;
					case SHIELD:
						// Shielding a player:
						Spell.SHIELD.cast(0, Game.this, caster, null, tempVector2);
						break;
					case PULSE:
						// Creating pulse explosion:
						Spell.PULSE.cast(0, Game.this, caster, null, tempVector2);
						break;
					case CONFUSION:
						// Confusing target:
						Spell.CONFUSION.cast(0, Game.this, caster, null, tempVector2);
						break;
					case SILENCE:
						// Paralyzing target:
						Spell.SILENCE.cast(0, Game.this, caster, null, tempVector2);
						break;
					default:
						break;
					}
				}
			}
		},0);
	}
	
	/**
	 * Lets the player cast his earth spell.
	 * @param player user's connection.
	 * @param x his current x mouse position.
	 * @param y his current y mouse position.
	 */
	public void handleEarthCast(final Connection player,final float x,final float y) {
		gameThread.schedule(new TimerTask() {
			@Override
			public void run() {
				Player caster = players.get(player);
				// Checking if player actually has a character:
				if(caster == null) {
					return;
				}
				
				if(caster != null && caster.canCastSpell(SpellType.EARTH.getIndex())) {
					caster.cancelDestination();
					
					// Getting spell position:
					if(caster.isConfused()) {
						getInvertedPosition(caster, x, y);
					}
					else {
						tempVector2.set(x,y);
					}
					
					switch(caster.getSpell(SpellType.EARTH)) {
					case QUAKE:
						// Creating explosion around the player:
						Spell.QUAKE.cast(0, Game.this, caster, null, null);
						break;
					case POISON:
						// Adding gas cloud projectile:
						Spell.POISON.cast(entityIndex.getAndIncrement(),
								Game.this, caster, circleShape, tempVector2);
						break;
					case ENTANGLE:
						// Adding an entangle projectile:
						Spell.ENTANGLE.cast(entityIndex.getAndIncrement(),
							Game.this, caster, circleShape, tempVector2);
						break;
					case CURE:
						// Healing the player:
						Spell.CURE.cast(0, Game.this, caster, null, tempVector2);
						break;
					case HOMING_ARROW:
						// Adding an arrow projectile:
						Spell.HOMING_ARROW.cast(entityIndex.getAndIncrement(),
							Game.this, caster, circleShape, tempVector2);
						break;
					case SUMMON_BEAST:
						// Summoning minotaur:
						Spell.SUMMON_BEAST.cast(entityIndex.getAndIncrement(), Game.this,
							caster, null, tempVector2);
						break;
					case THORNS:
						// Adding thorns projectiles:
						Spell.THORNS.cast(0,Game.this, caster, circleShape, tempVector2);
						break;
					default:
						break;
					}
				}
			}
		},0);
	}
	
	/**
	 * Lets the player cast his air spell.
	 * @param player user's connection.
	 * @param x his current x mouse position.
	 * @param y his current y mouse position.
	 */
	public void handleAirCast(final Connection player,final float x,final float y) {
		gameThread.schedule(new TimerTask() {
			@Override
			public void run() {
				Player caster = players.get(player);
				// Checking if player actually has a character:
				if(caster == null) {
					return;
				}
				
				// Getting spell position:
				if(caster.isConfused()) {
					getInvertedPosition(caster, x, y);
				}
				else {
					tempVector2.set(x,y);
				}
				
				if(caster != null && caster.canCastSpell(SpellType.AIR.getIndex())) {
					caster.cancelDestination();
					switch(caster.getSpell(SpellType.AIR)) {
					case HASTE:
						// Speeding the player up:
						Spell.HASTE.cast(0, Game.this, caster, null, tempVector2);
						break;
					case LEAP:
						// Moving the player in a given direction:
						Spell.LEAP.cast(0, Game.this, caster, null, tempVector2);
						break;
					case LIGHTNING_BOLT:
						// Adding a lightning bolt projectile:
						Spell.LIGHTNING_BOLT.cast(entityIndex.getAndIncrement(),
							Game.this, caster, circleShape, tempVector2);
						break;
					case TORNADO:
						// Adding a tornado "projectile":
						Spell.TORNADO.cast(entityIndex.getAndIncrement(),
							Game.this, caster, circleShape, tempVector2);
						break;
					case SWAP:
						// Switching players' positions:
						Spell.SWAP.cast(0, Game.this, caster, null, tempVector2);
						break;
					case TAUNT:
						// Changing projectiles' velocity:
						Spell.TAUNT.cast(0, Game.this, caster, null, tempVector2);
						break;
					case LINK:
						// Changing projectiles' velocity:
						Spell.LINK.cast(0, Game.this, caster, null, tempVector2);
						break;
					default:
						break;
					}
				}
			}
		},0);
	}
	
	/**
	 * Adds one kill for the specified player.
	 * @param player player who killed another user's character.
	 */
	public void addKill(Connection player) {
		GameRoomUser userInfo = usersInfo.get(player);
		userInfo.addKill();
		
		// Preparing packet with score update:
		SrvScoresUpdate packet = new SrvScoresUpdate();
		packet.nickname = userInfo.toString();
		packet.kills = userInfo.getKills();
		packet.deaths = userInfo.getDeaths();
		
		// Scheduling packet:
		SFXpackets.add(packet);
	}
	
	/**
	 * Adds one death for the specified player.
	 * @param player player whose character died.
	 */
	public void addDeath(Connection player) {
		GameRoomUser userInfo = usersInfo.get(player);
		userInfo.addDeath();
		
		// Preparing packet with score update:
		SrvScoresUpdate packet = new SrvScoresUpdate();
		packet.nickname = userInfo.toString();
		packet.kills = userInfo.getKills();
		packet.deaths = userInfo.getDeaths();
		
		// Scheduling packet:
		SFXpackets.add(packet);
	}
	
	/**
	 * Teleports a player to a random teleport location.
	 * @param player the player to be teleported.
	 * @param teleport the teleport he stepped into.
	 */
	public void teleport(final Player player, Teleport teleport) {
		if(player.getLastUsedTeleportIndex() != teleport.getEntityIndex()) {
			// Getting random teleport:
			Teleport destination = teleports.random();
			// Making sure it's not the same one:
			if(destination == teleport) {
				destination = teleports.get((destination.getEntityIndex()+1)%teleportsAmount);
			}
			
			// Sending teleport packet:
			SrvDisplaySFX teleportingOutPacket = new SrvDisplaySFX();
			teleportingOutPacket.SFXIndex = SFXIndexes.SWAP_CAST.getSFXIndex();
			teleportingOutPacket.x = player.getPlayerBody().getPosition().x;
			teleportingOutPacket.y = player.getPlayerBody().getPosition().y;
			SFXpackets.add(teleportingOutPacket);
			
			player.setLastUsedTeleport(destination.getEntityIndex());
			final Vector2 destinationPosition = new Vector2(destination.getPosition());
			// Teleporting the player (making sure it won't break the game update):
			gameThread.schedule(new TimerTask() {
				@Override
				public void run() {
					// Getting destination relative to the player:
					if(player.isMoving()) {
						tempVector2.set(player.getDestination());
						tempVector2.sub(player.getPlayerBody().getPosition());
					}
					
					player.getPlayerBody().setTransform(destinationPosition,
						player.getPlayerBody().getAngle());
					
					// Setting new destination:
					if(player.isMoving()) {
						player.setDestination(tempVector2.add(player
							.getPlayerBody().getPosition()));
					}

					// Sending teleport packet:
					SrvDisplaySFX teleportingInPacket = new SrvDisplaySFX();
					teleportingInPacket.SFXIndex = SFXIndexes.SWAP_CAST.getSFXIndex();
					teleportingInPacket.x = player.getPlayerBody().getPosition().x;
					teleportingInPacket.y = player.getPlayerBody().getPosition().y;
					SFXpackets.add(teleportingInPacket);
				}
			}, 0);
		}
	}
	
	/**
	 * Teleports a projectile to a random teleport location.
	 * @param projectile the projectile to be teleported.
	 * @param teleport the teleport that the projectile fell into.
	 */
	public void teleport(final Projectile projectile, Teleport teleport) {
		if(projectile.getLastUsedTeleportIndex() != teleport.getEntityIndex()) {
			// Getting random teleport:
			Teleport destination = teleports.random();
			// Making sure it's not the same one:
			if(destination == teleport) {
				destination = teleports.get((destination.getEntityIndex()+1)%teleportsAmount);
			}
			// Sending teleport packet:
			SrvDisplaySFX teleportingOutPacket = new SrvDisplaySFX();
			teleportingOutPacket.SFXIndex = SFXIndexes.SWAP_CAST.getSFXIndex();
			teleportingOutPacket.x = projectile.getProjectileBody().getPosition().x;
			teleportingOutPacket.y = projectile.getProjectileBody().getPosition().y;
			SFXpackets.add(teleportingOutPacket);
			
			projectile.setLastUsedTeleport(destination.getEntityIndex());
			final Vector2 destinationPosition = new Vector2(destination.getPosition());
			// Teleporting the projectile (making sure it won't break the game update):
			gameThread.schedule(new TimerTask() {
				@Override
				public void run() {
					projectile.getProjectileBody().setTransform(destinationPosition,
						projectile.getProjectileBody().getAngle());

					// Sending teleport packet:
					SrvDisplaySFX teleportingInPacket = new SrvDisplaySFX();
					teleportingInPacket.SFXIndex = SFXIndexes.SWAP_CAST.getSFXIndex();
					teleportingInPacket.x = projectile.getProjectileBody().getPosition().x;
					teleportingInPacket.y = projectile.getProjectileBody().getPosition().y;
					SFXpackets.add(teleportingInPacket);
				}
			}, 0);
		}
	}
	
	/**
	 * Stops the game thread and disposes of Box2D world.
	 */
	public void dispose() {
		gameThread.schedule(new TimerTask() {
			@Override
			public void run() {
				// Disposing of Box2D world:
				box2DWorld.dispose();
				circleShape.dispose();
				
				// Stopping game thread:
				gameThread.cancel();
			}
		}, 0);
	}
	
	/**
	 * Converts TMX (Tiled editor map format) coordinates into Box2D coordinates.
	 * Should be used to read the map objects.
	 * @param tiledCoords TMX coordinates.
	 * @return Box2D coordinates.
	 */
	private Vector2 tiledToBox2DCoords(Vector2 tiledCoords) {
		return tempVector2.set((tiledCoords.x + tiledCoords.y)/CAMERA_RATIO,
				(-(tiledCoords.x - tiledCoords.y)/2f)/CAMERA_RATIO);
	}
	
	/**
	 * Converts TMX (Tiled editor map format) coordinates into Box2D coordinates.
	 * Should be used to read the map objects.
	 * @param x TMX x index.
	 * @param y TMX y index.
	 * @return Box2D coordinates.
	 */
	private Vector2 tiledToBox2DCoords(float x,float y) {
		return tempVector2.set((x + y)/CAMERA_RATIO,(-(x - y)/2f)/CAMERA_RATIO);
	}
	
	/**
	 * Since TMX maps use different coordinates set, positions need to be corrected.
	 * @param tiledPosition tiled map position.
	 * @return tiled map position after correction.
	 */
	private Vector2 getActualTiledPosition(Vector2 tiledPosition) {
		return tempVector2.set(tiledPosition.x, mapHeight - tiledPosition.y);
	}
	
	/**
	 * Adds a player to the game room.
	 * @param player will be added.
	 */
	public void addPlayer(final Connection player) {
		gameThread.schedule(new TimerTask() {
			@Override
			public void run() {
				players.put(player,null);
				if(gameMode == GameMode.TEAM) {
					// Choosing team:
					if(teams.get(0).size() > teams.get(1).size()) {
						GameRoomUser newUser = new GameRoomUser(player,1);
						usersInfo.put(player, newUser);
						teams.get(1).add(newUser);
					}
					else {
						GameRoomUser newUser = new GameRoomUser(player,0);
						usersInfo.put(player, newUser);
						teams.get(0).add(newUser);
					}
				}
				else {
					// "Negative" team:
					usersInfo.put(player, new GameRoomUser(player,-1));
				}
			}
		}, 0);
		
	}
	
	/**
	 * After a player joins a game and his battle manager is initiated,
	 * he's ready to receive informations about current entities.
	 * Also sends packets with users scores to display on the chat.
	 * @param player user's connection.
	 */
	public void sendCurrentEntities(final Connection player) {
		gameThread.schedule(new TimerTask() {
			@Override
			public void run() {
				// Sending packets with character creation data:
				for(Player character : characters) {
					SrvCreateCharacter packet = new SrvCreateCharacter();
					packet.characterIndex = character.getEntityIndex();
					packet.teamIndex = character.getTeamIndex();
					packet.playerName = character.getPlayerName();
					packet.characterClass = character.getPlayerClass().getIndex();
					packet.isElite = character.isElite();
					packet.x = character.getPlayerBody().getPosition().x;
					packet.y = character.getPlayerBody().getPosition().y;
					player.sendTCP(packet);
				}
				// Sending packets with users' scores:
				for(GameRoomUser gameRoomUser : usersInfo.values()) {
					SrvScoresUpdate packet = new SrvScoresUpdate();
					packet.nickname = gameRoomUser.toString();
					packet.kills = gameRoomUser.getKills();
					packet.deaths = gameRoomUser.getDeaths();
					player.sendUDP(packet);
				}
			}
		}, 0);
	}
	
	/**
	 * Finds the player who is the nearest  one to the given position.
	 * @param position position (usually of the cursor).
	 * @return nearest character.
	 */
	public Player getNearestPlayer(Vector2 position) {
		Player nearestPlayer = null;
		float distance = Float.MAX_VALUE, currentDistance;
		
		// Checking distance for each character:
		for(Player player : characters) {
			currentDistance = LogicUtils.getDistanceIndicator(position,
				player.getPlayerBody().getPosition());
			// If distance from the character is lower than the current nearest...
			if(currentDistance < distance) {
				distance = currentDistance;
				nearestPlayer = player;
			}
		}
		
		return nearestPlayer;
	}
	
	/**
	 * Finds a player who is the nearest to the given player.
	 * @return nearest character other than player or null.
	 */
	public Player getNearestPlayer(Player player) {
		Player nearestPlayer = null;
		float distance = Float.MAX_VALUE, currentDistance;
		
		// Checking distance for each character:
		for(Player checkedPlayer : characters) {
			if(checkedPlayer.getEntityIndex() != player.getEntityIndex()) {
				currentDistance = LogicUtils.getDistanceIndicator(player
					.getPlayerBody().getPosition(),checkedPlayer.getPlayerBody().getPosition());
				// If distance from the character is lower than the current nearest...
				if(currentDistance < distance) {
					distance = currentDistance;
					nearestPlayer = checkedPlayer;
				}
			}
		}
		
		return nearestPlayer;
	}
	
	/**
	 * Finds enemy closest to the given summon. Used by the summon AI.
	 * @param summon summon looking for a target.
	 * @return closest enemy.
	 */
	public Player getNearestEnemy(Summon summon) {
		switch(gameMode) {
		case STARNDARD: {
			Player nearestPlayer = null;
			float distance = Float.MAX_VALUE, currentDistance;
			
			// Checking distance for each character:
			for(Player player : characters) {
				if(player != summon && player != summon.getCaster()) {
					currentDistance = LogicUtils.getDistanceIndicator
						(summon.getPlayerBody().getPosition(),
						player.getPlayerBody().getPosition());
					// If distance from the character is lower than the current nearest...
					if(currentDistance < distance) {
						distance = currentDistance;
						nearestPlayer = player;
					}
				}
			}
			return nearestPlayer;
		}
		case TEAM:
			Player nearestPlayer = null;
			float distance = Float.MAX_VALUE, currentDistance;
			
			// Checking distance for each character:
			for(Player player : characters) {
				if(player.getTeamIndex() != summon.getTeamIndex()) {
					currentDistance = LogicUtils.getDistanceIndicator
						(summon.getPlayerBody().getPosition(),
						player.getPlayerBody().getPosition());
					// If distance from the character is lower than the current nearest...
					if(currentDistance < distance) {
						distance = currentDistance;
						nearestPlayer = player;
					}
				}
			}
			return nearestPlayer;
		default:
			return null;
		}
		
		
		
	}
	
	/**
	 * Removes the player from the game room.
	 * @param player will be removed, if is present.
	 * @return true if was present.
	 */
	public boolean removePlayer(final Connection player) {
		if(players.containsKey(player)) {
			gameThread.schedule(new TimerTask() {
				@Override
				public void run() {		
					if(players.get(player) != null) {
						entitiesToRemove.add(players.get(player));
					}
					players.remove(player);
					GameRoomUser leavingUser = usersInfo.get(player);
					if(leavingUser != null) {
						// Updating player's scores:
						ServerManager.SERVER.getConnectionManager()
							.updatePlayerScore(usersInfo.get(player));
						
						if(gameMode == GameMode.TEAM) {
							// Removing player from his team array:
							teams.get(leavingUser.getTeamIndex())
								.remove(leavingUser);
						}
					}
					// Removing temporary player info:
					usersInfo.remove(player);
				}
			}, 0);
			return true;
		}
		return false;
	}
	
	/**
	 * Not safe to iterate from outside the class.
	 * @return list of players in the game room.
	 */
	public Set<Connection> getPlayers() {
		return players.keySet();
	}
	
	/**
	 * Sends a message to all players in the room.
	 * @param packet game chat message.
	 */
	public void sendMessage(final SrvGameChatMessage packet) {
		gameThread.schedule(new TimerTask() {
			@Override
			public void run() {
				for(Connection user : getPlayers()) {
					user.sendUDP(packet);
				}
			}
		}, 0);
	}
	
	/**
	 * Sends a message to a specific team in the game room.
	 * Room has to be in GameMode.TEAM mode.
	 * @param player user who sent the message.
	 * @param packet game chat message.
	 */
	public void sendTeamMessage(final Connection player,final SrvGameChatMessage packet) {
		gameThread.schedule(new TimerTask() {
			@Override
			public void run() {
				// Finding player:
				if(usersInfo.containsKey(player)) {
					// Checking his team index:
					int teamIndex = usersInfo.get(player).getTeamIndex();
					// Sending the message to all users in his team:
					for(GameRoomUser user : usersInfo.values()) {
						if(user.getTeamIndex() == teamIndex) {
							user.sendUDP(packet);
						}
					}
				}
			}
		}, 0);
	}
	
	/**
	 * @return informations about current game room players.
	 */
	public Collection<GameRoomUser> getUsersInfo() {
		return usersInfo.values();
	}
	
	/**
	 * Finds team index of a given player.
	 * @param player user's connection.
	 * @return team index of the selected player.
	 */
	public int getTeamIndex(Connection player) {
		return usersInfo.get(player).getTeamIndex();
	}
	
	/**
	 * @return true if full.
	 */
	public boolean isFull() {
		return players.size() == limit;
	}
	
	/**
	 * @return true if empty.
	 */
	public boolean isEmpty() {
		return players.isEmpty();
	}
	
	/**
	 * @return amount of players in the game room.
	 */
	public int getPlayersAmount() {
		return players.size();
	}
	
	/**
	 * @return max amount of players in the game room.
	 */
	public int getPlayersLimit() {
		return limit;
	}
	
	/**
	 * @return index of the game room's map.
	 */
	public int getMapIndex() {
		return mapIndex;
	}
	
	/**
	 * @return game room's index.
	 */
	public int getRoomIndex() {
		return roomIndex;
	}
	
	@Override
	public String toString() {
		return roomName;
	}
	
	@Override
	public int hashCode() {
		return roomName.hashCode();
	}
	
	/**
	 * Currently available game modes.
	 */
	public static enum GameMode {
		STARNDARD(0,'S'),TEAM(1,'T');
		
		private final int index;
		private final char modeName;
		private GameMode(int index,char modeName) {
			this.index = index;
			this.modeName = modeName;
		}
		
		/**
		 * @return index of the game mode.
		 */
		public int getIndex() {
			return index;
		}
		
		/**
		 * @return "name" of the mode to be sent to the clients.
		 */
		public char getModeName() {
			return modeName;
		}
		
		/**
		 * Finds the game mode with the given index.
		 * @param index game mode's index.
		 * @return game mode connected with the index or null for invalid index.
		 */
		public static GameMode getGameMode(int index) {
			for(GameMode gameMode : GameMode.values()) {
				if(gameMode.getIndex() == index) {
					return gameMode;
				}
			}
			return null;
		}
	}
}
