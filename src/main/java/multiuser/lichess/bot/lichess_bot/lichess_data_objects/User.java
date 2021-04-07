package multiuser.lichess.bot.lichess_bot.lichess_data_objects;

record User(
		String id,
		String name,
		String title,
		short rating,
		boolean patron,
		boolean provisional,
		boolean online,
		short lag
) {
}
