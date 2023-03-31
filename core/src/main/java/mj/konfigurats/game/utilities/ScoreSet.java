package mj.konfigurats.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import mj.konfigurats.Core;

/**
 * Used by the game chat to show how many kills and deaths
 * game room players have.
 * @author MJ
 */
public class ScoreSet {
	private Label label;
	private int teamIndex;
	private int kills,deaths;

	public ScoreSet(int teamIndex) {
		this.teamIndex = teamIndex;
		kills = deaths = 0;
	}

	public ScoreSet(int kills,int deaths) {
		teamIndex = -1;
		this.kills = kills;
		this.deaths = deaths;
	}

	{
		label = new Label(kills+"/"+deaths,((Core)Gdx.app.getApplicationListener())
			.getInterfaceManager().getSkin(),"dark");
	}

	/**
	 * @return amount of kills linked with a player.
	 */
	public int getKills() {
		return kills;
	}

	/**
	 * @return player's team index.
	 */
	public int getTeamIndex() {
		return teamIndex;
	}

	/**
	 * Sets the current team index. Used when the score packet comes before
	 * player creation packet.
	 * @param teamIndex current player's team.
	 */
	public void setTeamIndex(int teamIndex) {
		this.teamIndex = teamIndex;
	}

	/**
	 * Sets the current amount of player's kills.
	 * @param kills amount of kills.
	 */
	public void setKills(int kills) {
		this.kills = kills;
		updateLabel();
	}

	/**
	 * @return amount of deaths linked with a player.
	 */
	public int getDeaths() {
		return deaths;
	}

	/**
	 * Sets the current amount of player's deaths.
	 * @param deaths amount of deaths.
	 */
	public void setDeaths(int deaths) {
		this.deaths = deaths;
		updateLabel();
	}

	/**
	 * @return scores' label.
	 */
	public Label getLabel() {
		return label;
	}

	/**
	 * Sets the current player's score.
	 * @param kills amount of kills.
	 * @param deaths amount of deaths.
	 */
	public void setScores(int kills,int deaths) {
		this.kills = kills;
		this.deaths = deaths;
		updateLabel();
	}

	/**
	 * Updates text on the label.
	 */
	private void updateLabel() {
		label.setText(kills+"/"+deaths);
	}
}
