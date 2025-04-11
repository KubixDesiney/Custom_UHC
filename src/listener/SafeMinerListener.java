package listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import Rules.gameconfig;
import gamemodes.Gamestatus;
import teams.UHCTeamManager;
import test.main;

import java.util.ArrayList;
import java.util.List;

public class SafeMinerListener implements Listener {
    private final gameconfig config;
    private final UHCTeamManager teamManager;

    public SafeMinerListener(gameconfig config, UHCTeamManager teamManager) {
        this.config = config;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gameconfig.getInstance().isSafeMinerEnabled()) return;
        if (Gamestatus.getStatus() != 1) return; // Only during game
        
        Player player = event.getEntity();
        Player killer = player.getKiller();
        
        // Only handle natural deaths (no killer)
        if (killer != null) return;
        
        // Save player data
        Location deathLocation = player.getLocation();
        ItemStack[] inventory = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        List<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());
        
        // Schedule revival
        Bukkit.getScheduler().runTaskLater(main.getInstance(), () -> {
            if (player.isOnline()) {
                // Teleport back to death location
                player.teleport(deathLocation);
                
                // Restore inventory
                player.getInventory().setContents(inventory);
                player.getInventory().setArmorContents(armor);
                
                // Restore health and food
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setSaturation(20);
                
                // Restore effects
                for (PotionEffect effect : effects) {
                    player.addPotionEffect(effect);
                }
                
                // Restore scenarios
                if (config.isSuperHeroesEnabled()) {
                    // Reapply super hero power if needed
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
                
                if (config.isGoneFishinEnabled()) {
                    // Give back fishing rod if needed
                    GameStartListener gameStartListener = new GameStartListener(main.getInstance(), null, config);
                    gameStartListener.giveGoneFishinRods();
                }
                
                player.sendMessage("§aYou have been revived by the SafeMiner scenario!");
            }
        }, 20L); // 1 second delay
        
        // Prevent death message and keep inventory
        event.setDeathMessage(null);
        event.setKeepInventory(true);
        event.getDrops().clear();
    }
}