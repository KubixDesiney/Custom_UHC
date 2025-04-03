package teams;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import utilities.HotBarMessager;

import java.util.List;

public class TeamDistanceTracker {

    private final UHCTeamManager teamManager;
    private static final int MAX_HOTBAR_LENGTH = 60; // Typical hotbar character limit

    public TeamDistanceTracker(UHCTeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void updateHotBar(Player player) {
        try {
            String teamName = teamManager.getPlayerTeam(player);
            if (teamName == null || teamName.isEmpty()) {
                sendSafeMessage(player, ChatColor.RED + "No team!");
                return;
            }

            List<Player> teammates = UHCTeamManager.getPlayersInTeam(teamName);
            teammates.remove(player); // Remove self

            if (teammates.isEmpty()) {
                sendSafeMessage(player, ChatColor.YELLOW + "No teammates");
                return;
            }

            String message = buildTeammateMessage(player, teammates);
            sendSafeMessage(player, message);

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Team tracker error");
            e.printStackTrace();
        }
    }

    private String buildTeammateMessage(Player player, List<Player> teammates) {
        StringBuilder message = new StringBuilder();
        int remainingSpace = MAX_HOTBAR_LENGTH;

        for (Player teammate : teammates) {
            if (!teammate.isOnline()) continue;

            String teammateInfo = formatTeammateInfo(player, teammate);
            
            // Check if we have space for this teammate's info
            if (teammateInfo.length() + 1 > remainingSpace) { // +1 for space
                if (message.length() == 0) {
                    // If even one teammate's info is too long, truncate
                    return teammateInfo.substring(0, Math.min(teammateInfo.length(), MAX_HOTBAR_LENGTH));
                }
                break; // No more space
            }

            if (message.length() > 0) {
                message.append(" ");
                remainingSpace--;
            }

            message.append(teammateInfo);
            remainingSpace -= teammateInfo.length();
        }

        return message.toString();
    }

    private String formatTeammateInfo(Player player, Player teammate) {
        double distance = player.getLocation().distance(teammate.getLocation());
        Vector direction = teammate.getLocation().toVector().subtract(player.getLocation().toVector());
        direction.normalize();
        
        return getHealthColor(teammate) + getShortName(teammate.getName()) +
               ChatColor.AQUA + (int)distance + "m" +
               ChatColor.YELLOW + getArrowDirection(player, direction);
    }

    private String getShortName(String fullName) {
        // Show first 4 characters of name to save space
        return fullName.length() > 4 ? fullName.substring(0, 4) + "." : fullName;
    }

    private void sendSafeMessage(Player player, String message) {
        try {
            HotBarMessager.sendHotBarMessage(player, message);
        } catch (Exception e) {
            player.sendMessage(message); // Fallback to chat
        }
    }

    private ChatColor getHealthColor(Player player) {
        double healthPercent = (player.getHealth() / player.getMaxHealth()) * 100;
        if (healthPercent > 75) return ChatColor.GREEN;
        if (healthPercent > 30) return ChatColor.GOLD;
        return ChatColor.RED;
    }

    private String getArrowDirection(Player player, Vector direction) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        
        double angle = Math.toDegrees(Math.atan2(direction.getZ(), direction.getX()));
        angle = (angle - (yaw + 90) + 360) % 360;
        
        if (angle < 22.5 || angle >= 337.5) return "↑";
        if (angle < 67.5) return "↗";
        if (angle < 112.5) return "→";
        if (angle < 157.5) return "↘";
        if (angle < 202.5) return "↓";
        if (angle < 247.5) return "↙";
        if (angle < 292.5) return "←";
        return "↖";
    }
}