package multiuser.lichess.bot.lichess_bot;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

class LichessHttp {

	private static class StringFinder implements Flow.Subscriber<String> {

		private final String term;
		private Flow.Subscription subscription;
		private final CompletableFuture<Boolean> found = new CompletableFuture<>();

		private StringFinder(String term) {
			this.term = term;
		}

		@Override
		public void onSubscribe(Flow.Subscription subscription) {
			this.subscription = subscription;
			this.subscription.request(1);
		}

		@Override
		public void onNext(String line) {
			System.out.println(line);
			subscription.request(1);
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

		public CompletableFuture<Boolean> found() {
			return found;
		}
	}

	static final int API_DELAY = 20000;

	static final URI LICHESS_API_URL = URI.create("https://lichess.org/api/");

	static final String LICHESS_API_TOKEN = "CRTHHCQYACU245j6";

	JsonFactory jsonFactory = new JsonFactory().setCodec(new ObjectMapper());
	HttpClient client = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_2)
			.followRedirects(HttpClient.Redirect.ALWAYS)
			.build();

	HttpRequest.Builder getRequestBuilder(String path) {
		System.out.println(LICHESS_API_URL.resolve(path));
		return HttpRequest.newBuilder(LICHESS_API_URL.resolve(path))
				.header("Authorization", "Bearer " + LICHESS_API_TOKEN);
	}

	TreeNode createPost(String path) throws IOException, InterruptedException {
		return jsonFactory.createParser(client.send(getRequestBuilder(path).POST(HttpRequest.BodyPublishers.noBody())
				.build(), HttpResponse.BodyHandlers.ofString()).body()).readValueAsTree();
	}

	TreeNode createGet(String path) {
		StringFinder finder = new StringFinder("\n");
		HttpRequest request = HttpRequest.newBuilder(LICHESS_API_URL.resolve(path)).GET().build();
		client.sendAsync(request, HttpResponse.BodyHandlers.fromLineSubscriber(finder));
		finder
				.found()
				.exceptionally(__ -> false)
				.thenAccept(found -> System.out.println(
						"Completed " + LICHESS_API_URL.resolve(path) + " / found: " + found));
		// todo use body subsriber
		return null;
	}
}