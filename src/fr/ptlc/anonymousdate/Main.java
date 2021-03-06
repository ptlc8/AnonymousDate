package fr.ptlc.anonymousdate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;

@SuppressWarnings("unused")
public class Main {
	
	private static final String version = "1.0.0";
	private static final String creationDate = "14/02/2021";
	private static final String date = "14/02/2021";
	
	public static void main(String[] args) throws IOException {
		JDA jda = null;
		System.out.println("AnonymousDate " + getVersion() + ", par Ambi/PTLC_, le " + getDate() + ", pour 💕 Love Dating 💕, vive l'amour !");

		File tokenFile = new File("token.txt");
		String token = null;
		if (!tokenFile.exists())
			tokenFile.createNewFile();
		BufferedReader tokenReader = new BufferedReader(new FileReader(tokenFile));
		token = tokenReader.readLine();
		tokenReader.close();
		
		if (token == null) {
			System.out.println("Veuillez mettre le token du bot dans token.txt");
		} else {
			try {
				jda = JDABuilder.createDefault(token).build();
			} catch (LoginException e) {
				e.printStackTrace();
			}
			if (isBeta()) jda.getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.of(ActivityType.DEFAULT, " en cours de mise à jour"));
			else jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.of(ActivityType.LISTENING, " ce que tu as transmettre"));
			jda.addEventListener(new CommandsListener());
		}
	}
	
	public static String getVersion() {
		return version;
	}
	
	public static String getDate() {
		return date;
	}
	
	public static boolean isBeta() {
		return version.contains("beta");
	}
	
}
