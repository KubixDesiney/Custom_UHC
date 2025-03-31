package listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.HashMap;
import java.util.UUID;

public class DamageTracker implements Listener {

    // HashMap to store player damage dealt
    private final HashMap<UUID, Double> damageMap = new HashMap<>();

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            double damage = event.getFinalDamage(); // Get final damage after armor reductions
            
            // Update damage map
            damageMap.put(damager.getUniqueId(), damageMap.getOrDefault(damager.getUniqueId(), 0.0) + damage);
        }
    }

    // Method to get the top damager
    public Player getTopDamager() {
        Player topDamager = null;
        double maxDamage = 0.0;

        for (UUID uuid : damageMap.keySet()) {
            double totalDamage = damageMap.get(uuid);
            Player player = Bukkit.getPlayer(uuid);

            if (player != null && totalDamage > maxDamage) {
                maxDamage = totalDamage;
                topDamager = player;
            }
        }
        return topDamager;
    }

    // Method to get the damage of a specific player
    public double getPlayerDamage(Player player) {
        return damageMap.getOrDefault(player.getUniqueId(), 0.0);
    }

    // Reset damage data (Call this when game restarts)
    public void resetDamageData() {
        damageMap.clear();
    }
}