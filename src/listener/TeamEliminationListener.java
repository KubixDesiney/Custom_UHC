package listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
        Player player = event.getEntity();
        handlePlayerDeath(player);
        checkForGameEnd(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        checkForGameEnd(event.getPlayer());
    }

    private void handlePlayerDeath(Player player) {
        // Skip if game isn't running or player is pending revive
        if (Gamestatus.getStatus() != 1 || safeMinerListener.isPendingRevive(player.getUniqueId())) {
            return;
        }

        // Handle spectator mode
        if (gameconfig.getInstance().isSpectatorModeEnabled()) {
            // Put player in spectator mode
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.GRAY + "You are now spectating the match.");
            
            // Check if team was eliminated
            checkTeamElimination(player);
        } else {
            // Kick non-op players after 30 seconds
            if (!player.isOp()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.kickPlayer(ChatColor.RED + "You have been eliminated from the UHC!");
                    }
                }, 30 * 20L); // 30 seconds
            }
            // Put player in spectator mode temporarily
            player.setGameMode(GameMode.SPECTATOR);
            
            // Check if team was eliminated
            checkTeamElimination(player);
        }
    }

    private void checkTeamElimination(Player player) {
        String teamName = teamManager.getPlayerTeam(player);
        if (teamName != null && !teamManager.isTeamAlive(teamName)) {
            Bukkit.broadcastMessage(ChatColor.RED + "Team " + teamName + " has been eliminated!");
        }
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
            
            if (!player.isDead() && player.getGameMode() != GameMode.SPECTATOR) {
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
                            player.getGameMode() != GameMode.SPECTATOR &&
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

