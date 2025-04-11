package teams;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class UHCTeamManager {
    private final Map<String, TeamData> teams = new HashMap<>();
    private final static Map<UUID, String> playerTeams = new HashMap<>();
    private static Scoreboard scoreboard = null;
    private final ConfigManager configManager;
    public static List<Player> getPlayersInTeam(String teamName) {
       List<Player> players = new ArrayList<>();
       for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
           if (entry.getValue().equals(teamName)) {
               Player player = Bukkit.getPlayer(entry.getKey());
               if (player != null && player.isOnline()) {
                   players.add(player);
               }
           }
       }
       return players;
   }

    public UHCTeamManager(JavaPlugin plugin, ConfigManager configManager) {
        this.configManager = configManager;
    	ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            scoreboard = manager.getMainScoreboard();
        } else {
            throw new IllegalStateException("Scoreboard manager is null");
        }
    }
    public void clearAllTeams() {
        // Assuming teams is the internal list or map of teams
        teams.clear();
    }
    public boolean doesTeamExist(String teamName) {
        return getAllTeams().contains(teamName); // Check if the team is in the list
    }

    public void createTeam(String teamName, String teamColor, int size) {
        if (scoreboard.getTeam(teamName) != null) {
            throw new IllegalArgumentException("Team name '" + teamName + "' is already in use.");
        }

        Team team = scoreboard.registerNewTeam(teamName); // Create scoreboard team

        team.setAllowFriendlyFire(false);
        team.setCanSeeFriendlyInvisibles(true);

        // Apply the color visually by setting the players' team in-game.
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboard() == scoreboard) {
                team.addEntry(player.getName());
            }
        }

        // Create TeamData and store it
        TeamData teamData = new TeamData(teamName, teamColor, size, team);
        teams.put(teamName, teamData);

        Bukkit.broadcastMessage(ChatColor.GREEN + "Team '" + teamName + "' has been created with color: " + teamColor);
    }
    public void clearScoreboardTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            team.unregister(); // Remove the team from the scoreboard
        }
    }
    public void deleteTeam(String teamName) {
        // Remove the team from the internal team list
        if (teams.containsKey(teamName)) {
            teams.remove(teamName);  // Remove team from the list
        }

        // Remove from the scoreboard if you're using scoreboards to track teams
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.unregister();  // Unregister the team from the scoreboard
        }

        // Additional cleanup if needed (like removing players from the team)
    }

    public void joinTeam(Player player, String teamName) {
        String playerName = player.getName();
        TeamData teamData = teams.get(teamName);  // Get the TeamData object from the map
        if (teamData == null) {
            // Handle the case where the team does not exist
            Bukkit.getServer().broadcastMessage(ChatColor.RED + "Team " + teamName + " does not exist.");
            return;
        }

        Team team = teamData.scoreboardTeam;  // Get the actual Team object
        if (team == null) {
            // If there's no Team object, handle this error
            Bukkit.getServer().broadcastMessage(ChatColor.RED + "Invalid team data for team: " + teamName);
            return;
        }

        // Ensure the team isn't full
        if (getPlayersInTeam(teamName).size() < teamData.maxSize) {
            team.addEntry(playerName);
            playerTeams.put(player.getUniqueId(), teamName);
            Bukkit.getServer().broadcastMessage(ChatColor.GREEN + playerName + " joined team " + teamName);

            // Get the prefix from config
            String prefix = configManager.getTeamPrefix(teamName);
            
            // Execute TAB command with the prefix
            String command = "tab player " + player.getName() + " tabprefix " + prefix+" ";
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            // Also set the team color in scoreboard
            team.setPrefix(ChatColor.translateAlternateColorCodes('&', prefix));
        }
    }
    public TeamData getTeamData(String teamName) {
        return teams.get(teamName);  // Returns the TeamData for the specified team
    }
    public static List<String> getAllTeams() {
        List<String> teamNames = new ArrayList<>();
        for (Team team : scoreboard.getTeams()) { // Using instance variable scoreboard
            teamNames.add(team.getName());
        }
        return teamNames;
    }

    public void leaveTeam(Player player) {
        // Find the team of the player
        UUID playerUUID = player.getUniqueId();
        String teamName = playerTeams.get(playerUUID);

        if (teamName == null) {
            Bukkit.getServer().broadcastMessage(ChatColor.RED + "Player " + player.getName() + " is not in any team.");
            return;
        }

        // Get the TeamData for the team
        TeamData teamData = teams.get(teamName);
        if (teamData == null) {
            Bukkit.getServer().broadcastMessage(ChatColor.RED + "Team " + teamName + " does not exist.");
            return;
        }

        Team team = teamData.scoreboardTeam;
        if (team == null) {
            Bukkit.getServer().broadcastMessage(ChatColor.RED + "Invalid team data for team: " + teamName);
            return;
        }

        // Remove the player from the team (remove from scoreboard)
        team.removeEntry(player.getName());  // Remove player from the team

        // Remove the player from the playerTeams map
        playerTeams.remove(playerUUID);

        Bukkit.getServer().broadcastMessage(ChatColor.GREEN + player.getName() + " has left the team " + teamName);

        // Check if the team is empty, if so, remove the team
        if (getPlayersInTeam(teamName).isEmpty()) {
            // Unregister the team from the scoreboard
            team.unregister();

            // Remove the team from the internal map
            teams.remove(teamName);

            Bukkit.getServer().broadcastMessage(ChatColor.GREEN + "Team " + teamName + " has been removed as it is empty.");
        }
    }
    public Set<String> getAliveTeams() {
        Set<String> aliveTeams = new HashSet<>();
        for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline() && !player.isDead()) {
                aliveTeams.add(entry.getValue());
            }
        }
        return aliveTeams;
    }

    public int getAliveTeamCount() {
        return getAliveTeams().size();
    }

    public boolean isTeamAlive(String teamName) {
        for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
            if (entry.getValue().equals(teamName)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline() && !player.isDead()) {
                    return true;
                }
            }
        }
        return false;
    }
    public void updateTeamSize(String teamName, int newSize) {
        TeamData teamData = teams.get(teamName);
        if (teamData != null) {
            // Update the maxSize in TeamData
            teamData.maxSize = newSize;
            
            // If you want to notify about the change:
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Team " + teamName + 
                                  " size updated to " + newSize);
        } else {
            Bukkit.broadcastMessage(ChatColor.RED + "Team " + teamName + 
                                  " not found for size update");
        }
    }

    public String getPlayerTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public static class TeamData {
        String name;
        public String color;
        int maxSize;
        public Team scoreboardTeam;

        public TeamData(String name, String color, int maxSize, Team scoreboardTeam) {
            this.name = name;
            this.color = color;
            this.maxSize = maxSize;
            this.scoreboardTeam = scoreboardTeam;
        }
    }
}