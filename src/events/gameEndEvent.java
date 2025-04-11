package events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import test.main;

public class gameEndEvent extends Event {
	
	private static final HandlerList handlers = new HandlerList();
	private final String winningTeam;
	
	Player p;
	Player winner;
	Player topkiller;
	int winner_number;
	int top_number;
	
	public gameEndEvent(Player winner, Player topKiller) {
	    this.winner = winner;
	    this.topkiller = topKiller;
	    this.winningTeam = winner != null ? 
	        main.getInstance().getTeamManager().getPlayerTeam(winner) : null;
	}
	public String getWinningTeam() {
	    return winningTeam;
	}
	public Player getp() {
		return p;
	}
	public Player getWinner() {
		return winner;
	}
	public Player gettopkiller() {
		return topkiller;
	}
	public int getwinnernumber() {
		return winner_number;
	}
	public int gettopnumber() {
		return top_number;
	}
	
	public HandlerList getHandlers() {
		return handlers;
	}
	public static HandlerList getHandlerList() {
		return handlers;
	}

}
