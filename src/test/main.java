package test;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import Rules.gameconfig;
import commands.CommandCenter;
import decoration.ScoreboardHandler;
import events.events;
import gamemodes.Gamestatus;
import gamemodes.SwitchUHC;
import listener.DamageTracker;
import listener.GameStartListener;
import listener.GlobalVariableListener;
import listener.SafeMinerListener;
import listener.TeamChatListener;
import listener.TeamEliminationListener;
import teams.ConfigManager;
import teams.TeamDistanceTracker;
import teams.TeamSelectionSystem;
import teams.UHCTeamManager;

public class main extends JavaPlugin{
    private SwitchUHC switchUHC;
    private ConfigManager configManager;
	private UHCTeamManager teamManager;
	private DamageTracker damageTracker;
    private TeamDistanceTracker distanceTracker;
    private TeamSelectionSystem teamSelectionSystem;
    private gameconfig gameConfig;
    
	private static main instance;
	public gameconfig getGameConfig() {
	    return this.gameConfig; // Assuming you store the config instance as a field
	}
	
	@Override
	public void onEnable() {
		
		
		
	    this.saveDefaultConfig();
	    this.reloadConfig();
		int gamestatus = Gamestatus.getStatus();
		this.configManager = new ConfigManager(this);
	    this.gameConfig = new gameconfig(this);
	    gameConfig.goneFishinEnabled = getConfig().getBoolean("scenarios.gone_fishin", false);
        this.teamManager = new UHCTeamManager(this, configManager);
        distanceTracker = new TeamDistanceTracker(teamManager, this);
        distanceTracker.startTracking();
		if (gamestatus == 1) {
			

		} else {
			
			World world = Bukkit.getWorld("world");
			world.setPVP(false);
	        world.setGameRuleValue("doWeatherCycle", "false");  
	        world.setGameRuleValue("doDaylightCycle", "false");
		}
		instance = this;
		
		damageTracker = new DamageTracker();
        this.teamSelectionSystem = new TeamSelectionSystem(teamManager, this);
		ScoreboardHandler scoreBoard = new ScoreboardHandler(this,teamManager);
		CommandCenter commandCenter = new CommandCenter(teamManager, scoreBoard, configManager);
		SafeMinerListener safeMinerListener = new SafeMinerListener(gameConfig, teamManager);
		TeamEliminationListener teamEliminationListener = new TeamEliminationListener(teamManager, this, safeMinerListener);
	    this.teamSelectionSystem = new TeamSelectionSystem(teamManager, this);
	    // Clear all teams from the UHCTeamManager
	    teamManager.clearAllTeams();

	    // Clear all teams from the scoreboard
	    teamManager.clearScoreboardTeams();
	    Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
	        teamSelectionSystem.updateAllPlayersBanners();
	    }, 20L); // 1 second delay to ensure all players are loaded
	    // You can also broadcast a message indicating all teams have been cleared
	    Bukkit.getServer().broadcastMessage(ChatColor.RED + "All teams have been cleared at the start.");
	    // Proceed with other startup logic if needed
		getCommand("healall").setExecutor(commandCenter);
		getCommand("changeslot").setExecutor(commandCenter);
		getCommand("addslot").setExecutor(commandCenter);
		getCommand("Mode").setExecutor(commandCenter);
		getCommand("start").setExecutor(commandCenter);
		getCommand("finish").setExecutor(commandCenter);
		getCommand("enchant").setExecutor(commandCenter);
		getCommand("team").setExecutor(commandCenter);
		getCommand("mod").setExecutor(commandCenter);
		getServer().getPluginManager().registerEvents(safeMinerListener, this);
		getServer().getPluginManager().registerEvents(teamEliminationListener, this);
		getServer().getPluginManager().registerEvents(damageTracker, this);
	    getServer().getPluginManager().registerEvents(new GlobalVariableListener(teamManager, configManager,teamSelectionSystem), this);
	    getServer().getPluginManager().registerEvents(new TeamChatListener(teamManager), this);
		getServer().getPluginManager().registerEvents(new gameconfig(this), this);
		getServer().getPluginManager().registerEvents(new events(teamManager), this);
		getServer().getPluginManager().registerEvents(new ScoreboardHandler(this,teamManager), this);
		getServer().getPluginManager().registerEvents(new GameStartListener(this, scoreBoard, gameConfig), this);
		getServer().getConsoleSender().sendMessage(ChatColor.WHITE+"================");
		getServer().getConsoleSender().sendMessage(" ");
		getServer().getConsoleSender().sendMessage("Plugin name: "+ChatColor.YELLOW+"Custom_UHC");
		getServer().getConsoleSender().sendMessage("Creator:"+ChatColor.BLUE+"KubixDesiney");
		getServer().getConsoleSender().sendMessage("Status:"+ ChatColor.GREEN+"jawek fesfes");
		getServer().getConsoleSender().sendMessage(" ");
		getServer().getConsoleSender().sendMessage(ChatColor.WHITE+"================");
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
		    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		    Objective tabHealth = scoreboard.getObjective("tabhealth");

		    if (tabHealth != null) {
		        for (Player player : Bukkit.getOnlinePlayers()) {
		            Score score = tabHealth.getScore(player.getName());
		            score.setScore((int) player.getHealth()); // Update health
		        }
		    }
		}, 0L, 20L); // Runs every second (20 ticks)

	}
	@Override
	public void onDisable() {
		instance = null;
		saveConfig();
		getServer().getConsoleSender().sendMessage(ChatColor.WHITE+"================");
		getServer().getConsoleSender().sendMessage(" ");
		getServer().getConsoleSender().sendMessage("Plugin name: "+ChatColor.YELLOW+"Custom_UHC");
		getServer().getConsoleSender().sendMessage("Creator:"+ChatColor.BLUE+"KubixDesiney");
		getServer().getConsoleSender().sendMessage("Status:"+ ChatColor.RED+"Bye Bye");
		getServer().getConsoleSender().sendMessage(" ");
		getServer().getConsoleSender().sendMessage(ChatColor.WHITE+"================");
	}
	public TeamSelectionSystem getTeamSelectionSystem() {
	    return teamSelectionSystem;
	}
    public static main getInstance() {
        return instance;
    }
    public UHCTeamManager getTeamManager() {
        return teamManager;
    }
    public DamageTracker getDamageTracker() {
        return damageTracker;
    }
		

}