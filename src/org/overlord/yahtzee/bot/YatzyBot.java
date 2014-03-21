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

import org.overlord.yahtzee.DieNumberNotFoundException;
import org.overlord.yahtzee.GameCompleteException;
import org.overlord.yahtzee.GameStartedException;
import org.overlord.yahtzee.Player;
import org.overlord.yahtzee.PlayerNotExistsException;
import org.overlord.yahtzee.ReqRollException;
import org.overlord.yahtzee.Scoring;
import org.overlord.yahtzee.ScoringAlreadyPlayedException;
import org.overlord.yahtzee.Turn;
import org.overlord.yahtzee.UsedAllRollsException;
import org.overlord.yahtzee.Yahtzee;
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
	protected static final String VERSION        = "0.863";
	public    static final char   DEFAULT_PREFIX = '.';
	
	protected final YatzyUser user;
	protected final String    server;
	protected final String    channel;
	
	protected char    prefix     = DEFAULT_PREFIX;
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
		
		updateLocalText();
		
		if (activated) activate();
		else deactivate();
	}
	
	public char getPrefix() {
		return prefix;
	}
	
	public void setPrefix(char prefix) {
		this.prefix = prefix;
		updateLocalText();
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
					", Turns remaining: " + unchosen.size()
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
						
						if (first.equals(prefix + "help")) {
							showHelpMsg(event.getUser());
						} else if (first.equals(prefix + "credits")) {
							getBot().sendNotice(
								event.getUser(),
								"YatzyBot " + VERSION + " by Overlord Industries " +
								"(Chris Dennett / Dessimat0r / dessimat0r@gmail.com) " +
								"and other contributors (none yet, add your name and " +
								"e-mail here if you contribute). Running on PircBotX " +
								PircBotX.VERSION + "."
							);
						} else if (first.equals(prefix + "roll") || first.equals(prefix + "r")) {
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
		
									event.respond("#" + y.getTurn().getRolls() + ": dice: " + diceToString(rolled, false) + ", scores: " + getDiceStr(y.getTurn().getPlayer().getTotals(), scores));
								} catch (UsedAllRollsException e) {
									event.respond("Used all three rolls. Use .choose/.c <scoring> to choose a scoring.");
									return;
								} catch (DieNumberNotFoundException e) {
									event.respond("Couldn't find face number in dice to roll.");
									return;
								} catch (GameCompleteException e) {
									event.respond("Game complete. Please use .reset to reset the bot.");
									return;
								} catch (ReqRollException e) {
									event.respond("Must roll at least once to re-roll particular face numbers.");
									return;									
								}
							}
						} else if (first.equals(prefix + "hold") || first.equals(prefix + "h")) {
							if (y.getTurn() != null && event.getUser().getNick().equals(y.getTurn().getPlayer().getName())) {
								try {
									if (follow == null) {
										event.respond("Must choose some dice to hold!");
										return;
									}
									String[] tokens = follow.split(" ");
									ArrayList<Integer> holdall_dienums  = new ArrayList<Integer>();
									int[] holdall_nummatch = null;
		
									if (tokens[0].equals("all")) {
										if (tokens.length == 1) {
											event.respond("Must specify what die number(s) to hold!");
											return;
										}
										for (int i = 1; i < tokens.length; i++) {
											try {
												int num = Integer.valueOf(tokens[i]);
												if (num < 1 || num > 6) {
													event.respond("Found illegal number in die numbers (" + num + ") - must be inclusively between 1 and 6!");
													return;
												}
												for (int dienum : holdall_dienums) {
													if (dienum == num) {
														event.respond("Duplicate number in die numbers (" + num + ")!");
														return;	        									
													}
												}
												holdall_dienums.add(num);
											} catch (NumberFormatException nfe) {
												event.respond("Found invalid token in die numbers (" + tokens[i] + ")!");
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
												event.respond(
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
											event.respond("Hold nums not found: " + holdnums);
											return;
										}
									}
		
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
									event.respond("#" + y.getTurn().getRolls() + ": dice: " + diceToString(rolled, false) + ", scores: " + getDiceStr(y.getTurn().getPlayer().getTotals(), scores));
								} catch (NumberFormatException e4) {
									event.respond("Bad number format. Check your input. " + e4.getMessage());
									return;
								} catch (ReqRollException e) {
									event.respond("Must roll at least once!");
									return;
								} catch (UsedAllRollsException e) {
									event.respond("Used all three rolls. Use .choose/.c <scoring> to choose a scoring.");
									return;
								} catch (DieNumberNotFoundException e) {
									event.respond("Couldn't find face number in dice to hold.");
									return;
								} catch (GameCompleteException e) {
									event.respond("Game complete. Please use .reset to reset the bot.");
									return;
								}
							}
						} else if (first.equals(prefix + "check") || first.equals(prefix + "ch")) {
							if (y.getTurn() != null && event.getUser().getNick().equals(y.getTurn().getPlayer().getName())) {
								if (y.getTurn().getRolls() == 0) {
									event.respond("Must do at least one roll before checking scoring.");
									return;
								}
								ArrayList<Scoring> specific = null;
								if (follow != null) {
									String[] tokens = follow.split(" ");
									specific = new ArrayList<Scoring>();
									for (int i = 0; i < tokens.length; i++) {
										Scoring s = Yahtzee.SCORING_ABBRV_MAP.get(tokens[i].toLowerCase());
										if (s == null) {
											event.respond("Couldn't find the scoring for " + tokens[i].toLowerCase() + ".");
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
									event.respond("Scoring: " + getTotalsStr(scoring, true, true));
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
									event.respond("Scoring: " + getTotalsStr(unchosen, true, true));
									return;
								}	
							}
						} else if (first.equals(prefix + "start")) {
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
								} catch (GameStartedException e) {
									event.respond("Cannot start a game that has already started! Reset first.");
									return;
								} catch (GameCompleteException e) {
									event.respond("Cannot start a finished game! Reset first.");
									return;
								}
							} else {
								event.respond("Cannot start game if not participating!");
								return;
							}
						} else if (first.equals(prefix + "reset")) {
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
									return;
								} else {
									event.respond("Cannot reset game if not participating, or game not finished.");
									return;
								}
							}
						} else if (first.equals(prefix + "play")) {
							if (y.getPlayerMap().get(event.getUser().getNick()) == null) {
								try {
									y.addPlayer(new Player(event.getUser().getNick()));
									return;
								} catch (GameStartedException e) {
									event.respond("Game already started, cannot add new players.");
									return;
								}
							}
						} else if (first.equals(prefix + "deleteplayer")) {
							String[] tokens = trimmedMsg.split(" ");
							if (tokens.length >= 2) {
								String name = tokens[1];
								// check players
								boolean found = false;
								for (Player p : y.getPlayers()) {
									if (event.getUser().getNick().equals(p.getName())) {
										found = true;
										break;
									}
								}
								if (found) {
									try {
										y.removePlayer(name);
									} catch (PlayerNotExistsException e) {
										event.respond("Player '" + name + "' isn't participating in the current game.");
										return;
									}
								} else {
									event.respond("Cannot remove a player if requester isn't participating, or game finished.");
									return;
								}
							}
						} else if (first.equals(prefix + "choose") || first.equals(prefix + "c")) {
							if (y.getTurn() != null && event.getUser().getNick().equals(y.getTurn().getPlayer().getName())) {
								String[] tokens = trimmedMsg.split(" ");
								String chosen = tokens[1];
								Scoring s = Yahtzee.SCORING_ABBRV_MAP.get(chosen.toLowerCase());
								if (s == null) {
									event.respond("Not a valid scoring!");
									return;
								} else {
									try {
										y.getTurn().choose(s);
									} catch (ReqRollException e) {
										event.respond("Must roll at least once to choose dice for scoring.");
										return;
									} catch (ScoringAlreadyPlayedException e) {
										event.respond("Already scored for " + s.getName() + " with " + y.getTurn().getPlayer().getScore(s));
										return;
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
		"Hello, I am YatzyBot " + VERSION + " :) Please type %prefix%help for more info! " +
		"Initially coded by Chris Dennett (Dessimat0r), project source on GitHub for " +
		"further contributions (http://github.com/Dessimat0r/YatzyBot). " +
		"Running on PircBotX " + PircBotX.VERSION + "." + " Have fun! :)";
	;
	
	public static final String[] HELP_TEXT_ARR = new String[] {
		"Hello, I am YatzyBot " + VERSION + " :) Valid game actions: %prefix%play (add yourself as player), %prefix%start (start game, do this once all players have joined), %prefix%reset (reset game), %prefix%deleteplayer <player_name> (deletes a player if they stopped playing or left for some reason), %prefix%help (re-show help message)",
		"Valid rolling actions: %prefix%roll/%prefix%r {optional dice to reroll} (roll or re-roll particular dice), %prefix%hold/%prefix%h [all] {dice to hold} (hold particular dice when re-rolling), %prefix%choose/%prefix%c {SCORING_NAME} (choose scoring then finish your turn), %prefix%check/%prefix%ch (check scores)",
		"Please read gameplay information at http://en.wikipedia.org/wiki/Yatzy before playing!"
	};
	
	private String fixPrefix(String text) {
		return text.replace("%prefix%", "" + prefix);
	}
	
	private String[] fixPrefix(String[] text) {
		String[] newtext = new String[text.length];
		for (int i = 0; i < text.length; i++) {
			newtext[i] = fixPrefix(text[i]);
		}
		return newtext;
	}
	
	public String   initialHelpTextLocal;
	public String[] helpTextArrLocal;
	
	public void showInitialHelpMsg() {
		getBot().sendMessage(channelObj, initialHelpTextLocal);
	}
	
	public void updateLocalText() {
		initialHelpTextLocal = fixPrefix(INITIAL_HELP_TEXT);
		helpTextArrLocal     = fixPrefix(HELP_TEXT_ARR);
	}
	
	public void showHelpMsg(User user) {
		if (user == null) {
			for (String line : helpTextArrLocal) {
				getBot().sendMessage(channelObj, line);
			}
			return;
		}
		for (String line : helpTextArrLocal) {
			getBot().sendNotice(user, line);
		}
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
		return "YatzyBot[" + user.getStatusStr() + "]~" + user.getId() + "~" + user.getUserNickStr() + "@" + user.getUserServerStr() + "~" + channel;
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