package fr.ptlc.anonymousdate;

import java.util.function.Function;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class ReactionCommand {
	
	private String emoji;
	private Function<CommandsListener, Function<User, Function<MessageChannel, Boolean>>> onReaction;
	
	public ReactionCommand(String emoji, Function<CommandsListener, Function<User, Function<MessageChannel, Boolean>>> onReaction) {
		this.emoji = emoji;
		this.onReaction = onReaction;
	}
	
	public String getEmoji() {
		return emoji;
	}
	
	public boolean execute(CommandsListener commandsListener, User author, MessageChannel channel) {
		return onReaction.apply(commandsListener).apply(author).apply(channel);
	}
}