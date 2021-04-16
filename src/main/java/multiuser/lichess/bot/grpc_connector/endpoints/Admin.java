package multiuser.lichess.bot.grpc_connector.endpoints;

import io.grpc.stub.StreamObserver;
import me.tobiasliese.lichess_bot_protocol.AdministratingGrpc;
import me.tobiasliese.lichess_bot_protocol.FullUser;
import me.tobiasliese.lichess_bot_protocol.Status;
import me.tobiasliese.lichess_bot_protocol.User;
import multiuser.lichess.bot.lichess_bot.LichessGameManager;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class Admin extends AdministratingGrpc.AdministratingImplBase {

	private static final Map<Long, Thread> users = new HashMap<>();

	@Override
	public void logout(final User request, final StreamObserver<Status> responseObserver) {
		Logger.info(request);
		final var status = Status.newBuilder().setSuccess(true).build();
		responseObserver.onNext(status);
		responseObserver.onCompleted();
	}

	@Override
	public void login(final FullUser request, final StreamObserver<Status> responseObserver) {
		final var status = Status.newBuilder();
		final LichessGameManager gameManager = new LichessGameManager(request.getApiToken());
		boolean result = false;
		if (!users.containsKey(request.getUserId())) {
			try {
				final var thread = new Thread(() -> {
					try {
						gameManager.setup();
					} catch (final InterruptedException | ExecutionException e) {
						Thread.currentThread().interrupt();
					}
				});
				Thread.sleep(1000);
				result = true;
				users.put(request.getUserId(), thread);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		responseObserver.onNext(status.setSuccess(result).build());
	}
}
