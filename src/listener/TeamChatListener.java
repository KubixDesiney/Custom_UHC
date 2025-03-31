package listener;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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
        
        if (teamName != null) {
            String teamColor = teamManager.getTeamData(teamName).color;
            event.setFormat(teamColor + player.getName() + ChatColor.WHITE + ": " + event.getMessage());
        }
        
        // Check if the message starts with '!' for global chat
        if (message.startsWith("!")) {
            // Global chat, let it proceed without changes
        	UHCTeamManager.TeamData teamData = teamManager.getTeamData(teamName);
        	event.setMessage(message.substring(1));
            event.setFormat("§e[GLOBAL] §r"+teamName+" "+player.getName() + ": "+message);// Remove '!' from message
        } else {
            // Team chat
            if (teamName != null) {
                // Get the team data
                UHCTeamManager.TeamData teamData = teamManager.getTeamData(teamName);
                // Only allow the message to be sent to players in the same team
                event.setFormat(teamData.color + "[" + teamName + "] " + player.getName() + ": " + message);
            }
        }
    }
}

