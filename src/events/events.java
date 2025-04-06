package events;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;

import gamemodes.Gamestatus;
import gamemodes.gamemode;
import teams.UHCTeamManager;
import test.main;
import Rules.spec;
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
        		player.getInventory().clear();
        		player.setGameMode(GameMode.ADVENTURE);
        		player.setFallDistance(0);
        }
        }
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
		event.setQuitMessage(" ");
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
	  public void onPlayerDeath1(PlayerDeathEvent event) {
	      // Check if the entity is a player
	      if (event.getEntity() instanceof Player) {
	          Player player = (Player) event.getEntity();
	          int dead = 0;
	          dead = +1;
	          int alive = Gamestatus.getAlive();
	          Gamestatus.setAlive(alive - dead);
	          
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
	          for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
	              onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F);
	          }

	          // Spawn lightning without fire and damage
	          spawnSafeLightning(deathLocation);
	      }
	  }
	  private void spawnSafeLightning(Location location) {
		    // Store the location to track it
		    lightningLocations.add(location);
		    
		    // Spawn the lightning strike
		    LightningStrike lightning = location.getWorld().strikeLightningEffect(location);
		    
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
	public static void onPlayerDeath (PlayerDeathEvent event) {
		Player player =  event.getEntity();
		boolean specvalue = spec.getspect();
		int playersAlive = Gamestatus.getAlive();
		playersAlive--;
		Gamestatus.setAlive(playersAlive);
		if(specvalue == false) {
		    new BukkitRunnable() {
	            @Override
	            public void run() {
	                if (player.isOnline()) {
	                    player.kickPlayer("§eThanks for playing!");
	                }
	            }
	        }.runTaskLater(main.getInstance(), 600L); 
	    } else {
	    	player.setGameMode(GameMode.SPECTATOR);
	    }
		
			
		} 
		
	}
	

