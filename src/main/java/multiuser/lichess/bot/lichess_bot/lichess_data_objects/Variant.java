package multiuser.lichess.bot.lichess_bot.lichess_data_objects;

import com.fasterxml.jackson.annotation.JsonAlias;

record Variant(String key, String name, @JsonAlias("short") String abbreviation) {
}
