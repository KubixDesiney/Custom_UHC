package events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TeamSizeChangedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final int oldTeamSize;
    private final int newTeamSize;

    public TeamSizeChangedEvent(int oldTeamSize, int newTeamSize) {
        this.oldTeamSize = oldTeamSize;
        this.newTeamSize = newTeamSize;
    }

    public int getOldTeamSize() {
        return oldTeamSize;
    }

    public int getNewTeamSize() {
        return newTeamSize;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
