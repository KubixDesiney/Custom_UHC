package listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import Rules.gameconfig;
import events.ServerSlotChangedEvent;
import events.TeamSizeChangedEvent;
import teams.ConfigManager;
import teams.UHCTeamManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import java.util.List;
import java.util.Random;

public class GlobalVariableListener implements Listener {
    private final UHCTeamManager teamManager;
    private final ConfigManager configManager; // New config manager

    public GlobalVariableListener(UHCTeamManager teamManager, ConfigManager configManager) {
        this.teamManager = teamManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onTeamSizeChanged(TeamSizeChangedEvent event) {
        int newTeamSize = event.getNewTeamSize();
        int serverSlot = Bukkit.getServer().getMaxPlayers();
        int idealTeamCount = (serverSlot + newTeamSize - 1) / newTeamSize; // Proper rounding

        List<String> currentTeams = UHCTeamManager.getAllTeams();

        if (currentTeams.size() > idealTeamCount) {
            removeExtraTeams(currentTeams, currentTeams.size() - idealTeamCount);
        } else if (currentTeams.size() < idealTeamCount) {
            createNewTeams(currentTeams, idealTeamCount - currentTeams.size(), newTeamSize);
        }
    }

    private void removeExtraTeams(List<String> currentTeams, int teamsToRemove) {
        Random rand = new Random();
        for (int i = 0; i < teamsToRemove; i++) {
            String teamToRemove = currentTeams.get(rand.nextInt(currentTeams.size()));

            if (teamToRemove != null && !teamToRemove.isEmpty()) {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "/team delete " + teamToRemove);
                teamManager.deleteTeam(teamToRemove);
                currentTeams.remove(teamToRemove);
                Bukkit.getServer().broadcastMessage(ChatColor.RED + "Removed team: " + teamToRemove);
            }
        }
    }

    private void createNewTeams(List<String> currentTeams, int teamsToCreate, int teamSize) {
        for (int i = 0; i < teamsToCreate; i++) {
            String teamName = configManager.getRandomTeamName(currentTeams);

            // Ensure team doesn't already exist before creating
            if (teamManager.doesTeamExist(teamName)) {
                Bukkit.getServer().broadcastMessage(ChatColor.RED + "Skipping team creation: " + teamName + " (already exists)");
                continue;
            }

            // Get the color as a String from ConfigManager
            String teamColorString = configManager.getTeamColor(teamName);

            // Create team using the string color
            teamManager.createTeam(teamName, teamColorString, teamSize);

            // Broadcast team creation with color
            Bukkit.getServer().broadcastMessage(ChatColor.GREEN + "Created team: " + teamName + " with color: " + teamColorString);
        }
    }
    @EventHandler
    public void onServerSlotChanged(ServerSlotChangedEvent event) {
        int newSlotCount = event.getNewSlotCount();
        int newTeamSize = gameconfig.getTeamSize();
        int idealTeamCount = (newSlotCount + newTeamSize - 1) / newTeamSize;

        List<String> currentTeams = UHCTeamManager.getAllTeams();

        if (currentTeams.size() > idealTeamCount) {
            removeExtraTeams(currentTeams, currentTeams.size() - idealTeamCount);
        } else if (currentTeams.size() < idealTeamCount) {
            createNewTeams(currentTeams, idealTeamCount - currentTeams.size(), newTeamSize);
        }
    }
}

