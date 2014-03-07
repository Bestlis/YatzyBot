// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class Player {
	protected final String name;
	protected int totalScore = 0;	
	final Map<Scoring, Integer> totals = new EnumMap<Scoring, Integer>(Scoring.class);
	{
		for (int i = 0; i < Scoring.values().length; i++) {
			totals.put(Scoring.values()[i], -1);
		}
	}
	final Map<Scoring, Integer> um_totals = Collections.unmodifiableMap(totals);
	
	public Player(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public Map<Scoring, Integer> getTotals() {
		return um_totals;
	}

	public void setScore(Scoring scoring, int value) throws YatzyException {
		if (totals.get(scoring) > -1) {
			throw new ScoringAlreadyPlayedException("Player has already scored for " + scoring + " with " + totals.get(scoring));
		}
		totals.put(scoring, value);
		totalScore = 0;
		for (int score : totals.values()) {
			if (score > 0) {
				totalScore += score;
			}
		}
	}
	
	public int getScore(Scoring scoring) {
		return totals.get(scoring);
	}
	
	public int getTotalScore() {
		return totalScore;
	}
}
