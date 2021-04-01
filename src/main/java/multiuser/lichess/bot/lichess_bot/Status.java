package multiuser.lichess.bot.lichess_bot;

public enum Status {

	// game not yet started
	WAITING_FOR_CREATION,
	WAITING_FOR_ACCEPT,

	// game running
	WAITING_FOR_LICHESS,
	WAITING_FOR_BOT
}
