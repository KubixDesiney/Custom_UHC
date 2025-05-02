package listener;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
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
	        startHealthDisplayUpdater();
	    	setupPlayerDisplayNames();
	    	if (gameconfig.getInstance().isKingsEnabled()) {
	    	    selectKings();
	    	}
	        if (gameconfig.getInstance().isNetheribusEnabled()) {
	            int timeInMinutes = gameconfig.getInstance().getNetheribusTime();
	            startNetheribusCountdown(timeInMinutes * 60);
	        }

	    	 boolean isGoneFishinEnabled = ((main)Bukkit.getPluginManager().getPlugin("Custom_UHC"))
                     .getGameConfig().goneFishinEnabled;
	    	Bukkit.getLogger().info("[GAME START] Checking Gone Fishin': " + isGoneFishinEnabled);
	        Bukkit.getLogger().info("GameStartEvent received!");
	        UHCTeamManager teamManager = ((main) plugin).getTeamManager();
	        Bukkit.getLogger().info("[GAME START] Teams alive: " + 
	            teamManager.getAliveTeamCount());
	        assignUnassignedPlayersToTeams();
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
	    private void selectKings() {
	        UHCTeamManager teamManager = ((main) plugin).getTeamManager();
	        gameconfig config = gameconfig.getInstance();
	        
	        for (String teamName : UHCTeamManager.getAllTeams()) {
	            List<Player> teamPlayers = UHCTeamManager.getPlayersInTeam(teamName);
	            if (!teamPlayers.isEmpty()) {
	                // Select random king
	                Player king = teamPlayers.get(new Random().nextInt(teamPlayers.size()));
	                config.teamKings.put(teamName, king);
	                
	                // Give king double health
	                king.setMaxHealth(40);
	                king.setHealth(40);
	                
	                // Notify team members
	                String prefix = teamManager.getConfigManager().getTeamPrefix(teamName);
	                String kingMessage = ChatColor.translateAlternateColorCodes('&', 
	                    "§e§lUHC §f§l│ §r§eYour assigned king is: " + prefix + king.getName() + 
	                    " §eprotect him at all cost !");
	                
	                for (Player teammate : teamPlayers) {
	                    teammate.sendMessage(kingMessage);
	                }
	            }
	        }
	    }
	    private void startNetheribusCountdown(int seconds) {
	        gameconfig.setNetheribusCountdown(seconds);
	        gameconfig.setNetheribusActive(false);
	        
	        new BukkitRunnable() {
	            @Override
	            public void run() {
	                if (Gamestatus.getStatus() != 1) {
	                    cancel();
	                    return;
	                }
	                
	                int currentCountdown = gameconfig.getNetheribusCountdown();
	                
	                if (currentCountdown > 0) {
	                    gameconfig.setNetheribusCountdown(currentCountdown - 1);
	                    
	                    int minutes = currentCountdown / 60;
	                    int seconds = currentCountdown % 60;
	                    
	                    // Countdown announcements
	                    if (currentCountdown == 300) { // 5 minutes
	                        Bukkit.broadcastMessage("§e§lUHC §r§8➢ §eNetheribus will activate in §b5:00 §eminutes.");
	                    } else if (currentCountdown == 60) { // 1 minute
	                        Bukkit.broadcastMessage("§e§lUHC §r§8➢ §eNetheribus will activate in §b1:00 §eminute.");
	                    } else if (currentCountdown <= 10 && currentCountdown > 0) {
	                        Sound sound = Sound.valueOf("BLOCK_NOTE_PLING");
	                        for (Player p : Bukkit.getOnlinePlayers()) {
	                            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
	                        }
	                        Bukkit.broadcastMessage("§e§lUHC §r§8➢ §eNetheribus in §b" + 
	                            String.format("%02d:%02d", minutes, seconds) + " §eseconds!");
	                    }
	                } else if (currentCountdown == 0) {
	                    gameconfig.setNetheribusActive(true);
	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §cNetheribus has activated! Go to the Nether or take damage!");
	                    
	                    startNetheribusDamageTask();
	                    cancel();
	                }
	            }
	        }.runTaskTimer(plugin, 0L, 20L);
	    }

	    private void startNetheribusDamageTask() {
	        new BukkitRunnable() {
	            @Override
	            public void run() {
	                if (Gamestatus.getStatus() != 1) {
	                    cancel();
	                    return;
	                }
	                
	                for (Player player : Bukkit.getOnlinePlayers()) {
	                    if (player.getWorld().getName().equals("world")) { // Main world
	                        player.damage(0.5);
	                        player.sendMessage("§cYou're taking damage from Netheribus! Go to the Nether!");
	                    }
	                }
	            }
	        }.runTaskTimer(plugin, 0L, 20L); // Every second
	    }
	    private void setupPlayerDisplayNames() {
	        UHCTeamManager teamManager = ((main) plugin).getTeamManager();
	        
	        for (Player player : Bukkit.getOnlinePlayers()) {
	            String teamName = teamManager.getPlayerTeam(player);
	            String prefix = teamManager.getConfigManager().getTeamPrefix(teamName != null ? teamName : "");
	            
	            // Set player display name with team prefix
	            String displayName = ChatColor.translateAlternateColorCodes('&', 
	                (prefix != null ? prefix : "&f") + player.getName());
	            player.setDisplayName(displayName);
	            updateHealthDisplay(player);
	        }
	    }

	    private void updateHealthDisplay(Player player) {
	        // Format health to one decimal place
	        String healthStr = String.format("%.1f", player.getHealth());
	        
	        // Create health bar - 20 segments, filled based on current health
	        int healthSegments = (int) (player.getHealth() / player.getMaxHealth() * 20);
	        StringBuilder healthBar = new StringBuilder("§c");
	        for (int i = 0; i < 20; i++) {
	            healthBar.append(i < healthSegments ? "|" : " ");
	        }
	        
	        // Set the custom name visible above player's head
	        player.setCustomName(ChatColor.translateAlternateColorCodes('&', 
	            player.getDisplayName() + "\n" + healthBar.toString() + " §f" + healthStr));
	        player.setCustomNameVisible(true);
	    }

	    private void assignUnassignedPlayersToTeams() {
	        UHCTeamManager teamManager = ((main) plugin).getTeamManager();
	        gameconfig.getInstance();
			int teamSize = gameconfig.getTeamSize();
	        
	        // If team size is 1 (solo mode), we don't need to assign teams
	        if (teamSize <= 1) {
	            return;
	        }

	        // Get all online players who aren't in a team
	        List<Player> unassignedPlayers = new ArrayList<>();
	        for (Player player : Bukkit.getOnlinePlayers()) {
	            if (teamManager.getPlayerTeam(player) == null) {
	                unassignedPlayers.add(player);
	            }
	        }

	        // If no unassigned players, return
	        if (unassignedPlayers.isEmpty()) {
	            return;
	        }

	        // Get all available teams
	        List<String> allTeams = UHCTeamManager.getAllTeams();
	        if (allTeams.isEmpty()) {
	            Bukkit.getLogger().warning("No teams available to assign players!");
	            return;
	        }

	        // Assign unassigned players to random teams with available slots
	        Random random = new Random();
	        for (Player player : unassignedPlayers) {
	            // Find teams with available slots
	            List<String> availableTeams = new ArrayList<>();
	            for (String teamName : allTeams) {
	                if (UHCTeamManager.getPlayersInTeam(teamName).size() < teamSize) {
	                    availableTeams.add(teamName);
	                }
	            }

	            // If no teams with slots, break
	            if (availableTeams.isEmpty()) {
	                Bukkit.getLogger().warning("No teams with available slots left!");
	                break;
	            }

	            // Assign to random team
	            String randomTeam = availableTeams.get(random.nextInt(availableTeams.size()));
	            teamManager.joinTeam(player, randomTeam);
	            player.sendMessage(ChatColor.YELLOW + "You were automatically assigned to team " + randomTeam);
	        }
	    }
	    private void startHealthDisplayUpdater() {
	        // Update health displays every second
	        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
	            for (Player player : Bukkit.getOnlinePlayers()) {
	                updateHealthDisplay(player);
	            }
	        }, 20L, 20L);
	    }
	    @EventHandler
	    public void onPlayerJoin(PlayerJoinEvent event) {
	    	if (Gamestatus.getStatus() == 1) {
	            setupPlayerDisplayNames();
	            updateHealthDisplay(event.getPlayer());
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
	    void applyPower(Player player, int power) {
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
	    void clearPlayerPowers(Player player) {
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
	        UHCTeamManager teamManager = ((main) plugin).getTeamManager();
	        gameconfig.getInstance();
			int teamSize = gameconfig.getTeamSize();
	        
	        if (teamSize == 1) {
	            // Solo mode - teleport each player individually
	            for (Player player : Bukkit.getOnlinePlayers()) {
	                Location randomLocation = getRandomLocationInWorld();
	                player.teleport(randomLocation);
	                player.sendMessage(ChatColor.GREEN + "You have been teleported to a random location.");
	            }
	        } else {
	            // Team mode - teleport teams together
	            for (String teamName : UHCTeamManager.getAllTeams()) {
	                Bukkit.getLogger().info("Processing team: " + teamName);
	                List<Player> teamPlayers = UHCTeamManager.getPlayersInTeam(teamName);

	                if (!teamPlayers.isEmpty()) {
	                    Location randomLocation = getRandomLocationInWorld();
	                    for (Player player : teamPlayers) {
	                        Bukkit.getLogger().info("Teleporting player: " + player.getName());
	                        player.teleport(randomLocation);
	                        player.sendMessage(ChatColor.GREEN + "You have been teleported to your team's random location.");
	                    }
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
	            gameconfig.getInstance().teamKings.clear();
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
	                launchFireworks(winner.getLocation(), 5);
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
	    public int getPlayerPower(UUID playerId) {
	        return playerPowers.getOrDefault(playerId, -1);
	    }

	}

