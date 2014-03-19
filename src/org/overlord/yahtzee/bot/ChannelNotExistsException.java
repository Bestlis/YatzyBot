// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee.bot;

public class ChannelNotExistsException extends RuntimeException {
	public ChannelNotExistsException() {
	}
	
	public ChannelNotExistsException(String message) {
		super(message);
	}
}
