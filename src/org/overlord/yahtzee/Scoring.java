// YatzyBot, licensed using GPLv3.
// =================================================
// Contributions to this file from:
// * Chris Dennett (dessimat0r@gmail.com)

package org.overlord.yahtzee;

public enum Scoring {
	ACES("Aces", "1", 6, true) {
		@Override
		public int checkScore(int[] totals) {
			return totals[1 - 1] * 1;
		}
	},
	TWOS("Twos", "2", 12, true) {
		@Override
		public int checkScore(int[] totals) {
			return totals[2 - 1] * 2;
		}
	},
	THREES("Threes", "3", 18, true) {
		@Override
		public int checkScore(int[] totals) {
			return totals[3 - 1] * 3;
		}
	},
	FOURS("Fours", "4", 24, true) {
		@Override
		public int checkScore(int[] totals) {
			return totals[4 - 1] * 4;
		}
	},
	FIVES("Fives", "5", 30, true) {
		@Override
		public int checkScore(int[] totals) {
			return totals[5 - 1] * 5;
		}
	},
	SIXES("Sixes", "6", 36, true) {
		@Override
		public int checkScore(int[] totals) {
			return totals[6 - 1] * 6;
		}
	},
	ONE_PAIR("1 Pair", "1P", 12, true) {
		@Override
		public int checkScore(int[] totals) {
			for (int i = 5; i >= 0; i--) {
				if (totals[i] >= 2) {
					return 2 * (i + 1);
				}
			}
			return 0;
		}
	},
	TWO_PAIRS("2 Pairs", "2P", 22, true) {
		@Override
		public int checkScore(int[] totals) {
			int twos = 0;
			int score = 0;
			for (int i = 5; i >= 0; i--) {
				if (totals[i] >= 2) {
					score += 2 * (i + 1);
					if (++twos >= 2) break;
				}
			}
			if (twos >= 2) return score;
			return 0;
		}
	},
	THREE_OF_A_KIND("3 of a Kind", "3K", 18, true) {
		@Override
		public int checkScore(int[] totals) {
			for (int i = 5; i >= 0; i--) {
				if (totals[i] >= 3) {
					return 3 * (i + 1);
				}
			}
			return 0;
		}
	},
	FOUR_OF_A_KIND("4 of a Kind", "4K", 24, true) {
		@Override
		public int checkScore(int[] totals) {
			for (int i = 5; i >= 0; i--) {
				if (totals[i] >= 4) {
					return 4 * (i + 1);
				}
			}
			return 0;
		}
	},	
	SMALL_STRAIGHT("Small Straight", "SS", 15, false) {
		@Override
		public int checkScore(int[] totals) {
			boolean yeye = true;
			for (int i = 0; i < 5; i++) {
				if (totals[i] == 0) { yeye = false; break; }
			}
			if (yeye) return getMax();
			return 0;
		}
	},
	LARGE_STRAIGHT("Large Straight", "LS", 20, false) {
		@Override
		public int checkScore(int[] totals) {
			boolean yeye = true;
			for (int i = 1; i < 6; i++) {
				if (totals[i] == 0) { yeye = false; break; }
			}
			if (yeye) return getMax();
			return 0;
		}
	},
	HOUSE("House", "H", 28, true) {		
		@Override
		public int checkScore(int[] totals) {
			int score = 0;
			boolean foundTwo = false, foundThree = false;
			for (int i = 5; i >= 0; i--) {
				boolean used = false;
				if (!used && totals[i] >= 3 && !foundThree) {
					score += 3 * (i + 1);
					foundThree = true;
					used = true;
				}
				if (!used && totals[i] >= 2 && !foundTwo) {
					score += 2 * (i + 1);
					foundTwo = true;
					used = true;
				}
				if (foundThree && foundTwo) break;
			}
			if (foundThree && foundTwo) return score;
			return 0;
		}
	},
	YATZY("Yatzy!", "Y", 50, false) {
		@Override
		public int checkScore(int[] totals) {
			boolean yeye = false;
			for (int i = 0; i < 6; i++) {
				if (totals[i] == 5) {
					yeye = true; break;
				} else if (totals[i] > 0) {
					break;
				}
			}
			if (yeye) return 50;
			return 0;
		}
	},	
	CHANCE("Chance", "C", 36, true) {
		@Override
		public int checkScore(int[] totals) {
			int total = 0;
			for (int i = 0; i < totals.length; i++) {
				total += totals[i] * (i + 1);
			}
			return total;
		}
	}	;
	
	protected final String  name;
	protected final String  abrv;
	protected final int     max;
	protected final boolean variable;
	
	private Scoring(String name, String abrv, int max, boolean variable) {
		this.name = name;
		this.abrv = abrv;
		this.max  = max;
		this.variable = variable;
	}
	
	public boolean isVariable() {
		return variable;
	}
	
	public int getMax() {
		return max;
	}
	
	public String getName() {
		return name;
	}
	
	public String getAbrv() {
		return abrv;
	}
	
	public abstract int checkScore(int[] totals);
}
