package multiuser.lichess.bot;

import multiuser.lichess.bot.game.Game;
import multiuser.lichess.bot.game.Move;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GameTest {
	private final Game game = new Game();

	@Test
	void GameLegalMoveFilter() {
		var moves = new Move[3];
		moves[0] = new Move("user1", "e2e4");
		moves[1] = new Move("user2", "d2a4");
		moves[2] = new Move("user3", "a2a4");
		assertDoesNotThrow(() -> game.move(moves));
	}
}
