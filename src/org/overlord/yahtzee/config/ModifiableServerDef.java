package org.overlord.yahtzee.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModifiableServerDef {
	protected String id;
	protected String server;
	protected String botNick = "YatzyBot";
	protected final Map<String, String> passwords = new ConcurrentHashMap<String, String>();

	public ModifiableServerDef() {
		
	}

	public synchronized boolean removeUserPass(String username) {
		return passwords.remove(username) != null;
	}
	
	public synchronized boolean addUserPass(String username, String password) {
		return passwords.put(username, password) != null;
	}
	
	public String getPassword(String username) {
		return passwords.get(username);
	}	
	
	// returns old id
	public synchronized String setId(String id) {
		String old = id;
		this.id = id;
		return old;
	}
	
	public String getId() {
		return id;
	}
	
	public String getServer() {
		return server;
	}
	
	// returns old server
	public synchronized String setServer(String server) {
		String old = this.server;
		this.server = server;
		return old;
	}
	
	public String getBotNick() {
		return botNick;
	}
	
	// returns old botnick
	public String setBotNick(String botNick) {
		String old = this.botNick;
		this.botNick = botNick;
		return old;
	}
	
	public boolean isValid() {
		return id != null && server != null && botNick != null;
	}
}
