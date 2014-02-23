package org.overlord.yahtzee;

public class PercTest {
	public static void main(String[] args) {
		for (int i = 0; i <= Yahtzee.MAX_SCORING; i++) {
			System.out.println("#" + i + ": " + Yahtzee.getGoodScorePerc(i));
		}
	}
}
