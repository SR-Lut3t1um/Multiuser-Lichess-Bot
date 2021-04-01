package multiuser.lichess.bot;

import multiuser.lichess.bot.game.Game;
import multiuser.lichess.bot.game.Move;
import org.junit.jupiter.api.Test;

public class GameTest {
	Game game = new Game();

	@Test
	public void GameLegalMoveFilter() {
		Move[] moves = new Move[1];
		moves[0] = new Move("user1", "e2e4");
		game.move(moves);
		moves = new Move[3];
		moves[0] = new Move("user1", "e7e5");
		moves[1] = new Move("user2", "a7a5");
		moves[2] = new Move("user3", "a7a5");
		game.move(moves);
	}
}
