package multiuser.lichess.bot.lichess_bot.lichess_data_objects;


public record Challenge(
		String id,
		String url,
		String status,
		User challenger,
		User destUser,
		Variant variant,
		boolean rated,
		String speed,
		TimeControl timeControl,
		String color,
		Perf perf
) {
}
