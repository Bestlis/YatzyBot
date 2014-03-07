package org.overlord.yahtzee.bot;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.exception.NickAlreadyInUseException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;

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
	}
	
	protected static final ArrayList<YatzyUser> users = new ArrayList<YatzyUser>();
	protected final ArrayList<YatzyBot> bots = new ArrayList<YatzyBot>();
	
	protected final ServerDef serverDef;
	protected final PircBotX  bot;
	protected final ListenerAdapter<PircBotX> listener;
	protected volatile static String quitReason = null;
	
	public YatzyUser(String username, String server, String channels[], String[] admins, char adminMode) {
		this(new ServerDef(username, server, channels, admins, adminMode));
	}
	
	public YatzyUser(ServerDef def) {
		this.serverDef = def;
		this.bot = new PircBotX();
		bot.setName(def.getUsername());
		
		bot.getListenerManager().addListener(listener = new ListenerAdapter<PircBotX>() {
			@Override
			public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
				for (YatzyBot yb : bots) {
					yb.start();
				}
			}
		});		
		
		for (String channel : def.getChannels()) {
			Channel chanobj = bot.getChannel(channel);
			bots.add(new YatzyBot(this, chanobj));
		}
	}
	
	public YatzyUser(String nick, String server, String channel, String admin) {
		this(nick, server, new String[] { channel }, new String[] { admin }, 'r');
	}
	
	public ServerDef getServerDef() {
		return serverDef;
	}
	
	public synchronized void dispose(YatzyBot bot) {
		bot.dispose();
		bots.remove(bot);
	}
	
	public synchronized void connect() throws NickAlreadyInUseException, IrcException, IOException {
		System.out.println(this + ": start");
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
				new ServerDef(username, server, chansArr, admin == null ? null : new String[] { admin }, 'r')
			);
		}
		System.out.println(
			"Password is '" + PASSWORD + "'. Use this if you specified no administrators when privately messaging the bot."
		);
		for (ServerDef def : defs) {
			YatzyUser yu = new YatzyUser(def);
			users.add(yu);
		}
		for (YatzyUser yu : users) {
			int tries = 0;
			String initialNick = yu.getBot().getName();
			while (tries < 3 && !yu.getBot().isConnected()) {
				try {
					System.out.println(
						yu + ": attempting connection to " + yu.getServerDef().getServer() +
						": attempt #" + ++tries
					);
					yu.connect();
				} catch (NickAlreadyInUseException e) {
					String newnick = initialNick + tries;
					System.err.println(
						"Couldn't connect to server: " +
						yu.getServerDef().getServer() +
						": nickname in use: " + yu.getBot().getName() +
						" - changing nick to: " + newnick  + " ~ " +
						e.getMessage()
					);
					yu.getBot().setName(newnick);
					continue;
				} catch (IOException e) {
					System.err.println(
						"Couldn't connect to server: " +
						yu.getServerDef().getServer() + " ~ " +
						e.getMessage()
					);
					System.err.println(e.toString());
					return;
				} catch (IrcException e) {
					System.err.println(
						"Couldn't connect to server: " +
						yu.getServerDef().getServer() + " ~ " +
						e.getMessage()
					);
					System.err.println(e.toString());
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
		
		// dispose all apps
		// this might cause problems because of thread-safety, probably not.
		for (YatzyUser yu : users) {
			yu.dispose(quitReason != null ? quitReason : "Execution ended normally - no reason.");
		}
	}
	
	public synchronized void dispose(String reason) {
		System.out.println(this + ": dispose: "+ reason);
		bot.getListenerManager().removeListener(listener);
		for (YatzyBot yb : bots) {
			yb.dispose();
		}
		bot.quitServer(reason);
	}
	
	public static void quit(String reason) {
		if (!running) throw new IllegalStateException("Already quitting!");
		
		System.out.println(YatzyUser.class + ": quit");
		
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
}
