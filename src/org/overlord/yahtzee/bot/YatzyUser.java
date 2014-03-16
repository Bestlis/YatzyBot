package org.overlord.yahtzee.bot;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.overlord.yahtzee.InvalidNickException;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;
import org.pircbotx.exception.NickAlreadyInUseException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

public class YatzyUser {
	public static final String DEFAULT_NICK = "YatzyBot";
	public static final String PASSWORD = new BigInteger(130, new SecureRandom()).toString(32);
	
	protected static final Map<String, YatzyUser> users = new HashMap<String, YatzyUser>();
	protected static final Map<String, YatzyUser> um_users = Collections.unmodifiableMap(users);
	
	protected final Map<String, YatzyBot> bots = new HashMap<String, YatzyBot>();
	protected final Map<String, YatzyBot> um_bots = Collections.unmodifiableMap(bots);
	
	protected PircBotX bot;
	protected ListenerAdapter<PircBotX> listener;
	protected volatile static String quitReason = null;
	
	protected final Map<String, User> passAuthedUsers = new HashMap<String, User>();
	protected final Map<User, String> passAuthedUsers_user2un = new HashMap<User, String>();
	
	protected User user = null;
	
	protected boolean activated = true;
	protected boolean started   = false;
	protected String id;
	
	protected final String nick;
	protected final String server;
	
	protected final Map<String, String> passwords    = new HashMap<String, String>();
	protected final Map<String, String> um_passwords = Collections.unmodifiableMap(passwords);
	
	protected final static Map<String, String> ADMIN_PASSWORDS    = new HashMap<String, String>();
	protected final static Map<String, String> UM_ADMIN_PASSWORDS = Collections.unmodifiableMap(ADMIN_PASSWORDS);
	
	public YatzyUser(String id, String nick, String server, String[] channels, Map<String, String> passwords, boolean activated) {
		if (server == null) throw new IllegalArgumentException("Must specify a server.");
		this.id        = id;
		this.nick      = nick == null ? YatzyUser.DEFAULT_NICK : nick;
		this.server    = server;
		this.activated = activated;
		
		if (passwords != null) {
			this.passwords.putAll(passwords);
		}
		
		if (channels != null) {
			for (String channel : channels) {
				bots.put(channel, new YatzyBot(this, channel, true));
			}
		}
		
		if (activated) activate();
		else deactivate();
	}
	
	public String getId() {
		return id;
	}
	
	public boolean isActivated() {
		return activated;
	}
	
	public static boolean isRunning() {
		return running;
	}
	
	public YatzyBot addChannel(String channel, boolean activated) {
		if (channel == null || (channel = channel.trim()).isEmpty()) {
			throw new IllegalArgumentException("Channel cannot be null or empty in addChannel(..).");
		}
		YatzyBot yb = new YatzyBot(this, channel, activated);
		bots.put(channel, yb);
		ConfigManager.getInstance().writeWarn();
		return yb;
	}
	
	public String getPassword(String username) {
		return um_passwords.get(username);
	}
	
	public boolean removeUserPass(String username) {
		return passwords.remove(username) != null;
	}
	
	public boolean addUserPass(String username, String password) {
		if (!YatzyUser.isAlphanumeric(username) || !YatzyUser.isAlphanumeric(password)) {
			throw new InvalidNickException("Either username or password not alphanumerical!");
		}
		boolean result = passwords.put(username, password) != null;
		ConfigManager.getInstance().writeWarn();
		return result;
	}
	
	public static boolean removeAdminUserPass(String username) {
		boolean result = ADMIN_PASSWORDS.remove(username) != null;
		ConfigManager.getInstance().writeWarn();
		return result;
	}
	
	public static boolean addAdminUserPass(String username, String password) {
		if (!YatzyUser.isAlphanumeric(username) || !YatzyUser.isAlphanumeric(password)) {
			throw new InvalidNickException("Either username or password not alphanumerical!");
		}
		boolean result = ADMIN_PASSWORDS.put(username, password) != null;
		ConfigManager.getInstance().writeWarn();
		return result;
	}
	
	public static YatzyUser addServer(
		String id, String nick, String server, String[] channels,
		Map<String,String> passwords, boolean activated
	) {
		YatzyUser user = new YatzyUser(id, nick, server, channels, passwords, activated);
		users.put(id, user);
		ConfigManager.getInstance().writeWarn();
		return user;
	}
	
	public String getServer() {
		return server;
	}
	
	public String getNick() {
		return nick;
	}
	
	public static void pmLogAllAdmins(String message, boolean err) {
		for (YatzyUser yu : users.values()) {
			if (!yu.isStarted()) continue;
			if (!yu.getBot().isConnected()) continue;
			for (User u : yu.passAuthedUsers_user2un.keySet()) {
				StringBuilder b = new StringBuilder();
				if (err) {
					b.append("~ ").append(YatzyBot.COLOUR).append("4,1").append(message);
				} else {
					b.append("~ ").append(message);
				}
				yu.getBot().sendMessage(u, b.toString());
			}
		}
	}
	
	public boolean isAuthorised(User auser) {
		return passAuthedUsers_user2un.containsKey(auser);
	}
	
	public synchronized void dispose(YatzyBot bot) {
		bot.stop();
		bots.remove(bot);
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public String getStatusStr() {
		if (!isActivated()) {
			return "DEA";
		} else {
			if (!isStarted()) {
				return "STA";
			} else {
				if (!bot.isConnected()) {
					return "DCN";
				} else {
					return "CNT";
				}
			}
		}
	}
	
	public String getUserStr() {
		return "YatzyUser[" + getStatusStr() + "]~" + id + "~" + getUserNickStr() + "@" + getUserServerStr();
	}
	
	String getUserNickStr() {
		if (bot == null) {
			return "*" + getNick();
		}
		if (!bot.isConnected()) {
			return ">" + bot.getName();
		} else {
			return bot.getNick();
		}
	}
	
	String getUserServerStr() {
		if (bot == null) {
			return "*" + getServer();
		}
		if (!bot.isConnected()) {
			return ">" + getServer();
		} else {
			return bot.getServer();
		}
	}
	
	
	public void connect() {
		_out("start");
		
		// do connection
		String initialNick = getBot().getName();
		try {
			_out("attempting connection to " + getServer());
			bot.connect(server);
		} catch (NickAlreadyInUseException e) {
			_err(
				"Couldn't connect to server: " +
				getServer() +
				": nickname in use: " + getBot().getName()
			);
			YatzyUser.err(e.toString());
		} catch (IOException e) {
			_err(
				"Couldn't connect to server: " +
				getServer() + " ~ " +
				e.getMessage()
			);
			YatzyUser.err(e.toString());
			return;
		} catch (IrcException e) {
			_err(
				"Couldn't connect to server: " +
				getServer() + " ~ " +
				e.getMessage()
			);
			YatzyUser.err(e.toString());
			return;
		}
	}
	
	public synchronized void disconnect() {
		bot.disconnect();
	}
	
	public PircBotX getBot() {
		return bot;
	}
	
	public Map<String, YatzyBot> getBots() {
		return um_bots;
	}
	
	public static Map<String, YatzyUser> getUsers() {
		return users;
	}
	
	public static Map<String, String> getAdminPasswords() {
		return UM_ADMIN_PASSWORDS;
	}
	
	public Map<String, String> getPasswords() {
		return um_passwords;
	}
	
	protected static volatile boolean running = true;
	
	public static void main(String[] args) {
		// check if this is the first-run, query the ini file
		ConfigManager cm = ConfigManager.getInstance();
		boolean read = false;
		try {
			read = cm.read();
		} catch (IOException e) {
			System.err.println("Error while loading config: " + e.toString());
		}
		if (!read) {
			System.out.println("Looking for set-up arguments on command line...");
			if (args.length < 4) {
				System.out.println("Error: number of arguments < 4.");
				showArgText(true);
				return;
			}
			System.out.println("Found correct number of arguments.");
			String id_str = args[0].trim();
			String server_str = args[1].trim();
			String uname_str  = args[2].trim();
			if (!YatzyUser.isAlphanumeric(uname_str)) {
				System.out.println("Error: given username is not alphanumeric.");
			}
			String pword_str  = args[3].trim();
			addAdminUserPass(uname_str, pword_str);
			System.out.println("Added global administrator: " + uname_str);
			
			YatzyUser yu = YatzyUser.addServer(id_str, DEFAULT_NICK, server_str, null, null, true);
			System.out.println("Added initial server: " + id_str + ":" + yu.getNick() + "@" + server_str);
		}
		for (YatzyUser yu : users.values()) {
			yu.start();
		}
		
		// need to join to the individual threads here, until they complete.
		synchronized (YatzyUser.class) {
			try {
				while (running) {
					YatzyUser.class.wait();
					if (users.isEmpty()) {
						// no way to get new input
						quit("Not connected to any servers, no mechanism for bot to get input.");
					}
				}
			} catch (InterruptedException e) {
				quit("Interrupted in main loop.");
			}
		}
		
		out(
			"Qutting: " + (quitReason != null ? quitReason : "Execution ended normally - no reason.")
		);
		
		// dispose all apps
		// this might cause problems because of thread-safety, probably not.
		for (YatzyUser yu : users.values()) {
			yu.dispose(quitReason != null ? quitReason : "Execution ended normally - no reason.");
		}
	}
	
	public synchronized void dispose(String reason) {
		out("dispose: "+ reason);
		bot.getListenerManager().removeListener(listener);
		for (YatzyBot yb : bots.values()) {
			yb.stop();
		}
		bot.quitServer(reason);
	}
	
	public static void quit(String reason) {
		if (!running) throw new IllegalStateException("Already quitting!");
		
		out(YatzyUser.class + ": quit");
		
		quitReason = reason;
		
		// dispose all bots without leaving channels (quit servers with message)
		for (YatzyUser yu : users.values()) {
			yu.dispose(reason);
		}
		
		running = false;
	}
	
	public static void showArgText(boolean initial) {
		if (initial) {
			System.out.println(
				"Initial setup detected. Args required: <server id> <initial server> <admin username> <admin password>"
			);
		}
	}
	
	public void deleteChannel(String channel) {
		bots.remove(channel);
	}
	
	private static final Object lock = new Object();
	
	public static void out(String msg) {
		synchronized (lock) {
			System.out.println(msg);
			pmLogAllAdmins(msg, false);
		}	
	}
	
	public static void err(String msg) {
		synchronized (lock) {
			System.err.println(msg);
			pmLogAllAdmins(msg, true);
		}
	}
	
    public static boolean isAlphanumeric(String str) {
    	if (str == null || str.isEmpty())
    		throw new IllegalArgumentException("Passed null or empty string to isAlphanumeric(..)");
        for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isDigit(c) && !Character.isLetter(c))
                return false;
        }
        return true;
    }
	
    public static boolean isValidNick(String str) {
    	if (str == null || str.isEmpty()) return false;
        if (!Character.isLetter(str.charAt(0))) return false;
        return isAlphanumeric(str);
    }
    
    public void activate() {
    	if (activated) return;
    	if (started) throw new IllegalStateException("Should not be already started when trying to activate!");
    	
		activated = true;
    }
    
    public void deactivate() {
    	if (!activated) return;
    	if (started) throw new IllegalStateException("Cannot deactivate when started.");
    	
    	activated = false;
    }
    
    public void start() {
    	if (started) return;
    	if (!activated) throw new IllegalStateException("Cannot manipulate a deactivated user.");
		
		this.bot = new PircBotX();
		bot.setAutoNickChange(true);
		bot.setName(nick); 	
    	
		listener = new ListenerAdapter<PircBotX>() {
			@Override
			public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
				_out("Connected to " + bot.getServer());
				
				user = getBot().getUserBot();
				for (YatzyBot yb : bots.values()) {
					if (yb.isActivated()) {
						yb.start();
					}
				}
			}
			
			@Override
			public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) throws Exception {
				synchronized (YatzyUser.this) {
					String trimmedMsg = event.getMessage().trim();
					int spc_i = trimmedMsg.indexOf(' ');
					
					final String first  = spc_i == -1 ? trimmedMsg : trimmedMsg.substring(0,spc_i);
					final String follow = spc_i == -1 ? null : trimmedMsg.substring(spc_i + 1).trim();
					
					boolean auth = isAuthorised(event.getUser());
					
					if (first.equals("pass")) {
						if (auth) {
							event.respond("Already authorised, no further action needed.");
							return;
						}
						
						if (follow == null) {
							event.respond("Syntax: pass <username> <password>");
							return;
						}
						String[] split = follow.split(" ");
						if (split.length < 2) {
							event.respond(
								"Syntax: pass <username> <password>. Not enough arguments. "
							);
							return;
						}
						String username = split[0].trim();
						String password = split[1].trim();
						
						if (username == null || password == null) {
							event.respond(
								"Syntax: pass <username> <password>. Not enough arguments. "
							);
							return;
						}
						
						if (!isAuthorised(event.getUser())) {
							// check for another user using this username
							if (passAuthedUsers.get(username) != null) {
								event.respond(
									"Another user is already logged in using this username."
								);
								return;
							}
							
							boolean validated = false;
							boolean adminValidated = false;
							String chkpass = ADMIN_PASSWORDS.get(username);
							if (chkpass == null || password.equals(chkpass)) {
								 chkpass = um_passwords.get(username);
								 if (chkpass == null || password.equals(chkpass)) { 
								 } else {
									 validated = true;
								 }
							} else {
								adminValidated = true;
								validated = true;
							}
							if (validated) {
								passAuthedUsers.put(username, event.getUser());
								passAuthedUsers_user2un.put(event.getUser(), username);
								event.respond(
									"You are now authorised with password and user credentials. " +
									"This authorisation will remain while the bot is connected " +
									"and the bot is able to track any changes in your credentials. " +
									"(Password auth) " +
									(adminValidated ? "(Admin validated)" : "(Local validated)")
								);
								_out(
									"Authentication granted: " + event.getUser().getNick() + " (Password auth) " +
									(adminValidated ? "(Admin validated)" : "(Local validated)"),
									event.getUser()
								);
							} else {
								event.respond(
									"Authorisation failed! Check your password. " +
									"user: " + event.getUser().getNick() +
									" (Password auth)"
								);
								_out(
									"Authentication failed: " + event.getUser() + " (Password auth)",
									event.getUser()
								);
							}
						} else {
							event.respond("You are already identified :)");
						}
					} else if (first.equals("join")) {
						if (!isAuthorised(event.getUser())) {
							event.respond("Not authorised to perform this action. Try logging in first.");
							return;
						}
						String[] split = follow.split(" ");
						if (split.length < 1) {
							event.respond("Syntax: join {server id} <channel>. Specifying channel on its own will make the bot on this server join that channel. (Wrong number of arguments.)");
							return;
						}
						String server_id = null;
						String channel   = null;
						if (split.length >= 2) {
							server_id = split[0].trim();
							channel = split[1].trim();
						} else if (split.length >= 1) {
							channel = split[0].trim();			
						}
						YatzyBot yb = null;
						if (server_id == null) {
							yb = addChannel(channel, true);
						} else {
							// find server
							YatzyUser yu = um_users.get(server_id);
							if (yu == null) {
								event.respond("Couldn't find server for server ID '" + server_id + "'. Please check and try again.");
								return;
							}
							yb = yu.addChannel(channel, true);
							_out(
								"Requested to join channel " + channel + " on " +
								(server_id == null ? id : server_id) + ".", event.getUser()
							);
						}
						yb.start();
					} else if (first.equals("logout")) {
						if (!isAuthorised(event.getUser())) {
							event.respond("You cannot logout if you are not logged in (note: users cannot log out of the nickserv ident list).");
						} else {
							String username = passAuthedUsers_user2un.remove(event.getUser());
							if (username != null) {
								User user = passAuthedUsers.remove(username);
							}
							if (username == null) {
								event.respond("User was not in the password auth list.");
							} else {
								event.respond("Logged out.");
								_out(event.getUser() + " logged out (password auth)", event.getUser());
							}
						}
					}
				}
			}
		};
		
		bot.getListenerManager().addListener(listener);
		
		started = true;
		connect();
    }
    
    public void stop() {
    	if (!running) return;
    	if (!activated) throw new IllegalStateException("Cannot manipulate a deactivated user.");
    	bot.disconnect();
    	listener = null;
    	started = false;
    }
    
    public static void clearAdminPasswords() {
    	ADMIN_PASSWORDS.clear();
    }
    
    public static void addAdminPasswords(Map<String, String> passwords) {
    	ADMIN_PASSWORDS.putAll(passwords);
    }
    
    public void _out(String msg) {
    	_out(msg, null);
    }
    
    public void _err(String msg) {
    	_err(msg, null);
    }
    
    public void _out(String msg, User origin) {
    	System.out.println(getUserStr() + (origin == null ? "" : "[" + origin.getNick() + "]") + ": " + msg);
    	pmLogAllAdmins(getUserStr() + (origin == null ? "" : "[" + origin.getNick() + "]") + ": " + msg, false);
    }
    
    public void _err(String msg, User origin) {
    	System.err.println(getUserStr() + (origin == null ? "" : "[" + origin.getNick() + "]") + ": " + msg);
    	pmLogAllAdmins(getUserStr() + (origin == null ? "" : "[" + origin.getNick() + "]") + ": " + msg, true);
    }
}
