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
    private final Map<UUID, Integer> originalPowers = new HashMap<>();
    private final Map<UUID, Boolean> invinciblePlayers = new HashMap<>();
    private final Map<UUID, List<PotionEffect>> savedEffects = new HashMap<>();

    public SafeMinerListener(gameconfig config, UHCTeamManager teamManager) {
        this.config = config;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gameconfig.getInstance().isSafeMinerEnabled() || Gamestatus.getStatus() != 1) return;
        
        Player player = event.getEntity();
        
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
                if (gameconfig.getInstance().isDoubleHealthEnabled()) {
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

                        // Silent effect clearing
                        for (PotionEffect effect : player.getActivePotionEffects()) {
                            player.removePotionEffect(effect.getType());
                        }

                        // Apply saved effects using proper effect names
                        if (savedEffects.containsKey(player.getUniqueId())) {
                            for (PotionEffect effect : savedEffects.get(player.getUniqueId())) {
                                // Use proper Minecraft effect names
                                String effectName = getMinecraftEffectName(effect.getType());
                                if (effectName != null) {
                                    int duration = Math.min(effect.getDuration()/20, 1000000); 
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                        String.format("effect %s %s %d %d true",
                                            player.getName(),
                                            effectName,
                                            duration,
                                            effect.getAmplifier()));
                                }
                            }
                        }

                        // Reapply SuperHero power if enabled
                        if (gameconfig.getInstance().isSuperHeroesEnabled() && originalPowers.containsKey(player.getUniqueId())) {
                            GameStartListener gameStartListener = new GameStartListener(main.getInstance(), null, config);
                            gameStartListener.clearPlayerPowers(player);
                            gameStartListener.applyPower(player, originalPowers.get(player.getUniqueId()));
                        }

                        // Reapply CatEyes if enabled
                        if (gameconfig.getInstance().isCatEyesEnabled()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                "effect " + player.getName() + " night_vision 999999 0 true");
                        }

                        cleanupPlayerData(player.getUniqueId());
                    }
                }.runTaskLater(main.getInstance(), 60L); // 3 second delay
            }
        }.runTaskLater(main.getInstance(), 5L); // Small initial delay
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