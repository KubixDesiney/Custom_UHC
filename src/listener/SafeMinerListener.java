package listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
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
        
        // Mark player as pending revive
        pendingRevives.put(player.getUniqueId(), true);
        
        // Save player data
        Location deathLocation = player.getLocation();
        ItemStack[] inventory = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        List<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());
        
        // Schedule revival
        if (gameconfig.getInstance().isSpectatorModeEnabled()) {
            event.setDeathMessage(null);
            event.setKeepInventory(false);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.GRAY + "You are now spectating the match.");
        } else {
        	new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.kickPlayer(ChatColor.RED + "You died! Spectator mode is disabled.");
                }
                if (!player.isOnline() || !pendingRevives.containsKey(player.getUniqueId())) {
                    pendingRevives.remove(player.getUniqueId());
                    return;
                }
                
                // Properly revive player
                player.spigot().respawn();
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(deathLocation);
                
                // Restore inventory
                player.getInventory().setContents(inventory);
                player.getInventory().setArmorContents(armor);
                if (gameconfig.getInstance().isDoubleHealthEnabled()) {
                player.setHealth(40);
                } else {
                	player.setHealth(20);
                }
                player.setFoodLevel(20);
                player.setSaturation(20);
                
                // Restore effects
                for (PotionEffect effect : effects) {
                    player.addPotionEffect(effect);
                }
                
                // Restore scenarios
                restoreScenarios(player);
                
                player.sendMessage("§aYou have been revived by the SafeMiner scenario!");
                pendingRevives.remove(player.getUniqueId());
            }
        }.runTaskLater(main.getInstance(), 20L); // 1 second delay
        
        // Prevent death message and keep inventory
        event.setDeathMessage(null);
        event.setKeepInventory(true);
        event.getDrops().clear();
    }
    }
    
    public boolean isPendingRevive(UUID playerId) {
        return pendingRevives.containsKey(playerId);
    }
    
    private void restoreScenarios(Player player) {
        if (gameconfig.getInstance().isSuperHeroesEnabled()) {
            GameStartListener gameStartListener = new GameStartListener(main.getInstance(), null, config);
            gameStartListener.assignSuperPower(player);
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
        
        if (gameconfig.getInstance().isGoneFishinEnabled()) {
            GameStartListener gameStartListener = new GameStartListener(main.getInstance(), null, config);
            gameStartListener.giveGoneFishinRods();
        }
    }
}