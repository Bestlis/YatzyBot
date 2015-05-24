// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee;

import java.util.Arrays;
import java.util.Map;

import org.overlord.yahtzee.Yahtzee.TurnEndReason;

public class Turn {
	protected Yahtzee y;
	protected Player player;
	protected int[] diceVals;
	protected Map<Scoring, Integer> scores;
	protected int rolls = 0;
	protected boolean done = false;

	public Turn(Yahtzee y, Player p) {
		this.y = y;
		this.player = p;
	}
	
	public Yahtzee getYahtzy() {
		return y;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public int[] getDiceVals() {
		return diceVals;
	}
	
	public void rollAll() throws UsedAllRollsException, TurnNotCurrentException {
		checkTurn();
		if (rolls == 3) throw new UsedAllRollsException("Used all 3 rolls.");
		y.rollAll();
		diceVals = Yahtzee.dice2Vals(y.getDice(), null);
		Arrays.sort(diceVals);
		scores = y.getRollScores();
		rolls++;
	}
	
	public boolean[] rollNumbers(int[] which) throws ReqRollException, UsedAllRollsException, DieNumberNotFoundException, GameCompleteException, TurnNotCurrentException {
		checkTurn();
		if (rolls == 0) throw new ReqRollException("Must roll at least once to reroll certain numbers");
		if (rolls == 3) throw new UsedAllRollsException("Used all 3 rolls.");
		boolean[] rolled = y.rollNumbers(which);
		diceVals = Yahtzee.dice2Vals(y.getDice(), null);
		Arrays.sort(diceVals);
		scores = y.getRollScores();
		rolls++;
		return rolled;
	}
	
	private void checkTurn() throws TurnNotCurrentException {
		if (y.getTurn() != this) throw new TurnNotCurrentException();
	}
	
	public int getRolls() {
		return rolls;
	}
	
	public void choose(Scoring scoring) throws ReqRollException, ScoringAlreadyPlayedException, TurnNotCurrentException {
		checkTurn();
		if (rolls == 0) throw new ReqRollException("Must roll at least once!");
		player.setScore(scoring, scores.get(scoring));
		done = true;
		y.turnDone(this, TurnEndReason.SCORING_CHOSEN);
	}
}
