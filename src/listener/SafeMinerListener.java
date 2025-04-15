package listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

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
    private final Map<UUID, Boolean> pendingRevives = new HashMap<>();
    private final Map<UUID, Integer> originalPowers = new HashMap<>(); // Store original superhero powers
    private final Map<UUID, Boolean> invinciblePlayers = new HashMap<>(); // Track invincibility

    public SafeMinerListener(gameconfig config, UHCTeamManager teamManager) {
        this.config = config;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gameconfig.getInstance().isSafeMinerEnabled() || Gamestatus.getStatus() != 1) return;
        
        Player player = event.getEntity();
        
        // Skip if meetup has started or death was caused by another player
        if (gameconfig.getMeetupTime() <= 0 || player.getKiller() != null) {
            return;
        }
        
        // Store original superhero power if enabled
        if (gameconfig.getInstance().isSuperHeroesEnabled()) {
            GameStartListener gameStartListener = new GameStartListener(main.getInstance(), null, config);
            originalPowers.put(player.getUniqueId(), gameStartListener.getPlayerPower(player.getUniqueId()));
        }
        
        // Mark player as pending revive
        pendingRevives.put(player.getUniqueId(), true);
        
        // Save player data
        Location deathLocation = player.getLocation();
        ItemStack[] inventory = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        List<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());
        int xpLevel = player.getLevel(); // Store XP level
        float xpProgress = player.getExp(); // Store XP progress
        
        // Schedule revival with a 2-second delay (40 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !pendingRevives.containsKey(player.getUniqueId())) {
                    pendingRevives.remove(player.getUniqueId());
                    return;
                }
                
                // Properly revive player
                player.spigot().respawn();
                player.setGameMode(GameMode.SURVIVAL);
                
                // Small delay before teleporting to avoid issues
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.teleport(deathLocation);
                        
                        // Restore inventory
                        player.getInventory().setContents(inventory);
                        player.getInventory().setArmorContents(armor);
                        
                        // Restore health based on scenario
                        if (gameconfig.getInstance().isDoubleHealthEnabled()) {
                            player.setMaxHealth(40);
                            player.setHealth(40);
                        } else {
                            player.setHealth(20);
                        }
                        
                        // Restore food and saturation
                        player.setFoodLevel(20);
                        player.setSaturation(20);
                        
                        // Restore XP
                        player.setLevel(xpLevel);
                        player.setExp(xpProgress);
                        
                        // Restore effects
                        for (PotionEffect effect : effects) {
                            player.addPotionEffect(effect);
                        }
                        
                        // Restore scenarios
                        restoreScenarios(player);
                        
                        // Make player invincible for 20 seconds
                        makeInvincible(player);
                        
                        player.sendMessage(ChatColor.GREEN + "You have been revived by the SafeMiner scenario!");
                        player.sendMessage(ChatColor.GOLD + "You are invincible for 20 seconds!");
                        pendingRevives.remove(player.getUniqueId());
                    }
                }.runTaskLater(main.getInstance(), 5L);
            }
        }.runTaskLater(main.getInstance(), 40L); // 2 second delay
        
        // Prevent death message and keep inventory
        event.setDeathMessage(null);
        event.setKeepInventory(true);
        event.getDrops().clear();
    }
    
    private void makeInvincible(Player player) {
        invinciblePlayers.put(player.getUniqueId(), true);
        
        // Remove invincibility after 20 seconds (400 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                invinciblePlayers.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "Your invincibility has worn off!");
            }
        }.runTaskLater(main.getInstance(), 400L);
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        // Check for invincibility
        if (invinciblePlayers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        
        // Check for fall damage immunity (jump boost players)
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && 
            gameconfig.getInstance().isSuperHeroesEnabled() &&
            originalPowers.containsKey(player.getUniqueId()) &&
            originalPowers.get(player.getUniqueId()) == 4) { // Power 4 is jump boost
            event.setCancelled(true);
        }
    }
    
    public boolean isPendingRevive(UUID playerId) {
        return pendingRevives.containsKey(playerId);
    }
    
    private void restoreScenarios(Player player) {
        // Restore original superhero power if enabled
        if (gameconfig.getInstance().isSuperHeroesEnabled() && originalPowers.containsKey(player.getUniqueId())) {
            GameStartListener gameStartListener = new GameStartListener(main.getInstance(), null, config);
            gameStartListener.applyPower(player, originalPowers.get(player.getUniqueId()));
        }
        
        if (config.isCatEyesEnabled()) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION, 
                Integer.MAX_VALUE, 
                0, 
                false, 
                false
            ));
        }
    }
}