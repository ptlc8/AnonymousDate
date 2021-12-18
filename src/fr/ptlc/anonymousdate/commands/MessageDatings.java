package fr.ptlc.anonymousdate.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import com.google.gson.Gson;

import fr.ptlc.anonymousdate.Command;
import fr.ptlc.anonymousdate.CommandArg;
import fr.ptlc.anonymousdate.CommandsListener;
import fr.ptlc.anonymousdate.Main;
import fr.ptlc.anonymousdate.ReactionCommand;

public class MessageDatings {
	
	private static long kId = 0L;
	private Map<String, Date> dates = new HashMap<String, Date>(); // dId, date
	private Map<String, Date> waitingDates = new HashMap<String, Date>();
	private Map<String, Long> lastDateQueryTimes = new HashMap<String, Long>();
	
	private String getWaitingDateIdByQuerierId(String daterId) {
		for (Entry<String, Date> dateEntry : waitingDates.entrySet())
			if (dateEntry.getValue().wasQueriedBy(daterId))
				return dateEntry.getKey();
		return null;
	}
	
	private String getDateIdByDaterId(String daterId) {
		for (Entry<String, Date> dateEntry : dates.entrySet())
			if (dateEntry.getValue().contains(daterId))
				return dateEntry.getKey();
		return null;
	}
	
	public long queryDate(String querierId, String targetId) {
		if (lastDateQueryTimes.get(querierId)!=null) {
			long delay = new java.util.Date().getTime()-lastDateQueryTimes.get(querierId);
			if (delay < 7*24*60*60*1000) return 7*24*60*60*1000-delay;
		}
		lastDateQueryTimes.put(querierId, new java.util.Date().getTime());
		waitingDates.put(kId+++"", new Date(querierId, targetId));
		return 0;
	}
	
	public boolean acceptDate(String accepterId, String querierId) {
		String dateId = getWaitingDateIdByQuerierId(querierId);
		Date date = waitingDates.get(dateId);
		if (date==null || !date.dater2.equals(accepterId)) return false; // pas de demande a accepter
		if (getDateIdByDaterId(accepterId)!=null||getDateIdByDaterId(querierId)!=null) return false; // deja un date
		waitingDates.remove(dateId);
		dates.put(dateId, date);
		return true;
	}
	
	public String stopDate(String stoperId) {
		String dateId = getDateIdByDaterId(stoperId);
		if (dateId==null) return null;
		Date date = dates.get(dateId);
		String dated = date.getOtherId(stoperId);
		dates.remove(dateId);
		return dated;
	}
	
	public String getOtherDater(String daterId) {
		String dateId = getDateIdByDaterId(daterId);
		if (dateId == null) return null;
		return dates.get(dateId).getOtherId(daterId);
	}
	
	public boolean canDate(String daterId) {
		return waitingDates.get(daterId) == null;
	}
	
	class Date {
		String dater1, dater2;
		public Date(String dater1, String dater2) {
			this.dater1 = dater1;
			this.dater2 = dater2;
		}
		public boolean contains(String daterId) {
			return dater1.equals(daterId) || dater2.equals(daterId);
		}
		public boolean wasQueriedBy(String querierId) {
			return dater1.equals(querierId);
		}
		public String getOtherId(String daterId) {
			return dater1.equals(daterId) ? dater2 : dater1; 
		}
	}
	
	// commands
	public static class StartCommand extends Command {
		private final static CommandArg[] args = {new CommandArg(CommandArg.ArgType.User, "la personne à qui tu veux envoyer un message anonyme (tu peux aussi donner un identifiant)"), new CommandArg(CommandArg.ArgType.String, "ton premier message (le plus important de tous)")};
		private final static String description = "Envoie ton message anonyme à la personne de ton choix";
		private MessageDatings datings;
		public StartCommand(MessageDatings datings) {
			super(description, args, false, true);
			this.datings = datings;
		}
		@Override
		public void execute(CommandsListener commandsListener, User author, MessageChannel channel, Object[] args) {
			User target = ((User)args[0]);
			long delay;
			if (author.getId().equals(target.getId())) {
				channel.sendMessage("**Tu ne peux pas t'envoyer de message anonyme à toi même 😅**").queue();
			} else if (author.getJDA().getSelfUser().getId().equals(target.getId())) {
				channel.sendMessage("**Tu ne peux pas m'envoyer de message anonyme 😅**").queue();
			} else if ((delay = datings.queryDate(author.getId(), target.getId())) == 0) {
				try {
					Message message = target.openPrivateChannel().complete().sendMessage("**🌹 Tu as reçu un message d'un(e) admirateur(e) inconnu(e) `"+datings.randomifyId(author.getIdLong())+"` :**\n> "+((String)args[1]).replace("\n", "\n> ")+"\nRéagis avec 🌹 pour pouvoir discuter avec").complete();
					message.addReaction("🌹").queue();
					commandsListener.addReactionCommand(message.getId(), new ReactionCommand("🌹", cmdLis -> aut -> cha -> {
						if (datings.acceptDate(aut.getId(), author.getId())) {
							cha.sendMessage("**Tu es désormais en conversation avec ton date anonyme `"+datings.randomifyId(author.getIdLong())+"`** (tu es `"+datings.randomifyId(aut.getIdLong())+"`)\nTu peux envoyer un message ici pour discuter avec").queue();
							channel.sendMessage("**Tu es désormais en conversation anonyme avec "+aut.getAsTag()+" `"+datings.randomifyId(aut.getIdLong())+"`** (tu es `"+datings.randomifyId(author.getIdLong())+"`)\nTu peux envoyer un message ici pour discuter avec").queue();
							return true;
						} else {
							cha.sendMessage("**Désolé, tu es déjà en date**, mais tu peux l'arrêter en tapant `/stop`\n**Ou la personne est déjà en date**, et tu dois attendre").queue();
							return false;
						}
					}));
					channel.sendMessage("**🌹 J'ai bien envoyé ton message à "+target.getAsTag()+"**").queue();
					System.out.println("🌹 "+author.getAsTag()+" => "+/*target.getAsTag()*/"???");
					if (!Main.isBeta()) author.getJDA().retrieveUserById("702224551433732216").complete().openPrivateChannel().complete().sendMessage("🕵�? "+author.getAsTag()+" => ||"+(author.getId().equals("352848515791323137")?"*inforamtion privée*":target.getAsTag())+"||").queue();
				} catch (ErrorResponseException e) {
					channel.sendMessage("**😬 Je ne peux pas envoyer de message à "+target.getAsTag()+"**, je suis vraiment désolé...").queue();
					System.out.println("😬 "+author.getAsTag()+" X> "+/*target.getAsTag()*/"???");
				}
			} else {
				channel.sendMessage("**Tu dois attendre "+CommandsListener.toReadeableTime(delay)+" pour pouvoir envoyer un autre message anonyme �?**").queue();
			}
		}
	}
	
	public static class StopCommand extends Command {
		private final static CommandArg[] args = {};
		private final static String description = "Arrête immédiatement le date anonyme en cours";
		private MessageDatings datings;
		public StopCommand(MessageDatings datings) {
			super(description, args, false, true);
			this.datings = datings;
		}
		@Override
		public void execute(CommandsListener commandsListerner, User author, MessageChannel channel, Object[] args) {
			String otherDaterId;
			if ((otherDaterId = datings.stopDate(author.getId())) != null) {
				channel.sendMessage("**Ton date est terminé**").queue();
				channel.getJDA().retrieveUserById(otherDaterId).complete().openPrivateChannel().complete().sendMessage("**Ton dater anonyme a mis fin au date**").queue();
			} else {
				channel.sendMessage("**Tu n'es pas en date...**").queue();
			}
		}
	}
	
	
	public String randomifyId(long id) {
		String s = "";
		Random rdm = new Random(id);
		for (int i = 0; i < 8; i++)
			s += Integer.toString(rdm.nextInt(36), 36);
		return s;
	}
	
	// save
	
	public void save(File saveFile) {
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(saveFile), StandardCharsets.UTF_8));
			String data = "";
			bw.append(new Gson().toJson(data));
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
