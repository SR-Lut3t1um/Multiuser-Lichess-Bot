package multiuser.lichess.bot.lichess_bot;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;

public class LichessGameManager {

	static JsonFactory jsonFactory = new JsonFactory().setCodec(new ObjectMapper());
	HashMap<String, Thread> games = new HashMap<>();
	LichessHttp lichessHttp = new LichessHttp();

	private class StringFinder implements Flow.Subscriber<String> {

		private Flow.Subscription subscription;

		@Override
		public void onSubscribe(Flow.Subscription subscription) {
			this.subscription = subscription;
			this.subscription.request(1);
		}

		@Override
		public void onNext(String lines) {
			this.subscription.request(1);
			if (lines.isBlank()) return;
			try (var parser = jsonFactory.createParser(lines)) {
				var jsonTree = parser.readValueAsTree();
				if (jsonTree == null) return;
				if ("\"gameStart\"".equals(jsonTree.get("type").toString())) {
					games.put(jsonTree.get("game").get("id").toString(), new Thread(
							() -> {
								LichessBot lichessBot = new LichessBot();
								var gameId = jsonTree.get("game").get("id").toString();
								gameId = gameId.substring(1, gameId.length() - 1);
								lichessBot.playGame(gameId, true);
							}
					));
					games.get(jsonTree.get("game").get("id").toString()).start();
				} else {
					synchronized (System.out) {
						System.out.println("Unhandled Event" + jsonTree.get("type").toString() + "received.");
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		@Override
		public void onError(Throwable ex) {
			// TODO: expose the error
		}

		@Override
		public void onComplete() {
			// entire body was processed, but term was not found;
			// TODO: expose negative result
		}
	}

	public void setup() throws InterruptedException {
		synchronized (System.out) {
			System.out.println("Setting up Lichess Event listener");
		}
		try {
			lichessHttp.createEventListener(new StringFinder()).get();
		} catch (CompletionException | ExecutionException ignore) {
			// TODO: proper error handling
		}
	}

	public void clean() {
		for (var entry: games.entrySet()) {
			if (! entry.getValue().isAlive()) {
				games.remove(entry.getKey());
			}
		}
	}
}
