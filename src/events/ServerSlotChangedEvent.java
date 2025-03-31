package events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ServerSlotChangedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final int oldSlotCount;
    private final int newSlotCount;

    public ServerSlotChangedEvent(int oldSlotCount, int newSlotCount) {
        this.oldSlotCount = oldSlotCount;
        this.newSlotCount = newSlotCount;
    }

    public int getOldSlotCount() {
        return oldSlotCount;
    }

    public int getNewSlotCount() {
        return newSlotCount;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
