package fr.ptlc.anonymousdate;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class CommandInProgress {
	
	private Command command;
	private Object[] args;
	int progress = 0;
	
	public CommandInProgress(Command command) {
		this.command = command;
		this.args = new Object[command.getArgs().length];
	}
	
	public Command getCommand() {
		return command;
	}
	
	public int getProgress() {
		return progress;
	}
	
	public void pushArg(Object arg) {
		args[progress++] = arg;
	}
	
	public void execute(CommandsListener commandsListerner, User author, MessageChannel channel) {
		command.execute(commandsListerner, author, channel, args);
	}
}