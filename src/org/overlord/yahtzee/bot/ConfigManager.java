package org.overlord.yahtzee.bot;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;

public final class ConfigManager {
	public static final String CONFIG_PATH = "config.ini";
	
	private static final ConfigManager INSTANCE = new ConfigManager();
	
	protected boolean attemptedRead = false;
	protected boolean firstRun = false;
	
	public static ConfigManager getInstance() {
		return INSTANCE;
	}	
	
	private ConfigManager() {
	}
	
	String[] usernamePasswords2Arr(Map<String, String> userPasswds) {
		final ArrayList<String> arr = new ArrayList<String>();
		for (Entry<String, String> entry : userPasswds.entrySet()) {
			arr.add(entry.getKey() + ":" + entry.getValue());
		}
		return arr.toArray(new String[0]);
	}
	
	static String readFile(Path path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(path);
		return encoding.decode(ByteBuffer.wrap(encoded)).toString().trim();
	}
	
	public boolean read() throws IOException {
		attemptedRead = true;
		YatzyUser.out(
			"Reading config from disk. This will erase in-memory values."
		);
		Path inpath = Paths.get(CONFIG_PATH);
		File infile = inpath.toFile();
		if (!infile.exists() || readFile(inpath, StandardCharsets.UTF_8).isEmpty()) {
			YatzyUser.out(
				"Config file '" + infile.getAbsolutePath() + "' does not " +
				"exist or is empty. Treating as a first-run."
			);
			firstRun = true;
			return false;
		}
		final Ini ini = new Ini();
		try {
			ini.load(infile);
			firstRun = false;
		} catch (InvalidFileFormatException e) {
			throw new IllegalStateException(e);
		}
		Section s_general = ini.get("General");
		if (s_general == null) {
			s_general = ini.add("General");
		}
		Section s_servers = ini.get("Servers");
		if (s_servers == null) {
			s_servers = ini.add("Servers");
		}
		YatzyUser.out(
			"Read config from disk. Previous in-memory values erased and state " +
			"synched."
		);
		return true;
	}
	
	public boolean isFirstRun() {
		return firstRun;
	}
	
	public boolean isAttemptedRead() {
		return attemptedRead;
	}
	
	public void write() throws IOException {
		YatzyUser.out(
			"Writing config to disk. This will erase any previous stored config."
		);
		final Ini ini = new Ini();
		Section s_general = ini.add("General");
		String upstr = convertUP2Str(YatzyUser.getAdminPasswords());
		s_general.add("Admin_Passwords", upstr);
		
		Section s_servers = ini.add("Servers");
		for (Entry<String,YatzyUser> yue : YatzyUser.getUsers().entrySet()) {
			Section s_server = s_servers.addChild(yue.getKey());
			YatzyUser yu = yue.getValue();
			s_server.add("Server", yu.getServer());
			s_server.add("Nick", yu.getNick());
			String upstr2 = convertUP2Str(yu.getPasswords());
			s_server.add("Passwords", upstr2);
		}
		try {
			ini.store(new File(CONFIG_PATH));
		} catch (InvalidFileFormatException e) {
			throw new IllegalStateException(e);
		}
		YatzyUser.out("Wrote config to disk.");
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
