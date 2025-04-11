package events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import test.main;

public class gameEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String winningTeam;
    private final Player winner;
    private final Player topkiller;
    
    public gameEndEvent(Player winner, Player topKiller) {
        this.winner = winner;
        this.topkiller = topKiller;
        this.winningTeam = (winner != null && main.getInstance() != null && 
                          main.getInstance().getTeamManager() != null) ? 
                          main.getInstance().getTeamManager().getPlayerTeam(winner) : null;
    }
    
    // Add null-safe getters
    public String getWinningTeamSafe() {
        return winningTeam != null ? winningTeam : "No winning team";
    }
    
    public String getWinnerNameSafe() {
        return winner != null ? winner.getName() : "No winner";
    }
	public Player getWinner() {
		return winner;
	}
	public Player gettopkiller() {
		return topkiller;
	}
	
	public HandlerList getHandlers() {
		return handlers;
	}
	public static HandlerList getHandlerList() {
		return handlers;
	}

}
