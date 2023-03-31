package mj.konfigurats.gui.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import mj.konfigurats.Core;

/**
 * A widget displaying a simple health bar with a background.
 * When the health percent is changed, the bar will smoothly change its displayed value.
 * @author MJ
 */
public class HealthBar extends Widget {
	// Health bar images:
	private final Drawable background;
	private final NinePatchDrawable healthBar;
	// Health bar bounds:
	private final float width,height,healthBarHeight;
	// Control variables:
	private float currentHealth,currentDisplayedHealth;
	private boolean isUpToDate,fadesOutOnZero,isFading;
	private float smoothingSpeed;

	/**
	 * Creates a health bar with the default values.
	 */
	public HealthBar() {
		this(1f,2);
	}

	/**
	 * Creates a new health bar with the default images.
	 * @param initialPercent initial current health percent.
	 * @param smoothingSpeed the speed at which the displayed health percent is changed.
	 */
	public HealthBar(float initialPercent,float smoothingSpeed) {
		super();

		// Getting images:
		healthBar = new NinePatchDrawable(new NinePatch(((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getSkin().getRegion("healthbar-full"),4,4,0,0));
		background = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getSkin().getDrawable("healthbar-empty");

		// Setting bounds:
		width = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getSkin().getRegion("healthbar-empty").getRegionWidth();
		height = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getSkin().getRegion("healthbar-empty").getRegionHeight();
		healthBarHeight = ((Core)Gdx.app.getApplicationListener()).getInterfaceManager()
			.getSkin().getRegion("healthbar-full").getRegionHeight();

		// Setting control variables:
		currentDisplayedHealth = currentHealth = initialPercent;
		isUpToDate = true;
		fadesOutOnZero = isFading = false;
		this.smoothingSpeed = smoothingSpeed;
	}

	@Override
	public float getPrefWidth() {
		return width;
	}

	@Override
	public float getPrefHeight() {
		return height;
	}

	/**
	 * Sets the speed at which the displayed health value is changing to match the actual health percent.
	 * @param smoothingSpeed the smoothing speed. The higher, the slower the smoothing is. Should be about <1,10>.
	 */
	public void setSmoothingSpeed(int smoothingSpeed) {
		this.smoothingSpeed = smoothingSpeed;
	}

	/**
	 * If set to true, health bar will start to fade out each time
	 * it is set to 0. Default is false.
	 * @param fadesOutOnZero true to fade out on 0.
	 */
	public void setFadingOutOnZero(boolean fadesOutOnZero) {
		this.fadesOutOnZero = fadesOutOnZero;
		if(fadesOutOnZero) {
			addAction(Actions.alpha(0));
			isFading = true;
		}
	}

	/**
	 * Sets current health percent.
	 * @param currentHealth current health percent in the range of <0,1>.
	 */
	public void setCurrentHealth(float currentHealth) {
		if(currentHealth < 0f) {
			currentHealth = 0f;
		}

		// Checking if health bar should be faded in or out:
		if(fadesOutOnZero) {
			if(currentHealth > 0f) {
				// Is fading or invisible and gets non-zero value: fading in:
				if(isFading) {
					isFading = false;
					clearActions();
					addAction(Actions.fadeIn(0.5f));
				}
			}
			else {
				// Is visible, but gets zero value - fading out:
				if(!isFading) {
					isFading = true;
					clearActions();
					addAction(Actions.fadeOut(0.5f));
				}
			}
		}

		// Setting new current health value:
		this.currentHealth = currentHealth;
		// Making sure that the displayed value will be corrected:
		isUpToDate = false;
	}

	/**
	 * @return current health percent shown on the health bar.
	 */
	public float getCurrentHealth() {
		return currentHealth;
	}

	@Override
	public void act(float delta) {
		// If the displayed value is not up to date:
		if(!isUpToDate) {
			// Compating displayed health with current health:
			int compare = Float.compare(currentDisplayedHealth, currentHealth);

			// Displayed health is actually the same as the current health:
			if(compare == 0) {
				isUpToDate = true;
			}
			// Displayed health is lower than the actual health:
			else if(compare < 0) {
				if(currentHealth-currentDisplayedHealth < delta/smoothingSpeed) {
					currentDisplayedHealth = currentHealth;
					isUpToDate = true;
				}
				else {
					currentDisplayedHealth += delta/smoothingSpeed;
				}
			}
			// Displayed health is higher than the actual health:
			else {
				if(currentDisplayedHealth-currentHealth < delta/smoothingSpeed) {
					currentDisplayedHealth = currentHealth;
					isUpToDate = true;
				}
				else {
					currentDisplayedHealth -= delta/smoothingSpeed;
				}
			}
		}
		super.act(delta);
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		// Making sure health bar will be drawn with the right color and alpha:
		Color color = getColor();
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

		// Drawing background:
		background.draw(batch, getX(), getY(), getWidth(), getHeight());
		// Drawing health bar:
		if(currentDisplayedHealth > 0.02f) {
			healthBar.draw(batch, getX()+1, getY()+2,
				(getWidth()-2)*currentDisplayedHealth, healthBarHeight);
		}

		super.draw(batch, parentAlpha);
	}
}
