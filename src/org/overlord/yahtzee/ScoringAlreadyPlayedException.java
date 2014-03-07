// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee;

public class ScoringAlreadyPlayedException extends TurnException {
	public ScoringAlreadyPlayedException(String message) {
		super(message);
	}
}
