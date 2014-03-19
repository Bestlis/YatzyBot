package org.overlord.yahtzee;

public class TurnNotCurrentException extends IllegalStateException {
	public TurnNotCurrentException() {		
	}

	public TurnNotCurrentException(String msg) {
		super(msg);
	}

}
