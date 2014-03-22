package org.overlord.yahtzee.bot;

import org.overlord.yahtzee.IUserIdentifier;
import org.pircbotx.User;

public class BotUserIdentifier implements IUserIdentifier {
	protected User user;

	public BotUserIdentifier(User user) {
		if (user == null) throw new IllegalArgumentException(
			"Cannot set null user."
		);
		this.user = user;
	}

	@Override
	public String getNick() {
		return user.getNick();
	}
	
	public void setUser(User user) {
		if (user == null) throw new IllegalArgumentException(
			"Cannot set null user."
		);
		this.user = user;
	}
	
	public User getUser() {
		return user;
	}
}
