// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee;

public class DieNumberNotFoundException extends TurnException {
	public final int numToRoll;
	
	public DieNumberNotFoundException(int dieNum) {
		this.numToRoll = dieNum;
	}
	
	public DieNumberNotFoundException(String message, int dieNum) {
		super(message);
		this.numToRoll = dieNum;
	}
	
	public int getDieNum() {
		return numToRoll;
	}
}
