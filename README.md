YatzyBot 0.87 (21-03-2014)
==========================

A bot for playing the popular European Yatzy dice game in IRC (uses Java, PircBotX) with one or more players. Supports the following:

* Administrative functionality with username/password login, allowing joining different servers and channels from one application instance. (0.86+)
* Rolling all dies.
* Rolling particular dies (just specify numbers on the dies, e.g., 5 6 1 1).
* Holding particular dies (again, just specify the numbers, not the die indices).
* Holding all of particular dies (e.g., 2 3 will match all of the 2 dies and all of the 3 dies).
* Full score tracking, will choose the best dies to use automatically for the chosen scoring.
* Viewing of all possible scoring, including scores.
* Output after each roll shows which are good scorings, great scorings, and no scorings.
* Different command prefixes for different channels (0.87+).
* Games cannot be terminated by those not playing the game.

Limitations at the moment include:

* If everyone in a game quits, the bot does not realise yet and terminate the game - better to wait some minutes just to make sure they are really gone! (~0.87+)
* Can't specify particular scorings with 'hardcoded dice' to hold, such as Small Straight or Large Straight (this would look like `.hold SS` to hold precisely the dice numbered 1, 2, 3, 4 and 5, ignoring the rest). (~0.88+)

Usage:

`java -jar yatzybot.jar` if not the first time running, otherwise: `java -jar yatzybot.jar <server id> <server host> <admin username> <admin password>` to allow the bot to join a server for further configuration. If running on a shell via SSH, it is recommended you use `nohup` to prevent SSH hangup from terminating the bot. Default nick is 'YatzyBot'.

Following this, privately message it and send `pass <username> <password>` to authenticate, then get it to join a channel with `join {server id} <channel name>`. Alternatively, get it to join another server with `addserver <server id> <server host> {channel1,channel2...} {nick}`.

Example initial command line: `java -jar yatzybot.jar YourServer irc.yourserver.net mankocity tokyorulezz`.

Requirements:

* Java 6+
* Few hardware requirements, very small footprint. Would probably run on a C64 if compiled.

License:

Licensed under the GPL v3.