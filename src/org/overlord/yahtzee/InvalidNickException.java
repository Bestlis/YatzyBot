package org.overlord.yahtzee;

public class InvalidNickException extends RuntimeException {
	public InvalidNickException() {
	}

	public InvalidNickException(String message) {
		super(message);
	}
}
