package listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.connorlinfoot.titleapi.TitleAPI;

import Rules.gameconfig;
import gamemodes.Gamestatus;
import teams.UHCTeamManager;
import test.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SafeMinerListener implements Listener {
    private final gameconfig config;
    private final UHCTeamManager teamManager;
    private final main plugin;
    private final Map<UUID, Boolean> pendingRevives = new HashMap<>();
    private final Map<UUID, Integer> originalPowers = new HashMap<>();
    private final Map<UUID, Boolean> invinciblePlayers = new HashMap<>();
    private final Map<UUID, List<PotionEffect>> savedEffects = new HashMap<>();

    public SafeMinerListener(main plugin,gameconfig config, UHCTeamManager teamManager) {
    	this.plugin = plugin;
        this.config = config;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gameconfig.getInstance().isSafeMinerEnabled() || Gamestatus.getStatus() != 1) return;
        
        
        Player player = event.getEntity();
        gameconfig config = gameconfig.getInstance();
        if (config.isKingsEnabled()) {
            String teamName = teamManager.getPlayerTeam(player);
            Player king = config.getTeamKing(teamName);
            
            if (player.equals(king)) {
                // King died
                if (!config.isSafeMinerEnabled() || player.getKiller() != null) {
                    // Only apply effects if not safe miner or if killed by player
                    handleKingDeath(teamName, player);
                }
            }
        }
        
        if (gameconfig.getMeetupTime() <= 0 || player.getKiller() != null) {
            return;
        }
        
        // Store all relevant player data
        if (gameconfig.getInstance().isSuperHeroesEnabled()) {
            GameStartListener gameStartListener = new GameStartListener(main.getInstance(), null, config);
            originalPowers.put(player.getUniqueId(), gameStartListener.getPlayerPower(player.getUniqueId()));
        }
        // Store all active effects
        List<PotionEffect> effectsToSave = new ArrayList<>(player.getActivePotionEffects());
        savedEffects.put(player.getUniqueId(), effectsToSave);
        
        pendingRevives.put(player.getUniqueId(), true);
        Location deathLocation = player.getLocation();
        ItemStack[] inventory = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        int xpLevel = player.getLevel();
        float xpProgress = player.getExp();
        
        // Instant respawn
        event.setDeathMessage(null);
        event.setKeepInventory(true);
        event.getDrops().clear();
        player.spigot().respawn();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !pendingRevives.containsKey(player.getUniqueId())) {
                    cleanupPlayerData(player.getUniqueId());
                    return;
                }
                
                // Immediate state restoration
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(deathLocation);
                player.getInventory().setContents(inventory);
                player.getInventory().setArmorContents(armor);
                
                // Health and food
                if (gameconfig.getInstance().isDoubleHealthEnabled() || 
                        (gameconfig.getInstance().isSuperHeroesEnabled() && 
                         originalPowers.containsKey(player.getUniqueId()) && 
                         originalPowers.get(player.getUniqueId()) == 3)) {
                        player.setMaxHealth(40);
                        player.setHealth(40);
                    } else {
                        player.setHealth(20);
                    }
                player.setFoodLevel(20);
                player.setSaturation(20);
                player.setLevel(xpLevel);
                player.setExp(xpProgress);
                
                // Apply invincibility immediately
                makeInvincible(player);
                
                player.sendMessage(ChatColor.GREEN + "You have been revived by the SafeMiner scenario!");
                player.sendMessage(ChatColor.GOLD + "You are invincible for 20 seconds!");
                
                // Apply effects via commands after 3 seconds
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            cleanupPlayerData(player.getUniqueId());
                            return;
                        }
                        if (config.isKingsEnabled()) {
                            String teamName = teamManager.getPlayerTeam(player);
                            Player king = config.getTeamKing(teamName);
                            
                            if (player.equals(king)) {
                                // Restore king's double health
                                player.setMaxHealth(40);
                                player.setHealth(40);
                            }
                        }

                        // Clear existing effects silently
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect " + player.getName() +"clear");

                        // Restore health based on scenarios
                        if (gameconfig.getInstance().isDoubleHealthEnabled() || 
                            (gameconfig.getInstance().isSuperHeroesEnabled() && 
                             originalPowers.containsKey(player.getUniqueId()) && 
                             originalPowers.get(player.getUniqueId()) == 3)) {
                            player.setMaxHealth(40);
                            player.setHealth(40);
                        }

                        // Reapply SuperHero power if enabled
                        if (gameconfig.getInstance().isSuperHeroesEnabled() && originalPowers.containsKey(player.getUniqueId())) {
                            int power = originalPowers.get(player.getUniqueId());
                            switch (power) {
                                case 0: // Speed + Haste
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                        "effect " + player.getName() + " speed 1000000 1 true");
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                        "effect " + player.getName() + " haste 1000000 1 true");
                                    break;
                                case 1: // Strength
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                        "effect " + player.getName() + " strength 1000000 0 true");
                                    break;
                                case 2: // Resistance
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                        "effect " + player.getName() + " resistance 1000000 0 true");
                                    break;
                                case 3: // Double Health (already handled above)
                                    break;
                                case 4: // Jump Boost + Haste
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                        "effect " + player.getName() + " jump_boost 1000000 3 true");
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                        "effect " + player.getName() + " haste 1000000 1 true");
                                    break;
                            }
                        }

                        // Reapply CatEyes if enabled
                        if (gameconfig.getInstance().isCatEyesEnabled()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                "effect " + player.getName() + " night_vision 1000000 0 true");
                        }

                        // Reapply any other saved effects
                        if (savedEffects.containsKey(player.getUniqueId())) {
                            for (PotionEffect effect : savedEffects.get(player.getUniqueId())) {
                                String effectName = getMinecraftEffectName(effect.getType());
                                if (effectName != null) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                        String.format("effect %s %s %d %d true",
                                            player.getName(),
                                            effectName,
                                            Math.min(effect.getDuration()/20, 1000000),
                                            effect.getAmplifier()));
                                }
                            }
                        }
                    

                        cleanupPlayerData(player.getUniqueId());
                    }
                }.runTaskLater(main.getInstance(), 60L); // 3 second delay
            }
        }.runTaskLater(main.getInstance(), 5L); // Small initial delay
    }
    private void handleKingDeath(String teamName, Player king) {
        UHCTeamManager teamManager = ((main) plugin).getTeamManager();
        gameconfig config = gameconfig.getInstance();
        
        // Play sound and show title to team members
        Sound deathSound = Sound.valueOf("ENTITY_ENDERDRAGON_DEATH"); // Scary sound
        String prefix = teamManager.getConfigManager().getTeamPrefix(teamName);
        
        for (Player teammate : UHCTeamManager.getPlayersInTeam(teamName)) {
            teammate.playSound(teammate.getLocation(), deathSound, 1.0f, 1.0f);
            TitleAPI.sendTitle(teammate, 10, 70, 10, "§4§l⮚ §r§cTHE KING IS DEAD §4§l⮘", "");
            
            // Apply poison effect
            teammate.addPotionEffect(new PotionEffect(
                PotionEffectType.POISON, 
                30 * 20, // 30 seconds
                1 // Level 2
            ));
        }
        
        // Broadcast death message
        String broadcastMsg = ChatColor.translateAlternateColorCodes('&',
            "§e§lUHC §f§l│ §r" + prefix + king.getName() + 
            " §ethe king of " + prefix + teamName + " §eis dead !");
        Bukkit.broadcastMessage(broadcastMsg);
        
        // Remove king from config
        config.teamKings.remove(teamName);
    }
    // Helper method to get proper Minecraft effect names
    private String getMinecraftEffectName(PotionEffectType type) {
        if (type == null) return null;
        
        // Map Bukkit effect types to Minecraft names
        switch (type.getName()) {
            case "INCREASE_DAMAGE": return "strength";
            case "DAMAGE_RESISTANCE": return "resistance";
            case "FAST_DIGGING": return "haste";
            case "SPEED": return "speed";
            case "JUMP": return "jump_boost";
            case "NIGHT_VISION": return "night_vision";
            // Add more mappings as needed
            default: return type.getName().toLowerCase();
        }
    }
    
    private void makeInvincible(Player player) {
        invinciblePlayers.put(player.getUniqueId(), true);
        new BukkitRunnable() {
            @Override
            public void run() {
                invinciblePlayers.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "Your invincibility has worn off!");
            }
        }.runTaskLater(main.getInstance(), 400L); // 20 seconds
    }
    
    private void cleanupPlayerData(UUID playerId) {
        pendingRevives.remove(playerId);
        savedEffects.remove(playerId);
        originalPowers.remove(playerId);
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        if (invinciblePlayers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && 
            gameconfig.getInstance().isSuperHeroesEnabled() &&
            originalPowers.containsKey(player.getUniqueId()) &&
            originalPowers.get(player.getUniqueId()) == 4) {
            event.setCancelled(true);
        }
    }
    
    public boolean isPendingRevive(UUID playerId) {
        return pendingRevives.containsKey(playerId);
    }
}