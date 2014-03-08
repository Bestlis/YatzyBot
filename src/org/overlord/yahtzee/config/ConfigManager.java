package org.overlord.yahtzee.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;
import org.overlord.yahtzee.InvalidNickException;
import org.overlord.yahtzee.bot.YatzyUser;

public final class ConfigManager {
	public static final String CONFIG_PATH = "config.ini";
	
	private static final ConfigManager INSTANCE = new ConfigManager();
	
	protected final Map<String,ModifiableServerDef> servers = new ConcurrentHashMap<String,ModifiableServerDef>();
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
	
	public void read() throws IOException {
		YatzyUser.out(
			"Reading config from disk. This will erase in-memory values."
		);
		final Ini ini = new Ini();
		try {
			ini.load(new File(CONFIG_PATH));
		} catch (InvalidFileFormatException e) {
			throw new IllegalStateException(e);
		}
		Section s_general = ini.get("General");
		if (s_general == null) {
			s_general = ini.add("General");
		}
		Section s_servers = ini.get("General");
		if (s_servers == null) {
			s_servers = ini.add("Servers");
		}
		YatzyUser.out(
			"Read config from disk. Previous in-memory values erased and state " +
			"synched."
		);
	}
	
	public void write() throws IOException {
		YatzyUser.out(
			"Writing config to disk. This will erase any previous stored config."
		);
		final Ini ini = new Ini();
		Section s_general = ini.add("General");
		Section s_servers = ini.add("Servers");
		for (Entry<String,ModifiableServerDef> server : servers.entrySet()) {
			Section s_serverdef = s_servers.addChild(server.getKey());
		}
		try {
			ini.store(new File(CONFIG_PATH));
		} catch (InvalidFileFormatException e) {
			throw new IllegalStateException(e);
		}
		YatzyUser.out("Wrote config to disk.");
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
	
	public String convertUP2Str(Map<String, String> up) {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : up.entrySet()) {
			if (!first) sb.append(',');
			String e_str = entry.getKey().trim() + ":" + entry.getValue().trim();
			sb.append(e_str);
			first = false;
		}
		return sb.toString();
	}
	
	public Map<String, String> convertStr2UP(String str) {
		Map<String, String> ups = new HashMap<String, String>();
		String[] ups_arr = str.split(",");
		for (String up_e : ups_arr) {
			String[] up_arr = up_e.split(":");
			if (up_arr.length == 2) {
				String username = up_arr[0].trim();
				String password = up_arr[1].trim();		
				if (username.isEmpty() || password.isEmpty()) {
					throw new IllegalStateException(
						"Bad username & password in General section (username: " +
						username + ", password: " + password + ")."
					);
				}
				ups.put(username, password);
			}
		}
		return ups;
	}
}
