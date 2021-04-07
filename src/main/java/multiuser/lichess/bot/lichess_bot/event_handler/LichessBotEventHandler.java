package multiuser.lichess.bot.lichess_bot.event_handler;

import multiuser.lichess.bot.lichess_bot.lichess_data_objects.Challenge;

import java.util.HashMap;

public interface LichessBotEventHandler {
	void gameStart(HashMap<String, Thread> games, String gameId);

	void gameFinish(HashMap<String, Thread> games, String gameId);

	void challenge(Challenge challenge) throws InterruptedException;

	void challengeCanceled(Challenge challenge);

	void challengeDeclined(Challenge challenge);
}
