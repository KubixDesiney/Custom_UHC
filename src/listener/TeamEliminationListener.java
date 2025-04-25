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

        boolean isSoloMode = gameconfig.getTeamSize() < 2;
        int alivePlayers = 0;
        Player lastAlive = null;

        // Count alive players (excluding those pending revive)
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (safeMinerListener.isPendingRevive(player.getUniqueId())) {
                continue;
            }
            
            if (isPlayerAlive(player)) {
                alivePlayers++;
                lastAlive = player;
            }
        }

        if (isSoloMode) {
            // In solo mode, end game if only 1 player remains
            if (alivePlayers <= 1) {
                Player topKiller = plugin.getDamageTracker().getTopDamager();
                Bukkit.getPluginManager().callEvent(new gameEndEvent(lastAlive, topKiller));
            }
        } else {
            // In team mode, count alive teams (teams that started with players)
            int aliveTeams = 0;
            String lastAliveTeam = null;
            Player winner = null;
            
            // Check each team that had players at game start
            for (String teamName : teamManager.getAllTeams()) {
                if (teamManager.getPlayersInTeam(teamName).isEmpty()) {
                    continue; // Skip teams that never had players
                }
                
                boolean teamAlive = false;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (teamName.equals(teamManager.getPlayerTeam(player)) && 
                        isPlayerAlive(player) &&
                        !safeMinerListener.isPendingRevive(player.getUniqueId())) {
                        teamAlive = true;
                        winner = player;
                        break;
                    }
                }
                
                if (teamAlive) {
                    aliveTeams++;
                    lastAliveTeam = teamName;
                }
            }
            
            // End game if only 1 team remains with alive players
            if (aliveTeams <= 1) {
                Player topKiller = plugin.getDamageTracker().getTopDamager();
                Bukkit.getPluginManager().callEvent(new gameEndEvent(winner, topKiller));
            }
        }
    }
    private boolean isPlayerAlive(Player player) {
        return player != null && 
               !player.isDead() && 
               player.getGameMode() != GameMode.SPECTATOR;
    }
}

