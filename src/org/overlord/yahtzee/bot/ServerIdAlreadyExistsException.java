package org.overlord.yahtzee.bot;

public class ServerIdAlreadyExistsException extends RuntimeException {
	public ServerIdAlreadyExistsException() {
	}
	
	public ServerIdAlreadyExistsException(String message) {
		super(message);
	}
}