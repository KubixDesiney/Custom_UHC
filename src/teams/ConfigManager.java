package teams;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConfigManager {
    private final JavaPlugin plugin;
    private List<String> teamNames;
    private List<String> teamColors;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();  // Ensure config.yml exists
        FileConfiguration config = plugin.getConfig();

        teamNames = config.getStringList("teams.names");
        teamColors = config.getStringList("teams.colors");

        plugin.getLogger().info("Loaded Team Names: " + teamNames);
        plugin.getLogger().info("Loaded Team Colors: " + teamColors);
    }

    public String getRandomTeamName(List<String> currentTeams) {
        Random rand = new Random();
        if (teamNames.isEmpty()) {
            return "Team_" + rand.nextInt(10000); // Fallback if config is empty
        }

        List<String> availableNames = new ArrayList<>(teamNames);
        availableNames.removeAll(currentTeams); // Remove already used names

        plugin.getLogger().info("Available Team Names: " + availableNames);
        plugin.getLogger().info("Current Teams: " + currentTeams);

        if (availableNames.isEmpty()) {
            return "Team_" + rand.nextInt(10000); // Fallback if all names are taken
        }

        String chosenTeam = availableNames.get(rand.nextInt(availableNames.size()));
        plugin.getLogger().info("Chosen Team: " + chosenTeam);
        return chosenTeam;
    }


    public String getTeamColor(String teamName) {
        FileConfiguration config = plugin.getConfig();
        List<String> teamNames = config.getStringList("teams.names");
        List<String> teamColors = config.getStringList("teams.colors");

        plugin.getLogger().info("Finding color for team: " + teamName);
        plugin.getLogger().info("Configured team names: " + teamNames);
        plugin.getLogger().info("Configured team colors: " + teamColors);

        int index = teamNames.indexOf(teamName);
        if (index != -1 && index < teamColors.size()) {
            String colorString = teamColors.get(index).toUpperCase().replace(" ", "_"); // Fix format
            plugin.getLogger().info("Assigned color: " + colorString);
            return colorString; // Return color as a string
        } else {
            plugin.getLogger().warning("Team name not found: " + teamName);
        }
        return "WHITE"; // Default if no match is found
    }
}