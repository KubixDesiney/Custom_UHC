package events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class gameEndEvent extends Event {
	
	private static final HandlerList handlers = new HandlerList();
	
	Player p;
	Player winner;
	Player topkiller;
	int winner_number;
	int top_number;
	
	public gameEndEvent(Player winner,Player topkiller,int winner_number,int top_number, Player p) {
		this.p = p;
		this.winner = winner;
		this.topkiller = topkiller;
		this.winner_number = winner_number;
		this.top_number = top_number;
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
