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

public class main extends JavaPlugin {
    private SwitchUHC switchUHC;
    private ConfigManager configManager;
    private UHCTeamManager teamManager;
    private DamageTracker damageTracker;
    private TeamDistanceTracker distanceTracker;
    private TeamSelectionSystem teamSelectionSystem;
    private gameconfig gameConfig;
    
    private static main instance;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.reloadConfig();
        
        // Initialize config manager first
        this.configManager = new ConfigManager(this);
        
        // Initialize game config with proper plugin reference
        this.gameConfig = new gameconfig(this);
        
        // Load scenarios from config
        
        // Initialize team manager
        this.teamManager = new UHCTeamManager(this, configManager);
        this.distanceTracker = new TeamDistanceTracker(teamManager, this);
        distanceTracker.startTracking();
        
        // Set world rules
        World world = Bukkit.getWorld("world");
        if (world != null) {
            world.setPVP(false);
            world.setGameRuleValue("doWeatherCycle", "false");
            world.setGameRuleValue("doDaylightCycle", "false");
        }
        
        // Initialize other components
        this.damageTracker = new DamageTracker();
        this.teamSelectionSystem = new TeamSelectionSystem(teamManager, this);
        
        // Clear teams
        teamManager.clearAllTeams();
        teamManager.clearScoreboardTeams();
        
        // Initialize commands and listeners
        ScoreboardHandler scoreBoard = new ScoreboardHandler(this, teamManager);
        CommandCenter commandCenter = new CommandCenter(teamManager, scoreBoard, configManager);
        SafeMinerListener safeMinerListener = new SafeMinerListener(this, gameConfig, teamManager);
        TeamEliminationListener teamEliminationListener = new TeamEliminationListener(teamManager, this, safeMinerListener);
        
        // Register commands
        getCommand("healall").setExecutor(commandCenter);
        getCommand("changeslot").setExecutor(commandCenter);
        getCommand("addslot").setExecutor(commandCenter);
        getCommand("Mode").setExecutor(commandCenter);
        getCommand("start").setExecutor(commandCenter);
        getCommand("finish").setExecutor(commandCenter);
        getCommand("enchant").setExecutor(commandCenter);
        getCommand("team").setExecutor(commandCenter);
        getCommand("mod").setExecutor(commandCenter);
        
        // Register events
        getServer().getPluginManager().registerEvents(safeMinerListener, this);
        getServer().getPluginManager().registerEvents(teamEliminationListener, this);
        getServer().getPluginManager().registerEvents(damageTracker, this);
        getServer().getPluginManager().registerEvents(new GlobalVariableListener(teamManager, configManager, teamSelectionSystem), this);
        getServer().getPluginManager().registerEvents(new TeamChatListener(teamManager), this);
        getServer().getPluginManager().registerEvents(gameConfig, this); // Register the existing instance
        getServer().getPluginManager().registerEvents(new events(teamManager), this);
        getServer().getPluginManager().registerEvents(new ScoreboardHandler(this, teamManager), this);
        getServer().getPluginManager().registerEvents(new GameStartListener(this, scoreBoard, gameConfig), this);
        
        // Update player banners after 1 second
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            teamSelectionSystem.updateAllPlayersBanners();
        }, 20L);
        
        // Health scoreboard updater
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective tabHealth = scoreboard.getObjective("tabhealth");
            if (tabHealth != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Score score = tabHealth.getScore(player.getName());
                    score.setScore((int) player.getHealth());
                }
            }
        }, 0L, 20L);
        
        // Startup message
        getServer().getConsoleSender().sendMessage(ChatColor.WHITE + "================");
        getServer().getConsoleSender().sendMessage(" ");
        getServer().getConsoleSender().sendMessage("Plugin name: " + ChatColor.YELLOW + "Custom_UHC");
        getServer().getConsoleSender().sendMessage("Creator: " + ChatColor.BLUE + "KubixDesiney");
        getServer().getConsoleSender().sendMessage("Status: " + ChatColor.GREEN + "jawek fesfes");
        getServer().getConsoleSender().sendMessage(" ");
        getServer().getConsoleSender().sendMessage(ChatColor.WHITE + "================");
    }

    @Override
    public void onDisable() {
        // Clean up
        
        instance = null;
        saveConfig();
        
        // Shutdown message
        getServer().getConsoleSender().sendMessage(ChatColor.WHITE + "================");
        getServer().getConsoleSender().sendMessage(" ");
        getServer().getConsoleSender().sendMessage("Plugin name: " + ChatColor.YELLOW + "Custom_UHC");
        getServer().getConsoleSender().sendMessage("Creator: " + ChatColor.BLUE + "KubixDesiney");
        getServer().getConsoleSender().sendMessage("Status: " + ChatColor.RED + "Bye Bye");
        getServer().getConsoleSender().sendMessage(" ");
        getServer().getConsoleSender().sendMessage(ChatColor.WHITE + "================");
    }

    // Getters
    public gameconfig getGameConfig() {
        return this.gameConfig;
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