// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

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
	public static final boolean VERBOSE = false;
	
	public static final String CONFIG_PATH = "config.ini";
	
	private static final ConfigManager INSTANCE = new ConfigManager();
	
	protected boolean attemptedRead = false;
	protected boolean firstRun = false;
	
	public static final ConfigManager getInstance() {
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
	
	public boolean fileExists() {
		Path inpath = Paths.get(CONFIG_PATH);
		return inpath.toFile().exists();
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
			if (admin_passwords == null || (admin_passwords = admin_passwords.trim()).isEmpty()) {
				throw new IllegalStateException("No admin passwords found in config file. There should be at least one.");
			}
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
				String perform_s         = ss.get("Perform");
				
				if (serverHost == null) {
					throw new ParsingException("Server host not found in server section!");
				}
				boolean serverActive = true;
				if (serverActive_s != null) {
					if (serverActive_s.trim().equals("false")) {
						serverActive = false;
					}
				}
				ArrayList<String> channels = new ArrayList<String>();
				
				Map<String,String> serverPasswords = convertStr2UP(serverPasswords_s);
				Section s_channels = ss.getChild("Channels");
				if (s_channels != null) {
					for (String channelsect : s_channels.childrenNames()) {
						Section s_channel = s_channels.getChild(channelsect);
						channels.add(channelsect);
					}
				}
				String[] serverChannels = channels.toArray(new String[0]);
				YatzyUser yu = YatzyUser.addServer(
					serverId, serverNick, serverHost, serverChannels,
					serverPasswords, serverActive, perform_s
				);
				// now we need to add specific channel stuff
				if (s_channels != null) {
					for (String channelsect : s_channels.childrenNames()) {
						Section s_channel = s_channels.getChild(channelsect);
						YatzyBot yb = yu.getBots().get(channelsect);
						if (yb == null) {
							throw new IllegalStateException(
								"Couldn't find channel with name: '" +  channelsect + " after adding!"
							);
						}
						String prefixCh = s_channel.get("Prefix");
						if (prefixCh != null) {
							prefixCh = prefixCh.trim();
							if (!prefixCh.isEmpty()) {
								char prf = prefixCh.charAt(0);
								if (yb.getPrefix() != prf) {
									yb.setPrefix(prf);
								}
							}
						}
					}
				}
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
			YatzyUser.err("Couldn't write out config file: " + e.toString());
		}
	}
	
	public void write() throws IOException {
		if (VERBOSE) {
			YatzyUser.out(
				"Writing config to disk. This will erase any previous stored config."
			);
		}
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
				Section s_channels = s_server.addChild("Channels");
				for (YatzyBot yb : yu.getBots().values()) {
					String channel = yb.getChannel();
					Section s_channel = s_channels.addChild(channel);
					s_channel.add("Name", channel);
					if (yb.getPrefix() != YatzyBot.DEFAULT_PREFIX) {
						s_channel.add("Prefix", "" + yb.getPrefix());
					}
				}
			}
			if (!yu.isActivated()) {
				s_server.add("Activated", "false");
			}
			String perform = yu.getPerform();
			if (perform == null || perform.isEmpty()) {
				s_server.add("Perform", perform);
			}
		}
		try {
			ini.store(new File(CONFIG_PATH));
		} catch (InvalidFileFormatException e) {
			throw new IllegalStateException(e);
		}
		if (VERBOSE) {
			YatzyUser.out("Wrote config to disk.");
		}
	}
	
	public String convertUP2Str(Map<String, String> up) {
		if (up == null) return null;
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : up.entrySet()) {
			if (!first) sb.append(',');
			sb.append(entry.getKey().trim()).append(':').append(entry.getValue().trim());
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
						"Bad username & password in General section (username: '" +
						username + "', password: '" + password + "')."
					);
				}
				ups.put(username, password);
			}
		}
		return ups;
	}
}
