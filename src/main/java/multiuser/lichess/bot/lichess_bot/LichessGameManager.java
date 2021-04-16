package multiuser.lichess.bot.lichess_bot;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import multiuser.lichess.bot.lichess_bot.event_handler.LichessBotEventHandler;
import multiuser.lichess.bot.lichess_bot.event_handler.LichessBotEventHandlerImpl;
import multiuser.lichess.bot.lichess_bot.lichess_data_objects.Response;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;

public class LichessGameManager {

	private static final JsonFactory jsonFactory = new JsonFactory().setCodec(new ObjectMapper());
	private final HashMap<String, Thread> games = new HashMap<>();
	private final LichessHttp lichessHttp;

	public LichessGameManager(String lichessApiToken) {
		lichessHttp = new LichessHttp(lichessApiToken);
	}

	public void setup() throws InterruptedException, ExecutionException {
		Logger.info("Setting up Lichess Event listener");
		lichessHttp.createEventListener(new StringFinder()).get().body();
	}

	public void clean() {
		for (var entry : games.entrySet()) if (!entry.getValue().isAlive()) games.remove(entry.getKey());
	}

	private class StringFinder implements Flow.Subscriber<String> {

		private final LichessBotEventHandler lichessBotEventHandler = new LichessBotEventHandlerImpl(lichessHttp);
		private Flow.Subscription subscription;

		@Override
		public void onSubscribe(Flow.Subscription subscription) {
			this.subscription = subscription;
			this.subscription.request(1);
		}

		@Override
		public void onNext(String line) {
			subscription.request(1);
			if (line.isBlank()) return;
			try (var parser = jsonFactory.createParser(line)) {
				parser.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
				var jsonTree = parser.readValueAsTree();
				if (jsonTree == null) return;

				switch (jsonTree.get("type").toString().replace("\"", "")) {
					case "gameStart" -> lichessBotEventHandler.
							gameStart(games, jsonTree.get("game").get("id").toString().replace("\"", ""));
					case "gameFinish" -> lichessBotEventHandler.
							gameFinish(games, jsonTree.get("game").get("id").toString().replace("\"", ""));
					case "challenge" -> lichessBotEventHandler.
							challenge(new ObjectMapper().readValue(line, Response.class).challenge());
					default -> Logger.warn("Unhandled Event: " + jsonTree.get("type").toString() + " received.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				exit();
			}
		}

		@Override
		public void onError(Throwable throwable) {
			//
		}

		@Override
		public void onComplete() {
			// nothing to be done here...
		}

		private void exit() {
			for (var entry : games.entrySet()) entry.getValue().interrupt();
			Thread.currentThread().interrupt();
		}
	}
}
