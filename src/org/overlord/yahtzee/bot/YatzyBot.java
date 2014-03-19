// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee.bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.overlord.yahtzee.Player;
import org.overlord.yahtzee.RollNumberNotFoundException;
import org.overlord.yahtzee.Scoring;
import org.overlord.yahtzee.Turn;
import org.overlord.yahtzee.TurnException;
import org.overlord.yahtzee.Yahtzee;
import org.overlord.yahtzee.YatzyException;
import org.overlord.yahtzee.YatzyListener;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PartEvent;

public class YatzyBot {
	protected static final String VERSION = "0.86";
	
	protected final YatzyUser user;
	protected final String server;
	protected final String channel;
	protected Channel channelObj = null;
	
	protected boolean activated = true;
	protected boolean started   = false;
	
	protected Yahtzee y = new Yahtzee();
	protected ListenerAdapter<PircBotX> listener;

	public static int[] convertIntegers(final List<Integer> integers)
	{
		int[] ret = new int[integers.size()];
		Iterator<Integer> iterator = integers.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = iterator.next();
		}
		return ret;
	}

	public YatzyBot(final YatzyUser user, final String channel, boolean activated) {
		this.user       = user;
		this.server     = user.getServer();
		this.channel    = channel;
		this.activated  = activated;
		
		if (activated) activate();
		else deactivate();
	}
	
	public String getChannel() {
		return channel;
	}
	
	public Channel getChannelObj() {
		return channelObj;
	}
	
	public void start() {
		if (started) return;
		if (!activated) throw new IllegalStateException("Cannot manipulate a deactivated user.");
		
		this.channelObj = getBot().getChannel(channel);
		
		y = new Yahtzee();
		y.addListener(new YatzyListener() {
			@Override
			public void onTurn(Turn t) {
				Map<Scoring, Integer> unchosen =
					new EnumMap<Scoring, Integer>(Scoring.class)
				;
				Map<Scoring, Integer> chosen =
					new EnumMap<Scoring, Integer>(Scoring.class)
				;

				for (Entry<Scoring, Integer> entry : y.getTurn().getPlayer().getTotals().entrySet()) {
					if (entry.getValue() == -1) {
						unchosen.put(entry.getKey(), entry.getValue());
					} else {
						chosen.put(entry.getKey(), entry.getValue());
					}
				}
				getBot().sendMessage(
					channelObj,
					t.getPlayer().getName() + ", it is your turn. Totals: " +
					getTotalsStr(chosen, false, false) + ", Score: " +
					y.getTurn().getPlayer().getTotalScore() +
					", Turns remaining after this turn: " +
					(unchosen.size() - 1)
				);
			}

			@Override
			public void onStart(Yahtzee y) {
				getBot().sendMessage(channelObj, "The game has started!");
			}

			@Override
			public void onTurnComplete(Turn t) {
				Map<Scoring, Integer> unchosen = new EnumMap<Scoring, Integer>(Scoring.class);
				Map<Scoring, Integer> chosen = new EnumMap<Scoring, Integer>(Scoring.class);

				for (Entry<Scoring, Integer> entry : y.getTurn().getPlayer().getTotals().entrySet()) {
					if (entry.getValue() == -1) {
						unchosen.put(entry.getKey(), entry.getValue());
					} else {
						chosen.put(entry.getKey(), entry.getValue());
					}
				}

				getBot().sendMessage(
					channelObj,
					"Turn completed. Totals: " +
					getTotalsStr(chosen, false, false) +
					", Score: " + y.getTurn().getPlayer().getTotalScore() +
					", Turns remaining: " + (unchosen.size() - 1)
				);
			}

			@Override
			public void onAddPlayer(Player p) {
				getBot().sendMessage(channelObj, "Player added: " + p.getName());
			}

			@Override
			public void onReset(Yahtzee y) {
				getBot().sendMessage(channelObj, "Game reset! Please add players!");
			}

			@Override
			public void onGameComplete(Yahtzee y) {
				StringBuilder sb = new StringBuilder();
				boolean first = true;
				for (Player p : y.getPlayers()) {
					if (!first) { sb.append(", "); }
					sb.append(p.getName()).append(": ").append(p.getTotalScore());
					first = false;
				}
				getBot().sendMessage(
					channelObj,
					"Game complete! Final scores: " + sb.toString() + ". " +
					"Please use .reset to play again!"
				);
			}

			@Override
			public void onRemovePlayer(Player p) {
				getBot().sendMessage(channelObj, "Player removed: " + p.getName());
			}
		});
		
		getBot().getListenerManager().addListener(listener = new ListenerAdapter<PircBotX>() {
			@Override
			public void onJoin(JoinEvent<PircBotX> event) throws Exception {
				if (!getBot().getUserBot().equals(event.getUser())) return;
				if (channelObj == null) return;
				if (!event.getChannel().equals(channelObj)) return;
				showInitialHelpMsg();
				_out("Bot joined channel " + event.getChannel().getName() + ".");
			}
			
			@Override
			public void onKick(KickEvent<PircBotX> event) throws Exception {
				if (!getBot().getUserBot().equals(event.getRecipient())) return;
				if (channelObj == null) return;
				if (!event.getChannel().equals(channelObj)) return;				
				_out(
					"Bot kicked from channel by: " + event.getSource().getNick() +
					" (reason: \"" +
					(event.getReason() == null ? "none" : event.getReason()) +
					"\")."
				);
			}
			
			@Override
			public void onPart(PartEvent<PircBotX> event) throws Exception {
				if (!getBot().getUserBot().equals(event.getUser())) return;
				if (!event.getChannel().equals(channelObj)) return;
				_out("Bot parted channel: " + event.getChannel().getName());
			}
			
			@Override
			public void onMessage(MessageEvent<PircBotX> event) {
				synchronized (YatzyBot.this) {
					if (!event.getChannel().equals(channelObj)) return; // ignore
					if (event.getChannel() != null) {
						if (event.getMessage() == null) return;
						
						boolean hasCmdPr = event.getMessage().charAt(0) == '.';
						if (!hasCmdPr) return;						
						
						String trimmedMsg = event.getMessage().trim();
						if (trimmedMsg.isEmpty()) return;
						
						int spc_i = trimmedMsg.indexOf(' ');						
						
						final String first  = spc_i == -1 ? trimmedMsg : trimmedMsg.substring(0,spc_i);
						final String follow = spc_i == -1 ? null : trimmedMsg.substring(spc_i + 1).trim();
						
						if (first.equals(".help")) {
							showHelpMsg(event.getUser());
						} else if (first.equals(".credits")) {
							getBot().sendNotice(
								event.getUser(),
								"YatzyBot " + VERSION + " by Overlord Industries " +
								"(Chris Dennett / Dessimat0r / dessimat0r@gmail.com) " +
								"and other contributors (none yet, add your name and " +
								"e-mail here if you contribute). Running on PircBotX " +
								PircBotX.VERSION + "."
							);
						} else if (first.equals(".roll") || first.equals(".r")) {
							if (y.getTurn() != null && event.getUser().getNick().equals(y.getTurn().getPlayer().getName())) {
								final boolean[] rolled;
								try {
									if (follow == null) {
										y.getTurn().rollAll();
										rolled = new boolean[5];
										Arrays.fill(rolled, true);
									} else {
										String[] tokens = follow.split(" ");
										int[] nums = new int[tokens.length];
										// parse other numbers
										for (int i = 0; i < tokens.length; i++) {
											int num = Integer.parseInt(tokens[i]);
											nums[i] = num;
										}
										try {
											rolled = y.getTurn().rollNumbers(nums);
										} catch (RollNumberNotFoundException e) {
											getBot().sendMessage(
												event.getChannel(),
												"Die face value not found for rolling: " +
												e.getNumToRoll()
											);
											return;
										}
									}
									Map<Scoring, Integer> scores = y.getRollScores();
		
									Map<Scoring, Integer> unchosen = new EnumMap<Scoring, Integer>(Scoring.class);
									Map<Scoring, Integer> chosen = new EnumMap<Scoring, Integer>(Scoring.class);
		
									for (Entry<Scoring, Integer> entry : scores.entrySet()) {
										if (y.getTurn().getPlayer().getTotals().get(entry.getKey()) == -1) {
											unchosen.put(entry.getKey(), entry.getValue());
										} else {
											chosen.put(entry.getKey(), entry.getValue());
										}
									}
		
									getBot().sendMessage(channelObj, "#" + y.getTurn().getRolls() + ": dice: " + diceToString(rolled, false) + ", scores: " + getDiceStr(y.getTurn().getPlayer().getTotals(), scores));
								} catch (YatzyException e3) {
									getBot().sendMessage(channelObj, e3.getMessage());
								}
							}
						} else if (first.equals(".hold") || first.equals(".h")) {
							if (y.getTurn() != null && event.getUser().getNick().equals(y.getTurn().getPlayer().getName())) {
								boolean failed = false;
								try {
									if (follow == null) {
										getBot().sendMessage(channelObj, "Must choose some dice to hold!");
										return;
									}
									String[] tokens = follow.split(" ");
									ArrayList<Integer> holdall_dienums  = new ArrayList<Integer>();
									int[] holdall_nummatch = null;
		
									if (tokens[0].equals("all")) {
										if (tokens.length == 1) {
											getBot().sendMessage(channelObj, "Must specify what die number(s) to hold!");
											return;
										}
										for (int i = 1; i < tokens.length; i++) {
											try {
												int num = Integer.valueOf(tokens[i]);
												if (num < 1 || num > 6) {
													getBot().sendMessage(channelObj, "Found illegal number in die numbers (" + num + ") - must be inclusively between 1 and 6!");
													return;
												}
												for (int dienum : holdall_dienums) {
													if (dienum == num) {
														getBot().sendMessage(channelObj, "Duplicate number in die numbers (" + num + ")!");
														return;	        									
													}
												}
												holdall_dienums.add(num);
											} catch (NumberFormatException nfe) {
												getBot().sendMessage(channelObj, "Found invalid token in die numbers (" + tokens[i] + ")!");
												return;
											}
										}
										holdall_nummatch = new int[holdall_dienums.size()];
									}
									ArrayList<Integer> holdnums = new ArrayList<Integer>();
									ArrayList<Integer> rollnums = new ArrayList<Integer>();
		
									if (holdall_dienums.size() > 0) {
										dice_chk: for (int i = 0; i < y.getDice().length; i++) {
											int d = y.getDice()[i].getFaceValue();	
											for (int j = 0; j < holdall_dienums.size(); j++) {
												if (holdall_dienums.get(j) == d) {
													holdnums.add(d);
													holdall_nummatch[j]++;
													continue dice_chk;
												}
											}	
											rollnums.add(d);
										}
										for (int i = 0; i < holdall_nummatch.length; i++) {
											if (holdall_nummatch[i] == 0) {
												getBot().sendMessage(
													channelObj,
													"Hold all number not found in rolled dice: " +
													holdall_dienums.get(i)
												);
												return;
											}
										}
									} else {
										// parse other numbers
										for (int i = 0; i < tokens.length; i++) {
											int num = Integer.parseInt(tokens[i]);
											holdnums.add(num);
										}
		
										for (int i = 0; i < y.getDice().length; i++) {
											int d = y.getDice()[i].getFaceValue();
		
											if (holdnums.isEmpty()) {
												rollnums.add(d);
												continue;
											}
		
											ListIterator<Integer> iter = holdnums.listIterator();
		
											boolean found = false;
											while (iter.hasNext()) {
												int holdnum = iter.next().intValue();
												if (holdnum == d) {
													iter.remove();
													found = true;
													break;
												}
											}
											if (!found) {
												rollnums.add(d);
											}
										}
		
										if (!holdnums.isEmpty()) {
											getBot().sendMessage(channelObj, "Hold nums not found: " + holdnums);
											failed = true;
										}
									}
		
									if (!failed) {
										boolean[] rolled = y.getTurn().rollNumbers(convertIntegers(rollnums));
		
										Map<Scoring, Integer> scores = y.getRollScores();
		
										Map<Scoring, Integer> unchosen = new EnumMap<Scoring, Integer>(Scoring.class);
										Map<Scoring, Integer> chosen = new EnumMap<Scoring, Integer>(Scoring.class);
		
										for (Entry<Scoring, Integer> entry : scores.entrySet()) {
											if (y.getTurn().getPlayer().getTotals().get(entry.getKey()) == -1) {
												unchosen.put(entry.getKey(), entry.getValue());
											} else {
												chosen.put(entry.getKey(), entry.getValue());
											}
										}
										getBot().sendMessage(channelObj, "#" + y.getTurn().getRolls() + ": dice: " + diceToString(rolled, false) + ", scores: " + getDiceStr(y.getTurn().getPlayer().getTotals(), scores));
									};
								} catch (YatzyException e3) {
									getBot().sendMessage(channelObj, e3.getMessage());
								} catch (NumberFormatException e4) {
									getBot().sendMessage(channelObj, e4.getMessage());
								}
							}
						} else if (first.equals(".check") || first.equals(".ch")) {
							if (y.getTurn() != null && event.getUser().getNick().equals(y.getTurn().getPlayer().getName())) {
								if (y.getTurn().getRolls() == 0) {
									getBot().sendMessage(channelObj, "Must do at least one roll before checking scoring.");
									return;
								}
								ArrayList<Scoring> specific = null;
								if (follow != null) {
									String[] tokens = follow.split(" ");
									specific = new ArrayList<Scoring>();
									for (int i = 0; i < tokens.length; i++) {
										Scoring s = Yahtzee.SCORING_ABBRV_MAP.get(tokens[i].toLowerCase());
										if (s == null) {
											getBot().sendMessage(channelObj, "Couldn't find the scoring for " + tokens[i].toLowerCase() + ".");
											return;
										}
										specific.add(s);
									}
								}
		
								if (specific != null) {
									Map<Scoring, Integer> scoring = new EnumMap<Scoring, Integer>(Scoring.class);
									for (Scoring s : specific) {
										scoring.put(s, y.getRollScores().get(s));
									}
									getBot().sendMessage(channelObj, "Scoring: " + getTotalsStr(scoring, true, true));
								} else {
									Map<Scoring, Integer> unchosen = new EnumMap<Scoring, Integer>(Scoring.class);
									Map<Scoring, Integer> chosen = new EnumMap<Scoring, Integer>(Scoring.class);
		
									for (Entry<Scoring, Integer> entry : y.getRollScores().entrySet()) {
										if (y.getTurn().getPlayer().getTotals().get(entry.getKey()) == -1) {
											unchosen.put(entry.getKey(), entry.getValue());
										} else {
											chosen.put(entry.getKey(), entry.getValue());
										}
									}		        			
		
									getBot().sendMessage(channelObj, "Scoring: " + getTotalsStr(unchosen, true, true));		        					
								}	
							}
						} else if (first.equals(".start")) {
							boolean found = false;
							for (Player p : y.getPlayers()) {
								if (event.getUser().getNick().equals(p.getName())) {
									found = true;
									break;
								}
							}
							if (found) {
								try {
									y.start();
								} catch (YatzyException e) {
									getBot().sendMessage(channelObj, e.getMessage());
								}
							} else {
								getBot().sendMessage(channelObj, "Cannot start game if not participating!");
							}
						} else if (first.equals(".reset")) {
							try {
								if (!y.isStarted() || y.isFinished()) {
									y.reset();
								} else if (y.isStarted()) {
									// check players
									boolean found = false;
									for (Player p : y.getPlayers()) {
										if (event.getUser().getNick().equals(p.getName())) {
											found = true;
											break;
										}
									}
									if (found) {
										y.reset();
									} else {
										getBot().sendMessage(channelObj, "Cannot reset game if not participating, or game not finished!");
									}
								}
							} catch (YatzyException e) {
								getBot().sendMessage(channelObj, e.getMessage());
							}
						} else if (first.equals(".play")) {
							if (y.getPlayerMap().get(event.getUser().getNick()) == null) {
								try {
									y.addPlayer(new Player(event.getUser().getNick()));
								} catch (YatzyException e) {
									getBot().sendMessage(channelObj, e.getMessage());
								}
							}
						} else if (first.equals(".deleteplayer")) {
							String[] tokens = trimmedMsg.split(" ");
							if (tokens.length >= 2) {
								String name = tokens[1];
								try {
									// check players
									boolean found = false;
									for (Player p : y.getPlayers()) {
										if (event.getUser().getNick().equals(p.getName())) {
											found = true;
											break;
										}
									}
									if (found) {
										y.removePlayer(name);
									} else {
										getBot().sendMessage(channelObj, "Cannot remove player if not participating, or finished!");
									}
								} catch (YatzyException e) {
									getBot().sendMessage(channelObj, e.getMessage());
								}
							}
						} else if (first.equals(".choose") || first.equals(".c")) {
							if (y.getTurn() != null && event.getUser().getNick().equals(y.getTurn().getPlayer().getName())) {
								String[] tokens = trimmedMsg.split(" ");
								String chosen = tokens[1];
								Scoring s = Yahtzee.SCORING_ABBRV_MAP.get(chosen.toLowerCase());
								if (s == null) {
									getBot().sendMessage(channelObj, "Not a valid scoring!");
								} else {
									try {
										y.getTurn().choose(s);
									} catch (TurnException e) {
										getBot().sendMessage(channelObj, e.getMessage());
									} catch (YatzyException e3) {
										getBot().sendMessage(channelObj, e3.getMessage());
									}
								}	
							}
						}
					}
				}
			}
		});		
		getBot().joinChannel(channel);
		
		started = true;
	}
	
	// remember to remove from parent user lists
	public void stop() {
		if (!activated) throw new IllegalStateException("Cannot manipulate a deactivated getBot().");
		started = false;
		getBot().partChannel(channelObj);
		getBot().getListenerManager().removeListener(listener);
		y = null;
		//channelObj = null;
	}
	
	public String getServer() {
		return server;
	}
	
	public boolean isActivated() {
		return activated;
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public static final String INITIAL_HELP_TEXT =
		"Hello, I am YatzyBot " + VERSION + " :) Please type .help for more info! " +
		"Initially coded by Chris Dennett (Dessimat0r), project source on GitHub for " +
		"further contributions (http://github.com/Dessimat0r/YatzyBot). " +
		"Running on PircBotX " + PircBotX.VERSION + "." + " Have fun! :)";
	;
	
	public static final String HELP_TEXT =
		"Hello, I am YatzyBot " + VERSION + " :) Valid game actions: .play (add yourself as player), .start (start game, do this once all players have joined), .reset (reset game),  .deleteplayer <player_name> (deletes a player if they stopped playing or left for some reason), .help (re-show help message)\n" +
		"Valid rolling actions: .roll/.r {optional dice to reroll} (roll or re-roll particular dice), .hold/.h [all] {dice to hold} (hold particular dice when re-rolling), .choose/.c {SCORING_NAME} (choose scoring then finish your turn), .check/.ch (check scores)\n" +
		"Please read gameplay information at http://en.wikipedia.org/wiki/Yatzy before playing!"
	;
	public static final String[] HELP_TEXT_ARR = HELP_TEXT.split("(\\r?\\n?)\\{1,2\\}");
	
	public void showInitialHelpMsg() {
		getBot().sendMessage(channelObj, INITIAL_HELP_TEXT);
	}
	
	public void showHelpMsg(User user) {
		if (user == null) {
			for (String line : HELP_TEXT_ARR) {
				getBot().sendMessage(channelObj, line);
			}
			return;
		}
		getBot().sendNotice(user, HELP_TEXT);
	}
	
	public void showLeaveMsg(String message) {
		getBot().sendMessage(
			channelObj,
			"Thanks for playing with YatzyBot! :) (Leaving now! Reason: " + message + ")"
		);
	}

	public static final Comparator<Entry<Scoring, Integer>> dicevalcomp = new Comparator<Map.Entry<Scoring,Integer>>() {
		@Override
		public int compare(Entry<Scoring, Integer> o1, Entry<Scoring, Integer> o2) {
			return o2.getValue().compareTo(o1.getValue());
		}
	};
	
	public void onPart() {
		_out("Bot parted channel: " + channelObj.getName());
		channelObj = null;
	}

	public static final char COLOUR = '\u0003';


	public static final String getDiceStr(Map<Scoring, Integer> scores, Map<Scoring, Integer> scoring) {
		if (scoring.isEmpty()) return "None";

		boolean first = true;
		StringBuilder sb = new StringBuilder();

		//List<Entry<Scoring, Integer>> sorted = new ArrayList<Map.Entry<Scoring,Integer>>();
		//for (Entry<Scoring, Integer> entry : scoring.entrySet()) {
		//	sorted.add(entry);
		//}
		//Collections.sort(sorted, dicevalcomp);

		sb.append(COLOUR).append(0).append(',').append(1);
		sb.append("[ ");
		for (Entry<Scoring, Integer> entry : scoring.entrySet()) {
			boolean isGood = false;
			if (
					(!entry.getKey().isVariable() && entry.getValue() == entry.getKey().getMax()) ||
					(entry.getKey().isVariable() && (((double)entry.getValue() / (double)entry.getKey().getMax()) > Yahtzee.getGoodScorePerc(entry.getKey())))
					) {
				isGood = true;
			}
			if (!first) sb.append(" ");
			if (scores.get(entry.getKey()) == -1) {
				//sb.append(Colors.UNDERLINE);
				sb.append(COLOUR).append(0).append(',').append(1);
				sb.append(Colors.BOLD).append(Colors.BOLD);

				if (entry.getValue() == 0) {
					sb.append(COLOUR).append(15).append(',').append(1);
					sb.append(Colors.BOLD).append(Colors.BOLD);
				} else if (entry.getValue() > 0) {
					if (isGood) {
						sb.append(COLOUR).append(8).append(',').append(1);
					} else {
						sb.append(COLOUR).append(0).append(',').append(1);
					}
					sb.append(Colors.BOLD).append(Colors.BOLD);
				}
			} else {
				sb.append(COLOUR).append(1).append(',').append(14);
				sb.append(Colors.BOLD).append(Colors.BOLD);
			}
			sb.append(entry.getKey().getAbrv());
			sb.append(COLOUR).append(0).append(',').append(1);
			if (scores.get(entry.getKey()) == -1) {
				if (entry.getValue() > 0) {
					//sb.append(Colors.BOLD);
				}
				//sb.append(Colors.UNDERLINE);
			}
			first = false;
		}
		sb.append(COLOUR).append(0).append(',').append(1);
		sb.append(" ]");
		sb.append(Colors.NORMAL);
		return sb.toString();
	}


	public static final String getTotalsStr(Map<Scoring, Integer> scoring, boolean showAbrv, boolean showMax) {
		if (scoring.isEmpty()) return "None";

		boolean first = true;
		StringBuilder sb = new StringBuilder();
		for (Entry<Scoring, Integer> entry : scoring.entrySet()) {
			if (!first) sb.append(" ");
			sb.append("[");
			if (showAbrv) sb.append(entry.getKey().getAbrv()).append("/");
			sb.append(entry.getKey().getName()).append(": ").append(entry.getValue());
			if (showMax) sb.append("/").append(entry.getKey().getMax());
			sb.append("]");
			first = false;
		}
		return sb.toString();
	}

	public String diceToString(boolean[] rolled, boolean colours) {
		if (y.getDice() == null) return "null";
		int iMax = y.getDice().length - 1;
		if (iMax == -1)	return "[]";

		StringBuilder b = new StringBuilder();
		if (colours) {
			b.append(COLOUR).append(0).append(',').append(1);
		}
		b.append("[ ");
		for (int i = 0; ; i++) {
			if (colours) {
				if (rolled[i]) {
					b.append(COLOUR).append(0).append(',').append(12);
				} else {
					b.append(COLOUR).append(0).append(',').append(1);
				}
				b.append(Colors.BOLD).append(Colors.BOLD);
			}
			b.append(String.valueOf(y.getDice()[i]));
			if (colours) {
				b.append(COLOUR).append(0).append(',').append(1);
			}
			if (i == iMax) {
				b.append(" ]");
				if (colours) {
					b.append(Colors.NORMAL);
				}
				return b.toString();
			}
			b.append(", ");
		}
	}
	
	public PircBotX getBot() {
		return user.getBot();
	}
	
	void dispose() {
		getBot().getListenerManager().removeListener(listener);
	}
	
	public void activate() {
		if (activated) return;
		if (started) throw new IllegalArgumentException("Bot trying to active when started!");
		this.activated = true;
	}
	
	public void deactivate() {
		if (!activated) return;
		if (started) throw new IllegalArgumentException("Cannot deactivate a started bot!");
		this.activated = false;
	}
	
	public String getBotStr() {
		return "YatzyBot[" + user.getStatusStr() + "]~" + user.getUserNickStr() + "@" + user.getUserServerStr() + "~" + channel;
	}
	
    public void _out(String msg) {
    	synchronized (YatzyUser.OUTPUT_LOCK) {
    		_out(msg, null);
    	}
    }
    
    public void _err(String msg) {
    	synchronized (YatzyUser.OUTPUT_LOCK) {
    		_err(msg, null);
    	}
    }
	
    public void _out(String msg, User origin) {
    	synchronized (YatzyUser.OUTPUT_LOCK) {
    		System.out.println(getBotStr() + (origin == null ? "" : "[" + origin.getNick() + "]") + ": " + msg);
    		YatzyUser.pmLogAllAdmins(getBotStr() + (origin == null ? "" : "[" + origin.getNick() + "]") + ": " + msg, false);
    	}
    }
    
    public void _err(String msg, User origin) {
    	synchronized (YatzyUser.OUTPUT_LOCK) {
    		System.err.println(getBotStr() + (origin == null ? "" : "[" + origin.getNick() + "]") + ": " + msg);
    		YatzyUser.pmLogAllAdmins(getBotStr() + (origin == null ? "" : "[" + origin.getNick() + "]") + ": " + msg, true);
    	}
    }
}