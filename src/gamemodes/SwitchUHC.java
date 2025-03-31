package gamemodes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import teams.UHCTeamManager;

import java.util.*;

public class SwitchUHC {
    private final UHCTeamManager teamManager;
    private final JavaPlugin plugin;
    private final int switchInterval; // Time in minutes

    public SwitchUHC(JavaPlugin plugin, UHCTeamManager teamManager, int switchInterval) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.switchInterval = switchInterval;
        startSwitchTask();
    }

    private void startSwitchTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                executeSwitch();
            }
        }.runTaskTimer(plugin, switchInterval * 1200L, switchInterval * 1200L);
    }

    private void executeSwitch() {
        List<String> teams = UHCTeamManager.getAllTeams();
        if (teams.size() < 2) {
            Bukkit.broadcastMessage(ChatColor.RED + "Not enough teams to perform a switch!");
            return;
        }

        List<Player> allPlayers = new ArrayList<>();
        Map<String, List<Player>> teamPlayers = new HashMap<>();

        // Gather players in each team
        for (String team : teams) {
            List<Player> players = UHCTeamManager.getPlayersInTeam(team);
            if (!players.isEmpty()) {
                teamPlayers.put(team, players);
                allPlayers.addAll(players);
            }
        }

        if (teamPlayers.size() < 2) {
            Bukkit.broadcastMessage(ChatColor.RED + "Not enough teams with players to perform a switch!");
            return;
        }

        // Shuffle team list and pair them up
        List<String> availableTeams = new ArrayList<>(teamPlayers.keySet());
        Collections.shuffle(availableTeams);
        List<String> pairedTeams = new ArrayList<>();

        for (int i = 0; i < availableTeams.size() - 1; i += 2) {
            String team1 = availableTeams.get(i);
            String team2 = availableTeams.get(i + 1);
            pairedTeams.add(team1);
            pairedTeams.add(team2);

            Player player1 = getRandomPlayer(teamPlayers.get(team1));
            Player player2 = getRandomPlayer(teamPlayers.get(team2));

            if (player1 == null || player2 == null) continue;

            // Swap positions
            Location loc1 = player1.getLocation();
            Location loc2 = player2.getLocation();
            
            player1.teleport(loc2);
            player2.teleport(loc1);

            // Swap teams using joinTeam method
            teamManager.joinTeam(player1, team2);
            teamManager.joinTeam(player2, team1);

            Bukkit.broadcastMessage(ChatColor.GOLD + "Switch UHC! " + ChatColor.AQUA + player1.getName() + " and " + player2.getName() + " have swapped teams and places!");
        }

        // Handle odd team count by swapping last team with a random previous team
        if (availableTeams.size() % 2 != 0) {
            String lastTeam = availableTeams.get(availableTeams.size() - 1);
            String randomPairedTeam = pairedTeams.get(new Random().nextInt(pairedTeams.size()));

            Player lastTeamPlayer = getRandomPlayer(teamPlayers.get(lastTeam));
            Player randomPairedTeamPlayer = getRandomPlayer(teamPlayers.get(randomPairedTeam));

            if (lastTeamPlayer != null && randomPairedTeamPlayer != null) {
                Location loc1 = lastTeamPlayer.getLocation();
                Location loc2 = randomPairedTeamPlayer.getLocation();
                
                lastTeamPlayer.teleport(loc2);
                randomPairedTeamPlayer.teleport(loc1);
                
                // Swap teams using joinTeam method
                teamManager.joinTeam(lastTeamPlayer, randomPairedTeam);
                teamManager.joinTeam(randomPairedTeamPlayer, lastTeam);
                
                Bukkit.broadcastMessage(ChatColor.GOLD + "Switch UHC! " + ChatColor.AQUA + lastTeamPlayer.getName() + " and " + randomPairedTeamPlayer.getName() + " have swapped teams and places!");
            }
        }
    }

    private Player getRandomPlayer(List<Player> players) {
        if (players.isEmpty()) return null;
        return players.get(new Random().nextInt(players.size()));
    }
}

