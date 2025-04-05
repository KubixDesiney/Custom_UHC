package gamemodes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import teams.UHCTeamManager;
import java.util.*;

public class SwitchUHC {
    private final UHCTeamManager teamManager;
    private boolean isSwitching = false;
    private int switchCooldown = 0;

    public SwitchUHC(UHCTeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void executeSwitch() {
        if (isSwitching) {
            Bukkit.broadcastMessage(ChatColor.RED + "A switch is already in progress!");
            return;
        }

        isSwitching = true;
        
        try {
            List<String> teams = new ArrayList<>(UHCTeamManager.getAllTeams());
            if (teams.size() < 2) {
                Bukkit.broadcastMessage(ChatColor.RED + "Not enough teams to perform a switch!");
                return;
            }

            // Get all players from all teams
            Map<String, List<Player>> teamPlayers = new HashMap<>();
            for (String team : teams) {
                List<Player> players = UHCTeamManager.getPlayersInTeam(team);
                if (!players.isEmpty()) {
                    teamPlayers.put(team, new ArrayList<>(players));
                }
            }

            if (teamPlayers.size() < 2) {
                Bukkit.broadcastMessage(ChatColor.RED + "Not enough teams with players to perform a switch!");
                return;
            }

            // Shuffle teams and players
            List<String> shuffledTeams = new ArrayList<>(teamPlayers.keySet());
            Collections.shuffle(shuffledTeams);

            // Pair teams and switch players
            for (int i = 0; i < shuffledTeams.size() - 1; i += 2) {
                String team1 = shuffledTeams.get(i);
                String team2 = shuffledTeams.get(i + 1);

                List<Player> players1 = teamPlayers.get(team1);
                List<Player> players2 = teamPlayers.get(team2);

                if (!players1.isEmpty() && !players2.isEmpty()) {
                    Player player1 = getRandomPlayer(players1);
                    Player player2 = getRandomPlayer(players2);

                    if (player1 != null && player2 != null) {
                        // Store original locations
                        Location loc1 = player1.getLocation().clone();
                        Location loc2 = player2.getLocation().clone();

                        // Teleport players with safe teleport
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player1.teleport(loc2);
                                player2.teleport(loc1);
                            }
                        }.runTaskLater(Bukkit.getPluginManager().getPlugin("Custom_UHC"), 5L);

                        // Change teams
                        teamManager.leaveTeam(player1);
                        teamManager.leaveTeam(player2);
                        teamManager.joinTeam(player1, team2);
                        teamManager.joinTeam(player2, team1);

                        Bukkit.broadcastMessage(ChatColor.GOLD + "Switch UHC! " + 
                            ChatColor.AQUA + player1.getName() + " (" + team1 + ") and " + 
                            player2.getName() + " (" + team2 + ") have swapped teams!");
                    }
                }
            }
        } finally {
            isSwitching = false;
        }
    }

    private Player getRandomPlayer(List<Player> players) {
        if (players == null || players.isEmpty()) return null;
        return players.get(new Random().nextInt(players.size()));
    }

    public void startSwitchTimer(int initialTime) {
        switchCooldown = initialTime;
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (switchCooldown <= 0) {
                    executeSwitch();
                    switchCooldown = initialTime; 
                } else {
                    switchCooldown--;
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("Custom_UHC"), 0L, 20L); // Run every second
    }
}