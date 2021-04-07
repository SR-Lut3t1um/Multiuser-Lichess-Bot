package multiuser.lichess.bot.lichess_bot.lichess_data_objects;

record TimeControl(
		String type,
		int limit,
		short increment,
		String show
) {
}
