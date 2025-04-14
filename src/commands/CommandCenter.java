package commands;

import java.io.File;
import gamemodes.gamemode;
import teams.ConfigManager;
import teams.UHCTeamManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import Rules.gameconfig;
import decoration.ScoreboardHandler;
import events.ServerSlotChangedEvent;
import events.GameStartEvent;
import gamemodes.Gamestatus;

public class CommandCenter implements CommandExecutor{
	String maintag = "§8[§6§lCHANCE_MAKER§r§8] §7: ";
	private Field maxPlayersField;
    private final UHCTeamManager teamManager;
    private final ConfigManager configManager;
    public CommandCenter(UHCTeamManager teamManager,ScoreboardHandler scoreBoard,ConfigManager configManager) {
        this.configManager = configManager;
        this.teamManager = teamManager;
    }
	public void onDisable() {
        updateServerProperties();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player)) {
			return true;}
		
		if (cmd.getName().equalsIgnoreCase("healall")) {
			for(Player all : Bukkit.getServer().getOnlinePlayers()) {
				double maxHealth = all.getMaxHealth();
				double currentHealth = all.getHealth();
				double healAmount = maxHealth - currentHealth;
				all.setHealth(all.getHealth()+ healAmount);
		    Bukkit.broadcastMessage("§e§lUHC §r§eFinalHeal has been preformed for everybody.");
		}
		
		}
		if (cmd.getName().equalsIgnoreCase("changeslot")) {
			if(args.length > 1) {
				Player player = (Player) sender;
				player.sendMessage(maintag+"§cAn error has been occured ");
			} else if (args.length == 1) {
					try {
						changeSlots(Integer.parseInt(args[0]));
					} catch (NumberFormatException | ReflectiveOperationException e) {
						e.printStackTrace();
					}
		}
			
		}
		if (cmd.getName().equalsIgnoreCase("mod")) {
			if(sender.isOp()) {
				String message = String.join(" ", args);
				Bukkit.broadcastMessage(" ");
				Bukkit.broadcastMessage("§7§l〉 §r§c§lFOUNDER §r§c"+sender.getName()+" §8► §r§e"+message);
				Bukkit.broadcastMessage(" ");
			}
		}
		if (cmd.getName().equalsIgnoreCase("addslot")) {
			if (args.length != 1) {
				Player player = (Player) sender;
				player.sendMessage(maintag+"§cAn error has been occured");
			} else if (args.length == 1) {
				int maxslot = Bukkit.getServer().getMaxPlayers() + Integer.parseInt(args[0]);
				ServerSlotChangedEvent event = new ServerSlotChangedEvent(Bukkit.getMaxPlayers(),maxslot); // Replace with your actual constructor for the event
	            Bukkit.getServer().getPluginManager().callEvent(event);
				gameconfig.setSlot(maxslot);
				try {
					changeSlots(maxslot);
				} catch (ReflectiveOperationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if (cmd.getName().equalsIgnoreCase("Mode")) {
			if (args.length !=1) {
				Player player = (Player) sender;
				player.sendMessage(maintag+"§cAn error has been occured");
			} else if (args.length == 1) {
				if (args[0].equalsIgnoreCase("Mole")) {
					gamemode.setMode(0);
				}
				if (args[0].equalsIgnoreCase("WW")) {
					gamemode.setMode(1);
					
				}
				if (args[0].equalsIgnoreCase("CLASS")) {
					gamemode.setMode(2);
					
				}
			}
		}
		if(cmd.getName().equalsIgnoreCase("start")) {
			Gamestatus.setStatus(1);
			Player player = (Player) sender;
			// Replace with your actual constructor for the event
            Bukkit.getServer().getPluginManager().callEvent(new GameStartEvent(player));
            Bukkit.broadcastMessage("game started"); 
		}
		Player player = (Player) sender;

	    // Ensure that at least one argument is provided
	    if (args.length == 0) {
	        player.sendMessage(ChatColor.RED + "Usage: /team <create|delete|list|join|leave> <name> [color] [size]");
	        return true;
	    }

	    String action = args[0].toLowerCase();

	    // Handle "list" command separately as it does not require args[1]
	    if (action.equals("list")) {
	        List<String> teamNames = teamManager.getAllTeams();
	        if (teamNames.isEmpty()) {
	            player.sendMessage(ChatColor.YELLOW + "There are no teams available.");
	        } else {
	            player.sendMessage(ChatColor.GREEN + "Available teams: " + ChatColor.AQUA + String.join(", ", teamNames));
	        }
	        return true;
	    }
	    if (action.equals("show")) {
	        StringBuilder message = new StringBuilder();
	        List<String> teamNames = teamManager.getAllTeams();
	        
	        if (teamNames.isEmpty()) {
	            player.sendMessage(ChatColor.YELLOW + "No teams have been created.");
	            return true;
	        }
	        
	        // Iterate through each team and display its members
	        for (String teamName : teamNames) {
	            message.append("Team: ").append(teamName).append("\n");
	            // Get the list of players in the team
	            List<Player> teamPlayers = teamManager.getPlayersInTeam(teamName);
	            if (teamPlayers.isEmpty()) {
	                message.append("No players in this team.\n");
	            } else {
	                for (Player teamPlayer : teamPlayers) {
	                    message.append("- ").append(teamPlayer.getName()).append("\n");
	                }
	            }
	            message.append("\n");
	        }

	        // Send the message to the player
	        player.sendMessage(message.toString());
	        return true;
	    }

	    // For commands that require a team name, ensure args.length >= 2
	    if (args.length < 2) {
	        player.sendMessage(ChatColor.RED + "You need to specify a team name.");
	        return true;
	    }

	    String teamName = args[1];

	    switch (action) {
	    case "create":
	        if (args.length < 4) {
	            player.sendMessage(ChatColor.RED + "Usage: /team create <name> <color> <size>");
	            return true;
	        }
	        try {
	            int size = Integer.parseInt(args[3]);
	            // Get the prefix from config instead of using color argument
	            String prefix = configManager.getTeamPrefix(teamName);
	            teamManager.createTeam(teamName, prefix, size);
	            player.sendMessage(ChatColor.GREEN + "Team " + teamName + " created successfully!");
	        } catch (NumberFormatException e) {
	            player.sendMessage(ChatColor.RED + "Size must be a number!");
	        }
	        break;

	    case "join":
	        if (args.length < 2) {
	            player.sendMessage(ChatColor.RED + "Usage: /team join <name>");
	            return true;
	        }
	        teamName = args[1];
	        try {
	            teamManager.joinTeam(player, teamName);
	            player.sendMessage(ChatColor.GREEN + "You have joined team " + teamName + "!");
	        } catch (Exception e) {
	            player.sendMessage(ChatColor.RED + "Error joining team: " + e.getMessage());
	        }
	        break;

	    case "leave":
	        teamManager.leaveTeam(player);
	        player.sendMessage(ChatColor.YELLOW + "You have left your team.");
	        break;

	    default:
	        player.sendMessage(ChatColor.RED + "Invalid subcommand!");
	}
	
		return true;
}
	
    public void changeSlots(int args) throws ReflectiveOperationException {
        Method serverGetHandle = Bukkit.getServer().getClass().getDeclaredMethod("getHandle");
        Object playerList = serverGetHandle.invoke(Bukkit.getServer());

        if (this.maxPlayersField == null) {
            this.maxPlayersField = getMaxPlayersField(playerList);
        }

        this.maxPlayersField.setInt(playerList, args);
    }
    private Field getMaxPlayersField(Object playerList) throws ReflectiveOperationException {
        Class<?> playerListClass = playerList.getClass().getSuperclass();

        try {
            Field field = playerListClass.getDeclaredField("maxPlayers");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            for (Field field : playerListClass.getDeclaredFields()) {
                if (field.getType() != int.class) {
                    continue;
                }

                field.setAccessible(true);

                if (field.getInt(playerList) == Bukkit.getServer().getMaxPlayers()) {
                    return field;
                }
            }

            throw new NoSuchFieldException("Unable to find maxPlayers field in " + playerListClass.getName());
        }
    }	
    private void updateServerProperties() {
        Properties properties = new Properties();
        File propertiesFile = new File("server.properties");

        try {
            try (InputStream is = new FileInputStream(propertiesFile)) {
                properties.load(is);
            }

            String maxPlayers = Integer.toString(Bukkit.getServer().getMaxPlayers());

            if (properties.getProperty("max-players").equals(maxPlayers)) {
                return;
            }

            properties.setProperty("max-players", maxPlayers);

            try (OutputStream os = new FileOutputStream(propertiesFile)) {
                properties.store(os, "Minecraft server properties");
            }
        } catch (IOException e) {
            
        }
    }
}
