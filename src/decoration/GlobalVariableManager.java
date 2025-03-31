package decoration;

import org.bukkit.Bukkit;

import events.ServerSlotChangedEvent;
import events.TeamSizeChangedEvent;

public class GlobalVariableManager {
    private int teamSize;
    private int serverSlot;

    public void setTeamSize(int newTeamSize) {
        if (newTeamSize != this.teamSize) {
            int oldTeamSize = this.teamSize;
            this.teamSize = newTeamSize;
            Bukkit.getServer().getPluginManager().callEvent(new TeamSizeChangedEvent(oldTeamSize, newTeamSize));
        }
    }

    public void setServerSlot(int newServerSlot) {
        if (newServerSlot != this.serverSlot) {
            int oldSlotCount = this.serverSlot;
            this.serverSlot = newServerSlot;
            Bukkit.getServer().getPluginManager().callEvent(new ServerSlotChangedEvent(oldSlotCount, newServerSlot));
        }
    }

    public int getTeamSize() {
        return teamSize;
    }

    public int getServerSlot() {
        return serverSlot;
    }
}
