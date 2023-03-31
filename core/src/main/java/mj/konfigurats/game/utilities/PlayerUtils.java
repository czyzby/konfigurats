package mj.konfigurats.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import mj.konfigurats.Core;
import mj.konfigurats.game.entities.Player.PlayerAnimationType;
import mj.konfigurats.game.entities.Player.PlayerClass;

public class PlayerUtils {
	private PlayerUtils() {}
	public static final int DIRECTIONS_AMOUNT = 8,PLAYER_IMAGE_SIZE = 128;

	private static TextureRegion[][][][] PLAYER_ANIMATIONS;

	/**
	 * Splitting player animations textures into atlases ready to render.
	 * Should be called once - after loading all of the game assets.
	 */
	public static void createPlayerAnimations() {
		PLAYER_ANIMATIONS = new TextureRegion[(PlayerClass.values().length
			-PlayerClass.getSummonsAmount())*2+PlayerClass.getSummonsAmount()]
			[PlayerAnimationType.values().length][][];
		for(int playerClass=0; playerClass<PLAYER_ANIMATIONS.length; playerClass++) {
			for(int frame=0; frame<PLAYER_ANIMATIONS[playerClass].length; frame++) {
				// One animation for each direction:
				PLAYER_ANIMATIONS[playerClass][frame] = TextureRegion.split
					(((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
					.getAsset("game/sprites/"+playerClass+"-"+frame+".png",
					Texture.class),PLAYER_IMAGE_SIZE,PLAYER_IMAGE_SIZE);
			}
		}
	}

	/**
	 * @param playerClass index of the player's class.
	 * @return a set of animations for the given class.
	 */
	public static TextureRegion[][][] getPlayerAnimationSet(int playerClass) {
		return PLAYER_ANIMATIONS[playerClass];
	}
}
