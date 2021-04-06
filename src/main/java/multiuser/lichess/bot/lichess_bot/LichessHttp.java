package multiuser.lichess.bot.lichess_bot;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static java.net.http.HttpRequest.BodyPublishers.noBody;

class LichessHttp {

	private static class StringFinder implements Flow.Subscriber<String> {

		private static class StringContainer {
			public String string = "";

			@Override
			public String toString() {
				return string;
			}
		}

		private Flow.Subscription subscription;
		final StringContainer event = new StringContainer();

		@Override
		public void onSubscribe(Flow.Subscription subscription) {
			this.subscription = subscription;
			this.subscription.request(1);
		}

		@Override
		public void onNext(String line) {
			subscription.request(1);
			event.string = line;

			if (line.isBlank()) {
				// subscription.request(1);
				return;
			}

			synchronized (event) {
				event.notifyAll();
			}
		}


		@Override
		public void onError(Throwable ex) {
			ex.printStackTrace();
		}

		@Override
		public void onComplete() {
			// entire body was processed, but term was not found;
			// TODO: expose negative result
		}
	}

	static final int API_DELAY = 20000;

	static final URI LICHESS_URL = URI.create("https://lichess.org/");

	static final String LICHESS_API_TOKEN = "CRTHHCQYACU245j6";

	static long lastRequest;

	JsonFactory jsonFactory = new JsonFactory().setCodec(new ObjectMapper());
	HttpClient client = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_2)
			.followRedirects(HttpClient.Redirect.ALWAYS)
			.build();

	HttpRequest.Builder getRequestBuilder(String path) {
		return HttpRequest.newBuilder(LICHESS_URL.resolve(path))
				.header("Authorization", "Bearer " + LICHESS_API_TOKEN);
	}

	TreeNode createRequest(String path, String method, HttpRequest.BodyPublisher bodyPublisher) throws IOException, InterruptedException {
		String body = null;
		do {
			if (lastRequest - ( System.currentTimeMillis() + API_DELAY) > 0 )
				Thread.sleep(lastRequest - ( System.currentTimeMillis() + API_DELAY ) );
			if (body != null) Thread.sleep(API_DELAY);

			body = client.send(
					getRequestBuilder(path)
							.timeout(Duration.ofSeconds(1))
							.method(method, bodyPublisher)
							.header("Content-Type", "application/json")
							.header("Accept", "application/json")
							.build(),
					HttpResponse.BodyHandlers.ofString()
			).body();

			lastRequest = System.currentTimeMillis();
		} while (body.equals("Too many requests. Please retry in a moment."));
		TreeNode json;
		try (var jsonParser = jsonFactory.createParser(body)) {
			json = jsonParser.readValueAsTree();
			return json;
		} catch (JsonParseException ignore) {
			System.err.println(LICHESS_URL.resolve(path));
			System.out.println(body);
			return null;
		}
	}

	TreeNode createPost(String path, HttpRequest.BodyPublisher bodyPublisher) throws IOException, InterruptedException {
		return createRequest(path, "POST", bodyPublisher);
	}

	TreeNode createPost(String path) throws IOException, InterruptedException {
		return createPost(path, HttpRequest.BodyPublishers.ofString("{}"));
	}

	TreeNode createGet(String path) throws IOException, InterruptedException {
		return createRequest(path, "GET", noBody());
	}

	String[] waitForMoves(String gameId, short receivedMoves) throws InterruptedException {
		StringFinder finder = new StringFinder();
		HttpRequest request = getRequestBuilder("api/bot/game/stream/" + gameId).GET().build();

		if (lastRequest - ( System.currentTimeMillis() + API_DELAY) > 0 )
			Thread.sleep(lastRequest - ( System.currentTimeMillis() + API_DELAY ) );

		synchronized (finder.event) {
			client.sendAsync(request, HttpResponse.BodyHandlers.fromLineSubscriber(finder));
			lastRequest = System.currentTimeMillis();
			try {
				String[] moves = new String[0];
				do {
					finder.event.wait();
					try (var jsonParser = jsonFactory.createParser(finder.event.string)) {
						TreeNode jsonTree = jsonParser.readValueAsTree();
						jsonTree = jsonTree.get("state");
						if (jsonTree == null) continue;
						jsonTree = jsonTree.get("moves");
						if (jsonTree == null) continue;
						moves = jsonTree.toString().replace("\"", "").split(" ");
					}
				} while ( moves.length <= receivedMoves );
				return Arrays.copyOfRange(moves, receivedMoves, moves.length);
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	String waitForMove(String gameId, short receivedMoves) throws InterruptedException {
		return waitForMoves(gameId, receivedMoves)[0];
	}

	TreeNode makeMove(String gameId, String move) {
		try {
			return createPost("api/bot/game/" + gameId + "/move/" + move);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	String[] getPlayedMoves(String gameId) throws IOException, InterruptedException {
		var result = createGet("game/export/" + gameId);
		List<String> moves = new LinkedList<>(
				Arrays.asList(
						result
								.get("moves")
								.toString()
								.replace("\"", "")
								.split(" ")
				)
		);

		List<String> missingMoves = new LinkedList<>(Arrays.asList(waitForMoves(gameId, (short) moves.size())));
		var attempts = 0;
		while (missingMoves.size() < 3 && attempts < 3) {
			missingMoves.addAll(Arrays.stream(waitForMoves(gameId, (short) (moves.size() + missingMoves.size()))).toList());
			attempts++;
		}
		moves.addAll(missingMoves);
		return moves.toArray(new String[0]);
	}

	CompletableFuture<HttpResponse<Void>> createEventListener(Flow.Subscriber<? super String> subscriber) {
		HttpRequest request = getRequestBuilder("api/stream/event").GET().build();
		return client.sendAsync(request, HttpResponse.BodyHandlers.fromLineSubscriber(subscriber));
	}
}