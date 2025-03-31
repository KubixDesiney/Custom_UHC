package listener;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import events.GameStartEvent;
import events.gameEndEvent;
import teams.UHCTeamManager;
import decoration.ScoreboardHandler;


	public class GameStartListener implements Listener {
	    private final ScoreboardHandler scoreboardHandler;
	    private final JavaPlugin plugin;

	    public GameStartListener(JavaPlugin plugin, ScoreboardHandler scoreboardHandler) {
	        this.plugin = plugin;
	        this.scoreboardHandler = scoreboardHandler;
	    }
		
		@EventHandler
		public void onGameStart(GameStartEvent e) {
		    World world = Bukkit.getWorlds().get(0);
		    Bukkit.broadcastMessage("Game started");

		    if (world != null) {
		        // Set game rules for UHC
		        world.setDifficulty(Difficulty.HARD); // Set difficulty to Hard
		        
		        // Disable natural health regeneration using a command
		        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule naturalRegeneration false");
		        showHealthInTablist();
		        teleportTeamsToRandomLocation();
		    }
		}
	    private void showHealthInTablist() {
	        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
	            // Loop through all players
	            for (Player player : Bukkit.getOnlinePlayers()) {
	                // Get the player's scoreboard
	                Scoreboard scoreboard = player.getScoreboard();

	                // Check if the health objective exists
	                Objective healthObjective = scoreboard.getObjective("health");

	                if (healthObjective == null) {
	                    // Create the health objective if it doesn't exist
	                    healthObjective = scoreboard.registerNewObjective("health", "health"); // Health objective type
	                    healthObjective.setDisplayName("§c❤ Health"); // Set the display name (you can customize this)
	                    healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST); // Ensure it's displayed in the tab list
	                }

	                // Update health for each player in the tab list
	                for (Player target : Bukkit.getOnlinePlayers()) {
	                    // Update the player's health value in the tab list
	                    healthObjective.getScore(target.getName()).setScore((int) target.getHealth());
	                }
	            }
	        }, 0L, 20L); // Update every second (20 ticks = 1 second)
	    }
	    private void teleportTeamsToRandomLocation() {
	        // Get all teams and teleport players in each team
	        for (String teamName : UHCTeamManager.getAllTeams()) {
	            Bukkit.getLogger().info("Processing team: " + teamName);
	            List<Player> teamPlayers = UHCTeamManager.getPlayersInTeam(teamName); // Get the players in the team

	            if (!teamPlayers.isEmpty()) {
	                // Generate a random location within the world border
	                Location randomLocation = getRandomLocationInWorld();

	                // Teleport each player in the team to the random location
	                for (Player player : teamPlayers) {
	                    Bukkit.getLogger().info("Teleporting player: " + player.getName());
	                    player.teleport(randomLocation);
	                    player.sendMessage(ChatColor.GREEN + "You have been teleported to your team's random location.");
	                }
	            }
	        }
	    }


	    // Helper method to generate a random location within the world border
	    private Location getRandomLocationInWorld() {
	        World world = Bukkit.getWorld("world"); // Replace with the appropriate world name
	        WorldBorder border = world.getWorldBorder();
	        double borderSize = border.getSize() / 2;

	        // Generate random x and z coordinates within the world border
	        double randomX = (Math.random() * borderSize * 2) - borderSize + world.getWorldBorder().getCenter().getX();
	        double randomZ = (Math.random() * borderSize * 2) - borderSize + world.getWorldBorder().getCenter().getZ();

	        // Set a fixed y-coordinate (can be modified for random height if desired)
	        double randomY = world.getHighestBlockYAt(new Location(world, randomX, 0, randomZ)) + 1;

	        // Return the random location
	        return new Location(world, randomX, randomY, randomZ);
	    }
	
		
		
	

	    
	
	@EventHandler
	public void onGameEnd(gameEndEvent e) {
		Bukkit.broadcastMessage("§e§l――――――――――――――――――――――――");
		Bukkit.broadcastMessage(" ");
		Bukkit.broadcastMessage("      §e§lGAME ENDED ");
		Bukkit.broadcastMessage(" ");
		Bukkit.broadcastMessage(" §7§l• §eWinner: "+e.getWinner().getName());
		Bukkit.broadcastMessage(" §7§l• §r§eTop killer: "+e.gettopkiller().getName()+" §ewith §c§l"+e.gettopnumber()+" §eto his name !");
		Bukkit.broadcastMessage(" ");
		Bukkit.broadcastMessage("§e§l――――――――――――――――――――――――");
		Bukkit.broadcastMessage("§7You will be kicked out of the server in 30 seconds...");
		try {
			TimeUnit.SECONDS.sleep(30);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Player p = e.getp();
		p.kickPlayer("§eThanks for playing !");
	}

	}

