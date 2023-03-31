package mj.konfigurats.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import mj.konfigurats.Core;
import mj.konfigurats.managers.InterfaceManager.Screens;

/**
 * Abstract implementation of the GameScreen interface.
 * @author MJ
 */
public abstract class AbstractScreen implements GameScreen {
	protected final Stage stage;

	public AbstractScreen() {
		stage = new Stage(new ScreenViewport(),
			((Core)Gdx.app.getApplicationListener())
			.getInterfaceManager().getStageBatch());
	}

	@Override
	public void resize(final int width,final int height) {
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				stage.getViewport().update(width,height,true);
			}
		});
	}

	@Override
	public void show() {
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				// Removing all dialogs:
				for(Actor actor : stage.getActors()) {
					if(actor instanceof Dialog) {
						actor.remove();
					}
				}
				// Fading the screen in:
				stage.addAction(Actions.sequence(Actions.alpha(0),Actions.fadeIn(0.5f),
					Actions.run(new Runnable() {
						@Override
						public void run() {
							Gdx.input.setInputProcessor(stage);
						}
					})));
			}
		});
	}

	@Override
	public void hide(final Screens screen) {
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				Gdx.input.setInputProcessor(null);

				// Fading the screen out:
				stage.addAction(Actions.sequence(Actions.fadeOut(0.5f),
					Actions.run(new Runnable() {
					@Override
					public void run() {
						// Making sure all unnecessary dialogs are removed:
						((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
							.removeConnectingDialog();
						((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
							.displaySettings(false);

						// Showing new screen:
						((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
							.showScreen(screen);
					}
				})));
			}
		});
	}

	@Override
	public void dispose() {
		stage.dispose();
	}

	@Override
	public void render(float delta) {
		stage.act(delta);
		stage.draw();
	}

	@Override
	public Stage getStage() {
		return stage;
	}

	@Override
	public void pause() {
		// TODO
	}

	@Override
	public void resume() {
		// TODO
	}

	@Override
	public float getBackgroundOffset() {
		// No background.
		return 0;
	}

	@Override
	public void setBackgroundOffset(float offset) {
		// No background.
	}
}
