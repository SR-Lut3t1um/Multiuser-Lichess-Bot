package multiuser.lichess.bot.grpc_connector.endpoints;

import io.grpc.stub.StreamObserver;
import me.tobiasliese.lichess_bot_protocol.FullUser;
import me.tobiasliese.lichess_bot_protocol.Status;
import me.tobiasliese.lichess_bot_protocol.UsersGrpc;
import multiuser.lichess.bot.lichess_bot.LichessGameManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class Admin extends UsersGrpc.UsersImplBase {

	private static final Map<Long, Thread> users = new HashMap<>();

	@Override
	public void add(FullUser request, StreamObserver<Status> responseObserver) {
		var status = Status.newBuilder();
		LichessGameManager gameManager = new LichessGameManager(request.getApiToken());
		boolean result = false;
		if (!users.containsKey(request.getUserId())) try {
			var thread = new Thread(() -> {
				try {
					gameManager.setup();
				} catch (InterruptedException | ExecutionException e) {
					Thread.currentThread().interrupt();
				}
			});
			Thread.sleep(1000);
			result = true;
			users.put(request.getUserId(), thread);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		responseObserver.onNext(status.setSuccess(result).build());
	}
}
