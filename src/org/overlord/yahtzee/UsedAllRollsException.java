// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee;

public class UsedAllRollsException extends TurnException {
	public UsedAllRollsException(String message) {
		super(message);
	}
}
