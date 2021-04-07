package multiuser.lichess.bot.lichess_bot;

import multiuser.lichess.bot.game.Game;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.http.HttpRequest;


public class LichessBot {

	private final Game game = new Game();
	private final LichessHttp lichessHttp = new LichessHttp();
	private String gameId;
	private Status status = Status.WAITING_FOR_CREATION;
	private short receivedMoves = (short) 0;
	private boolean isBotToMove;

	public void createGame(String user) throws IOException, InterruptedException {
		Logger.info("creating game ");
		// be sure the bot is not already in a game
		if (gameId == null) {
			gameId = lichessHttp.createPost(
					"challenge/" + user, // path
					HttpRequest.BodyPublishers.ofString("{\"color\": \"black\"}")) // body
					.get("challenge").get("id").toString().replace("\"", "");
			status = Status.WAITING_FOR_ACCEPT;
		} else throw new UnsupportedOperationException("This bot is already in a game!");
	}

	private void setUpGame() throws IOException, InterruptedException {
		var moves = lichessHttp.getPlayedMoves(gameId);
		game.loadMoves(moves);
		if (moves.length % 2 != 0) isBotToMove = !isBotToMove;
		receivedMoves += moves.length;
	}

	public void playGame(String gameId, boolean isWhite) throws IOException, InterruptedException {
		isBotToMove = !isWhite;
		this.gameId = gameId;
		setUpGame();
		if (isBotToMove) status = Status.WAITING_FOR_BOT;
		else status = Status.WAITING_FOR_LICHESS;
		String move;
		do {
			Logger.debug(status);
			move = switch (status) {
				case WAITING_FOR_LICHESS -> waitForLichessMove();
				case WAITING_FOR_BOT -> waitForPlayerMove();
				default -> null;
			};
			try {
				if (move != null) {
					game.move(move);
					lichessHttp.makeMove(gameId, move);
				}
			} catch (IllegalArgumentException ignore) {
				Logger.error("could not perform move: " + move);
				status = Status.WAITING_FOR_RECOVERY;
			}
		} while (status.equals(Status.WAITING_FOR_BOT) || status.equals(Status.WAITING_FOR_LICHESS));
	}

	private String waitForLichessMove() throws InterruptedException {
		var move = lichessHttp.waitForMove(gameId, receivedMoves);
		status = Status.WAITING_FOR_BOT;
		receivedMoves++;
		return move;
	}

	private String waitForPlayerMove() {
		final var move = "f4h4";
		status = Status.WAITING_FOR_LICHESS;
		receivedMoves++;
		return move;
	}

	public Status getStatus() {
		return status;
	}
}
