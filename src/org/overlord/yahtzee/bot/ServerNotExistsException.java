// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee.bot;

public class ServerNotExistsException extends RuntimeException {
	public ServerNotExistsException() {
	}
	
	public ServerNotExistsException(String message) {
		super(message);
	}
}