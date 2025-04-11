// TeamEliminationListener.java
package listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import teams.UHCTeamManager;
import test.main;
import events.gameEndEvent;

public class TeamEliminationListener implements Listener {
    private final UHCTeamManager teamManager;
    private final main plugin;

    public TeamEliminationListener(UHCTeamManager teamManager, main plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        checkForGameEnd();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        checkForGameEnd();
    }

    private void checkForGameEnd() {
        int aliveTeams = teamManager.getAliveTeamCount();
        if (aliveTeams <= 1) {
            // Game should end
            String winningTeam = null;
            if (aliveTeams == 1) {
                winningTeam = teamManager.getAliveTeams().iterator().next();
            }
            
            Player winner = null;
            if (winningTeam != null) {
                // Find first alive player in winning team
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (winningTeam.equals(teamManager.getPlayerTeam(player)) && !player.isDead()) {
                        winner = player;
                        break;
                    }
                }
            }
            
            Player topKiller = plugin.getDamageTracker().getTopDamager();
            Bukkit.getPluginManager().callEvent(new gameEndEvent(winner, topKiller));
        }
    }
}
