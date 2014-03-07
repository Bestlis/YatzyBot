// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee.bot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.overlord.yahtzee.Player;
import org.overlord.yahtzee.RollException;
import org.overlord.yahtzee.ScoreException;
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
import org.pircbotx.hooks.events.MessageEvent;

public class YatzyBot {
	protected static final String VERSION = "0.84";
	
	protected final PircBotX bot;
	protected final YatzyUser user;
	protected final String server;
	protected final String channel;
	protected final Channel channelObj;
	protected final ListenerAdapter<PircBotX> listener;
	
	protected final Yahtzee y = new Yahtzee();

	public static int[] convertIntegers(List<Integer> integers)
	{
		int[] ret = new int[integers.size()];
		Iterator<Integer> iterator = integers.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = iterator.next();
		}
		return ret;
	}

	public YatzyBot(YatzyUser user, final Channel channelObj) {
		this.user       = user;
		this.bot        = user.getBot();
		this.server     = user.getServerDef().getServer();
		this.channelObj = channelObj;
		this.channel    = channelObj.getName();
		
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
				bot.sendMessage(
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
				bot.sendMessage(channelObj, "The game has started!");
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

				bot.sendMessage(
					channelObj,
					"Turn completed. Totals: " +
					getTotalsStr(chosen, false, false) +
					", Score: " + y.getTurn().getPlayer().getTotalScore() +
					", Turns remaining: " + (unchosen.size() - 1)
				);
			}

			@Override
			public void onAddPlayer(Player p) {
				bot.sendMessage(channelObj, "Player added: " + p.getName());
			}

			@Override
			public void onReset(Yahtzee y) {
				bot.sendMessage(channelObj, "Game reset! Please add players!");
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
				bot.sendMessage(
					channelObj,
					"Game complete! Final scores: " + sb.toString() + ". " +
					"Please use .reset to play again!"
				);
			}

			@Override
			public void onRemovePlayer(Player p) {
				bot.sendMessage(channelObj, "Player removed: " + p.getName());
			}
		});
		
		bot.getListenerManager().addListener(listener = new ListenerAdapter<PircBotX>() {
			@Override
			public void onMessage(MessageEvent<PircBotX> event) {
				synchronized (YatzyBot.this) {
					if (!event.getChannel().equals(channelObj)) {
						return; // ignore
					}
					if (event.getChannel() != null) {
						if (event.getMessage() == null || event.getMessage().isEmpty()) return;
						
						boolean hasCmdPr = event.getMessage().charAt(0) == '.';
						if (!hasCmdPr) return;
						int spc_i = event.getMessage().indexOf(' ');
						
						final String first  = spc_i == -1 ? event.getMessage() : event.getMessage().substring(0,spc_i);
						final String follow = spc_i == -1 ? null : event.getMessage().substring(spc_i + 1);
						
						if (first.equals(".help")) {
							showHelpMsg(event.getUser());
						} else if (first.equals(".credits")) {
							bot.sendNotice(
								event.getUser(),
								"YatzyBot " + VERSION + " by Overlord Industries " +
								"(Chris Dennett / Dessimat0r / dessimat0r@gmail.com) " +
								"and other contributors (none yet, add your name and " +
								"e-mail here if you contribute)."
							);
						} else if (first.equals(".roll") || first.equals(".r")) {
							if (y.getTurn() != null && event.getUser().getNick().equals(y.getTurn().getPlayer().getName())) {
								final boolean[] rolled;
								try {
									if (follow == null) {
										rolled = y.getTurn().roll();
									} else {
										String[] tokens = follow.split(" ");
										int[] nums = new int[tokens.length];
										// parse other numbers
										for (int i = 0; i < tokens.length; i++) {
											int num = Integer.parseInt(tokens[i]);
											nums[i] = num;
										}
										rolled = y.getTurn().rollNumbers(nums);
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
		
									bot.sendMessage(channelObj, "#" + y.getTurn().getRolls() + ": dice: " + diceToString(rolled, false) + ", scores: " + getDiceStr(y.getTurn().getPlayer().getTotals(), scores));
								} catch (TurnException e1) {
									bot.sendMessage(channelObj, e1.getMessage());
								} catch (RollException e2) {
									bot.sendMessage(channelObj, e2.getMessage());
								} catch (YatzyException e3) {
									bot.sendMessage(channelObj, e3.getMessage());
								}
							}
						} else if (first.equals(".hold") || first.equals(".h")) {
							if (y.getTurn() != null && event.getUser().getNick().equals(y.getTurn().getPlayer().getName())) {
								boolean failed = false;
								try {
									if (follow == null) {
										bot.sendMessage(channelObj, "Must choose some dice to hold!");
										return;
									}
									String[] tokens = follow.split(" ");
									ArrayList<Integer> holdall_dienums  = new ArrayList<Integer>();
									int[] holdall_nummatch = null;
		
									if (tokens[0].equals("all")) {
										if (tokens.length == 1) {
											bot.sendMessage(channelObj, "Must specify what die number(s) to hold!");
											return;
										}
										for (int i = 1; i < tokens.length; i++) {
											try {
												int num = Integer.valueOf(tokens[i]);
												if (num < 1 || num > 6) {
													bot.sendMessage(channelObj, "Found illegal number in die numbers (" + num + ") - must be inclusively between 1 and 6!");
													return;
												}
												for (int dienum : holdall_dienums) {
													if (dienum == num) {
														bot.sendMessage(channelObj, "Duplicate number in die numbers (" + num + ")!");
														return;	        									
													}
												}
												holdall_dienums.add(num);
											} catch (NumberFormatException nfe) {
												bot.sendMessage(channelObj, "Found invalid token in die numbers (" + tokens[i] + ")!");
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
												bot.sendMessage(
													channelObj,
													"Hold- ll number not found in rolled dice: " + holdall_dienums.get(i)
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
											bot.sendMessage(channelObj, "Hold nums not found: " + holdnums);
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
										bot.sendMessage(channelObj, "#" + y.getTurn().getRolls() + ": dice: " + diceToString(rolled, false) + ", scores: " + getDiceStr(y.getTurn().getPlayer().getTotals(), scores));
									}
								} catch (TurnException e1) {
									bot.sendMessage(channelObj, e1.getMessage());
								} catch (RollException e2) {
									bot.sendMessage(channelObj, e2.getMessage());
								} catch (YatzyException e3) {
									bot.sendMessage(channelObj, e3.getMessage());
								} catch (NumberFormatException e4) {
									bot.sendMessage(channelObj, e4.getMessage());
								}
							}
						} else if (first.equals(".check") || first.equals(".ch")) {
							if (y.getTurn() != null && event.getUser().getNick().equals(y.getTurn().getPlayer().getName())) {
								if (y.getTurn().getRolls() == 0) {
									bot.sendMessage(channelObj, "Must do at least one roll before checking scoring.");
									return;
								}
								String[] tokens = event.getMessage().split(" ");
								ArrayList<Scoring> specific = null;
								if (tokens.length > 1) {
									specific = new ArrayList<Scoring>();
									for (int i = 1; i < tokens.length; i++) {
										Scoring s = Yahtzee.SCORING_ABBRV_MAP.get(tokens[i].toLowerCase());
										if (s == null) {
											bot.sendMessage(channelObj, "Couldn't find the scoring for " + tokens[1].toLowerCase() + ".");
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
									bot.sendMessage(channelObj, "Scoring: " + getTotalsStr(scoring, true, true));
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
		
									bot.sendMessage(channelObj, "Scoring: " + getTotalsStr(unchosen, true, true));		        					
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
									bot.sendMessage(channelObj, e.getMessage());
								}
							} else {
								bot.sendMessage(channelObj, "Cannot start game if not participating!");
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
										bot.sendMessage(channelObj, "Cannot reset game if not participating, or game not finished!");
									}
								}
							} catch (YatzyException e) {
								bot.sendMessage(channelObj, e.getMessage());
							}
						} else if (first.equals(".play")) {
							if (y.getPlayerMap().get(event.getUser().getNick()) == null) {
								try {
									y.addPlayer(new Player(event.getUser().getNick()));
								} catch (YatzyException e) {
									bot.sendMessage(channelObj, e.getMessage());
								}
							}
						} else if (first.equals(".deleteplayer")) {
							String[] tokens = event.getMessage().split(" ");
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
										bot.sendMessage(channelObj, "Cannot remove player if not participating, or finished!");
									}
								} catch (YatzyException e) {
									bot.sendMessage(channelObj, e.getMessage());
								}
							}
						} else if (first.equals(".choose") || first.equals(".c")) {
							if (y.getTurn() != null && event.getUser().getNick().equals(y.getTurn().getPlayer().getName())) {
								String[] tokens = event.getMessage().split(" ");
								String chosen = tokens[1];
								Scoring s = Yahtzee.SCORING_ABBRV_MAP.get(chosen.toLowerCase());
								if (s == null) {
									bot.sendMessage(channelObj, "Not a valid scoring!");
								} else {
									try {
										y.getTurn().choose(s);
									} catch (TurnException e) {
										bot.sendMessage(channelObj, e.getMessage());
									} catch (ScoreException e) {
										bot.sendMessage(channelObj, e.getMessage());
									} catch (YatzyException e3) {
										bot.sendMessage(channelObj, e3.getMessage());
									}
								}	
							}
						}
					}
				}
			}
		});
	}
	
	public String getChannel() {
		return channel;
	}
	
	public Channel getChannelObj() {
		return channelObj;
	}
	public void start() {
		bot.joinChannel(channel);
		showInitialHelpMsg();
	}
	
	public synchronized void leave(String reason) {
		bot.partChannel(channelObj, reason); // part channel
		dispose(); // best to dispose after parting
	}
	
	// remember to remove from parent user lists
	synchronized void dispose() {
		bot.getListenerManager().removeListener(listener); // remove listener
	}
	
	public String getServer() {
		return server;
	}
	
	public static final String INITIAL_HELP_TEXT =
		"Hello, I am YatzyBot " + VERSION + " :) Please type .help for more info! Initially coded by Chris Dennett (Dessimat0r), project source on GitHub for further contributions (http://github.com/Dessimat0r/YatzyBot). Have fun! :)";
	;
	
	public static final String HELP_TEXT =
		"Hello, I am YatzyBot " + VERSION + " :) Valid game actions: .play (add yourself as player), .start (start game, do this once all players have joined), .reset (reset game),  .deleteplayer <player_name> (deletes a player if they stopped playing or left for some reason), .help (re-show help message)\n" +
		"Valid rolling actions: .roll/.r {optional dice to reroll} (roll or re-roll particular dice), .hold/.h [all] {dice to hold} (hold particular dice when re-rolling), .choose/.c {SCORING_NAME} (choose scoring then finish your turn), .check/.ch (check scores)\n" +
		"Please read gameplay information at http://en.wikipedia.org/wiki/Yatzy before playing!"
	;
	public static final String[] HELP_TEXT_ARR = HELP_TEXT.split("(\\r?\\n?)\\{1,2\\}");
	
	public void showInitialHelpMsg() {
		bot.sendMessage(channelObj, INITIAL_HELP_TEXT);
	}
	
	public void showHelpMsg(User user) {
		if (user == null) {
			for (String line : HELP_TEXT_ARR) {
				bot.sendMessage(channelObj, line);
			}
			return;
		}
		bot.sendNotice(user, HELP_TEXT);
	}
	
	public void showLeaveMsg(String message) {
		bot.sendMessage(channelObj, "Thanks for playing with YatzyBot! :) (Leaving now! Reason: " + message + ")");
	}

	public static final Comparator<Entry<Scoring, Integer>> dicevalcomp = new Comparator<Map.Entry<Scoring,Integer>>() {
		@Override
		public int compare(Entry<Scoring, Integer> o1, Entry<Scoring, Integer> o2) {
			return o2.getValue().compareTo(o1.getValue());
		}
	};

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
		if (y.getDice() == null)
			return "null";
		int iMax = y.getDice().length - 1;
		if (iMax == -1)
			return "[]";

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
}