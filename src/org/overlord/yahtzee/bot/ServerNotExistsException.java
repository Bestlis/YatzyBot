package org.overlord.yahtzee.bot;

public class ServerNotExistsException extends RuntimeException {
	public ServerNotExistsException() {
	}
	
	public ServerNotExistsException(String message) {
		super(message);
	}
}