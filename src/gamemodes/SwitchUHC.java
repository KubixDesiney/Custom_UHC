package gamemodes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import teams.UHCTeamManager;
import java.util.*;

public class SwitchUHC {
    private final UHCTeamManager teamManager;

    public SwitchUHC(UHCTeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void executeSwitch() {
        List<String> teams = UHCTeamManager.getAllTeams();
        if (teams.size() < 2) {
            Bukkit.broadcastMessage(ChatColor.RED + "Not enough teams to perform a switch!");
            return;
        }

        Map<String, List<Player>> teamPlayers = new HashMap<>();
        for (String team : teams) {
            List<Player> players = UHCTeamManager.getPlayersInTeam(team);
            if (!players.isEmpty()) {
                teamPlayers.put(team, players);
            }
        }

        if (teamPlayers.size() < 2) {
            Bukkit.broadcastMessage(ChatColor.RED + "Not enough teams with players to perform a switch!");
            return;
        }

        // Shuffle and pair teams
        List<String> availableTeams = new ArrayList<>(teamPlayers.keySet());
        Collections.shuffle(availableTeams);

        for (int i = 0; i < availableTeams.size() - 1; i += 2) {
            String team1 = availableTeams.get(i);
            String team2 = availableTeams.get(i + 1);

            Player player1 = getRandomPlayer(teamPlayers.get(team1));
            Player player2 = getRandomPlayer(teamPlayers.get(team2));

            if (player1 != null && player2 != null) {
                Location loc1 = player1.getLocation();
                Location loc2 = player2.getLocation();
                
                player1.teleport(loc2);
                player2.teleport(loc1);

                teamManager.joinTeam(player1, team2);
                teamManager.joinTeam(player2, team1);

                Bukkit.broadcastMessage(ChatColor.GOLD + "Switch UHC! " + 
                    ChatColor.AQUA + player1.getName() + " and " + player2.getName() + 
                    " have swapped teams and places!");
            }
        }
    }

    private Player getRandomPlayer(List<Player> players) {
        if (players.isEmpty()) return null;
        return players.get(new Random().nextInt(players.size()));
    }
}