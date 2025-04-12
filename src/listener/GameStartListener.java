package listener;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.connorlinfoot.titleapi.TitleAPI;

import Rules.gameconfig;
import events.GameStartEvent;
import events.gameEndEvent;
import gamemodes.Gamestatus;
import teams.UHCTeamManager;
import test.main;
import decoration.ScoreboardHandler;


	public class GameStartListener implements Listener {
	    private final JavaPlugin plugin;
	    private final gameconfig config;
	    private final Map<UUID, Integer> playerPowers = new HashMap<>(); 
	    private final Set<UUID> jumpBoostPlayers = new HashSet<>();

	    public GameStartListener(JavaPlugin plugin, ScoreboardHandler scoreboardHandler,gameconfig config) {
	        this.config = config;
	        this.plugin = plugin;
	    }
		
	    @EventHandler
	    public void onGameStart(GameStartEvent e) {
	    	 boolean isGoneFishinEnabled = ((main)Bukkit.getPluginManager().getPlugin("Custom_UHC"))
                     .getGameConfig().goneFishinEnabled;
	    	Bukkit.getLogger().info("[GAME START] Checking Gone Fishin': " + isGoneFishinEnabled);
	        Bukkit.getLogger().info("GameStartEvent received!");
	        UHCTeamManager teamManager = ((main) plugin).getTeamManager();
	        Bukkit.getLogger().info("[GAME START] Teams alive: " + 
	            teamManager.getAliveTeamCount());
	        giveStartingItemsToAllPlayers();
	        World world = Bukkit.getWorld("world");
	        if (world == null) {
	            Bukkit.getLogger().warning("World is null!");
	            return;
	        }
	        if (gameconfig.getInstance().isCatEyesEnabled()) {
	            // Apply night vision to all players
	            for (Player player : Bukkit.getOnlinePlayers()) {
	                player.addPotionEffect(new PotionEffect(
	                    PotionEffectType.NIGHT_VISION, 
	                    Integer.MAX_VALUE, // Infinite duration
	                    0, // Amplifier 0 (normal strength)
	                    false, // No particles
	                    false // Not ambient
	                ));
	            }
	        }
	        if (gameconfig.getInstance().isDoubleHealthEnabled()) {
	            // Apply to all online players
	            for (Player player : Bukkit.getOnlinePlayers()) {
	                // Set max health to 40 (double normal) and fill it
	                player.setMaxHealth(40);
	                player.setHealth(40);               
	            }
	        }
	        if (gameconfig.getInstance().isMasterLevelEnabled()) {
	            int xpAmount = config.getMasterLevelAmount();
	            
	            // Apply to all online players
	            for (Player player : Bukkit.getOnlinePlayers()) {
	                player.setLevel(xpAmount);
	                player.setExp(0.99f); // Almost full XP bar
	            }
	        }
	        if (gameconfig.getInstance().isSuperHeroesEnabled()) {
	            playerPowers.clear(); // Reset for new game
	            for (Player player : Bukkit.getOnlinePlayers()) {
	                assignSuperPower(player);
	            }
	        }
	        
	        world.setDifficulty(Difficulty.HARD);
	        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule naturalRegeneration false");
	        if (gameconfig.getInstance().goneFishinEnabled){
	            Bukkit.getLogger().info("Gone Fishin' scenario is enabled - giving rods");
	            giveGoneFishinRods();
	        }
	        

	        showHealthInTablist();
	        teleportTeamsToRandomLocation();
	    }
	    @EventHandler
	    public void onPlayerJoin(PlayerJoinEvent event) {
	    	if (Gamestatus.getStatus() == 1) {
	    		if (gameconfig.getInstance().isCatEyesEnabled()) {
	    			event.getPlayer().addPotionEffect(new PotionEffect(
	    					PotionEffectType.NIGHT_VISION, 
	    					Integer.MAX_VALUE,
	    					0,
	    					false,
	    					false
	            ));
	        }
	    	    if (gameconfig.getInstance().isDoubleHealthEnabled()) {
	    	        Player player = event.getPlayer();
	    	        player.setMaxHealth(40);
	    	        player.setHealth(40);
	    	    }
	    	    if (gameconfig.getInstance().isMasterLevelEnabled()) {
	    	        Player player = event.getPlayer();
	    	        int xpAmount = gameconfig.getInstance().getMasterLevelAmount();
	    	        player.setLevel(xpAmount);
	    	        player.setExp(0.99f);
	    	    }
	    	    if (gameconfig.getInstance().isSuperHeroesEnabled()) {
	    	        assignSuperPower(event.getPlayer());
	    	    }
	    	} else {
	            for (Player player : Bukkit.getOnlinePlayers()) {
	                clearPlayerPowers(player);
	            }
	    	}
	    	
	    }
	    void assignSuperPower(Player player) {
	        // Only assign if they don't already have a power
	        if (!playerPowers.containsKey(player.getUniqueId())) {
	            int power = new Random().nextInt(5);
	            playerPowers.put(player.getUniqueId(), power);
	            applyPower(player, power);
	        } else {
	            // Reapply their original power
	            applyPower(player, playerPowers.get(player.getUniqueId()));
	        }
	    }
	    private void applyPower(Player player, int power) {
	        // Clear existing effects first
	        clearPlayerPowers(player);
	        
	        switch (power) {
	            case 0: // Speed + Haste
	                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
	                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, 1));
	                player.sendMessage("§b⚡ Your SuperHero Power: Speed II + Haste II");
	                break;
	                
	            case 1: // Strength
	                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0));
	                player.sendMessage("§c💪 Your SuperHero Power: Strength I");
	                break;
	                
	            case 2: // Resistance
	                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0));
	                player.sendMessage("§a🛡️ Your SuperHero Power: Resistance I");
	                break;
	                
	            case 3: // Double Health
	                player.setMaxHealth(40);
	                player.setHealth(40);
	                player.sendMessage("§4❤️ Your SuperHero Power: Double Health (40 HP)");
	                break;
	                
	            case 4: // Jump Boost + Haste
	                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 3)); // Jump Boost IV (amplifier 3)
	                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, 1));
	                player.sendMessage("§e🐇 Your SuperHero Power: Jump Boost IV + Haste II");
	                jumpBoostPlayers.add(player.getUniqueId()); // Track this player
	                break;
	        }
	    }
	    private void clearPlayerPowers(Player player) {
	        for (PotionEffect effect : player.getActivePotionEffects()) {
	        	jumpBoostPlayers.remove(player.getUniqueId());
	            player.removePotionEffect(effect.getType());
	        }
	        if (player.getMaxHealth() > 20) {
	            player.resetMaxHealth();
	        }
	    }
	    @EventHandler
	    public void onPlayerDamage(EntityDamageEvent event) {
	        if (event.getEntity() instanceof Player && 
	            event.getCause() == EntityDamageEvent.DamageCause.FALL) {
	            Player player = (Player) event.getEntity();
	            if (jumpBoostPlayers.contains(player.getUniqueId())) {
	                event.setCancelled(true);
	            }
	        }
	    }
	    private void giveStartingItemsToAllPlayers() {
	        // Get the saved starting inventory from gameconfig
	        ItemStack[] startingInventory = gameconfig.getStartingInventory();
	        ItemStack[] startingArmor = gameconfig.getStartingArmor();

	        for (Player player : Bukkit.getOnlinePlayers()) {
	            // Clear the player's inventory completely
	            player.getInventory().clear();
	            player.getInventory().setArmorContents(null);

	            // Give saved items (if they exist)
	            if (startingInventory != null) {
	                player.getInventory().setContents(startingInventory);
	            }
	            if (startingArmor != null) {
	                player.getInventory().setArmorContents(startingArmor);
	            }

	            player.updateInventory();
	            player.sendMessage(ChatColor.GREEN + "§aYou received the starting items!");
	        }
	    }

	    void giveGoneFishinRods() {
	        Bukkit.getLogger().info("[DEBUG] Attempting to give rods to " + Bukkit.getOnlinePlayers().size() + " players");
	        
	        for (Player player : Bukkit.getOnlinePlayers()) {
	            Bukkit.getLogger().info("[DEBUG] Processing " + player.getName());
	            player.getInventory().addItem(createGoneFishinRod());
	            player.sendMessage(ChatColor.AQUA + "You received a Gone Fishin' rod!");
	        }
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
	            Player topDamager = (damageTracker != null) ? damageTracker.getTopDamager() : null;
	            double topDamage = (topDamager != null) ? damageTracker.getPlayerDamage(topDamager) : 0.0;
	            Player winner = e.getWinner();
	            Player topKiller = e.gettopkiller();

	            // Play EXP gain sound to all players
	            Sound levelUpSound;
	            try {
	                levelUpSound = Sound.valueOf("ENTITY_PLAYER_LEVELUP"); // 1.12.2 sound
	            } catch (IllegalArgumentException ex) {
	                levelUpSound = Sound.valueOf("LEVEL_UP"); // Fallback for other versions
	            }
	            
	            for (Player player : Bukkit.getOnlinePlayers()) {
	                if (player != null) {
	                    player.playSound(player.getLocation(), levelUpSound, 1.0f, 1.0f);
	                    TitleAPI.sendTitle(player, 5, 5, 5, "§e§lGAME ENDED", "§aThanks for playing !");
	                }
	            }

	            // Broadcast game results - with null checks
	            Bukkit.broadcastMessage("§e§l――――――――――――――――――――――――");
	            Bukkit.broadcastMessage(" ");
	            Bukkit.broadcastMessage("      §e§lGAME ENDED ");
	            Bukkit.broadcastMessage(" ");
	            Bukkit.broadcastMessage(" §7§l• §eWinner: " + (winner != null ? winner.getName() : "None"));
	            Bukkit.broadcastMessage(" §7§l• §eTop Killer: " + (topKiller != null ? topKiller.getName() : "None") + 
	                                  " §ewith §c§l" + (topKiller != null ? e.gettopkiller() : "0") + " §ekills!");
	            Bukkit.broadcastMessage(" §7§l• §eTop Damager: " + (topDamager != null ? topDamager.getName() : "None") + 
	                                  " §ewith §c§l" + topDamage + " §edamage dealt!");
	            Bukkit.broadcastMessage(" ");
	            Bukkit.broadcastMessage("§e§l――――――――――――――――――――――――");
	            Bukkit.broadcastMessage("§7You will be kicked out of the server in 30 seconds...");

	            // Launch fireworks at winner's location if exists
	            if (winner != null) {
	                launchFireworks(winner.getLocation(), 3);
	            }
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

