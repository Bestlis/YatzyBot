YatzyBot 0.812
==============

A bot for playing the popular European Yatzy dice game in IRC (uses Java, PircBotX) with one or more players. Supports the following:

* Rolling all dies.
* Rolling particular dies (just specify numbers on the dies, e.g., 5 6 1 1).
* Holding particular dies (again, just specify the numbers, not the die indices).
* Holding all of particular dies (e.g., 2 3 will match all of the 2 dies and all of the 3 dies).
* Full score tracking, will choose the best dies to use automatically for the chosen scoring.
* Viewing of all possible scoring, including scores.
* Output after each roll shows which are good scorings, great scorings, and no scorings.
* Games cannot be terminated by those not playing the game.

Limitations at the moment include:

* Can only join one IRC server and channel, use multiple instances of the bot to join more.
* If everyone in a game quits, the bot does not realise yet and terminate the game - better to wait some minutes just to make sure they are really gone!
* Can't specify particular scorings with 'hardcoded dice' to hold, such as Small Straight or Large Straight (this would look like `.hold LS` to hold precisely the dice numbered 1, 2, 3, 4 and 5, ignoring the rest). A nice shortcut. This feature will almost certainly arrive in future versions.

Usage:

`java -jar yatzybot.jar [server] [channel]`. If running on a shell via SSH, it is recommended you use `nohup` to prevent SSH hangup from terminating the bot.

License:

Licensed under the GPL v3.