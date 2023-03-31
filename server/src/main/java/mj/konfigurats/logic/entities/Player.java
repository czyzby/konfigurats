package mj.konfigurats.logic.entities;

import java.util.Iterator;
import java.util.LinkedList;

import mj.konfigurats.logic.Game;
import mj.konfigurats.logic.ScheduledEvent;
import mj.konfigurats.logic.physics.BodyInformation;
import mj.konfigurats.logic.physics.LogicUtils;
import mj.konfigurats.logic.physics.SpellUtils.Spell;
import mj.konfigurats.logic.physics.SpellUtils.SpellType;
import mj.konfigurats.network.GamePackets.SrvSetSpellCooldown;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.esotericsoftware.kryonet.Connection;

/**
 * Class containing and allowing to maintain a single playable character.
 * @author MJ
 */
public class Player implements Entity {
	// Linear "gravity":
	protected final static float
		MOVEMENT_SLOWDOWN = 1000f,
		PUSHBACK_THRESHOLD = 5f;
	
	// Player chosen properties:
	private final PlayerClass playerClass;
	private final Spell[] spells;
	private final float[] spellCooldowns;
	private final int teamIndex;
	
	// Current player properties:
	private final int entityIndex;
	private final String playerName;
	private final boolean isElite;
	private float playerSpeed,damageModificator,
	 cooldownModificator,maxHealth,pushbackSlowdown;
	
	// Control variables:
	private final Game game;
	private final Connection owner;
	private final LinkedList<ScheduledEvent> scheduledEvents;
	private Player lastDamageDealer;
	private Summon summon;
	private int lastUsedTeleport;
	
	// Character's state variables:
	protected float currentHealth;
	private float currentHealthPercent;
	protected int isConfused;
	private int isImmobilized,isParalyzed,isCursed,isShielded,isBlazing,inLava;
	protected boolean isDead;
	private boolean isRemoved,isPushed;
	
	// Box2D variables:
	private final Body playerBody;
	private Vector2 destination;
	
	// Player's sprite informations:
	private PlayerState state;
	private byte direction;
	
	public Player(int entityIndex,Connection owner,String playerName,
		Game game,PlayerClass playerClass,boolean isElite,
		Vector2 position,int teamIndex,Spell... spells) {
		// Assigning final variables:
		this.entityIndex = entityIndex;
		if(owner != null) {
			this.playerName = owner.toString();
		}
		else {
			this.playerName = playerName;
		}
		this.playerClass = playerClass;
		this.spells = spells;
		this.spellCooldowns = new float[SpellType.values().length];
		this.playerBody = playerClass.getBodyInformation()
			.createNewBody(game.getBox2DWorld(),game.getCircleShape(),
			this,position.x,position.y);
		this.isElite = isElite;
		this.scheduledEvents = new LinkedList<ScheduledEvent>();
		this.game = game;
		this.owner = owner;
		this.teamIndex = teamIndex;
		
		// Assigning control variables:
		direction = LogicUtils.S;
		playerSpeed = playerClass.getSpeed();
		pushbackSlowdown = playerClass.getPushbackSlowdown();
		damageModificator = playerClass.getDamageModificator();
		cooldownModificator = playerClass.getCooldownModificator();
		maxHealth = playerClass.getHealth();
		currentHealth = maxHealth;
		currentHealthPercent = 1f;
		state = PlayerState.STANCE;
		isImmobilized = isParalyzed = isShielded = isConfused = inLava = 0;
		isDead = isRemoved = false;
		lastUsedTeleport = -1;
	}
	
	//////////////////////////////////////////////////////////////////
	// Final classification data.
	//////////////////////////////////////////////////////////////////
	
	/**
	 * @return character's owner's connection.
	 */
	public Connection getOwner() {
		return owner;
	}
	
	/**
	 * @return character's game room.
	 */
	public Game getGame() {
		return game;
	}

	/**
	 * @return nickname of the player that is controlling the character.
	 */
	public String getPlayerName() {
		return playerName;
	}
	
	/**
	 * @return index of the player's team or negative number for none.
	 */
	public int getTeamIndex() {
		return teamIndex;
	}
	
	/**
	 * @return true if the player uses an elite sprite.
	 */
	public boolean isElite() {
		return isElite;
	}
	
	/**
	 * @return current player's class.
	 */
	public PlayerClass getPlayerClass() {
		return playerClass;
	}
	
	/**
	 * @return player's Box2D body.
	 */
	public Body getPlayerBody() {
		return playerBody;
	}
	
	@Override
	public int getEntityIndex() {
		return entityIndex;
	}
	
	@Override
	public void destroy(Game game) {
		if(!isRemoved) {
			isRemoved = true;
			isDead = true;
			
			// Killing summon:
			if(hasSummon()) {
				summon.killSummon();
			}
			
			// Destoying player's body:
			playerBody.getWorld().destroyBody(playerBody);
			
			// Removing character from the list:
			game.getCharacters().remove(this);
			game.removeCharacterReference(owner);
		}
	}
	
	@Override
	public void update(float delta,Game game) {
		updateSpellCooldowns(delta);
		updateScheduledEvents(delta);
		checkIfInLava();
		updateMovement(delta);
	}
	
	//////////////////////////////////////////////////////////////////
	// Character's display data.
	//////////////////////////////////////////////////////////////////
	
	/**
	 * @return current player state (animation type).
	 */
	public PlayerState getState() {
		return state;
	}
	
	/**
	 * @param state current player animation type.
	 */
	public void setPlayerState(PlayerState state) {
		this.state = state;
	}
	
	/**
	 * Sets the state to walking or default stance, depending on current destination.
	 */
	public void refreshState() {
		// If there is a destination that the character is trying to reach:
		if(destination != null) {
			// Walking animation:
			this.state = PlayerState.WALK;
		}
		// If there's not...
		else {
			// Standing animation:
			this.state = PlayerState.STANCE;
		}
	}
	
	/**
	 * @return the direction that player is currently facing. See LogicUtils.
	 */
	public byte getDirection() {
		return direction;
	}
	
	/**
	 * @param direction will be the new character's direction.
	 */
	public void setDirection(byte direction) {
		this.direction = direction;
	}
	
	//////////////////////////////////////////////////////////////////
	// Character's state management.
	//////////////////////////////////////////////////////////////////
	
	/**
	 * Schedules an event, usually involving the player.
	 * @param event will be scheduled and executed as long as the player
	 * is alive.
	 */
	public void scheduleEvent(ScheduledEvent event) {
		scheduledEvents.add(event);
	}
	
	/**
	 * Updates player's scheduled events.
	 * @param delta time passed since the last update.
	 */
	protected void updateScheduledEvents(float delta) {
		// Updating scheduled events:
		Iterator<ScheduledEvent> iterator = scheduledEvents.iterator();
		while(iterator.hasNext()) {
			ScheduledEvent event = iterator.next();
			if(event.update(delta)) {
				// If the event has finished:
				iterator.remove();
			};
		}
	}
	
	/**
	 * Links a summon with a player.
	 * @param summon may be null to remove the summon.
	 */
	public void setSummon(Summon summon) {
		this.summon = summon;
	}
	
	/**
	 * @return current player's summon or null.
	 */
	public Summon getSummon() {
		return summon;
	}
	
	/**
	 * @return true if player has a summon.
	 */
	public boolean hasSummon() {
		return summon != null;
	}
	
	/**
	 * Triggered by the lava map objects, sets the player on lava.
	 * @param enter
	 */
	public void enterLava(boolean enter) {
		if(enter) {
			inLava++;
		}
		else {
			if(inLava()) {
				inLava--;
			}
		}
	}
	
	/**
	 * @return true if the player touches lava.
	 */
	public boolean inLava() {
		return inLava > 0;
	}
	
	/**
	 * Sets player "on fire", allowing him to leave trail of fire behind.
	 * @param blazing true to set blazing, false to remove the effect.
	 */
	public void setBlazing(boolean blazing) {
		if(blazing) {
			isBlazing++;
		}
		else {
			if(isBlazing()) {
				isBlazing--;
			}
		}
	}
	
	/**
	 * @return true if is under effect of blazing feet.
	 */
	public boolean isBlazing() {
		return isBlazing > 0;
	}
	
	/**
	 * Confuses the player, inverting his input data.
	 * @param confused true to confuse, false to remove.
	 */
	public void setConfused(boolean confused) {
		if(confused) {
			// Changing current player's destination:
			if(destination != null) {
				destination.set(LogicUtils.getInvertedPosition(this, destination));
				// Recalculating direction:
				direction = LogicUtils.getPlayerAngle(playerBody.getPosition(), destination);
			}
			
			isConfused++;
		}
		else  {
			if(isConfused()) {
				isConfused--;
			}
		}
	}
	
	/**
	 * @return true if player is confused.
	 */
	public boolean isConfused() {
		return isConfused > 0;
	}
	
	/**
	 * Curses the player, making him lose a high amount of health after some time.
	 * @param cursed true to curse, false to remove.
	 */
	public void setCursed(boolean cursed) {
		if(cursed) {
			isCursed++;
		}
		else {
			if(isCursed()) {
				isCursed--;
			}
		}
	}
	
	/**
	 * @return true if character is cursed.
	 */
	public boolean isCursed() {
		return isCursed > 0;
	}

	/**
	 * Attempts to heal a curse.
	 * @param probability the chance to heal a curse. <0,1>
	 */
	public void healCurse(float probability) {
		if(isCursed()) {
			if(MathUtils.random() <= probability) {
				setCursed(false);
				setConfused(false);
			}
		}
	}
	
	/**
	 * Shields the player, protecting him from damage and some effects.
	 * @param isShielded true to shield, false to remove.
	 */
	public void setShielded(boolean isShielded) {
		if(isShielded) {
			this.isShielded++;
		}
		else {
			if(this.isShielded > 0) {
				this.isShielded--;
			}
		}
	}
	
	/**
	 * @return true if character is shielded.
	 */
	public boolean isShielded() {
		return isShielded > 0;
	}
	
	/**
	 * Freezes the player, immobilizing and paralyzing him.
	 * @param freeze true to freeze, false to remove.
	 */
	public void freezePlayer(boolean freeze) {
		immobilizePlayer(freeze);
		paralyzePlayer(freeze);
	}
	
	/**
	 * Immobilizes the player, making him unable to move.
	 * @param immobilize true to immobilize, false to remove.
	 */
	public void immobilizePlayer(boolean immobilize) {
		if(immobilize) {
			isImmobilized++;
			cancelDestination();
		}
		else {
			if(isImmobilized()) {
				isImmobilized--;
			}
		}
	}
	
	
	/**
	 * Immobilizes the player, making him unable to move.
	 * @param immobilize true to immobilize, false to remove.
	 */
	public void paralyzePlayer(boolean paralyze) {
		if(paralyze) {
			isParalyzed++;
		}
		else {
			if(isParalyzed()) {
				isParalyzed--;
			}
		}
	}

	/**
	 * @return true if the character is paralyzed and cannot cast spells.
	 */
	public boolean isParalyzed() {
		return isParalyzed > 0;
	}
	
	/**
	 * @return true if the character is immobilized and cannot move.
	 */
	public boolean isImmobilized() {
		return isImmobilized > 0;
	}
	
	//////////////////////////////////////////////////////////////////
	// Spell management.
	//////////////////////////////////////////////////////////////////

	/**
	 * Updates all spells' cooldowns.
	 * @param delta time passes since the last updates.
	 */
	private void updateSpellCooldowns(float delta) {
		for(int i=0;i<spellCooldowns.length;i++) {
			if(spellCooldowns[i] > 0f) {
				spellCooldowns[i] -= delta;
			}
		}
	}
	
	/**
	 * Checks if a spell is currently available.
	 * @param spellType spell's element.
	 * @return true if the spell is available, false if not.
	 */
	public boolean canCastSpell(int spellType) {
		return !isDead && !isParalyzed() && spellCooldowns[spellType] <= 0f;
	}
	
	/**
	 * Applies cooldown for the chosen spell.
	 * @param spellType spell's element.
	 * @param cooldown time (in seconds) of spell's unavailability.
	 */
	public void applySpellCooldown(byte spellType,float cooldown) {
		// Setting spell cooldown:
		spellCooldowns[spellType] = cooldown*cooldownModificator;
		// Sending packet to the player to let him know about the recast time:
		SrvSetSpellCooldown packet = new SrvSetSpellCooldown();
		packet.spellType = spellType;
		packet.cooldown = spellCooldowns[spellType];
		owner.sendUDP(packet);
	}
	
	/**
	 * @return current cooldown modificator.
	 */
	public float getCooldownModificator() {
		return cooldownModificator;
	}
	
	/**
	 * @return current damage modificator.
	 */
	public float getDamageModificator() {
		return damageModificator;
	}
	
	//////////////////////////////////////////////////////////////////
	// Movement management.
	//////////////////////////////////////////////////////////////////
	
	/**
	 * Updates player's position according to movement data.
	 * @param delta time passed since the last update.
	 */
	protected void updateMovement(float delta) {
		isPushed = false;
		// Applying "gravity":
		if(!playerBody.getLinearVelocity().equals(Vector2.Zero)) {
			// Checking if the body is moving at a unusually high speed:
			isPushed = (Math.abs(playerBody.getLinearVelocity().x) +
				Math.abs(playerBody.getLinearVelocity().y)) > PUSHBACK_THRESHOLD;
			
			// If it is - slowing it down a bit:
			if(isPushed) {
				playerBody.applyForceToCenter(pushbackSlowdown *
					-playerBody.getLinearVelocity().x,pushbackSlowdown *
					-playerBody.getLinearVelocity().y, true);
			}
			// If it isn't - slowing it down by the usual "gravity" force:
			else {
				playerBody.applyForceToCenter(MOVEMENT_SLOWDOWN *
					-playerBody.getLinearVelocity().x,MOVEMENT_SLOWDOWN *
					-playerBody.getLinearVelocity().y, true);
			}
		}
		// If the player is trying to reach a point:
		if(destination != null) {
			// If he has reached it:
			if(playerBody.getFixtureList().get(0).testPoint(destination)) {
				cancelDestination();
			}
			// If he hasn't yet:
			else {
				// If the character isn't already moving too fast and if he's not immobilized:
				if(!isPushed && isImmobilized <= 0 && playerSpeed > MOVEMENT_SLOWDOWN) {
					// Applying force in the calculated direction:
					float xWidth = playerBody.getPosition().x - destination.x,
						yHeight = playerBody.getPosition().y - destination.y,
						wayLength = (float)Math.sqrt(xWidth*xWidth+yHeight*yHeight);
					playerBody.applyForceToCenter(-(playerSpeed * xWidth)/wayLength,
						-(playerSpeed * yHeight)/wayLength, true);
				}
			}
		}
	}
	
	/**
	 * @return true if character is moving, false for no destination.
	 */
	public boolean isMoving() {
		return destination != null;
	}
	
	/**
	 * Changes current character's speed.
	 * @param speed will be added to the current speed.
	 */
	public void modifySpeed(float speed) {
		this.playerSpeed += speed;
	}
	
	/**
	 * @return current player's speed.
	 */
	public float getSpeed() {
		return playerSpeed;
	}
	
	/**
	 * @param spellType spell type.
	 * @return player's spell connected with the given type.
	 */
	public Spell getSpell(SpellType spellType) {
		return spells[spellType.getIndex()];
	}
	
	/**
	 * Sets the player destination.
	 * @param x destination's x in the Box2D world.
	 * @param y destination's y in the Box2D world.
	 */
	public void setDestination(Vector2 destination) {
		if(!isImmobilized() && !isDead) {
			if(playerBody.getPosition().x != destination.x || playerBody.getPosition().y != destination.y) {
				this.destination = new Vector2(destination);
				direction = LogicUtils.getPlayerAngle(playerBody.getPosition(),this.destination);
			}
		}
	}

	/**
	 * Nullifies character's destination.
	 */
	public void cancelDestination() {
		destination = null;
	}
	
	/**
	 * Does nothing for regular players. In case of summons, helps the AI
	 * determine if they should look for another target.
	 * @param teleported true if the character has been recently moved.
	 */
	public void setTeleported(boolean teleported) {
		// Does nothing - players have no AI.
	}
	
	/**
	 * @return index of the last used teleport or -1 for none.
	 */
	public int getLastUsedTeleportIndex() {
		return lastUsedTeleport;
	}
	
	/**
	 * @param teleportIndex will be set as the index of the last used teleport.
	 */
	public void setLastUsedTeleport(int teleportIndex) {
		this.lastUsedTeleport = teleportIndex;
	}
	
	/**
	 * Sets last used teleport index to -1, allowing the player to be teleported by
	 * all teleports.
	 * @param teleportIndex index of the teleport that ends contact with the player.
	 */
	public void resetLastUsedTeleport(int teleportIndex) {
		if(teleportIndex == lastUsedTeleport) {
			lastUsedTeleport = -1;
		}
	}
	
	/**
	 * @return current destination or null.
	 */
	public Vector2 getDestination() {
		return destination;
	}
	
	//////////////////////////////////////////////////////////////////
	// Health points management.
	//////////////////////////////////////////////////////////////////
	
	/**
	 * Calculates current health percent according to current health points amount
	 * and max health points of the player's class.
	 */
	public void recalculateHealthPercent() {
		currentHealthPercent = currentHealth / maxHealth;
	}
	
	/**
	 * @return current player's health points percent.
	 */
	public float getCurrentHealthPercent() {
		return currentHealthPercent;
	}
	
	/**
	 * Should be triggered by each update to check if the player is in lava and
	 * should suffer damage.
	 */
	protected void checkIfInLava() {
		// If the player touches lava, he suffers damage:
		if(inLava()) {
			modifyHealth(null,-0.35f,false,true);
		}
	}
	
	/**
	 * Changes current player's health according to the given data.
	 * @param damageDealer player causing the health modification.
	 * Accepts nulls - null won't change health modification amount
	 * and won't change current damage dealer.
	 * @param healthAmount health modification. Negative deals damage, positive heals.
	 * @param changeAnimation true to switch current player's animation to block.
	 * @param affectsShield true if the health modification is lowered by shield.
	 */
	public void modifyHealth(Player damageDealer,float healthAmount,
		boolean changeAnimation,boolean affectsShield) {
		if(!isDead) {
			// Damage modification affects shield and the character is shielded:
			if(affectsShield && isShielded()) {
				if(healthAmount < 0f) {
					healthAmount *= 0.25f;
				}
			}
			
			// Adding damage modificator:
			if(damageDealer != null) {
				healthAmount *= damageDealer.getDamageModificator();
				
				// If it's actually damage:
				if(healthAmount < 0f && damageDealer != this) {
					// Setting the person who dealt damage as the last damage dealer:
					setLastDamageDealer(damageDealer);
					
					// Team mode - lowered damage for allies:
					if(damageDealer.getTeamIndex() >= 0
						&& damageDealer.getTeamIndex() == teamIndex) {
						healthAmount *= 0.4f;
					}
				}
			}
	
			// Applying health change:
			currentHealth += healthAmount;
			
			// Setting block animation:
			if(changeAnimation) {
				state = PlayerState.BLOCK;
			}
			
			// Making sure health is not out of bounds:
			if(currentHealth > maxHealth) {
				currentHealth = maxHealth;
			}
			// Checking if player is dead:
			else if(currentHealth <= 0f) {
				// Sets the player as dead:
				setDead();
			}
			
			recalculateHealthPercent();
		}
	}
	
	/**
	 * Changes the last damage dealer, provided he actually exists.
	 * @param lastDamageDealer the last person that did damage to the player.
	 */
	public void setLastDamageDealer(Player lastDamageDealer) {
		this.lastDamageDealer = lastDamageDealer;
	}
	
	/**
	 * Finalizes player's death - removes him from players lists,
	 * adds scores, etc.
	 */
	protected void setDead() {
		currentHealth = 0f;
		isDead = true;
		destination = null;
		
		if(lastDamageDealer!= null) {
			// Getting actual killer:
			if(lastDamageDealer instanceof Summon) {
				lastDamageDealer = ((Summon)lastDamageDealer).getCaster();
			}
			
			// Refreshing point counters:
			if(lastDamageDealer != this && (lastDamageDealer.getTeamIndex() < 0 ||
				lastDamageDealer.getTeamIndex() != this.getTeamIndex())) {
				game.addKill(lastDamageDealer.getOwner());
			}
		}
		game.addDeath(owner);
		
		// Telling the game to remove the player completely:
		game.getEntitiesToRemove().add(this);
	}
	
	/**
	 * @return true if character has no health left or left the arena.
	 */
	public boolean isDead() {
		return isDead || isRemoved;
	}
	
	/**
	 * Contains all current players' characters' types.
	 * @author MJ
	 */
	public static enum PlayerClass {
		LICH(new PlayerClassBuilder((byte)0,false,BodyInformation.LICH)
			.health(90).damageMod(1.35f).cooldownMod(1.1f).speed(1900f).pushbackSlowdown(175f)),
		GOBLIN(new PlayerClassBuilder((byte)1,false,BodyInformation.GOBLIN)
			.health(80).damageMod(0.9f).cooldownMod(0.8f).speed(3000f).pushbackSlowdown(150f)),
		WITCH(new PlayerClassBuilder((byte)2,false,BodyInformation.WITCH)
			.cooldownMod(0.95f).speed(2550f)),
		JUGGERNAUT(new PlayerClassBuilder((byte)3,false,BodyInformation.JUGGERNAUT)
			.health(135).damageMod(0.8f).cooldownMod(1.1f).speed(2250f).pushbackSlowdown(350f)),
		MINOTAUR(new PlayerClassBuilder((byte)8,true,BodyInformation.MINOTAUR)
			.summonName("'s Minotaur").health(100).damageMod(0.75f).cooldownMod(0f).speed(4250f).pushbackSlowdown(500f)),
		FIRE_ANT(new PlayerClassBuilder((byte)9,true,BodyInformation.FIRE_ANT)
			.summonName("'s Fire Ant").health(70).damageMod(0.65f).cooldownMod(0f).speed(2500f).pushbackSlowdown(125f));
		
		private final byte index;
		private final String summonName;
		private final boolean isSummon;
		private final float speed,pushbackSlowdown,health,damageModificator,cooldownModificator;
		private final BodyInformation bodyInformation;
		private PlayerClass(PlayerClassBuilder playerClassBuilder) {
			this.index = playerClassBuilder.index;
			this.summonName = playerClassBuilder.summonName;
			this.isSummon = playerClassBuilder.isSummon;
			this.bodyInformation = playerClassBuilder.bodyInformation;
			this.speed = playerClassBuilder.speed;
			this.health = playerClassBuilder.health;
			this.damageModificator = playerClassBuilder.damageModificator;
			this.cooldownModificator = playerClassBuilder.cooldownModificator;
			this.pushbackSlowdown = playerClassBuilder.pushbackSlowdown;
		}
		
		/**
		 * Player class builder for clearer class creation and defining default values.
		 * @author MJ
		 */
		private static class PlayerClassBuilder {
			// Unique class values:
			private final byte index;
			private final boolean isSummon;
			private final BodyInformation bodyInformation;
			// Extra class modificators:
			private float speed,pushbackSlowdown,health,
				damageModificator,cooldownModificator;
			private String summonName;
			private PlayerClassBuilder(byte index,boolean isSummon, BodyInformation bodyInformation) {
				this.index = index;
				this.isSummon = isSummon;
				this.bodyInformation = bodyInformation;
				// Default values:
				health = 100f;
				speed = 2500f;
				pushbackSlowdown = 225f;
				damageModificator = 1f;
				cooldownModificator = 1f;
				summonName = "";
			}
			/**
			 * If it's a summon class, it should have a name starting with "'s ".
			 */
			private PlayerClassBuilder summonName(String summonName) {
				this.summonName = summonName;
				return this;
			}
			/**
			 * Should be in range of 1500-3000. 2500 is default.
			 */
			private PlayerClassBuilder speed(float speed) {
				this.speed = speed;
				return this;
			}
			/**
			 *  Should be in range of 150-300. 200 is default.
			 */
			private PlayerClassBuilder pushbackSlowdown(float pushbackSlowdown) {
				this.pushbackSlowdown = pushbackSlowdown;
				return this;
			}
			
			/**
			 * Should be in range of 75-150. 100 is default.
			 */
			private PlayerClassBuilder health(float health) {
				this.health = health;
				return this;
			}
			/**
			 * Should be in range of 0.5-1.5. 1 is default.
			 */
			private PlayerClassBuilder damageMod(float damageModificator) {
				this.damageModificator = damageModificator;
				return this;
			}
			/**
			 * Should be in range of 0.5-1.5. 1 is default.
			 */
			private PlayerClassBuilder cooldownMod(float cooldownModificator) {
				this.cooldownModificator = cooldownModificator;
				return this;
			}
		}
		
		/**
		 * @return class' index.
		 */
		public byte getIndex() {
			return index;
		}
		
		/**
		 * @return false for playable class, true for summon.
		 */
		public boolean isSummon() {
			return isSummon;
		}
		
		/**
		 * @return class' default speed in newtons per game tick.
		 */
		public float getSpeed() {
			return speed;
		}
		
		/**
		 * @return class' Box2D body information.
		 */
		public BodyInformation getBodyInformation() {
			return bodyInformation;
		}
		
		/**
		 * @return default max health of the class.
		 */
		public float getHealth() {
			return health;
		}
		
		/**
		 * @return the modificator that influences time between spell recasts.
		 */
		public float getCooldownModificator() {
			return cooldownModificator;
		}
		
		/**
		 * @return the modificator that changes the damage done by spells.
		 */
		public float getDamageModificator() {
			return damageModificator;
		}
		
		/**
		 * @return the force which works as "gravity" when the character is pushed.
		 */
		public float getPushbackSlowdown() {
			return pushbackSlowdown;
		}
		
		/**
		 * @param index class index.
		 * @return class with the given index. Null for invalid index.
		 */
		public static PlayerClass getPlayerClass(byte index) {
			for(PlayerClass playerClass : PlayerClass.values()) {
				if(playerClass.getIndex() == index) {
					return playerClass;
				}
			}
			return null;
		}
		
		@Override
		public String toString() {
			return summonName;
		}
	}
	
	public static enum PlayerState {
		STANCE((byte)0),WALK((byte)1),CAST1((byte)2),
		CAST2((byte)3),BLOCK((byte)4),DEATH((byte)5);
		
		private final byte index;
		private PlayerState(byte index) {
			this.index = index;
		}
		
		public byte getIndex() {
			return index;
		}
	}
}
