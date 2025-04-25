package listener;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import gamemodes.Gamestatus;
import teams.UHCTeamManager;
import org.bukkit.entity.Player;

public class TeamChatListener implements Listener {

    private final UHCTeamManager teamManager;

    public TeamChatListener(UHCTeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String teamName = teamManager.getPlayerTeam(player);
        
        // If game is NOT running (status != 1), use normal vanilla chat
        if (Gamestatus.getStatus() != 1) {
            // Just add team prefix if player is in a team, but show to everyone
            if (teamName != null) {
                String teamPrefix = teamManager.getConfigManager().getTeamPrefix(teamName);
                String coloredPrefix = ChatColor.translateAlternateColorCodes('&', teamPrefix);
                event.setFormat(coloredPrefix + player.getName() + ChatColor.WHITE + ": " + message);
            }
            return; // Let normal chat handling proceed
        }
        
        // Game is running (status == 1) - implement team chat system
        event.setCancelled(true); // Cancel the original event to handle messaging ourselves
        
        String formattedMessage;
        
        if (message.startsWith("!")) {
            // Global chat
            String globalMessage = message.substring(1);
            String teamPrefix = teamName != null ? teamManager.getConfigManager().getTeamPrefix(teamName) : "";
            
            formattedMessage = String.format("§e[GLOBAL] %s%s §7>> §f%s",
                teamPrefix,
                player.getName(),
                globalMessage);
            
            // Send to all players
            for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
                onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', formattedMessage));
            }
        } else {
            // Team chat - if player has no team, send to everyone
            if (teamName == null) {
                formattedMessage = String.format("%s: %s",
                    player.getName(),
                    message);
                
                // Send to all players
                for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
                    onlinePlayer.sendMessage(formattedMessage);
                }
                return;
            }
            
            // Player is in a team - send only to teammates
            formattedMessage = String.format("%s%s §7>> §f%s",
                teamManager.getConfigManager().getTeamPrefix(teamName),
                player.getName(),
                message);
            
            // Send to teammates
            for (Player teammate : UHCTeamManager.getPlayersInTeam(teamName)) {
                teammate.sendMessage(ChatColor.translateAlternateColorCodes('&', formattedMessage));
            }
        }
    }
}

