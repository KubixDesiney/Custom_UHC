package events;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;

import gamemodes.gamemode;
import teams.UHCTeamManager;
import Rules.spec;
import utilities.HotBarMessager;

public class events implements Listener {
	private final UHCTeamManager teamManager;
    private Set<Location> lightningLocations = new HashSet<>();
    public events(UHCTeamManager teamManager) {
    	this.teamManager = teamManager;
    }
	@EventHandler
	public static void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		int mode = gamemode.getMode();
		System.out.print("gamemode: "+mode);
	    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
	    
	    if (scoreboard.getObjective("showhealth") != null) {
	        player.setScoreboard(scoreboard); // Apply the scoreboard to the player
	    }
		event.setJoinMessage("");
		player.sendMessage(" ");
		player.sendMessage(" ");
		player.sendMessage(" §e§lCHANCEMAKER §r§f | Beta version");
		player.sendMessage(" ");
		player.sendMessage("   §7§l⦁ §e§lHOST §r§f ");
		player.sendMessage(" ");
		if(mode == 0) {
			player.sendMessage("   §7§l⦁ §e§lGAMEMODE §r§c§lUHC MOLE");
		}else if (mode == 1) {
			player.sendMessage("   §7§l⦁ §e§lGAMEMODE §r§c§l UHC WEREWOLF");
		} else if (mode == 2) {
			player.sendMessage("   §7§l⦁ §e§lGAMEMODE §r§6§lUHC CLASSIC");
		}
		player.sendMessage(" ");
		player.sendMessage("   §7§l⦁ §9§lDISCORD §r§8[§eclick§8]");
		player.sendMessage(" ");
		player.sendMessage(" "); 
		for(Player all : Bukkit.getServer().getOnlinePlayers()) {
			try {
				HotBarMessager.sendHotBarMessage(all, "§a§l➢ §e"+player.getDisplayName()+" §ehas joined §e(§a"+Bukkit.getServer().getOnlinePlayers().size()+"§e/§c"+Bukkit.getServer().getMaxPlayers()+"§e)");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	@EventHandler
	public static void onPlayerQuit(PlayerQuitEvent event) {
		event.setQuitMessage(" ");
		Player player = event.getPlayer();
		for(Player all : Bukkit.getServer().getOnlinePlayers()) {
			try {
				HotBarMessager.sendHotBarMessage(all, "§c§l◄ §r§e"+player.getDisplayName()+" §ehas left§e(§a"+Bukkit.getServer().getOnlinePlayers().size()+"§e/§c"+Bukkit.getServer().getMaxPlayers()+"§e)");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	  @EventHandler
	    public void onPlayerDeath1(PlayerDeathEvent event) {
	        // Check if the entity is a player
	        if (event.getEntity() instanceof Player) {
	            Player player = (Player) event.getEntity();

	            // Get the location of the player's death
	            Location deathLocation = player.getLocation();
	            String teamName = teamManager.getPlayerTeam(player);
	            EntityDamageEvent lastDamageEvent = player.getLastDamageCause();
	            String causeMessage = (lastDamageEvent != null) ? getCauseMessage(lastDamageEvent.getCause()) : "unknown";
	            // Create the custom death message
	            String deathMessage = teamName + " " + player.getName() + " died by " + causeMessage;

	            // Set the custom death message
	            event.setDeathMessage(deathMessage);

	            // Play the wither spawn sound at the death location
	            Sound sound = Sound.valueOf("ENTITY_WITHER_SPAWN");
	            deathLocation.getWorld().playSound(deathLocation, sound, 1.0F, 1.0F);

	            // Strike lightning at the death location
	            LightningStrike lightning = deathLocation.getWorld().strikeLightning(deathLocation);
	            lightningLocations.add(lightning.getLocation());


	            // Ensure that no fire is caused by the lightning strike
	            }
	        }
	  private String getCauseMessage(EntityDamageEvent.DamageCause cause) {
		    switch (cause) {
		        case FALL: return "fall";
		        case FIRE: return "fire";
		        case DROWNING: return "drowning";
		        case LAVA: return "lava";
		        case LIGHTNING: return "lightning";
		        case SUFFOCATION: return "suffocation";
		        case ENTITY_EXPLOSION: return "explosion";
		        case STARVATION: return "starvation";
		        case POISON: return "poison";
		        default: return "unknown";
		    }
		}
	    @EventHandler
	    public void onLightningStrikeDamage(EntityDamageByEntityEvent event) {
	        // Check if the entity is damaged by lightning
	        if (event.getDamager() instanceof LightningStrike && event.getEntity() instanceof Player) {
	            event.setCancelled(true); // Cancel the damage caused by lightning
	        }
	    }
	        // Listen for block burn events
	        @EventHandler
	        public void onBlockBurn(BlockBurnEvent event) {
	            Block block = event.getBlock();
	            Location blockLocation = block.getLocation();

	            // Check if the block is near any lightning strike location
	            boolean isNearLightning = false;
	            for (Location lightningLocation : lightningLocations) {
	                if (blockLocation.distanceSquared(lightningLocation) <= 25) {  // Use distanceSquared for optimization
	                    isNearLightning = true;
	                    break;
	                }
	            }

	            // If the block is near a lightning strike, cancel the burn event
	            if (isNearLightning) {
	                event.setCancelled(true);
	            }
	        }
	
	
	@EventHandler
	public static void onPlayerDeath (PlayerDeathEvent event) {
		Player player =  event.getEntity();
		boolean specvalue = spec.getspect();
		if(specvalue == false) {
			try {
				TimeUnit.MINUTES.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			player.kickPlayer("§eThanks for playing !");
		}
	}
	
}
