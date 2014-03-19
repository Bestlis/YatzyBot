// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee.bot;

public class ServerIdAlreadyExistsException extends RuntimeException {
	public ServerIdAlreadyExistsException() {
	}
	
	public ServerIdAlreadyExistsException(String message) {
		super(message);
	}
}