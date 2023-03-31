package mj.konfigurats.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import mj.konfigurats.Core;
import mj.konfigurats.managers.InterfaceManager;

/**
 * Additionally to the usual AbstractScreen method implementations
 * InterfaceScreen contains and draws the default scrolled background.
 * @author MJ
 */
public abstract class AbstractInterfaceScreen extends AbstractScreen {
	protected Window window;
	protected TiledDrawable background;
	// Control variables:
	private float backgroundWidth, backgroundOffset, scrollingSpeed;

	public AbstractInterfaceScreen() {
		super();
	}

	@Override
	public void create() {
		// Getting default interface background:
		background = new TiledDrawable(new TextureRegion(((Core)
			Gdx.app.getApplicationListener()).getInterfaceManager().
			getAsset("global/images/bg-interface.png",Texture.class)));
		// Setting background control variables:
		backgroundWidth = background.getRegion().getRegionWidth();
		scrollingSpeed = 20f;

		// Creating a window with the default interface skin:
		window = new Window("",((Core)Gdx.app.getApplicationListener())
			.getInterfaceManager().getSkin());
		// Adding window to the stage.
		stage.addActor(window);

		// Adding listener to allow playing click sounds:
		stage.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				SoundUtilities.playMousePressSound();
				return true;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y,
					int pointer, int button) {
				SoundUtilities.playMouseReleaseSound();
			}
		});
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		InterfaceManager.centerWindow(window);
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// Setting background offset:
		backgroundOffset += delta * scrollingSpeed;
		backgroundOffset %= backgroundWidth;

		if(background != null) {
			stage.getBatch().begin();
				stage.getBatch().disableBlending();
				background.draw(stage.getBatch(), -backgroundOffset, 0,
					Gdx.graphics.getWidth()+backgroundWidth,
					Gdx.graphics.getHeight());
				stage.getBatch().enableBlending();
			stage.getBatch().end();
		}

		super.render(delta);
	}

	@Override
	public float getBackgroundOffset() {
		return backgroundOffset;
	}

	@Override
	public void setBackgroundOffset(float offset) {
		backgroundOffset = offset;
	}

	/**
	 * Sets the background scrolling speed. Default is 20.
	 * @param scrollingSpeed new scrolling speed.
	 */
	public void setScrollingSpeed(float scrollingSpeed) {
		this.scrollingSpeed = scrollingSpeed;
	}
}
