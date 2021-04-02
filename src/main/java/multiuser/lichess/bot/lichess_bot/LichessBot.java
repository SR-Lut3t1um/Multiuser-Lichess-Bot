package multiuser.lichess.bot.lichess_bot;

import multiuser.lichess.bot.game.Game;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.Arrays;


public class LichessBot {

	LichessHttp lichessHttp = new LichessHttp();
	String gameId;
	private Status status = Status.WAITING_FOR_CREATION;
	private final Game game = new Game();
	private short receivedMoves = (short) 0;
	private boolean isBotToMove;

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

	private void setUpGame() throws IOException, InterruptedException {
		var moves = lichessHttp.getPlayedMoves(gameId);
		System.out.println(game);
		System.out.println(Arrays.toString(moves));
		for (String move: moves) {
			try {
				game.move(move);
			} catch (IllegalArgumentException ignore) {
				Thread.currentThread().interrupt();
			}
		}
		isBotToMove = ! isBotToMove;
	}

	public void playGame(String gameId, boolean isWhite) throws IOException, InterruptedException {
		isBotToMove = isWhite;
		this.gameId = gameId;
		setUpGame();
		if (isBotToMove)
			status = Status.WAITING_FOR_BOT;
		else
			status = Status.WAITING_FOR_LICHESS;
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
		var move = lichessHttp.waitForMove("api/bot/game/stream/" + gameId, receivedMoves);
		status = Status.WAITING_FOR_BOT;
		receivedMoves++;
		return move;
	}

	private String waitForPlayerMove() {
		var move = "b8c6";
		status = Status.WAITING_FOR_LICHESS;
		receivedMoves++;
		System.out.println(lichessHttp.makeMove(gameId, move));
		return move;
	}

	public Status getStatus() {
		return status;
	}
}
