package mj.konfigurats.logic.entities;

import mj.konfigurats.logic.Game;

public interface Entity {
	public void update(float delta,Game game);
	public int getEntityIndex();
	public void destroy(Game game);
}
