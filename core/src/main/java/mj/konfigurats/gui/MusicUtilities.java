package mj.konfigurats.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;
import com.badlogic.gdx.math.MathUtils;
import mj.konfigurats.Core;

/**
 * Utility class taking care of music. Do not initialize.
 * @author MJ
 */
public class MusicUtilities {
	private MusicUtilities() {}

	public static final int BATTLE_THEMES_AMOUNT = 7;

	private static Music MENU_THEME;
	private static Music[] BATTLE_THEMES;
	private static boolean IS_IN_MENU;
	private static int CURRENT_BATTLE_THEME;

	/**
	 * Assigns music files. Should be called when all assets are loaded.
	 */
	public static void prepareMusic() {
		// Preparing menu theme:
		MENU_THEME = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getAsset("global/sounds/menu.mp3",Music.class);
		MENU_THEME.setLooping(true);
		IS_IN_MENU = true;

		// Preparing game themes:
		BATTLE_THEMES = new Music[BATTLE_THEMES_AMOUNT];
		for(int i=0; i < MusicUtilities.BATTLE_THEMES_AMOUNT; i++) {
			BATTLE_THEMES[i] = ((Core)Gdx.app.getApplicationListener())
				.getInterfaceManager().getAsset("game/music/battle"+i+".mp3",
				Music.class);
			BATTLE_THEMES[i].setOnCompletionListener (new OnCompletionListener() {
				@Override
				public void onCompletion(Music music) {
					if(!IS_IN_MENU && GameSettings.MUSIC_ON) {
						Gdx.app.postRunnable(new Runnable() {
							@Override
							public void run() {
								startGameMusic();
							};
						});
					}
				}
			});
		}

	}

	/**
	 * Starts menu theme. Should be called when the player enters a regular
	 * game interface screen.
	 */
	public static void startMenuMusic() {
		if(!IS_IN_MENU) {
			IS_IN_MENU = true;
			BATTLE_THEMES[CURRENT_BATTLE_THEME].stop();
		}
		if(GameSettings.MUSIC_ON) {
			// Playing menu theme:
			MENU_THEME.setVolume(GameSettings.MUSIC_VOLUME);
			if(!MENU_THEME.isPlaying()) {
				MENU_THEME.play();
			}
		}
	}

	/**
	 * Starts one of game's battle themes. Should be called when the player
	 * enters a game room.
	 */
	public static void startGameMusic() {
		if(IS_IN_MENU) {
			IS_IN_MENU = false;
			MENU_THEME.stop();
		}
		if(GameSettings.MUSIC_ON) {
			// Playing random theme:
			int oldTheme = CURRENT_BATTLE_THEME;
			CURRENT_BATTLE_THEME = MathUtils.random(BATTLE_THEMES_AMOUNT-1);
			if(CURRENT_BATTLE_THEME == oldTheme) {
				CURRENT_BATTLE_THEME ++;
				CURRENT_BATTLE_THEME %= BATTLE_THEMES_AMOUNT;
			}
			BATTLE_THEMES[CURRENT_BATTLE_THEME].setVolume(GameSettings.MUSIC_VOLUME);
			BATTLE_THEMES[CURRENT_BATTLE_THEME].play();
		}
	}

	/**
	 * Makes sure the currently played music's volume is up to date.
	 */
	public static void validateMusicVolume() {
		// If the music is actually on...
		if(GameSettings.MUSIC_ON) {
			if(IS_IN_MENU) {
				// If player is in the menu...
				MENU_THEME.setVolume(GameSettings.MUSIC_VOLUME);
			}
			else {
				// If player is in a game room...
				BATTLE_THEMES[CURRENT_BATTLE_THEME].setVolume(GameSettings.MUSIC_VOLUME);
			}
		}
	}

	/**
	 * Makes sure that the music is (not) played according to the current settings.
	 */
	public static void validateMusic() {
		// Music turned on:
		if(GameSettings.MUSIC_ON) {
			if(IS_IN_MENU) {
				startMenuMusic();
			}
			else {
				startGameMusic();
			}
		}
		// Music turned off:
		else {
			if(IS_IN_MENU) {
				MENU_THEME.stop();
			}
			else {
				BATTLE_THEMES[CURRENT_BATTLE_THEME].stop();
			}
		}
	}
}
