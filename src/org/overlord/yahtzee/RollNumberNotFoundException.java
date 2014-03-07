// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee;

public class RollNumberNotFoundException extends TurnException {
	public final int numToRoll;
	
	public RollNumberNotFoundException(String message, int numToRoll) {
		super(message);
		this.numToRoll = numToRoll;
	}
	
	public int getNumToRoll() {
		return numToRoll;
	}
}
