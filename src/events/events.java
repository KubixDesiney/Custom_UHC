package events;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;

import gamemodes.Gamestatus;
import gamemodes.gamemode;
import teams.UHCTeamManager;
import test.main;
import utilities.HotBarMessager;

public class events implements Listener {
	private final UHCTeamManager teamManager;
    private Set<Location> lightningLocations = new HashSet<>();
    public events(UHCTeamManager teamManager) {
    	this.teamManager = teamManager;
    }
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (Gamestatus.getStatus() != 1 && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }
	@EventHandler
	public static void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		int mode = gamemode.getMode();
		System.out.print("gamemode: "+mode);
		int gamestatus = Gamestatus.getStatus();
        if (!player.isOp()) {
        	if (gamestatus == 0) {
                Location spawn = new Location(Bukkit.getWorld("world"), 0, 150, 0);
                player.teleport(spawn);
        		player.setGameMode(GameMode.ADVENTURE);
        		player.setFallDistance(0);
        }
        }
	    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
	    
	    if (scoreboard.getObjective("showhealth") != null) {
	        player.setScoreboard(scoreboard); 
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
			player.sendMessage("   §7§l⦁ §e§lGAMEMODE §r§6§lUHC SWITCH");
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
		event.setQuitMessage(null);
		Player player = event.getPlayer();
		for(Player all : Bukkit.getServer().getOnlinePlayers()) {
			try {
				HotBarMessager.sendHotBarMessage(all, "§c§l◄ §r§e"+player.getDisplayName()+" §ehas left§e(§a"+(Bukkit.getServer().getOnlinePlayers().size() - 1)+"§e/§c"+Bukkit.getServer().getMaxPlayers()+"§e)");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
	    Player player = event.getEntity();
	    Location deathLocation = player.getLocation();

	    // Set up custom prefix
	    String UHCPrefix = "§e§lUHC §f§l| §r";

	    // Team prefix
	    String teamName = teamManager.getPlayerTeam(player);
	    String teamPrefix = teamManager.getConfigManager().getTeamPrefix(teamName);
	    String coloredTeamPrefix = ChatColor.translateAlternateColorCodes('&', teamPrefix);

	    // Get vanilla death message
	    String vanillaMessage = event.getDeathMessage();

	    // Reconstruct custom message using vanilla text after player name
	    String playerName = player.getName();
	    String suffixMessage = vanillaMessage != null && vanillaMessage.contains(playerName)
	        ? vanillaMessage.substring(vanillaMessage.indexOf(playerName) + playerName.length()).trim()
	        : "died.";

	    String deathMessage = UHCPrefix + coloredTeamPrefix + playerName + " §e" + suffixMessage;

	    // Apply the new message
	    event.setDeathMessage(deathMessage);

	    // Play sound
	    Sound sound = Sound.valueOf("ENTITY_WITHER_SPAWN");
	    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
	        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F);
	    }

	    spawnSafeLightning(deathLocation);

	    // Update alive count
	    int alive = Gamestatus.getAlive();
	    Gamestatus.setAlive(alive - 1);
	}
	  private void spawnSafeLightning(Location location) {
		    // Store the location to track it
		    lightningLocations.add(location);
		    
		    // Schedule a task to remove the location after a short delay
		    new BukkitRunnable() {
		        @Override
		        public void run() {
		            lightningLocations.remove(location);
		        }
		    }.runTaskLater(main.getInstance(), 20L); // Remove after 1 second (20 ticks)
		}
	// Add this event handler to prevent damage from our lightning strikes
	  @EventHandler
	  public void onEntityDamage(EntityDamageByEntityEvent event) {
	      if (event.getDamager() instanceof LightningStrike) {
	          LightningStrike lightning = (LightningStrike) event.getDamager();
	          if (lightningLocations.contains(lightning.getLocation())) {
	              event.setCancelled(true);
	          }
	      }
	  }

	  // Add this event handler to prevent fire from our lightning strikes
	  @EventHandler
	  public void onBlockBurn(BlockBurnEvent event) {
	      Block block = event.getBlock();
	      for (Location loc : lightningLocations) {
	          if (loc.getWorld().equals(block.getWorld())) {
	              double distance = loc.distanceSquared(block.getLocation());
	              if (distance <= 9) { // Within 3 blocks (3 squared is 9)
	                  event.setCancelled(true);
	                  return;
	              }
	          }
	      }
	  }
	
		
	}
	

