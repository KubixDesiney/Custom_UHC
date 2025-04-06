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
        // No need to call saveDefaultConfig() here if you want to retain custom values in config.yml
        FileConfiguration config = plugin.getConfig();

        // Ensure that the "teams.names" and "teams.colors" are properly initialized in the config if not already
        if (!config.contains("teams.names")) {
            config.set("teams.names", new ArrayList<String>()); // Set a default empty list if not present
        }
        if (!config.contains("teams.colors")) {
            config.set("teams.colors", new ArrayList<String>()); // Set a default empty list if not present
        }

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

    public String getTeamPrefix(String teamName) {
        FileConfiguration config = plugin.getConfig();
        List<String> teamNames = config.getStringList("teams.names");
        List<String> teamPrefixes = config.getStringList("teams.prefixes");
        
        int index = teamNames.indexOf(teamName);
        if (index != -1 && index < teamPrefixes.size()) {
            return teamPrefixes.get(index);
        }
        return "&f"; // Default white if not found
    }

    // Add a method to add teams dynamically if you need to do so programmatically
    public void addTeam(String teamName, String teamColor) {
        FileConfiguration config = plugin.getConfig();
        List<String> currentNames = config.getStringList("teams.names");
        List<String> currentColors = config.getStringList("teams.colors");

        if (!currentNames.contains(teamName)) {
            currentNames.add(teamName);
            currentColors.add(teamColor);
            config.set("teams.names", currentNames);
            config.set("teams.colors", currentColors);
            plugin.saveConfig(); // Save the config after modifying it
            plugin.getLogger().info("Added new team: " + teamName);
        }
    }
}
