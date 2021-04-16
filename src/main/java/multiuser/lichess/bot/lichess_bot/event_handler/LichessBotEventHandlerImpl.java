package multiuser.lichess.bot.lichess_bot.event_handler;

import multiuser.lichess.bot.lichess_bot.LichessBot;
import multiuser.lichess.bot.lichess_bot.LichessHttp;
import multiuser.lichess.bot.lichess_bot.lichess_data_objects.Challenge;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.HashMap;

public class LichessBotEventHandlerImpl implements LichessBotEventHandler {
	private final LichessHttp lichessHttp;

	public LichessBotEventHandlerImpl(LichessHttp lichessHttp) {
		this.lichessHttp = lichessHttp;
	}

	@Override
	public void gameStart(HashMap<String, Thread> games, String gameId) {
		games.put(gameId, new Thread(
				() -> {
					LichessBot lichessBot = new LichessBot(lichessHttp);
					try {
						lichessBot.playGame(gameId, true);
					} catch (IOException | InterruptedException ignore) {
						Thread.currentThread().interrupt();
					}
				}
		));
		games.get(gameId).start();
	}

	@Override
	public void gameFinish(HashMap<String, Thread> games, String gameId) {
		games.get(gameId).interrupt();
		games.remove(gameId);
	}

	@Override
	public void challenge(Challenge challenge) throws InterruptedException {
		try {
			lichessHttp.acceptChallenge(challenge.id());
		} catch (IOException ignore) {
			Logger.error("IO Error while trying to open a HTTP connection");
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void challengeCanceled(Challenge challenge) {
		// nothing to do here
	}

	@Override
	public void challengeDeclined(Challenge challenge) {
		// nothing to do here
	}
}
