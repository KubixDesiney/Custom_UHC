package events;


import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    Player p;
    public GameStartEvent(Player p) {
    	this.p = p;
    }
	public Player getp() {
		return p;
	}
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

