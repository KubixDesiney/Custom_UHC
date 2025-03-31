package teams;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import utilities.HotBarMessager;

public class TeamDistanceTracker {

    private final UHCTeamManager teamManager;

    public TeamDistanceTracker(UHCTeamManager teamManager) {
        this.teamManager = teamManager;
    }

    // Method to update the hotbar with teammates' info (distance and direction)
    public void updateHotBar(Player player) {
        String teamName = teamManager.getPlayerTeam(player);

        if (teamName == null || teamName.isEmpty()) {
            return; // Player is not in a team
        }

        // Get all players in the player's team
        for (Player teammate : UHCTeamManager.getPlayersInTeam(teamName)) {
            if (teammate == player) continue; // Skip the player himself

            // Calculate the distance between the player and the teammate
            double distance = player.getLocation().distance(teammate.getLocation());

            // Get direction vector (relative to the player's facing direction)
            Vector direction = teammate.getLocation().subtract(player.getLocation()).toVector();
            direction.normalize();  // Normalize direction vector to get the unit vector

            // Determine the arrow symbol pointing to the direction
            String directionArrow = getArrowDirection(player,direction);

            // Create a message for the teammate with the direction arrow
            String message = ChatColor.GREEN + teammate.getName() +" "+ 
                             ChatColor.AQUA  + (int)distance + "m " +
                             ChatColor.YELLOW + " " + directionArrow;

            // Send this message as a hotbar message
            try {
                HotBarMessager.sendHotBarMessage(player, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getArrowDirection(Player player, Vector direction) {
        double angle = Math.toDegrees(Math.atan2(direction.getZ(), direction.getX()));

        if (angle < 0) {
            angle += 360;
        }

        float playerYaw = player.getLocation().getYaw();
        if (playerYaw < 0) {
            playerYaw += 360; 
        }

        double relativeAngle = (angle - playerYaw + 360) % 360;

        // Return the appropriate arrow based on the relative angle
        if (relativeAngle < 22.5 || relativeAngle >= 337.5) {
            return "↑"; // North
        } else if (relativeAngle >= 22.5 && relativeAngle < 67.5) {
            return "↗"; // Northeast
        } else if (relativeAngle >= 67.5 && relativeAngle < 112.5) {
            return "→"; // East
        } else if (relativeAngle >= 112.5 && relativeAngle < 157.5) {
            return "↘"; // Southeast
        } else if (relativeAngle >= 157.5 && relativeAngle < 202.5) {
            return "↓"; // South
        } else if (relativeAngle >= 202.5 && relativeAngle < 247.5) {
            return "↙"; // Southwest
        } else if (relativeAngle >= 247.5 && relativeAngle < 292.5) {
            return "←"; // West
        } else {
            return "↖"; // Northwest
        }
    }
}
