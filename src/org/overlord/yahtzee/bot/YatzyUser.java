package org.overlord.yahtzee.bot;

import java.util.ArrayList;

import org.pircbotx.PircBotX;

public class YatzyUser {
	public static class ServerDef {
		protected final String   username;
		protected final String   server;
		protected final String[] channels;
		
		public ServerDef(String username, String server, String[] channels) {
			this.username = username;
			this.server   = server;
			this.channels = channels;
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
	}
	
	protected static final ArrayList<YatzyUser> users = new ArrayList<YatzyUser>();
	protected final ArrayList<YatzyBot> bots = new ArrayList<YatzyBot>();
	
	protected final ServerDef serverDef;
	protected final PircBotX  bot;
	
	public YatzyUser(String username, String server, String channels[]) {
		this(new ServerDef(username, server, channels));
	}
	
	public YatzyUser(ServerDef def) {
		this.serverDef = def;
		this.bot = new PircBotX();
		bot.setName(def.getUsername());
		for (String channel : def.getChannels()) {
			bots.add(new YatzyBot(this, channel));
		}
	}
	
	public YatzyUser(String nick, String server, String channel) {
		this(nick, server, new String[] { channel });
	}
	
	public ServerDef getServerDef() {
		return serverDef;
	}
	
	public void start() {
		for (YatzyBot yb : bots) {
			yb.start();
		}
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
			
			String username = "YatzyBot";
			String server   = null;
			
			int at_index = serverT.indexOf('@');
			
			if (at_index != -1) {
				username = serverT.substring(0, at_index);
				server   = serverT.substring(at_index + 1);
			} else {
				server = serverT;
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
			defs.add(new ServerDef(username, server, chansArr));
		}
		for (ServerDef def : defs) {
			YatzyUser yu = new YatzyUser(def);
			users.add(yu);
		}
		for (YatzyUser yu : users) {
			yu.start();
		}
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
