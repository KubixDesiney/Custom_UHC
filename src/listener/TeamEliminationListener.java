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
import gamemodes.Gamestatus;
import Rules.gameconfig;

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
        // Check if game is in progress
        if (Gamestatus.getStatus() != 1) return;

        boolean isSoloMode = gameconfig.getTeamSize() == 1;
        int alivePlayers = 0;
        Player lastAlive = null;

        // Count alive players - ignore those who died naturally if SafeMiner is enabled
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isDead() || 
                (gameconfig.getInstance().isSafeMinerEnabled() && player.getKiller() == null)) {
                alivePlayers++;
                lastAlive = player;
            }
        }

        if (isSoloMode) {
            // Solo mode - end when only one player remains (not counting SafeMiner revives)
            if (alivePlayers <= 1) {
                Player topKiller = plugin.getDamageTracker().getTopDamager();
                Bukkit.getPluginManager().callEvent(new gameEndEvent(lastAlive, topKiller));
            }
        } else {
            // Team mode - end when only one team remains
            int aliveTeams = teamManager.getAliveTeamCount();
            if (aliveTeams <= 1) {
                String winningTeam = null;
                Player winner = null;
                
                if (aliveTeams == 1) {
                    winningTeam = teamManager.getAliveTeams().iterator().next();
                    // Find first alive player in winning team (including SafeMiner revived players)
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (winningTeam.equals(teamManager.getPlayerTeam(player)) && 
                            (!player.isDead() || 
                             (gameconfig.getInstance().isSafeMinerEnabled() && player.getKiller() == null))) {
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
}

