package multiuser.lichess.bot.lichess_bot;

import multiuser.lichess.bot.game.Game;

import java.io.IOException;
import java.net.http.HttpRequest;


public class LichessBot {

	LichessHttp lichessHttp = new LichessHttp();
	String gameId;
	private Status status = Status.WAITING_FOR_CREATION;
	private final Game game = new Game();
	private short receivedMoves = (short) 0;

	public void createGame(String user) throws IOException, InterruptedException {
		synchronized (System.out) {
			System.out.println("creating game ");
		}
		// be sure the bot is not already in a game
		if (gameId == null) {
			gameId = lichessHttp.createPost(
					"challenge/" + user, // path
					HttpRequest.BodyPublishers.ofString("{\"color\": \"black\"}")) // body
					.get("challenge").get("id").toString().replace("\"", "");
			status = Status.WAITING_FOR_ACCEPT;
		} else {
			throw new UnsupportedOperationException("This bot is already in a game!");
		}
	}

	public void playGame(String gameId, boolean isWhite) throws IOException {
		this.gameId = gameId;
		if (isWhite)
			status = Status.WAITING_FOR_LICHESS;
		else
			status = Status.WAITING_FOR_BOT;
		String move;
		do {
			switch (status) {
				case WAITING_FOR_LICHESS -> move = waitForLichessMove();
				case WAITING_FOR_BOT -> move = waitForPlayerMove();
				default -> {
					return;
				}
			}
			game.move(move);
		} while (status.equals(Status.WAITING_FOR_BOT) || status.equals(Status.WAITING_FOR_LICHESS));
	}

	private String waitForLichessMove() throws IOException {
		var move = lichessHttp.waitForMove("bot/game/stream/" + gameId, receivedMoves);
		status = Status.WAITING_FOR_BOT;
		receivedMoves++;
		return move;
	}

	private String waitForPlayerMove() {
		var move = "e7e5";
		status = Status.WAITING_FOR_LICHESS;
		receivedMoves++;
		System.out.println(lichessHttp.makeMove(gameId, move));
		return move;
	}

	public Status getStatus() {
		return status;
	}
}
