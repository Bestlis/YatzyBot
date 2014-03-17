package org.overlord.yahtzee.bot;

public class ChannelNotExistsException extends RuntimeException {
	public ChannelNotExistsException() {
	}
	
	public ChannelNotExistsException(String message) {
		super(message);
	}
}
