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
        
        pendingRevives.put(player.getUniqueId(), true);
        Location deathLocation = player.getLocation();
        ItemStack[] inventory = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        List<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());
        int xpLevel = player.getLevel();
        float xpProgress = player.getExp();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !pendingRevives.containsKey(player.getUniqueId())) {
                    pendingRevives.remove(player.getUniqueId());
                    return;
                }
                
                player.spigot().respawn();
                player.setGameMode(GameMode.SURVIVAL);
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Restore basic player state
                        player.teleport(deathLocation);
                        player.getInventory().setContents(inventory);
                        player.getInventory().setArmorContents(armor);
                        
                        // Restore health based on scenarios
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
                        
                        // Restore all effects
                        for (PotionEffect effect : effects) {
                            player.addPotionEffect(effect);
                        }
                        
                        // Fully restore all scenarios
                        restoreAllScenarios(player);
                        
                        // Apply invincibility
                        makeInvincible(player);
                        
                        player.sendMessage(ChatColor.GREEN + "You have been revived by the SafeMiner scenario!");
                        player.sendMessage(ChatColor.GOLD + "You are invincible for 20 seconds!");
                        pendingRevives.remove(player.getUniqueId());
                    }
                }.runTaskLater(main.getInstance(), 5L);
            }
        }.runTaskLater(main.getInstance(), 40L);
        
        event.setDeathMessage(null);
        event.setKeepInventory(true);
        event.getDrops().clear();
    }
    
    private void restoreAllScenarios(Player player) {
        // Restore SuperHero power first
        if (gameconfig.getInstance().isSuperHeroesEnabled() && originalPowers.containsKey(player.getUniqueId())) {
            GameStartListener gameStartListener = new GameStartListener(main.getInstance(), null, config);
            gameStartListener.clearPlayerPowers(player); // Clear first to avoid conflicts
            gameStartListener.applyPower(player, originalPowers.get(player.getUniqueId()));
        }
        
        // Restore other scenarios
        if (config.isCatEyesEnabled()) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION, 
                Integer.MAX_VALUE, 
                0, 
                false, 
                false
            ));
        }
        
        
        if (gameconfig.getInstance().isMasterLevelEnabled()) {
            int xpAmount = config.getMasterLevelAmount();
            player.setLevel(xpAmount);
            player.setExp(0.99f);
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
        }.runTaskLater(main.getInstance(), 400L);
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