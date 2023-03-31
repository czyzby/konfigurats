package mj.konfigurats.gui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import mj.konfigurats.managers.InterfaceManager.Screens;

/**
 * Should be implemented by every screen in the game.
 * @author MJ
 */
public interface GameScreen {
	/**
	 * Separated from the constructor, this method should be called when all assets for the screen are loaded.
	 */
	public void create();

	/**
	 * Resizes the screen. Run each time the screen is shown or resized.
	 * @param width new width.
	 * @param height new height.
	 */
	public void resize (int width, int height);

	/**
	 * Shows the screen.
	 */
	public void show();

	/**
	 * Hides the screen.
	 */
	public void hide(Screens screen);

	/**
	 * Destroys screen's objects that need disposing.
	 */
	public void dispose();

	/**
	 * Updates screen's logic according to the received packet
	 * @param packet network packet.
	 */
	public void update(Object packet);

	/**
	 * Draws the screen.
	 * @param delta time passed since the last rendering.
	 */
	public void render(float delta);

	/**
	 * Called on Android when the application is paused.
	 */
	public void pause();

	/**
	 * Called on Android when the application is resumed.
	 */
	public void resume();

	/**
	 * @return screen's stage.
	 */
	public Stage getStage();

	/**
	 * @return current background offset of the scrolled interface background.
	 * Screens with no scrolled background will return 0.
	 */
	public float getBackgroundOffset();

	/**
	 * Sets the current scrolled background offset.
	 * @param offset the new offset.
	 */
	public void setBackgroundOffset(float offset);
}
