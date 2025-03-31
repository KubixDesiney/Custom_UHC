package decoration;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;

import Rules.gameconfig;
import gamemodes.Gamestatus;
import teams.UHCTeamManager;
import utilities.SimpleScoreboard;

public class ScoreboardHandler implements Listener {
    private final UHCTeamManager teamManager;

    private final JavaPlugin plugin;
    private final Map<Player, SimpleScoreboard> playerScoreboards = new HashMap<>(); // Store each player's scoreboard

    public ScoreboardHandler(JavaPlugin plugin,UHCTeamManager teamManager) {
    	this.teamManager = teamManager;
        this.plugin = plugin;
        startGlobalScoreboardUpdater();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setupScoreboard(player);
    }
    public Scoreboard getPlayerScoreboard(Player player) {
        SimpleScoreboard simpleScoreboard = playerScoreboards.get(player);
        return (simpleScoreboard != null) ? simpleScoreboard.getScoreboard() : null;
    }

    private void setupScoreboard(Player player) {
        SimpleScoreboard simpleScoreboard = new SimpleScoreboard("Game Info");

        // Save the scoreboard instance for this player
        playerScoreboards.put(player, simpleScoreboard);

        // Initialize the scoreboard
        updateStaticScoreboard(player, simpleScoreboard);

        // Set the scoreboard for the player
        simpleScoreboard.send(player);
        Bukkit.getLogger().info("Scoreboard set for player: " + player.getName());
    }

    public void startGlobalScoreboardUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (playerScoreboards.isEmpty()) return;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.isOnline()) continue; // Skip if player left

                    SimpleScoreboard simpleScoreboard = playerScoreboards.get(player);
                    if (simpleScoreboard == null) continue;

                    updateDynamicScoreboard(player, simpleScoreboard);
                    simpleScoreboard.update();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Updates every second
    }

    private void updateStaticScoreboard(Player player, SimpleScoreboard simpleScoreboard) {
        String gamename = gameconfig.getGameName();
        simpleScoreboard.setTitle("§6§l" + gamename);
        simpleScoreboard.add("§7Mode: §b" + gameconfig.getTeamSize() + "§bvs" + gameconfig.getTeamSize(), 13);
        simpleScoreboard.add("§7Players : §e" + Bukkit.getOnlinePlayers().size() + "§7/§e" + Bukkit.getMaxPlayers(), 12);
        simpleScoreboard.add("§6mc.chancemaker.com", 9);
        simpleScoreboard.add("§7――――――――――――――――――――――――", 8);
    }

    private void updateDynamicScoreboard(Player player, SimpleScoreboard simpleScoreboard) {
        String gamename = gameconfig.getGameName();
        simpleScoreboard.setTitle("§6§l" + gamename);
        long elapsedTime = gameconfig.getGameElapsedTime();
        int minutes = (int) (elapsedTime / 60000);
        int seconds = (int) ((elapsedTime / 1000) % 60);
        String formattedTime = String.format("%02d:%02d", minutes, seconds);
        simpleScoreboard.add("§eTimer : §b" + formattedTime, 13);

        int pvpTime = gameconfig.getPvPTime();
        String formattedPvPTime = gameconfig.formatTime(pvpTime);

        int meetupTime = gameconfig.getMeetupTime();
        String formattedMeetupTime = gameconfig.formatTime(meetupTime);

        World world = Bukkit.getWorld("world");
        if (world == null) return;
        WorldBorder border = world.getWorldBorder();
        int borderSize = (int) border.getSize() / 2;

        int gameStatus = Gamestatus.getStatus();
        if (gameStatus == 0) {
            updateWaitingPhase(simpleScoreboard, formattedPvPTime, formattedMeetupTime, borderSize);
        } else {
            updateGameInProgress(player, simpleScoreboard, formattedPvPTime, formattedMeetupTime, borderSize);
        }
    }

    private void updateWaitingPhase(SimpleScoreboard simpleScoreboard, String formattedPvPTime, String formattedMeetupTime, int borderSize) {
        simpleScoreboard.add(" ", 14);
        simpleScoreboard.add("§7Mode: §b" + gameconfig.getTeamSize() + "§bvs" + gameconfig.getTeamSize(), 13);
        simpleScoreboard.add("§7Players : §e" + Bukkit.getOnlinePlayers().size() + "§7/§e" + Bukkit.getMaxPlayers(), 12);
        simpleScoreboard.add(" ", 11);
        simpleScoreboard.add("§6mc.chancemaker.com", 9);
        simpleScoreboard.add("§7――――――――――――――――――――――――", 8);
    }

    private void updateGameInProgress(Player player, SimpleScoreboard simpleScoreboard, String formattedPvPTime, String formattedMeetupTime, int borderSize) {

        double playerX = player.getLocation().getX();
        double playerZ = player.getLocation().getZ();

        double deltaX = 0 - playerX; 
        double deltaZ = 0 - playerZ; 

        double angle = Math.toDegrees(Math.atan2(-deltaX, deltaZ)); 
        float playerYaw = player.getLocation().getYaw();
        if (angle < 0) {
            angle += 360;
        }
        if (playerYaw < 0) {
            playerYaw += 360; 
        }

        double relativeAngle = angle - playerYaw;
        if (relativeAngle < 0) {
            relativeAngle += 360; 
        }


        // Calculate the distance to the center
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Get the arrow direction based on the calculated angle
        String arrow = getArrowDirection(relativeAngle);

        // Format the distance
        String distanceText = String.format("§b%.1f", distance); // Format distance to one decimal place
        String arrowText = arrow + " §r§b" + distanceText; // Combine arrow and distance

        // Ensure the arrow text fits within the 16 character limit
        if (arrowText.length() > 16) {
            arrowText = arrowText.substring(0, 16); // Truncate if it exceeds 16 characters
        }

        // Update scoreboard with the new arrow and distance
        simpleScoreboard.add("§7--Players--", 16);
        simpleScoreboard.add("§eTeams: §7(" + Bukkit.getOnlinePlayers().size() + "§7)", 15);
        simpleScoreboard.add("§7--Time--", 14);
        simpleScoreboard.add("§ePvP : §b" + formattedPvPTime, 12);
        simpleScoreboard.add("§eMeetup : §b" + formattedMeetupTime, 11);
        simpleScoreboard.add("§7--Border size--", 10);
        simpleScoreboard.add("§b" + borderSize + " §7/ -§b" + borderSize, 9);
        simpleScoreboard.add("§7--info--", 8);
        simpleScoreboard.add("§eTeam:"+teamManager.getPlayerTeam(player), 7);
        simpleScoreboard.add("§eCenter: §b§l" + arrowText, 6); // Display the arrow and distance text
        simpleScoreboard.add("§eMode: §b" + gameconfig.getTeamSize() + "§bvs" + gameconfig.getTeamSize(), 5);
        simpleScoreboard.add("§7----------------", 4);
        simpleScoreboard.add("§6mc.chancemaker.net", 3);

    }

    // Determine the arrow direction based on the angle
    private String getArrowDirection(double angle) {
        if (angle >= 337.5 || angle < 22.5) {
            return "↑"; // Forward
        } else if (angle >= 22.5 && angle < 67.5) {
            return "↗"; // Forward-Right
        } else if (angle >= 67.5 && angle < 112.5) {
            return "→"; // Right
        } else if (angle >= 112.5 && angle < 157.5) {
            return "↘"; // Back-Right
        } else if (angle >= 157.5 && angle < 202.5) {
            return "↓"; // Backward
        } else if (angle >= 202.5 && angle < 247.5) {
            return "↙"; // Back-Left
        } else if (angle >= 247.5 && angle < 292.5) {
            return "←"; // Left
        } else {
            return "↖"; // Forward-Left
        }
    }
}
