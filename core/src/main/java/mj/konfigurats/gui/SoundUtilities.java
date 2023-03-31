package mj.konfigurats.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import mj.konfigurats.Core;
import mj.konfigurats.game.entities.Player;
import mj.konfigurats.game.utilities.SpellUtils;

public class SoundUtilities {
	private SoundUtilities() {}
	public static final int GAME_SOUNDS_AMOUNT = 32;
	private static final float SOUND_DISTANCE_THRESHOLD = 256f;

	// Global game sounds:
	private static Sound MOUSE_PRESS, MOUSE_RELEASE;
	private static Sound[] GAME_SOUNDS;

	/**
	 * Assigns interface sounds after they are loaded. Should be run before the
	 * first interface screen is shown.
	 */
	public static void prepareInterfaceSounds() {
		MOUSE_PRESS = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getAsset("global/sounds/mouse-press.ogg",Sound.class);
		MOUSE_RELEASE = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getAsset("global/sounds/mouse-release.ogg",Sound.class);
	}

	/**
	 * Assigns game sounds. Should be run after all assets are loaded.
	 */
	public static void prepareGameSounds() {
		GAME_SOUNDS = new Sound[GAME_SOUNDS_AMOUNT];
		for(int i=0; i < GAME_SOUNDS_AMOUNT; i++) {
			GAME_SOUNDS[i] = ((Core)Gdx.app.getApplicationListener())
				.getInterfaceManager().getAsset("game/sounds/"+i+".mp3",Sound.class);
		}
	}

	public static void playGameSound(int soundIndex,Vector2 position) {
		// We're doing all the work only if it's needed:
		if(GameSettings.SOUNDS_ON) {
			Player userCharacter = ((Core)Gdx.app.getApplicationListener())
				.getInterfaceManager().getBattleManager().getUserCharacter();
			if(userCharacter != null) {
				// Getting sound distance:
				float distance = SpellUtils.getDistance(position.x, position.y,
					userCharacter.getX(), userCharacter.getY());
				distance = SOUND_DISTANCE_THRESHOLD - distance;
				if(distance > 0f) {
					// Playing the sound louder if it's close by:
					GAME_SOUNDS[soundIndex].play(GameSettings.SOUNDS_VOLUME
						*(distance/SOUND_DISTANCE_THRESHOLD));
				}
			}
		}
	}

	/**
	 * Plays the mouse press sounds, provided the sounds are not turned off.
	 */
	public static void playMousePressSound() {
		if(GameSettings.SOUNDS_ON && MOUSE_PRESS != null) {
			MOUSE_PRESS.play(GameSettings.SOUNDS_VOLUME);
		}
	}

	/**
	 * Plays the mouse release sounds, provided the sounds are not turned off.
	 */
	public static void playMouseReleaseSound() {
		if(GameSettings.SOUNDS_ON && MOUSE_RELEASE != null) {
			MOUSE_RELEASE.play(GameSettings.SOUNDS_VOLUME);
		}
	}

	/**
	 * Sounds are kept by the asset manager, but it doesn't hurt
	 * to make sure they are properly disposed.
	 */
	public static void dispose() {
		if(MOUSE_PRESS != null) {
			MOUSE_PRESS.dispose();
		}
		if(MOUSE_RELEASE != null) {
			MOUSE_RELEASE.dispose();
		}
		if(GAME_SOUNDS != null) {
			for(Sound sound : GAME_SOUNDS) {
				if(sound != null) {
					sound.dispose();
				}
			}
		}
	}
}
