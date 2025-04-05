package listener;


import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.connorlinfoot.titleapi.TitleAPI;

import Rules.gameconfig;
import events.GameStartEvent;
import events.gameEndEvent;
import teams.UHCTeamManager;
import test.main;
import decoration.ScoreboardHandler;


	public class GameStartListener implements Listener {
	    private final ScoreboardHandler scoreboardHandler;
	    private final JavaPlugin plugin;
	    private final gameconfig config;

	    public GameStartListener(JavaPlugin plugin, ScoreboardHandler scoreboardHandler,gameconfig config) {
	        this.config = config;
	        this.plugin = plugin;
	        this.scoreboardHandler = scoreboardHandler;
	    }
		
	    @EventHandler
	    public void onGameStart(GameStartEvent e) {
	        Bukkit.getLogger().info("GameStartEvent received!");
	        
	        World world = Bukkit.getWorld("world");
	        if (world == null) {
	            Bukkit.getLogger().warning("World is null!");
	            return;
	        }

	        
	        world.setDifficulty(Difficulty.HARD);
	        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule naturalRegeneration false");
	        if (config.goneFishinEnabled) {
	            Bukkit.getLogger().info("Gone Fishin' scenario is enabled - giving rods");
	            giveGoneFishinRods();
	        }
	        

	        showHealthInTablist();
	        teleportTeamsToRandomLocation();
	    }

	    private void giveGoneFishinRods() {
	        ItemStack rod = createGoneFishinRod();
	        
	        for (Player player : Bukkit.getOnlinePlayers()) {
	            try {
	                // Clear existing rods
	                player.getInventory().remove(Material.FISHING_ROD);
	                
	                // Give new rod
	                player.getInventory().addItem(rod.clone());
	                player.sendMessage(ChatColor.AQUA + "You received a Gone Fishin' rod!");
	            } catch (Exception ex) {
	                Bukkit.getLogger().warning("Error giving rod to " + player.getName() + ": " + ex.getMessage());
	            }
	        }
	        
	        Bukkit.broadcastMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Gone Fishin' scenario is active!");
	    }

	    private ItemStack createGoneFishinRod() {
	        ItemStack rod = new ItemStack(Material.FISHING_ROD);
	        ItemMeta meta = rod.getItemMeta();
	        
	        meta.setDisplayName(ChatColor.AQUA + "Gone Fishin' Rod");
	        meta.addEnchant(Enchantment.LUCK, 255, true);
	        meta.addEnchant(Enchantment.LURE, 3, true);
	        meta.spigot().setUnbreakable(true);
	        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
	        
	        List<String> lore = new ArrayList<>();
	        lore.add(ChatColor.GRAY + "Special Scenario Rod");
	        lore.add("");
	        lore.add(ChatColor.GREEN + "Unbreakable");
	        lore.add(ChatColor.GREEN + "Max Luck of the Sea");
	        lore.add(ChatColor.GREEN + "Max Lure");
	        meta.setLore(lore);
	        
	        rod.setItemMeta(meta);
	        return rod;
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
	        DamageTracker damageTracker = main.getInstance().getDamageTracker();
	        Player topDamager = damageTracker.getTopDamager();
	        double topDamage = (topDamager != null) ? damageTracker.getPlayerDamage(topDamager) : 0.0;
	        Player winner = e.getWinner();
	        Player topKiller = e.gettopkiller();

	        // Play EXP gain sound to all players
	        for (Player player : Bukkit.getOnlinePlayers()) {
	            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
	            TitleAPI.sendTitle(player,3,3,3,"§e§lGAME ENDED","§aThanks for playing !");
	        }

	        // Broadcast game results
	        Bukkit.broadcastMessage("§e§l――――――――――――――――――――――――");
	        Bukkit.broadcastMessage(" ");
	        Bukkit.broadcastMessage("      §e§lGAME ENDED ");
	        Bukkit.broadcastMessage(" ");
	        Bukkit.broadcastMessage(" §7§l• §eWinner: " + winner.getName());
	        Bukkit.broadcastMessage(" §7§l• §eTop Killer: " + topKiller.getName() + " §ewith §c§l" + e.gettopkiller() + " §ekills!");
	        Bukkit.broadcastMessage(" §7§l• §eTop Damager: " + (topDamager != null ? topDamager.getName() : "None") 
	                + " §ewith §c§l" + topDamage + " §edamage dealt!");
	        Bukkit.broadcastMessage(" ");
	        Bukkit.broadcastMessage("§e§l――――――――――――――――――――――――");
	        Bukkit.broadcastMessage("§7You will be kicked out of the server in 30 seconds...");

	        // Launch fireworks at winner's location
	        launchFireworks(winner.getLocation(), 3);

	        // Delay before kicking players
	        new BukkitRunnable() {
	            @Override
	            public void run() {
	                for (Player player : Bukkit.getOnlinePlayers()) {
	                    player.kickPlayer("§eThanks for playing!");
	                }
	            }
	        }.runTaskLater(main.getInstance(), 600L); // 600L = 30 seconds (20 ticks = 1 sec)
	    }

	    private void launchFireworks(Location loc, int amount) {
	        World world = loc.getWorld();
	        for (int i = 0; i < amount; i++) {
	            Firework firework = (Firework) world.spawnEntity(loc, EntityType.FIREWORK);
	            FireworkMeta meta = firework.getFireworkMeta();
	            meta.addEffect(FireworkEffect.builder()
	                    .withColor(org.bukkit.Color.ORANGE)
	                    .withFade(org.bukkit.Color.YELLOW)
	                    .with(FireworkEffect.Type.BALL_LARGE)
	                    .withFlicker()
	                    .build());
	            meta.setPower(2);
	            firework.setFireworkMeta(meta);
	        }
	    }

	}

