package teams;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import utilities.HotBarMessager;

import java.util.List;
import java.util.logging.Level;

public class TeamDistanceTracker {

    private final UHCTeamManager teamManager;
    private final JavaPlugin plugin;
    private int taskId = -1;

    public TeamDistanceTracker(UHCTeamManager teamManager, JavaPlugin plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;
    }

    public void startTracking() {
        // Cancel previous task if running
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        
        // Schedule new repeating task (20 ticks = 1 second)
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateAllPlayers, 0L, 20L);
    }

    public void stopTracking() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerHotbar(player);
        }
    }

    private void updatePlayerHotbar(Player player) {
        try {
            
          
            
            String teamName = teamManager.getPlayerTeam(player);
            if (teamName == null || teamName.isEmpty()) {
                sendDebugMessage(player, ChatColor.RED + "No team!");
                return;
            }

            List<Player> teammates = UHCTeamManager.getPlayersInTeam(teamName);
            teammates.remove(player); // Remove self

            if (teammates.isEmpty()) {
                sendDebugMessage(player, ChatColor.YELLOW + "No teammates");
                return;
            }

            // Find closest teammate
            Player closest = null;
            double minDistance = Double.MAX_VALUE;
            
            for (Player teammate : teammates) {
                if (!teammate.isOnline()) continue;
                
                double distance = player.getLocation().distance(teammate.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = teammate;
                }
            }

            if (closest != null) {
                Vector direction = closest.getLocation().toVector().subtract(player.getLocation().toVector());
                direction.normalize();
                
                String message = String.format("%s%s §7%.0fm §e%s",
                    getHealthColor(closest),
                    getShortName(closest.getName()),
                    minDistance,
                    getArrowDirection(player, direction));
                
                sendDebugMessage(player, message);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "TeamTracker error for " + player.getName(), e);
            sendDebugMessage(player, ChatColor.RED + "Team tracker error");
        }
    }

    private void sendDebugMessage(Player player, String message) {
        plugin.getLogger().info("Sending to " + player.getName() + ": " + message);
        try {
            HotBarMessager.sendHotBarMessage(player, message);
        } catch (Exception e) {
            player.sendMessage(message); // Fallback to chat
        }
    }

    private String getShortName(String name) {
        return name.length() > 4 ? name.substring(0, 4) + "." : name;
    }

    private ChatColor getHealthColor(Player player) {
        double healthPercent = player.getHealth() / player.getMaxHealth();
        if (healthPercent > 0.75) return ChatColor.GREEN;
        if (healthPercent > 0.25) return ChatColor.YELLOW;
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