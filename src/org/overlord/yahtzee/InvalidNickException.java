// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee;

public class InvalidNickException extends RuntimeException {
	public InvalidNickException() {
	}

	public InvalidNickException(String message) {
		super(message);
	}
}
