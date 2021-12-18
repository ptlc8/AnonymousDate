package fr.ptlc.anonymousdate;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import fr.ptlc.anonymousdate.commands.MessageDatings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;

public class CommandsListener implements EventListener {
	
	private MessageDatings datings = new MessageDatings();
	private Map<String, Command> commands = new HashMap<String, Command>();
	private Map<String, CommandInProgress> commandsInProgress = new HashMap<String, CommandInProgress>();
	private Map<String, UsersSearch> usersSearches = new HashMap<String, UsersSearch>();
	private Map<String, ReactionCommand> reactionCommands = new HashMap<String, ReactionCommand>();
	
	public CommandsListener() {
		commands.put("help", new Command("Affiche la liste des commandes", new CommandArg[0], true, true){
			@Override
			public void execute(CommandsListener commandsListerner, User author, MessageChannel channel, Object[] args) {
				boolean inPrivate = channel.getType().equals(ChannelType.PRIVATE);
				boolean inGuild = channel.getType().equals(ChannelType.TEXT);
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Liste des commandes");
				eb.setColor(Color.RED);
				for (Entry<String, Command> commandEntry : commands.entrySet()) {
					eb.addField("/"+commandEntry.getKey(), commandEntry.getValue().getDescription()+(inGuild&&!commandEntry.getValue().isGuildCommand()&&commandEntry.getValue().isPrivateCommand()?" (en MP uniquement)":"")+(inPrivate&&commandEntry.getValue().isGuildCommand()&&!commandEntry.getValue().isPrivateCommand()?" (sur un serveur uniquement)":""), false);
				}
				eb.setFooter(author.getJDA().getSelfUser().getName()+" version "+Main.getVersion(), author.getJDA().getSelfUser().getAvatarUrl());
				channel.sendMessage(eb.build()).queue();
			}});
		commands.put("stop", new MessageDatings.StopCommand(datings));
		commands.put("letter", new MessageDatings.StartCommand(datings));
	}
	
	@Override
	public void onEvent(GenericEvent e) {
		if (e instanceof PrivateMessageReceivedEvent || e instanceof GuildMessageReceivedEvent) {
			JDA jda = e.getJDA();
			Message message = e instanceof PrivateMessageReceivedEvent ? ((PrivateMessageReceivedEvent)e).getMessage() : ((GuildMessageReceivedEvent)e).getMessage();
			User author = message.getAuthor();
			MessageChannel channel = message.getChannel();
			boolean isPrivate = e instanceof PrivateMessageReceivedEvent;
			boolean isGuild = e instanceof GuildMessageReceivedEvent;
			if (author.equals(jda.getSelfUser())) return;
			User foundUser = null;
			String msg = message.getContentRaw();
			if (usersSearches.get(author.getId())!=null) { // si recherche d'utilisateur en cours
				UsersSearch userSearch = usersSearches.get(author.getId());
				if (!userSearch.isFinish()) {
					channel.sendMessage("La recherche d'utilisateur a été annulée.").queue();
				}
				else if (userSearch.getUsers().size()==0) {
					// let it flow lol
				}
				else if (isInteger(message.getContentDisplay()) && Integer.parseInt(message.getContentDisplay())<userSearch.getUsers().size()) {
					foundUser = userSearch.getUsers().get(Integer.parseInt(message.getContentRaw()));
				}
				else {
					channel.sendMessage("`"+message.getContentRaw()+"` n'est pas un numéro valide.").queue();
				}
				usersSearches.remove(author.getId());
			}
			if (commandsInProgress.get(channel.getId()+author.getId())!=null && "cancel".equals(msg)) { // commande en cours à annuler
				commandsInProgress.remove(channel.getId()+author.getId());
				channel.sendMessage("La commande a été stopée.").queue();
			}
			if (commandsInProgress.get(channel.getId()+author.getId())!=null && commandsInProgress.get(channel.getId()+author.getId()).getProgress()<commandsInProgress.get(channel.getId()+author.getId()).getCommand().getArgs().length) { // commande en cours
				Command command = commandsInProgress.get(channel.getId()+author.getId()).getCommand();
				int progress = commandsInProgress.get(channel.getId()+author.getId()).getProgress();
				switch (command.getArgs()[progress].getType()) {
				case Channel:
					break;
				case Emote:
					break;
				case Integer:
					if (!isInteger(message.getContentRaw())) {
						channel.sendMessage("Ce n'est pas un nombre entier valide pour être `"+command.getArgs()[progress].getName()+"`").queue();
					}
					commandsInProgress.get(channel.getId()+author.getId()).pushArg(Integer.parseInt(message.getContentRaw()));
					break;
				case Float:
					if (!isFloat(message.getContentRaw())) {
						channel.sendMessage("Ce n'est pas un nombre décimal valide pour être `"+command.getArgs()[progress].getName()+"`").queue();
					}
					commandsInProgress.get(channel.getId()+author.getId()).pushArg(Float.parseFloat(message.getContentRaw()));
					break;
				case String:
					commandsInProgress.get(channel.getId()+author.getId()).pushArg(message.getContentRaw());
					break;
				case User:
					if (foundUser!=null) {
						commandsInProgress.get(channel.getId()+author.getId()).pushArg(foundUser);
					}
					else if (message.getMentionedUsers().size()>0) {
						commandsInProgress.get(channel.getId()+author.getId()).pushArg(message.getMentionedUsers().get(0));
					}
					else if (isLong(message.getContentRaw()) && (foundUser=jda.retrieveUserById(message.getContentRaw()).complete())!=null) {
						commandsInProgress.get(channel.getId()+author.getId()).pushArg(foundUser);
					} else {
						usersSearches.put(author.getId(), new UsersSearch(jda, message.getContentRaw(), (users) -> {
							if (users.size()!=0) {
								String u = "";
								int i = 0;
								for (User user : users)
									u += "**"+i+++"** : "+user.getAsTag()+"\n";
								channel.sendMessage("J'ai trouvé des utilisateur avec ce nom !\n"+u+"Donne moi le numéro correspondant à la bonne personne ou sinon redonne moi son nom (ou son identifiant)").queue();
							} else {
								channel.sendMessage("Je n'ai trouvé personne avec ce nom ou cet identifiant :/\nDonne-moi son nom d'utilisateur Discord ?").queue();
							}
						}));
					}
					break;
				}
			}
			else if (msg.startsWith("/")) { // nouvelle commande
				String commandName = msg.replaceFirst("/", "").split(" ")[0];
				if (commands.get(commandName)!=null && ((commands.get(commandName).isGuildCommand()&&isGuild) || (commands.get(commandName).isPrivateCommand()&&isPrivate)))
					commandsInProgress.put(channel.getId()+author.getId(), new CommandInProgress(commands.get(commandName)));
			}
			if (usersSearches.get(author.getId())!=null) return;
			if (commandsInProgress.get(channel.getId()+author.getId())!=null) {
				Command command = commandsInProgress.get(channel.getId()+author.getId()).getCommand();
				int progress = commandsInProgress.get(channel.getId()+author.getId()).getProgress();
				if (command.getArgs().length==progress) {
					commandsInProgress.get(channel.getId()+author.getId()).execute(this, author, channel);
					commandsInProgress.remove(channel.getId()+author.getId());
				} else {
					channel.sendMessage("Quel est *"+command.getArgs()[progress].getName()+"* ? (envoie `cancel` pour annuler)").queue();
				}
				return;
			}
			else { // message normal
				if (isPrivate) {
					if (datings.getOtherDater(author.getId())==null) {
						if (datings.canDate(author.getId())) {
							channel.sendMessage("Hey ! Tu n'as pas de date en cours, mais tu peux envoyer un premier message anonyme à la personne de ton choix en envoyant `/letter`. Mais attention tu n'as droit qu'à une seule personne !").queue();
						}
					} else {
						for (Attachment attachement : message.getAttachments())
							msg += "\n"+attachement.getUrl();
						User otherDater = jda.retrieveUserById(datings.getOtherDater(author.getId())).complete();
						otherDater.openPrivateChannel().complete().sendMessage("**Ton date anonyme `"+datings.randomifyId(author.getIdLong())+"` : **\n> "+msg.replace("\n", "\n> ")).queue();
						message.addReaction("✔").queue();
						//System.out.println("["+author.getAsTag()+" => "+otherDater.getAsTag()+"] "+msg.replace("\n", "\n\t"));
					}
				}
			}
		}
		if (e instanceof MessageReactionAddEvent) {
			MessageReactionAddEvent event = (MessageReactionAddEvent)e;
			if (event.getUser().equals(event.getJDA().getSelfUser())) return;
			if (event.getReactionEmote().isEmoji() && reactionCommands.get(event.getMessageId())!=null) {
				ReactionCommand rCommand = reactionCommands.get(event.getMessageId());
				if (rCommand.getEmoji().equals(event.getReactionEmote().getEmoji())) {
					if (rCommand.execute(this, event.getUser(), event.getChannel()))
						reactionCommands.remove(event.getMessageId());
				}
			}
		}
		
	}
	
	public void addReactionCommand(String messageId, ReactionCommand reactionCommand) {
		reactionCommands.put(messageId, reactionCommand);
	}
	
	public static boolean isInteger(String toTest) {
		try {
			Integer.parseInt(toTest);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public static boolean isFloat(String toTest) {
		try {
			Float.parseFloat(toTest);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public static boolean isLong(String toTest) {
		try {
			Long.parseLong(toTest);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public static String toReadeableTime(long time) {
		time /= 1000;
		String s = "";
		if (time >= 24*60*60) {
			s += (time/24/60/60)+"j ";
			time %= 24*60*60;
		}
		if (time >= 60*60) {
			s += (time/60/60)+"h ";
			time %= 60*60;
		}
		if (time >= 60) {
			s += (time/60)+"m ";
			time %= 60;
		}
		if (time > 0) {
			s += time+"s";
		}
		return s;
	}
	
}
