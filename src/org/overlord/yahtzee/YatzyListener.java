// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee;

import org.overlord.yahtzee.Yahtzee.TurnEndReason;

public interface YatzyListener {
	public void onStart(Yahtzee y);
	public void onTurnComplete(Turn t, TurnEndReason reason);
	public void onTurn(Turn t);
	public void onAddPlayer(Player p);
	public void onGameComplete(Yahtzee y);
	public void onRemovePlayer(Player p);
}
