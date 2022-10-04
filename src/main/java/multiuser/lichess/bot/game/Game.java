package multiuser.lichess.bot.game;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;
import com.github.bhlangonijr.chesslib.move.MoveList;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Game {

	private static final Supplier<IllegalArgumentException> illegalMove = () ->
			new IllegalArgumentException("move is not legal!");
	private static final Supplier<IllegalArgumentException> noLegalMove = () ->
			new IllegalArgumentException("no legal move was received!");


	/**
	 * This current position/board of the game
	 */
	private final Board board = new Board();
	/**
	 * The GameID that lichess has assigned to this game
	 * This field may be null this means that this game was not yet created at lichess
	 */
	String lichessGameId;
	/**
	 * The moves that were played
	 */
	LinkedList<Move> moves = new LinkedList<>();
	/**
	 * All votes for moves that were legal
	 */
	LinkedList<Map<String, List<String>>> votes = new LinkedList<>();

	/**
	 * This method takes an array of moves and removes every entry that is not a legal move
	 * it then creates a map of all moves as keys and a list of the player(s) that voted for it as the value.
	 *
	 * @param moves the moves that shall be filtered and sorted
	 * @return a map with the move as a key and a list of every user that wanted to make the move.
	 */
	private Map<String, List<String>> filterInvalidMoves(Move[] moves) {
		return Arrays.stream(moves).filter(move ->
				board.legalMoves().stream().anyMatch(legalMove ->
						legalMove.toString().equals(move.move()))
		).collect(Collectors.groupingBy(Move::move, Collectors.mapping(Move::playerName, Collectors.toList())));
	}

	public void loadFen(String fen) {
		board.loadFromFen(fen);
	}

	public void loadMoves(String[] moves) {
		MoveList moveList = new MoveList();
		moveList.loadFromSan(String.join(" ", moves));
		board.loadFromFen(moveList.getFen());
	}

	public void move(String move) {
		Map<String, com.github.bhlangonijr.chesslib.move.Move> sanMoves = new HashMap<>();
		Map<String, com.github.bhlangonijr.chesslib.move.Move> lanMoves = new HashMap<>();
		for (com.github.bhlangonijr.chesslib.move.Move legalMove : MoveGenerator.generateLegalMoves(board)) {
			sanMoves.put(legalMove.getSan(), legalMove);
			lanMoves.put(legalMove.toString(), legalMove);
		}
		board.doMove(
				board.legalMoves().stream().filter(m -> m.toString().equals(move)).findAny().orElseThrow(illegalMove)
		);
	}

	public void move(Move[] moves) {
		var move = filterInvalidMoves(moves).entrySet().stream()
				.max(Comparator.comparing(entry -> entry.getValue().size())).orElseThrow(noLegalMove);
		move(move.getKey());
	}
}
