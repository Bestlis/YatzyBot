// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Yahtzee {
	public static Map<String, Scoring> SCORING_MAP = new HashMap<String, Scoring>();
	public static Map<String, Scoring> SCORING_ABBRV_MAP = new HashMap<String, Scoring>();
	{
		for (Scoring s : Scoring.values()) {
			SCORING_MAP.put(s.name(), s);
			SCORING_ABBRV_MAP.put(s.getAbrv().toLowerCase(), s);
		}
	}
	protected final List<YatzyListener> listeners = new CopyOnWriteArrayList<YatzyListener>();
	
	protected final Map<String, Player> playerMap = new HashMap<String, Player>();
	protected final List<Player> players = new ArrayList<Player>();
	protected final Die[] dice = new Die[5];
	{
		for (int i = 0; i < dice.length; i++) {
			dice[i] = new Die();
		}
	}
	protected Turn turn;
	protected boolean started  = false;
	protected boolean finished = false;
	
	public Yahtzee() {

	}

	public synchronized boolean[] rollNumbers(int[] which) throws GameCompleteException, DieNumberNotFoundException {
		if (finished) throw new GameCompleteException("Game complete!");
		
		boolean[] toRoll = new boolean[5];
		for (int i = 0; i < which.length; i++) {
			int numToRoll = which[i];
			// find the number
			boolean found = false;
			for (int j = 0; j < 5; j++) {
				if (!toRoll[j] && dice[j].getFaceValue() == numToRoll) {
					toRoll[j] = true;
					found = true;
					break;
				}
			}
			if (!found) {
				throw new DieNumberNotFoundException(
					"Couldn't find die index of " + numToRoll +
					" to roll again! (arr: " + Arrays.toString(which) +
					", index: " + i + ")", numToRoll
				);
			}
		}
		for (int i = 0; i < toRoll.length; i++) {
			if (toRoll[i]) {
				dice[i].roll();
			}
		}
		Arrays.sort(dice);
		return toRoll;
	}
	
	public synchronized void rollAll() {
		for (int i = 0; i < dice.length; i++) {
			dice[i].roll();
		}
		Arrays.sort(dice);
	}
	
	public synchronized Map<Scoring, Integer> getRollScores() {
		Map<Scoring, Integer> map = new EnumMap<Scoring, Integer>(Scoring.class);
		for (Scoring s : Scoring.values()) {
			map.put(s, s.checkScore(vals2Scores(dice2Vals(dice, null), null)));
		}
		return map;
	}

	public static final int[] dice2Vals(Die[] dice, int[] store) {
		if (store == null) store = new int[5];
		for (int i = 0; i < dice.length; i++) {
			store[i] = dice[i].getFaceValue();
		}
		return store;
	}

	public static final int[] vals2Scores(int[] vals, int[] store) {
		if (store == null) store = new int[6];
		for (int i = 0; i < vals.length; i++) {
			store[vals[i] - 1]++;
		}
		return store;
	}

	public final String getDiceStr() {
		return Arrays.toString(dice);
	}
	
	public final String getSortedDiceStr() {
		int[] vals = dice2Vals(dice, null);
		Arrays.sort(vals);
		return Arrays.toString(vals);
	}
	
	public synchronized void resetDice() {
		for (int i = 0; i < dice.length; i++) {
			dice[i].reset();
		}
	}
	
	public synchronized void addPlayer(Player p) throws GameStartedException {
		if (started) throw new GameStartedException("Cannot add players after game start!");
		
		this.players.add(p);
		this.playerMap.put(p.getName(), p);
		
		for (YatzyListener l : listeners) {
			l.onAddPlayer(p);
		}		
	}

	public Die[] getDice() {
		return dice;
	}
	
	public Turn getTurn() {
		return turn;
	}
	
	public synchronized void start() throws GameStartedException, GameCompleteException {
		if (started) throw new GameStartedException();
		if (finished) throw new GameCompleteException();
		started = true;
		for (YatzyListener l : listeners) {
			l.onStart(this);
		}
		turn = new Turn(this, players.get(0));
		for (YatzyListener l : listeners) {
			l.onTurn(turn);
		}
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public void addListener(YatzyListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(YatzyListener listener) {
		listeners.remove(listener);
	}

	synchronized void turnDone(Turn turn) {
		int index = players.indexOf(turn.getPlayer());
		int next = ++index % players.size();
		
		if (next == 0) {
			boolean found = false;
			for (Integer s : turn.getPlayer().getTotals().values()) {
				if (s == -1) {
					found = true;
					break;
				}
			}
			if (!found) {
				// finish game
				finished = true;
				for (YatzyListener l : listeners) {
					l.onGameComplete(this);
				}
				return;
			}
		}
		
		resetDice();
		Player nextPlayer = players.get(next);
		this.turn = new Turn(this, nextPlayer);
		for (YatzyListener l : listeners) {
			l.onTurn(this.turn);
		}
	}

	public synchronized void turnComplete(Turn turn2) {
		for (YatzyListener l : listeners) {
			l.onTurnComplete(turn);
		}		
	}

	public Map<String, Player> getPlayerMap() {
		return playerMap;
	}
	
	public List<Player> getPlayers() {
		return players;
	}

	public synchronized void removePlayer(String name) throws PlayerNotExistsException {
		Player p = playerMap.get(name);
		if (p == null) throw new PlayerNotExistsException("Player " + name + " doesn't exist!");
		if (turn != null && turn.getPlayer() == p) {
			turnComplete(turn);
			turnDone(turn);
		}
		playerMap.remove(name);
		players.remove(p);
		for (YatzyListener l : listeners) {
			l.onRemovePlayer(p);
		}
	}
	
	public static final int MAX_SCORING;
	static {
		int max = 0;
		for (Scoring s : Scoring.values()) {
			if (s.getMax() > max) max = s.getMax();
		}
		MAX_SCORING = max;
	}
	public static final double getGoodScorePerc(Scoring s) {
		return getGoodScorePerc(s.getMax());
	}
	public static final double getGoodScorePerc(int max) {
		double ofMax = (double)max / (double)MAX_SCORING;
		double perc = Math.log(ofMax+1)/Math.log(5);
		return perc;
	}

	public void dispose() {
		// TODO Auto-generated method stub
		
	}
}
