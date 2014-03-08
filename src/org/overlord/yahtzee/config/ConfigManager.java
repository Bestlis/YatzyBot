package org.overlord.yahtzee.config;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.overlord.yahtzee.InvalidNickException;
import org.overlord.yahtzee.bot.YatzyUser;
import org.trendafilov.confucius.Configurable;
import org.trendafilov.confucius.Configuration;

public final class ConfigManager {
	public static final String CONFIG_PATH = "config.ini";
	
	private static final ConfigManager INSTANCE = new ConfigManager();
	protected final Configurable config = Configuration.getInstance();
	protected final Map<String,String> passwords = new ConcurrentHashMap<String,String>();
	
	public static ConfigManager getInstance() {
		return INSTANCE;
	}	
	
	private ConfigManager() {
		
	}
	
	public void clear() {
		passwords.clear();
	}
	
	String[] usernamePasswords2Arr(Map<String, String> userPasswds) {
		final ArrayList<String> arr = new ArrayList<String>();
		for (Entry<String, String> entry : userPasswds.entrySet()) {
			arr.add(entry.getKey() + ":" + entry.getValue());
		}
		return arr.toArray(new String[0]);
	}
	
	public void read() {
		
	}
	
	public void write() {
		
	}
	
	public boolean removeUserPass(String username) {
		return passwords.remove(username) != null;
	}
	
	public boolean addUserPass(String username, String password) {
		if (!YatzyUser.isAlphanumeric(username) || !YatzyUser.isAlphanumeric(password)) {
			throw new InvalidNickException("Either username or password not alphanumerical!");
		}
		return passwords.put(username, password) != null;
	}
	
	public String getPassword(String username) {
		return passwords.get(username);
	}
}
