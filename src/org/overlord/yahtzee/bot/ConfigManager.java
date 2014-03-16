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

import sun.security.pkcs.ParsingException;

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
		if (s_general != null) {
			String admin_passwords = s_general.get("Admin_Passwords");
			Map<String, String> ups = convertStr2UP(admin_passwords);
			YatzyUser.clearAdminPasswords();
			YatzyUser.addAdminPasswords(ups);
		}
		Section s_servers = ini.get("Servers");
		if (s_servers != null) {
			String[] server_sections = s_servers.childrenNames();
			for (String serverId : server_sections) {
				Section ss = s_servers.getChild(serverId);
				if (ss == null) throw new IllegalStateException(
					"Couldn't get server section even though it was listed as a child!"
				);
				String serverHost        = ss.get("Server");
				String serverNick        = ss.get("Nickname");
				String serverPasswords_s = ss.get("Passwords");
				String serverChannels_s  = ss.get("Channels");
				String serverActive_s    = ss.get("Activated");
				
				if (serverHost == null) {
					throw new ParsingException("Server host not found in server section!");
				}
				boolean serverActive = true;
				if (serverActive_s != null && serverActive_s.trim().equals("false")) {
					serverActive = false;
				}
				
				Map<String,String> serverPasswords = convertStr2UP(serverPasswords_s);
				
				String[] serverChannels = null;
				if (serverChannels_s != null && !(serverPasswords_s = serverPasswords_s.trim()).isEmpty()) {
					serverChannels = serverChannels_s.split(",");
					for (int i = 0; i < serverChannels.length; i++) {
						serverChannels[i] = serverChannels[i].trim();
					}
				}
				YatzyUser.addServer(
					serverId, serverNick, serverHost, serverChannels,
					serverPasswords, serverActive
				);
			}
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
	
	public void writeWarn() {
		try {
			write();
		} catch (IOException e) {
			YatzyUser.pmLogAllAdmins("Couldn't write out config file: " + e.toString(), true);
		}
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
			s_server.add("Nickname", yu.getNick());
			String pwordStr = convertUP2Str(yu.getPasswords());
			if (pwordStr != null && !pwordStr.isEmpty()) {
				s_server.add("Passwords", pwordStr);
			}
			if (!yu.getBots().isEmpty()) {
				StringBuilder sb = new StringBuilder();
				boolean first = true;
				for (YatzyBot bot : yu.getBots().values()) {
					if (!first) sb.append(',');
					sb.append(bot.getChannel());
					first = false;
				}
				s_server.add("Channels", sb.toString());
			}
			if (!yu.isActivated()) {
				s_server.add("Activated", "false");
			}
		}
		try {
			ini.store(new File(CONFIG_PATH));
		} catch (InvalidFileFormatException e) {
			throw new IllegalStateException(e);
		}
		YatzyUser.out("Wrote config to disk.");
	}
	
	public String convertUP2Str(Map<String, String> up) {
		if (up == null) return null;
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
		if (str == null || (str=str.trim()).isEmpty()) return null;
		Map<String, String> ups = new HashMap<String, String>();
		String[] ups_arr = str.split(",");
		for (String up_e : ups_arr) {
			String[] up_arr = up_e.split(":");
			if (up_arr.length == 2) {
				String username = up_arr[0].trim();
				String password = up_arr[1].trim();		
				if (username.isEmpty() || password.isEmpty()) {
					throw new IllegalArgumentException(
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
