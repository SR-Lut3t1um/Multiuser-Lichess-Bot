package multiuser.lichess.bot.lichess_bot;

import java.io.IOException;
import static multiuser.lichess.bot.lichess_bot.LichessHttp.API_DELAY;


public class LichessBot {

	LichessHttp lichessHttp = new LichessHttp();
	String gameId;
	private Status status = Status.WAITING_FOR_CREATION;

	public void createGame(String user) throws IOException, InterruptedException {
		// be sure the bot is not already in a game
		if (gameId == null) {
			gameId = lichessHttp.createPost("challenge/" + user).get("challenge").get("id").toString().replace("\"", "");
			System.out.println(gameId);
			status = Status.WAITING_FOR_ACCEPT;
		} else {
			throw new UnsupportedOperationException("This bot is already in a game!");
		}
		waitForPlayerMove();
	}

	private void waitForPlayerMove() throws IOException, InterruptedException {
		do {
			System.out.println(lichessHttp.createGet("bot/game/stream/" + gameId));
			Thread.sleep(API_DELAY);
		} while (true);
	}

	public Status getStatus() {
		return status;
	}
}
