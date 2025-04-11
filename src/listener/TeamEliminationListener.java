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
    private final SafeMinerListener safeMinerListener;

    public TeamEliminationListener(UHCTeamManager teamManager, main plugin, SafeMinerListener safeMinerListener) {
        this.teamManager = teamManager;
        this.plugin = plugin;
        this.safeMinerListener = safeMinerListener;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        checkForGameEnd(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        checkForGameEnd(event.getPlayer());
    }

    private void checkForGameEnd(Player affectedPlayer) {
        if (Gamestatus.getStatus() != 1) return;

        boolean isSoloMode = gameconfig.getTeamSize() == 1;
        int alivePlayers = 0;
        Player lastAlive = null;

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip players who are pending revive
            if (safeMinerListener.isPendingRevive(player.getUniqueId())) {
                continue;
            }
            
            if (!player.isDead()) {
                alivePlayers++;
                lastAlive = player;
            }
        }

        if (isSoloMode) {
            if (alivePlayers <= 1) {
                Player topKiller = plugin.getDamageTracker().getTopDamager();
                Bukkit.getPluginManager().callEvent(new gameEndEvent(lastAlive, topKiller));
            }
        } else {
            int aliveTeams = teamManager.getAliveTeamCount();
            if (aliveTeams <= 1) {
                String winningTeam = null;
                Player winner = null;
                
                if (aliveTeams == 1) {
                    winningTeam = teamManager.getAliveTeams().iterator().next();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (winningTeam.equals(teamManager.getPlayerTeam(player)) && 
                            !player.isDead() && 
                            !safeMinerListener.isPendingRevive(player.getUniqueId())) {
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

