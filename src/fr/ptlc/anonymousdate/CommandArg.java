package fr.ptlc.anonymousdate;

public class CommandArg {
	
	private ArgType type;
	private String name;
	
	public CommandArg(ArgType type, String name) {
		this.type = type;
		this.name = name;
	}
	
	public ArgType getType() {
		return type;
	}
	
	public String getName() {
		 return name;
	}
	
	public static enum ArgType {
		String, Float, Integer, Emote, User, Channel;
	}
}
