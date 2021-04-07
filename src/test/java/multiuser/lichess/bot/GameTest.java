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
		moves[0] = new Move("user1", "e7e5");
		moves[1] = new Move("user2", "a7a5");
		moves[2] = new Move("user3", "a7a5");
		assertDoesNotThrow(() -> game.move(moves));
	}
}
