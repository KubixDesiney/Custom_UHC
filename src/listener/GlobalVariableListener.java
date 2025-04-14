package listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import Rules.gameconfig;
import events.ServerSlotChangedEvent;
import events.TeamSizeChangedEvent;
import teams.ConfigManager;
import teams.TeamSelectionSystem;
import teams.UHCTeamManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

public class GlobalVariableListener implements Listener {
    private final UHCTeamManager teamManager;
    private final ConfigManager configManager;
    private final TeamSelectionSystem teamSelectionSystem;

    public GlobalVariableListener(UHCTeamManager teamManager, ConfigManager configManager, TeamSelectionSystem teamSelectionSystem) {
        this.teamManager = teamManager;
        this.configManager = configManager;
        this.teamSelectionSystem = teamSelectionSystem;
    }

    @EventHandler
    public void onTeamSizeChanged(TeamSizeChangedEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            teamSelectionSystem.giveSelectionBanner(player);
        }
        int newTeamSize = event.getNewTeamSize();
        int serverSlot = Bukkit.getServer().getMaxPlayers();
        int idealTeamCount = (serverSlot + newTeamSize - 1) / newTeamSize;

        List<String> currentTeams = UHCTeamManager.getAllTeams();

        // Update the size of all existing teams first
        for (String teamName : currentTeams) {
            teamManager.updateTeamSize(teamName, newTeamSize);
            Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "Updated team " + teamName + " size to " + newTeamSize);
        }

        // Then adjust the number of teams if needed
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

            if (teamManager.doesTeamExist(teamName)) {
                Bukkit.getServer().broadcastMessage(ChatColor.RED + "Skipping team creation: " + teamName + " (already exists)");
                continue;
            }

            String teamColorString = configManager.getTeamPrefix(teamName);
            teamManager.createTeam(teamName, teamColorString, teamSize);
            Bukkit.getServer().broadcastMessage(ChatColor.GREEN + "Created team: " + teamName + " with color: " + teamColorString);
        }
    }

    @EventHandler
    public void onServerSlotChanged(ServerSlotChangedEvent event) {
        int newSlotCount = event.getNewSlotCount();
        int newTeamSize = gameconfig.getTeamSize();
        int idealTeamCount = (newSlotCount + newTeamSize - 1) / newTeamSize;

        List<String> currentTeams = UHCTeamManager.getAllTeams();

        // Update the size of all existing teams first
        for (String teamName : currentTeams) {
            teamManager.updateTeamSize(teamName, newTeamSize);
            Bukkit.getServer().broadcastMessage(ChatColor.YELLOW + "Updated team " + teamName + " size to " + newTeamSize);
        }

        if (currentTeams.size() > idealTeamCount) {
            removeExtraTeams(currentTeams, currentTeams.size() - idealTeamCount);
        } else if (currentTeams.size() < idealTeamCount) {
            createNewTeams(currentTeams, idealTeamCount - currentTeams.size(), newTeamSize);
        }
    }
}

