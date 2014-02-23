package org.overlord.yahtzee;

public interface YatzyListener {
	public void onStart(Yahtzee y);
	public void onTurnComplete(Turn t);
	public void onTurn(Turn t);
	public void onAddPlayer(Player p);
	public void onReset(Yahtzee y);
	public void onGameComplete(Yahtzee y);
	public void onRemovePlayer(Player p);
}
