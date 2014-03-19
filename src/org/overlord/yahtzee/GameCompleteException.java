// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee;

public class GameCompleteException extends YatzyException {
	public GameCompleteException() {
	}
	
	public GameCompleteException(String message) {
		super(message);
	}
}
