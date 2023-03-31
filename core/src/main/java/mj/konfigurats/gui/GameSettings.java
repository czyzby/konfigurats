package mj.konfigurats.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;


/**
 * Utility class containing game settings. Probably temporary.
 * @author MJ
 */
public class GameSettings {
	private GameSettings() {}
	
	public static boolean 
		SOUNDS_ON,
		MUSIC_ON,
		FULLSCREEN,
		VSYNC,
		MOUSE_CONTROL,
		CHAT_IN_GAME_MANAGEMENT,
		SHOW_GAME_INTERFACE;
	
	public static float
		SOUNDS_VOLUME,
		MUSIC_VOLUME;
	
	static { //TODO get and save user's settings?
		// Sounds settings:
		SOUNDS_ON = true;
		SOUNDS_VOLUME = 1f;
		MUSIC_ON = true;
		MUSIC_VOLUME = 0.7f;
		
		// Game settings:
		FULLSCREEN = false;
		VSYNC = true;
		MOUSE_CONTROL = false;
		CHAT_IN_GAME_MANAGEMENT = true;
		SHOW_GAME_INTERFACE = true;
	}
	
	public static void switchToFullScreen(boolean fullscreen) {
		if(FULLSCREEN != fullscreen) {
			FULLSCREEN = fullscreen;
			
			if(FULLSCREEN) {
				Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
				Gdx.graphics.setFullscreenMode(mode);
			}
			else {
				Gdx.graphics.setWindowedMode(1024, 512);
			}
		}
	}
}
