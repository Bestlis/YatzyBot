package org.overlord.yahtzee.bot;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;
import org.pircbotx.exception.NickAlreadyInUseException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

public class YatzyUser {
	public static final String PASSWORD = new BigInteger(130, new SecureRandom()).toString(32);
	
	public static class ServerDef {
		protected final String   username;
		protected final String   server;
		protected final String[] channels;
		protected final String[] admins;
		protected final char     adminMode;
		
		public ServerDef(String username, String server, String[] channels, String[] admins, char adminMode) {
			this.username  = username;
			this.server    = server;
			this.channels  = channels;
			this.admins    = admins;
			this.adminMode = adminMode;
		}
		
		public String getUsername() {
			return username;
		}
		
		public String[] getChannels() {
			return channels;
		}
		
		public String getServer() {
			return server;
		}
		
		public String[] getAdmins() {
			return admins;
		}
		
		public char getAdminMode() {
			return adminMode;
		}
		
		@Override
		public String toString() {
			return "ServerDef["+username+"@"+server+"~" + Arrays.toString(admins) + " "+ Arrays.toString(channels) +"]";
		}
	}
	
	protected static final ArrayList<YatzyUser> users = new ArrayList<YatzyUser>();
	protected final ArrayList<YatzyBot> bots = new ArrayList<YatzyBot>();
	
	protected final ServerDef serverDef;
	protected final PircBotX  bot;
	protected final ListenerAdapter<PircBotX> listener;
	protected volatile static String quitReason = null;
	protected final CopyOnWriteArrayList<User> passAuthUsers = new CopyOnWriteArrayList<User>();
	protected User user = null;
	protected static volatile int _ID = 0;
	protected final int id = _ID++;
	
	public YatzyUser(String username, String server, String channels[], String[] admins, char adminMode) {
		this(new ServerDef(username, server, channels, admins, adminMode));
	}
	
	public YatzyUser(ServerDef def) {
		this.serverDef = def;
		this.bot = new PircBotX();
		
		bot.setAutoNickChange(true);
		bot.setName(def.getUsername());
		
		bot.getListenerManager().addListener(listener = new ListenerAdapter<PircBotX>() {
			@Override
			public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
				user = getBot().getUserBot();
				for (YatzyBot yb : bots) {
					yb.start();
				}
			}
			
			@Override
			public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) throws Exception {
				synchronized (YatzyUser.this) {
					String trimmedMsg = event.getMessage().trim();
					int spc_i = trimmedMsg.indexOf(' ');
					
					final String first  = spc_i == -1 ? trimmedMsg : trimmedMsg.substring(0,spc_i);
					final String follow = spc_i == -1 ? null : trimmedMsg.substring(spc_i + 1).trim();
					
					if (first.equals("pass")) {
						if (follow == null) {
							boolean auth = isAuthorised(event.getUser());
							event.respond(
								"Syntax: pass <password>. Password is given in the " +
								"console when the bot starts up. " +
								(auth ?
									"Already authorised, no further action needed." :
									"User is not authorised at present."
								)
							);
							return;
						}
						if (!isAuthorised(event.getUser())) {
							if (follow.equals(YatzyUser.PASSWORD)) {
								passAuthUsers.add(event.getUser());
								event.respond(
									"You are now authorised with password and user credentials. " +
									"This authorisation will remain while the bot is connected " +
									"and the bot is able to track any changes in your credentials."
								);
								out(
									"Authentication granted: " + event.getUser().getNick() + "@" +
									event.getUser().getHostmask() + "@" + bot.getServer() +
									"~" + bot.getName() + " (password auth)"
								);
							} else {
								event.respond(
									"Authorisation failed! Check your password. " +
									"user: " + event.getUser().getNick() +
									" (password auth)"
								);
								out(
									"Authentication failed: " + event.getUser() + "@" +
									event.getUser().getHostmask() + "@" + bot.getServer() +
									"~" + bot.getName() + " (password auth)"
								);
							}
						} else {
							event.respond("You are already identified :)");
						}
					} else if (first.equals("join")) {
						if (!isAuthorised(event.getUser())) {
							
						}
					} else if (first.equals("logout")) {
						if (!isAuthorised(event.getUser())) {
							event.respond("You cannot logout if you are not logged in (note: users cannot log out of the nickserv ident list).");
						} else {
							boolean contained = passAuthUsers.remove(event.getUser());
							if (!contained) {
								event.respond("User was not in the password auth list (note: users cannot log out of the nickserv ident list).");
							} else {
								event.respond("Logged out.");
								out(event.getUser() + " logged out (password auth)");
							}
						}
					}
				}
			}
		});		
		
		for (String channel : def.getChannels()) {
			Channel chanobj = bot.getChannel(channel);
			bots.add(new YatzyBot(this, chanobj));
		}
	}
	
	public static void pmLogAllAdmins(String message, boolean err) {
		for (YatzyUser yu : users) {
			if (!yu.getBot().isConnected()) continue;
			for (User u : yu.passAuthUsers) {
				StringBuilder b = new StringBuilder();
				if (err) {
					b.append("~ ").append(YatzyBot.COLOUR).append("4,1").append(message);
				} else {
					b.append("~ ").append(message);
				}
				yu.getBot().sendMessage(u, b.toString());
			}
			for (String u_str : yu.getServerDef().getAdmins()) {
				User u = yu.getBot().getUser(u_str);
				if (!u.isVerified()) continue;
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
	
	public YatzyUser(String nick, String server, String channel, String admin)	{
		this(nick, server, new String[] { channel }, new String[] { admin }, 'r');
	}
	
	public boolean isAuthorised(User auser) {
		if (passAuthUsers.contains(auser)) return true;
		String[] admins = serverDef.getAdmins();
		for (String admin : admins) {
			if (admin.equals(auser.getNick())) {
				if (auser.isVerified()) return true;
			}
		}
		return false;
	}
	
	public ServerDef getServerDef() {
		return serverDef;
	}
	
	public synchronized void dispose(YatzyBot bot) {
		bot.dispose();
		bots.remove(bot);
	}
	
	public synchronized void connect() throws NickAlreadyInUseException, IrcException, IOException {
		YatzyUser.out(this + ": start");
		bot.connect(serverDef.getServer());
	}
	
	public synchronized void disconnect() {
		bot.disconnect();
	}
	
	public PircBotX getBot() {
		return bot;
	}
	
	public ArrayList<YatzyBot> getBots() {
		return bots;
	}
	
	public static ArrayList<YatzyUser> getUsers() {
		return users;
	}
	
	protected static volatile boolean running = true;
	
	public static void main(String[] args) {
		if (args.length < 2 || args.length % 2 != 0) {
			showArgText();
			return;
		}
		ArrayList<ServerDef> defs = new ArrayList<ServerDef>();
		for (int i = 0; i < (args.length / 2); i++) {
			String   serverT  = args[i * 2];
			String   chans    = args[(i * 2) + 1];
			String[] chansArr = null;
			
			String username  = "YatzyBot";
			String server    = null;
			String admin     = null;
			char   adminMode = 'r';
			
			int at_index    = serverT.indexOf('@');
			
			if (at_index != -1) {
				username = serverT.substring(0, at_index);
				String serverOrig = serverT.substring(at_index + 1);
				int tilde_index = serverOrig.indexOf('~');
				if (tilde_index != -1) {
					server = serverOrig.substring(0, tilde_index);
					admin = serverOrig.substring(tilde_index + 1);
				} else {
					server = serverOrig;
				}
			} else {
				int tilde_index = serverT.indexOf('~');
				if (tilde_index != -1) {
					server = serverT.substring(0, tilde_index);
					admin = serverT.substring(tilde_index + 1);
				} else {
					server = serverT;
				}
			}
			
			if (!chans.equals("none")) {
				chansArr = chans.split(",");
				for (String chan : chansArr) {
					if (!chan.startsWith("#")) {
						throw new IllegalArgumentException(
							"Non-channel specified as channel: " + chan + "."
						);
					}
				}
			}
			defs.add(
				new ServerDef(
					username, server, chansArr,
					admin == null ? null : new String[] { admin },
					adminMode
				)
			);
		}
		System.out.println("Defs: " + defs.toString());
		System.out.println(
			"Password is '" + PASSWORD + "'. " +
			"Use this if you specified no administrators when " +
			"privately messaging the bot."
		);
		for (ServerDef def : defs) {
			YatzyUser yu = new YatzyUser(def);
			users.add(yu);
		}
		for (YatzyUser yu : users) {
			String initialNick = yu.getBot().getName();
			while (!yu.getBot().isConnected()) {
				try {
					YatzyUser.out(
						yu + ": attempting connection to " + yu.getServerDef().getServer()
					);
					yu.connect();
				} catch (NickAlreadyInUseException e) {
					YatzyUser.err(
						"Couldn't connect to server: " +
						yu.getServerDef().getServer() +
						": nickname in use: " + yu.getBot().getName()
					);
					continue;
				} catch (IOException e) {
					YatzyUser.err(
						"Couldn't connect to server: " +
						yu.getServerDef().getServer() + " ~ " +
						e.getMessage()
					);
					YatzyUser.err(e.toString());
					return;
				} catch (IrcException e) {
					YatzyUser.err(
						"Couldn't connect to server: " +
						yu.getServerDef().getServer() + " ~ " +
						e.getMessage()
					);
					YatzyUser.err(e.toString());
					return;
				}
			}
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
		
		YatzyUser.out(
			"Qutting: " + (quitReason != null ? quitReason : "Execution ended normally - no reason.")
		);
		
		// dispose all apps
		// this might cause problems because of thread-safety, probably not.
		for (YatzyUser yu : users) {
			yu.dispose(quitReason != null ? quitReason : "Execution ended normally - no reason.");
		}
	}
	
	public synchronized void dispose(String reason) {
		YatzyUser.out(this + ": dispose: "+ reason);
		bot.getListenerManager().removeListener(listener);
		for (YatzyBot yb : bots) {
			yb.dispose();
		}
		bot.quitServer(reason);
	}
	
	public static void quit(String reason) {
		if (!running) throw new IllegalStateException("Already quitting!");
		
		out(YatzyUser.class + ": quit");
		
		quitReason = reason;
		
		// dispose all bots without leaving channels (quit servers with message)
		for (YatzyUser yu : users) {
			yu.dispose(reason);
		}
		
		running = false;
	}
	
	public static void showArgText() {
		System.out.println(
			"Error: args required: one or more of {<server/nick@server> " +
			"<channels (comma delimitered list, 'none' for no channels)>}. " +
			"For example: 'irc.yourserver.net #yatzy,#chatland pimppi@irc.myserver.fi " +
			"#hissi,#kissa'. If unspecified, nick will be 'YatzyBot' by default. There " +
			"must be no spaces between channels and comma delimiters."
		);
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
}
