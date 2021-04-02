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
				subscription.request(1);
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

	String waitForMove(String path, short receivedMoves) throws IOException {
		StringFinder finder = new StringFinder();
		HttpRequest request = getRequestBuilder(path).GET().build();

		synchronized (finder.event) {
			client.sendAsync(request, HttpResponse.BodyHandlers.fromLineSubscriber(finder));
			try {
				String[] moves;
				do {
					finder.event.wait();
					try (var jsonParser = jsonFactory.createParser(finder.event.string)) {
						TreeNode jsonTree = jsonParser.readValueAsTree();
						moves = jsonTree.get("state").get("moves").toString().replace("\"", "").split(" ");
					}
				} while (moves.length <= receivedMoves);
				return moves[receivedMoves];
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	TreeNode makeMove(String gameId, String move) {
		try {
			return createPost("bot/game/" + gameId + "/move/" + move);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	String[] getPlayedMoves(String gameID) throws IOException, InterruptedException {
		var tree = createGet("game/export/" + gameID);
		return tree.get("moves").toString().replace("\"", "").split(" ");
	}

	CompletableFuture<HttpResponse<Void>> createEventListener(Flow.Subscriber<? super String> subscriber) {
		HttpRequest request = getRequestBuilder("api/stream/event").GET().build();
		return client.sendAsync(request, HttpResponse.BodyHandlers.fromLineSubscriber(subscriber));
	}
}