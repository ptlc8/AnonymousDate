package fr.ptlc.anonymousdate;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public abstract class Command {
	
	private CommandArg[] args;
	private String description;
	private boolean inGuild, inPrivate;
	
	public abstract void execute(CommandsListener commandsListerner, User author, MessageChannel channel, Object[] args);
	
	public Command(String description, CommandArg[] args, boolean inGuild, boolean inPrivate) {
		this.args = args;
		this.description = description;
		this.inGuild = inGuild;
		this.inPrivate = inPrivate;
	}
	
	public CommandArg[] getArgs() {
		return args;
	}
	
	public String getDescription() {
		return description;
	}
	
	public boolean isGuildCommand() {
		return inGuild;
	}
	
	public boolean isPrivateCommand() {
		return inPrivate;
	}
	
}