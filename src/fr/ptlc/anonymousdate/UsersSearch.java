package fr.ptlc.anonymousdate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class UsersSearch {
	
	private List<User> users;
	private int waitingQueriesNumber;
	
	public UsersSearch(JDA jda, String name, Consumer<List<User>> onSearchEnd) {
		users = new ArrayList<User>();
		waitingQueriesNumber = 0;
		for (Guild g : jda.getGuilds()) {
			waitingQueriesNumber++;
			g.retrieveMembersByPrefix(name, 3).onSuccess((members)->{
				for (Member m : members)
					users.add(m.getUser());
				waitingQueriesNumber--;
				if (waitingQueriesNumber==0)
					onSearchEnd.accept(users);
			}).onError((error)->{
				waitingQueriesNumber--;
				if (waitingQueriesNumber==0)
					onSearchEnd.accept(users);
			});
		}
	}
	
	public boolean isFinish() {
		return waitingQueriesNumber==0;
	}
	
	public List<User> getUsers() {
		return users;
	}
}