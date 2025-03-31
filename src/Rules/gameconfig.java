package Rules;


import test.main;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import decoration.ScoreboardHandler;
import events.GameStartEvent;
import events.TeamSizeChangedEvent;
import gamemodes.Gamestatus;
import teams.UHCTeamManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class gameconfig implements Listener {
    private static String gameName = "HOST";
    private static gameconfig instance;
    private final Map<String, Integer> dropRates = new HashMap<>();

    private final String MENU_TITLE = "Drop Rate Settings";
    private boolean isNetherAccessEnabled = true;
    private World mainWorld = Bukkit.getWorld("world");
    private final Random random = new Random();
    public static double borderSpeed = 1.0; // Default speed is 1 block per second
    public static double finalBorderSize = 100.0; // Set the final border size (you can modify this dynamically)
    private boolean borderShrinking = false; // Track if the border should shrink

    private World world = Bukkit.getWorld("world"); // You can change this if you're using a different world
    private WorldBorder worldBorder = world.getWorldBorder();
    public int getDropRate(String key) {
        return dropRates.getOrDefault(key, 0);
    }

    public static gameconfig getInstance() {
        return instance;
    }

    private static long gameStartTime;
    public static long getGameElapsedTime() {
    	return System.currentTimeMillis() -gameStartTime;
    }
    private static int teamSize = 1;
    private static int pvpTime = 0;
    private static int meetupTime=0;
    private static int Slot = Bukkit.getMaxPlayers();
    UHCTeamManager manager = ((main) Bukkit.getPluginManager().getPlugin("Custom_UHC")).getTeamManager();
    private final Map<Integer, String> actions = new HashMap<>();
    private final main plugin; 
    public gameconfig(main plugin) {
    	instance=this;
        this.plugin = plugin;
        for (String key : new String[]{"APPLE", "GOLDEN_APPLE", "FLINT", "FEATHER", "ARROW", "XP_BOTTLE", "ENDER_PEARL"}) {
            dropRates.put(key, plugin.getConfig().getInt("drop_rates." + key, 0));
        }
    }


    public void openMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 36, ChatColor.GRAY + "Game Configuration");
        int maxPlayers = Bukkit.getMaxPlayers();
        addPlayerHead(menu,0,maxPlayers);

        addTeamConfigItem(menu,1);
        addItem(menu, 2, Material.EYE_OF_ENDER, "§eSpectators Configuration", "§7Control §aaccess §7to spectators.", "", "§e➢ §7Status: ", "", "§6§l➢ §eClick to change");
        addItem(menu, 3, Material.LAPIS_BLOCK, "§eInitial Border", "§7Set the §asize §7of the starting border.", "", "§e➢ §7Status: ", "", "§6§l➢ §eClick to proceed");
        addItem(menu, 4, Material.REDSTONE_BLOCK, "§eFinal Border", "§7Set the §asize §7of the final border.", "", "§e➢ §7Status: ", "", "§6§l➢ §eClick to proceed");
        addItem(menu, 5, Material.WATCH, "§eBorder Speed", "§7Control the §aborder speed.", "", "§e➢ §7Status: ", "", "§6§l➢ §eClick to adjust");
        addItem(menu, 6, Material.BARRIER, "§eBorder Start Time", "§7Control the time before the border shrinks.", "", "§e➢ §7Time: §e"+formatTime(meetupTime), "", "§6§l➢ §eClick to proceed");
        addItem(menu, 7, Material.DIAMOND_SWORD, "§ePvP Time", "§7Set the time before §aPvP §7is enabled.", "", "§e➢ §7Time: §e"+formatTime(pvpTime), "", "§6§l➢ §eClick to proceed");
        addItem(menu, 8, Material.PORTAL, "§eNether Access", "§7Toggle §aNether access.", "", "§e➢ §7Status: ", "", "§6§l➢ §eClick to change");
        addItem(menu, 9, Material.APPLE, "§eDrops Configuration", "§7Adjust item drop rates.", "", "§6§l➢ §eClick to change");
        addItem(menu, 10, Material.PAPER, "§eGame Rules", "§7Modify game rules like §aiPvP §7and §aFriendlyFire.", "", "§6§l➢ §eClick to proceed");
        addItem(menu, 11, Material.BREWING_STAND_ITEM, "§ePotions Configuration", "§7Manage potion availability and strength.", "", "§6§l➢ §eClick to proceed");
        addItem(menu, 12, Material.CHEST, "§eStarting Items", "§7Modify players' starting inventory.", "", "§6§l➢ §eClick to change");
        addItem(menu, 27, Material.BOOK, "§eScenarios", "§7View available game scenarios.", "", "§6§l➢ §eClick to proceed");
        addItem(menu, 28, Material.BOOK_AND_QUILL, "§eGame name");
        addItem(menu, 31, Material.EMERALD_BLOCK, "§2§l⮚ §a§lLet's go! §2§l⮘", "§7Start the game with selected settings!");

        player.openInventory(menu);
    }
    public void openDrop(Player player) {
        Inventory menu = Bukkit.createInventory(null, 18, MENU_TITLE);
        
        menu.setItem(0, createItem(Material.APPLE, "Apple Drop Rate: " + dropRates.get("APPLE") + "%"));
        menu.setItem(1, createItem(Material.GOLDEN_APPLE, "Golden Apple Drop Rate: " + dropRates.get("GOLDEN_APPLE") + "%"));
        menu.setItem(2, createItem(Material.FLINT, "Flint Drop Rate: " + dropRates.get("FLINT") + "%"));
        menu.setItem(3, createItem(Material.FEATHER, "Feather Drop Amount: " + dropRates.get("FEATHER")));
        menu.setItem(4, createItem(Material.ARROW, "Arrow Drop Amount: " + dropRates.get("ARROW")));
        menu.setItem(7, createItem(Material.EXP_BOTTLE, "XP Boost: "  + dropRates.getOrDefault("XP_BOTTLE", 0) + "%"));
        menu.setItem(8, createItem(Material.ENDER_PEARL, "Ender Pearl Drop Rate: " + dropRates.get("ENDER_PEARL") + "%"));
        menu.setItem(14, createItem(Material.TRIPWIRE, "Go Back"));
        
        player.openInventory(menu);
    }
    @EventHandler
    public void onInventoryClick4(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || !event.getView().getTitle().equals(MENU_TITLE)) return;

        event.setCancelled(true);
        
        Material clickedItem = event.getCurrentItem() != null ? event.getCurrentItem().getType() : null;
        if (clickedItem == null) return;

        switch (clickedItem) {
            case APPLE, GOLDEN_APPLE, FLINT, EXP_BOTTLE, ENDER_PEARL -> updateDropRate(player, clickedItem, 25, 100);
            case FEATHER, ARROW -> updateDropRate(player, clickedItem, 1, 4);
            case TRIPWIRE -> openMenu(player); // Ensure this command is handled elsewhere
        }
    }
    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
    private void updateDropRate(Player player, Material material, int increment, int max) {
        String key = material.name();

        dropRates.putIfAbsent(key, 0);
        int newRate = dropRates.get(key) + increment;
        if (newRate > max) newRate = 0; // Reset when max exceeded

        dropRates.put(key, newRate);
        plugin.getConfig().set("drop_rates." + key, newRate);
        plugin.saveConfig(); // Save to config.yml
        player.sendMessage("Updated " + key + " to " + newRate);

        openDrop(player); // Refresh menu
    }
    public void openBorderInitial(Player player) {
        Inventory menu = Bukkit.createInventory(null, 18, ChatColor.GRAY + "Initial Border");

        // First row (Red, Orange, Yellow Banners)
        ItemStack redBanner = createBanner2(ChatColor.RED, -500);
        ItemStack orangeBanner = createBanner2(ChatColor.WHITE,-100);
        ItemStack yellowBanner = createBanner2(ChatColor.YELLOW, -50);
        menu.setItem(0, redBanner);
        menu.setItem(1, orangeBanner);
        menu.setItem(2, yellowBanner);

        // Empty slot
        menu.setItem(3, new ItemStack(Material.AIR));

        menu.setItem(4, new ItemStack(Material.LAPIS_BLOCK));

        // Empty slot
        menu.setItem(5, new ItemStack(Material.AIR));

        // Second row (Cyan, Lime, Dark Green Banners)
        ItemStack cyanBanner = createBanner2(ChatColor.AQUA, 50);
        ItemStack limeBanner = createBanner2(ChatColor.GREEN, 100);
        ItemStack darkGreenBanner = createBanner2(ChatColor.DARK_GREEN, 500);
        menu.setItem(6, cyanBanner);
        menu.setItem(7, limeBanner);
        menu.setItem(8, darkGreenBanner);

        // Arrow to return to the previous menu (for example, slot 4)
        ItemStack returnArrow = new ItemStack(Material.ARROW);
        ItemMeta returnMeta = returnArrow.getItemMeta();
        returnMeta.setDisplayName(ChatColor.YELLOW + "Return to Previous Menu");
        returnArrow.setItemMeta(returnMeta);
        menu.setItem(14, returnArrow); // Slot 4 is for the return arrow

        player.openInventory(menu);
    }
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        // If this is the game configuration menu
        if (inventory.getName().equals(ChatColor.GRAY + "Game Configuration")) {
            event.setCancelled(true); // Prevent item pickup

            // Handle Border Speed (Watch)
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
            ItemMeta itemMeta = event.getCurrentItem().getItemMeta();

            if (itemMeta.getDisplayName().equals("§eBorder Speed")) {
                if (event.getAction() == InventoryAction.PICKUP_ALL || event.getAction() == InventoryAction.PICKUP_HALF) {
                    // Right-click: Increase speed by 0.5 blocks per second
                    borderSpeed += 0.5;
                    player.sendMessage("§aBorder speed increased to " + borderSpeed + " blocks per second.");
                } else if (event.getAction() == InventoryAction.PLACE_SOME) {
                    // Left-click: Reset to 1.0 blocks per second
                    borderSpeed = 1.0;
                    player.sendMessage("§aBorder speed reset to 1.0 blocks per second.");
                }
            }
        }

        // If the item clicked is the Final Border Size (Redstone Block), open the final border size menu
        if (inventory.getName().equals(ChatColor.GRAY + "Game Configuration") && event.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
            // Open the final border size menu
            openFinalBorderSizeMenu(player);
        }
    }
  
    
    public void openFinalBorderSizeMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 18, ChatColor.GRAY + "Final Border Size");

        // Same layout as the initial border size menu
        ItemStack redBanner = createBanner2(ChatColor.RED, -500);
        ItemStack orangeBanner = createBanner2(ChatColor.WHITE, -100);
        ItemStack yellowBanner = createBanner2(ChatColor.YELLOW, -50);
        menu.setItem(0, redBanner);
        menu.setItem(1, orangeBanner);
        menu.setItem(2, yellowBanner);

        // Empty slot
        menu.setItem(3, new ItemStack(Material.AIR));

        menu.setItem(4, new ItemStack(Material.REDSTONE_BLOCK));

        // Empty slot
        menu.setItem(5, new ItemStack(Material.AIR));

        // Second row (Cyan, Lime, Dark Green Banners)
        ItemStack cyanBanner = createBanner2(ChatColor.AQUA, 50);
        ItemStack limeBanner = createBanner2(ChatColor.GREEN, 100);
        ItemStack darkGreenBanner = createBanner2(ChatColor.DARK_GREEN, 500);
        menu.setItem(6, cyanBanner);
        menu.setItem(7, limeBanner);
        menu.setItem(8, darkGreenBanner);

        // Arrow to return to the previous menu
        ItemStack returnArrow = new ItemStack(Material.ARROW);
        ItemMeta returnMeta = returnArrow.getItemMeta();
        returnMeta.setDisplayName(ChatColor.YELLOW + "Return to Previous Menu");
        returnArrow.setItemMeta(returnMeta);
        menu.setItem(14, returnArrow);

        player.openInventory(menu);
    }
    @EventHandler
    public void onFinalBorderSizeMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // If this is the Final Border Size menu
        if (inventory.getName().equals(ChatColor.GRAY + "Final Border Size")) {
            event.setCancelled(true); // Prevent item pickup

            // Handle banner clicks to adjust the final border size
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
            ItemMeta itemMeta = event.getCurrentItem().getItemMeta();

            // Adjust the final border size based on clicked item
            if (itemMeta.getDisplayName().equals(ChatColor.RED + "§e500 blocks")) {
                finalBorderSize = 500;
                player.sendMessage("§aFinal border size set to 500 blocks.");
            } else if (itemMeta.getDisplayName().equals(ChatColor.GREEN + "§e100 blocks")) {
                finalBorderSize = 100;
                player.sendMessage("§aFinal border size set to 100 blocks.");
            } else if (itemMeta.getDisplayName().equals(ChatColor.YELLOW + "§e50 blocks")) {
                finalBorderSize = 50;
                player.sendMessage("§aFinal border size set to 50 blocks.");
            } else if (itemMeta.getDisplayName().equals(ChatColor.AQUA + "§e25 blocks")) {
                finalBorderSize = 25;
                player.sendMessage("§aFinal border size set to 25 blocks.");
            }

            // Handle the return arrow to go back to the main menu
            if (itemMeta.getDisplayName().equals(ChatColor.YELLOW + "Return to Previous Menu")) {
                openMenu(player); // Open the main menu
            }
        }
    }
    
    private ItemStack createBanner2(ChatColor color, int amount) {
        ItemStack banner = new ItemStack(Material.BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + "Amount: " + amount);
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to adjust server slots by " + amount);
            meta.setLore(lore);
            
            // Check if the color is a valid DyeColor
            DyeColor dyeColor = getDyeColorFromChatColor(color);
            if (dyeColor != null) {
                meta.setBaseColor(dyeColor);
            } else {
                // Fallback to default color if invalid
                Bukkit.getLogger().warning("Invalid DyeColor: " + color.name() + ". Using default color instead.");
                meta.setBaseColor(DyeColor.WHITE); // Default fallback color
            }

            banner.setItemMeta(meta);
        }
        return banner;
    }
    public void openSlotManagementMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 18, ChatColor.GRAY + "Slot Management");

        // First row (Red, Orange, Yellow Banners)
        ItemStack redBanner = createBanner(ChatColor.RED, -10);
        ItemStack orangeBanner = createBanner(ChatColor.WHITE, -5);
        ItemStack yellowBanner = createBanner(ChatColor.YELLOW, -1);
        menu.setItem(0, redBanner);
        menu.setItem(1, orangeBanner);
        menu.setItem(2, yellowBanner);

        // Empty slot
        menu.setItem(3, new ItemStack(Material.AIR));

        // Player head and amount (server slot number)
        int maxPlayers = Bukkit.getMaxPlayers();
        addPlayerHead(menu,4,maxPlayers);

        // Empty slot
        menu.setItem(5, new ItemStack(Material.AIR));

        // Second row (Cyan, Lime, Dark Green Banners)
        ItemStack cyanBanner = createBanner(ChatColor.AQUA, 1);
        ItemStack limeBanner = createBanner(ChatColor.GREEN, 5);
        ItemStack darkGreenBanner = createBanner(ChatColor.DARK_GREEN, 10);
        menu.setItem(6, cyanBanner);
        menu.setItem(7, limeBanner);
        menu.setItem(8, darkGreenBanner);

        // Arrow to return to the previous menu (for example, slot 4)
        ItemStack returnArrow = new ItemStack(Material.ARROW);
        ItemMeta returnMeta = returnArrow.getItemMeta();
        returnMeta.setDisplayName(ChatColor.YELLOW + "Return to Previous Menu");
        returnArrow.setItemMeta(returnMeta);
        menu.setItem(14, returnArrow); // Slot 4 is for the return arrow

        player.openInventory(menu);
    }
    private ItemStack createBanner(ChatColor color, int amount) {
        ItemStack banner = new ItemStack(Material.BANNER, 1);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + "Amount: " + amount);
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to adjust map border by " + amount);
            meta.setLore(lore);
            
            // Check if the color is a valid DyeColor
            DyeColor dyeColor = getDyeColorFromChatColor(color);
            if (dyeColor != null) {
                meta.setBaseColor(dyeColor);
            } else {
                // Fallback to default color if invalid
                Bukkit.getLogger().warning("Invalid DyeColor: " + color.name() + ". Using default color instead.");
                meta.setBaseColor(DyeColor.WHITE); // Default fallback color
            }

            banner.setItemMeta(meta);
        }
        return banner;
    }

    private DyeColor getDyeColorFromChatColor(ChatColor color) {
        try {
            // Attempt to map the ChatColor to a DyeColor, using a valid color mapping
            switch (color) {
                case DARK_BLUE:
                    return DyeColor.BLUE;
                case DARK_AQUA:
                    return DyeColor.CYAN;
                case DARK_GRAY:
                    return DyeColor.GRAY;
                case DARK_GREEN:
                    return DyeColor.GREEN;
                case DARK_PURPLE:
                    return DyeColor.PURPLE;
                case DARK_RED:
                    return DyeColor.RED;
                case LIGHT_PURPLE:
                    return DyeColor.PINK;
                case BLUE:
                    return DyeColor.BLUE;
                case AQUA:
                    return DyeColor.CYAN;
                case GREEN:
                    return DyeColor.GREEN;
                case RED:
                    return DyeColor.RED;
                case YELLOW:
                    return DyeColor.YELLOW;
                case WHITE:
                    return DyeColor.WHITE;
                default:
                    return null;
            }
        } catch (Exception e) {
            // Log any errors that occur during mapping
            return null;
        }
    }
 // Adjust border size command execution
    private void executeBorderSizeCommand(Player player, int amount) {
        // Calculate the new border size, taking the division by 2 into account
        int newBorderSize = amount * 2; // Multiply by 2 to account for division by 2

        // Execute the world border command with the new border size
        World world = Bukkit.getWorld("world");
        if (world == null) return;
        WorldBorder border = world.getWorldBorder();
        double somme = border.getSize() + newBorderSize;
        player.performCommand("worldborder set "  + somme);
        System.out.println("World border set to " + somme);
    }
    @EventHandler
    public void onInventoryClick2(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GRAY + "Initial Border")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();

            // Handle banner clicks for border size adjustment
            if (event.getSlot() == 0) { // Red Banner (-500)
                executeBorderSizeCommand(player, -500);
            } else if (event.getSlot() == 1) { // Orange Banner (-100)
                executeBorderSizeCommand(player, -100);
            } else if (event.getSlot() == 2) { // Yellow Banner (-50)
                executeBorderSizeCommand(player, -50);
            } else if (event.getSlot() == 6) { // Cyan Banner (+50)
                executeBorderSizeCommand(player, 50);
            } else if (event.getSlot() == 7) { // Lime Banner (+100)
                executeBorderSizeCommand(player, 100);
            } else if (event.getSlot() == 8) { // Dark Green Banner (+500)
                executeBorderSizeCommand(player, 500);
            } else if (event.getSlot() == 14) { // Arrow (Go back)
                // Go back to the previous menu
                openMenu(player); // Or the method to open the previous menu
            }
        }
    }
    @EventHandler
    public void onInventoryClick1(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GRAY + "Slot Management")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();

            // Handle banner clicks
            if (event.getSlot() == 0) { // Red Banner (-10)
                executeSlotCommand(player, -10);
            } else if (event.getSlot() == 1) { // Orange Banner (-5)
                executeSlotCommand(player, -5);
            } else if (event.getSlot() == 2) { // Yellow Banner (-1)
                executeSlotCommand(player, -1);
            } else if (event.getSlot() == 6) { // Cyan Banner (+1)
                executeSlotCommand(player, 1);
            } else if (event.getSlot() == 7) { // Lime Banner (+5)
                executeSlotCommand(player, 5);
            } else if (event.getSlot() == 8) { // Dark Green Banner (+10)
                executeSlotCommand(player, 10);
            } else if (event.getSlot() == 14) { // Arrow (Go back)
                // Go back to the previous menu
                openMenu(player); // Or the method to open the previous menu
            }
        }
    }

    private void executeSlotCommand(Player player, int amount) {
        // Execute the /addslot command with the appropriate amount
        player.performCommand("addslot "+amount);
        System.out.println("Server slot upgraded to "+amount);
    }
    private void addTeamConfigItem(Inventory menu, int slot) {
        ItemStack item = new ItemStack(Material.BANNER, Math.min(teamSize, 64));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eTeams Configuration");
            meta.setLore(List.of(
                    "§7Control the §asize",
                    "§7of teams in the game.",
                    "",
                    "§e➢ §7Team Size: §a" + teamSize,
                    "",
                    "§6§l➢ §eClick to increase"
            ));
            item.setItemMeta(meta);
        }
        menu.setItem(slot, item);
    }
    private void addPlayerHead(Inventory menu, int slot, int maxPlayers) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, Math.min(maxPlayers, 64), (short) 3); // Steve Head
        ItemMeta meta = skull.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Max Players: " + ChatColor.AQUA + maxPlayers);
            skull.setItemMeta(meta);
        }

        menu.setItem(slot, skull);
    }

    private void addItem(Inventory menu, int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        menu.setItem(slot, item);
    }    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            ItemStack comparator = new ItemStack(Material.REDSTONE_COMPARATOR);
            ItemMeta meta = comparator.getItemMeta();
            meta.setDisplayName("§eGame Config");
            comparator.setItemMeta(meta);
            player.getInventory().addItem(comparator);
        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() != null && event.getItem().hasItemMeta() && event.getItem().getItemMeta().getDisplayName().equals("§eGame Config")) {
            openMenu(player);
        }
        if (event.getItem() != null && event.getItem().hasItemMeta() && event.getItem().getItemMeta().getDisplayName().equals("§ePvP Time")) {
            addPvPTime(5); // Each click adds 5 minutes (300 seconds)
            openMenu(player); // Update menu to reflect the new PvP time
        }
        if (event.getItem() != null && event.getItem().hasItemMeta() && event.getItem().getItemMeta().getDisplayName().equals("§eBorder Start Time")) {
            addMeetupTime(5); // Each click adds 5 minutes (300 seconds)
            openMenu(player); // Update menu to reflect the new Meetup time
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GRAY + "Game Configuration")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();

            if (event.getSlot() == 28) { // Game Name Button
                player.closeInventory();
                giveGameNameBook(player);
            } else if (event.getSlot() == 1) {
            	int newTeamSize = gameconfig.getTeamSize() +1;
            	gameconfig.setTeamSize(newTeamSize);
            	openMenu(player);
            } else if (event.getSlot() == 7) { // PvP Time button clicked
                addPvPTime(5); // Add 5 minutes
                openMenu(player); // Update menu to reflect new PvP time
            } else if (event.getSlot() == 6) {
            	addMeetupTime(5);
            	openMenu(player);
            } else if (event.getSlot() == 0) {
            	openSlotManagementMenu(player);
            } else if (event.getSlot() == 3) {
            	openBorderInitial(player);
            } else if (event.getSlot() == 9) {
            	openDrop(player);
            }
        }
    }


    private void giveGameNameBook(Player player) {
        ItemStack book = new ItemStack(Material.BOOK_AND_QUILL);
        player.getInventory().addItem(book);
        player.sendMessage(ChatColor.GREEN + "You received a book! Type the game name in it.");
    }
    @EventHandler
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        Player player = event.getPlayer();
        BookMeta bookMeta = event.getNewBookMeta();

        if (bookMeta.hasPages()) {
            String newGameName = ChatColor.stripColor(bookMeta.getPage(1).trim());

            if (!newGameName.isEmpty()) {
                gameName = newGameName; // ✅ Update game name
                player.sendMessage(ChatColor.GREEN + "Game name updated: " + ChatColor.AQUA + gameName);
            }
        }

        // ✅ Remove the book from the player's inventory after signing
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("Custom_UHC"), () -> {
            player.getInventory().remove(Material.BOOK_AND_QUILL);
        }, 1L);
    }
    private void addMeetupTime(int minutes) {
    	meetupTime += minutes * 60;
    }
    public static int getMeetupTime() {
    	return meetupTime;
    }
    private void addPvPTime(int minutes) {
        pvpTime += minutes * 60; // Convert minutes to seconds and add
    }
    public static int getPvPTime() {
        return pvpTime;
    }
     public static String formatTime(int timeInSeconds) {
        int minutes = timeInSeconds / 60;
        int seconds = timeInSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

     @EventHandler
     public void onGameStart2(GameStartEvent e) {
    	    Player player = e.getp();
    	    
    	    // Set the initial world border size
    	    // Example initial size, adjust as needed
    	    worldBorder.setCenter(0,0); // Optionally set the center of the world border to the world spawn location

    	    // Set the default border speed
    	    worldBorder.setDamageAmount(0.0); // Optional: disables damage from border shrink
    	    worldBorder.setDamageBuffer(5); // Optional: set a buffer where players will be safe from the border damage
    	    
    	    // Border doesn't shrink until meetup time reaches 0
    	    borderShrinking = false;
    	    ScoreboardHandler scoreboardHandler = new ScoreboardHandler(plugin, manager); // Pass your plugin reference here
    	    scoreboardHandler.startGlobalScoreboardUpdater();
    	    
    	    new BukkitRunnable() {
    	        @Override
    	        public void run() {
     	            Sound sound = Sound.valueOf("BLOCK_STONE_BUTTON_CLICK_OFF");
    	            Sound sound2 = Sound.valueOf("ENTITY_ELDER_GUARDIAN_CURSE");
    	            if (meetupTime > 0) {
    	            	if (meetupTime == 10 ) {
    	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
    	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
    	                    }
    	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §eMap will start shrinking in §b10 §eseconds.");
    	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §eHez za3ktk w emchi ll centre ");
    	            	}
    	            	if (meetupTime == 5 ) {
    	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
    	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
    	                    }
    	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §eMap will start shrinking in §b5 §eseconds.");
    	            	}
    	            	if (meetupTime == 4 ) {
    	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
    	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
    	                    }
    	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §eMap will start shrinking in §b4 §eseconds.");
    	            	}
    	            	if (meetupTime == 3 ) {
    	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
    	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
    	                    }
    	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §eMap will start shrinking in §b3 §eseconds.");
    	            	}
    	            	if (meetupTime == 2 ) {
    	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
    	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
    	                    }
    	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §eMap will start shrinking in §b2 §eseconds.");
    	            	}
    	            	if (meetupTime == 1 ) {
    	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
    	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
    	                    }
    	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §eMap will start shrinking in §b1 §eseconds.");

    	            	}
    	                meetupTime--;
    	                // The scoreboard will be updated every second in the startUpdatingScoreboard method
    	            } else if (meetupTime == 0) {
	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound2, 1.0F, 1.0F); // Play sound
	                    }
	                    Bukkit.broadcastMessage(" ");
	                    Bukkit.broadcastMessage(" ");
	                    Bukkit.broadcastMessage("§7§l➤ §e§lPhase: §3§lMeetUp time ");
	                    Bukkit.broadcastMessage(" ");
	                    Bukkit.broadcastMessage("§7§l➤ §e§lInfo: §r§7Map size will start shrinking. (hez s7abek w emchi l centre fighty)");
	                    Bukkit.broadcastMessage(" ");
	                    Bukkit.broadcastMessage(" ");
    	                disableNetherAccess();
    	                teleportPlayersInNether();
    	            	setBorderVerif(true);
    	            	startShrinkingBorder();
    	            	
    	                cancel();
    	            }
    	        }
    	    }.runTaskTimer(Bukkit.getPluginManager().getPlugin("Custom_UHC"), 0L, 20L);
    	}
  // Disable Nether access (Set a flag or modify game rules)
     private void disableNetherAccess() {
         isNetherAccessEnabled = false;  // This flag can be used in your logic to prevent Nether access
         
         // Optionally, you can do something like removing access to the Nether dimension
         // or simply preventing players from entering by checking if they try to go into the portal.
         Bukkit.broadcastMessage("§cNether access has been disabled as meetup time has reached 0.");
     }
     @EventHandler
     public void onPortalUse(PlayerInteractEvent event) {
         Player player = event.getPlayer();
         ItemStack item = player.getInventory().getItemInHand();
         
         // Check if Nether access is disabled
         if (!isNetherAccessEnabled) {
             if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.PORTAL) {
                 event.setCancelled(true);  // Cancel the portal interaction (prevent entry into the Nether)
                 player.sendMessage("§cNether access has been disabled.");
             }
         }
     }

     // Teleport players in the Nether to the main world within the border
     private void teleportPlayersInNether() {
         for (Player player : Bukkit.getOnlinePlayers()) {
             if (player.getWorld().getName().equals("world_nether")) {
                 // Get the player’s location in the Nether
                 Location netherLocation = player.getLocation();
                 
                 // Teleport to a safe location within the border in the main world
                 Location worldSpawn = mainWorld.getSpawnLocation();
                 // Ensure the player is within the world border
                 WorldBorder border = mainWorld.getWorldBorder();
                 double x = Math.max(Math.min(netherLocation.getX(), border.getCenter().getX() + border.getSize() / 2), border.getCenter().getX() - border.getSize() / 2);
                 double z = Math.max(Math.min(netherLocation.getZ(), border.getCenter().getZ() + border.getSize() / 2), border.getCenter().getZ() - border.getSize() / 2);
                 Location safeLocation = new Location(mainWorld, x, worldSpawn.getY(), z);
                 
                 // Teleport the player
                 player.teleport(safeLocation);
                 player.sendMessage("§aYou have been teleported back to the main world as the Nether is disabled.");
             }
         }
     }
     public boolean getBorderVerif() {
    	    return borderShrinking;
    	}

    	public void setBorderVerif(boolean verif) {
    	    if (!borderShrinking) {
    	        borderShrinking = true; // Lock the border speed after it is set
    	    }
    	}
     private void startShrinkingBorder() {
    	    World world = Bukkit.getWorld("world"); // Use your actual world if it's different
    	    WorldBorder worldBorder = world.getWorldBorder();
    	    
    	    // Set the final size of the border and calculate the duration based on the speed
    	    double finalBorderSize = 100.0; // Set the final border size (adjust as needed)
    	    double distanceToShrink = worldBorder.getSize() - finalBorderSize;
    	    long duration = (long) (distanceToShrink / borderSpeed * 20L); // Convert seconds to ticks

    	    // Shrink the border to the final size over the calculated duration
    	    worldBorder.setSize(finalBorderSize, duration);

    	    // Optionally, broadcast a message to players
    	    Bukkit.broadcastMessage("§aThe world border is shrinking to its final size!");
    	}
     @EventHandler
     public void onGameStart(GameStartEvent e) {
         Player player = e.getp();
         
         gameStartTime = System.currentTimeMillis();
         startGameTimer();

         // Create an instance of ScoreboardHandler to start the scoreboard updates
         ScoreboardHandler scoreboardHandler = new ScoreboardHandler(plugin, manager);
         scoreboardHandler.startGlobalScoreboardUpdater();
         
         new BukkitRunnable() {
             @Override
             public void run() {
 	            Sound sound = Sound.valueOf("BLOCK_STONE_BUTTON_CLICK_OFF");
	            Sound sound2 = Sound.valueOf("ENTITY_ENDERDRAGON_GROWL");
	            if (pvpTime > 0) {
	                if (pvpTime == 10) {
	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
	                    }
	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §cPvP will be enabled in §e10 §cseconds.");
	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §e10 §cseconds wtnjmou tetneykou (blou8a o5ra)");
	                }

	                if (pvpTime == 5) {
	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
	                    }
	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §cPvP will be enabled in §e5 §cseconds.");
	                }
	                
	                if (pvpTime == 4) {
	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
	                    }
	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §cPvP will be enabled in §e4 §cseconds.");
	                }

	                if (pvpTime == 3) {
	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
	                    }
	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §cPvP will be enabled in §e3 §cseconds.");
	                }

	                if (pvpTime == 2) {
	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
	                    }
	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §cPvP will be enabled in §e2 §cseconds.");
	                }

	                if (pvpTime == 1) {
	                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
	                        onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0F, 1.0F); // Play sound
	                    }
	                    Bukkit.broadcastMessage("§e§lUHC §r§8➢ §cPvP will be enabled in §e1 §csecond.");
	                }

	                pvpTime--; // Decrease pvpTime after the checks
	            } else if (pvpTime == 0) {
	                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
	                    onlinePlayer.playSound(onlinePlayer.getLocation(), sound2, 1.0F, 1.0F); // PvP activated sound
	                }

	                Bukkit.broadcastMessage("§7§o⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯");
	                Bukkit.broadcastMessage(" ");
	                Bukkit.broadcastMessage("§7§l➤ §e§lPhase: §c§lPvP Activated");
	                Bukkit.broadcastMessage(" ");
	                Bukkit.broadcastMessage("§7§l➤§e§lInfo: §r§7Players can kill each other now. (ely tal9ah 9odemk nikou) ");
	                Bukkit.broadcastMessage(" ");
	                Bukkit.broadcastMessage(" ");
	                Bukkit.broadcastMessage("§7§o⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⯯⯯⯯");
	                world.setPVP(true); // Enable PvP
	                cancel();
	            }
	        }
	    }.runTaskTimer(Bukkit.getPluginManager().getPlugin("Custom_UHC"), 0L, 20L);
	}
     public void startGameTimer() {
    	    new BukkitRunnable() {
    	        @Override
    	        public void run() {
    	            if (Gamestatus.getStatus() != 1) {
    	                cancel(); // Stop the timer if the game isn't in progress
    	            }

    	            // Update the scoreboard every second
    	            ScoreboardHandler scoreboardHandler = new ScoreboardHandler(plugin, manager);
    	            for (Player player : Bukkit.getOnlinePlayers()) {
    	                scoreboardHandler.startGlobalScoreboardUpdater();
    	            }
    	        }
    	    }.runTaskTimer(plugin, 0L, 20L); // Run every 20 ticks (1 second)
    	}
     

    public static int getTeamSize() {
        return teamSize;
    }

    public static void setTeamSize(int newSize) {
    		Bukkit.getServer().getPluginManager().callEvent(new TeamSizeChangedEvent(teamSize,newSize));
    		teamSize = newSize;
    		
    }

    public static String getGameName() {
        return gameName;
    }
    public static void setGameName(String newName) {
    	gameName = newName;
    }
    public static int getSlot() {
        return Slot;
    }
    public static void setSlot(int newSlot) {
    	Slot = newSlot;
    }

@EventHandler
public void onBlockBreak(BlockBreakEvent event) {
    Material block = event.getBlock().getType();

    // Apple drop from trees
    if (block == Material.LEAVES || block == Material.LEAVES_2) {
        int appleChance = gameconfig.getInstance().getDropRate("APPLE"); // Get rate from gameconfig
        int goldenAppleChance = gameconfig.getInstance().getDropRate("GOLDEN_APPLE"); // Get golden apple drop rate
        Bukkit.getLogger().info("Apple Drop Chance: " + appleChance);
        Bukkit.getLogger().info("Golden Apple Drop Chance: " + goldenAppleChance);
        if (random.nextInt(100) < goldenAppleChance) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.GOLDEN_APPLE));
        }
        if (random.nextInt(100) < appleChance) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.APPLE));
            Bukkit.getLogger().info("Apple drop chance: " + gameconfig.getInstance().getDropRate("APPLE"));
        }
    }

    // Flint drop from gravel
    if (block == Material.GRAVEL) {
        int flintChance = gameconfig.getInstance().getDropRate("FLINT");
        if (random.nextInt(100) < flintChance) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.FLINT));
            Bukkit.getLogger().info("FLINT drop chance: " + gameconfig.getInstance().getDropRate("FLINT"));
        }
    }
}

@EventHandler
public void onEntityDeath(EntityDeathEvent event) {
    EntityType entity = event.getEntityType();

    // Feather drop from chickens
    if (entity == EntityType.CHICKEN) {
        int extraFeathers = gameconfig.getInstance().getDropRate("FEATHER");
        if (extraFeathers > 0) {
            event.getDrops().add(new ItemStack(Material.FEATHER, extraFeathers));
            Bukkit.getLogger().info("Chieck drop chance: " + gameconfig.getInstance().getDropRate("FEATHER"));
        }
    }

    // Arrow drop from skeletons
    if (entity == EntityType.SKELETON) {
        int extraArrows = gameconfig.getInstance().getDropRate("ARROW");
        if (extraArrows > 0) {
            event.getDrops().add(new ItemStack(Material.ARROW, extraArrows));
            Bukkit.getLogger().info("Arrow drop chance: " + gameconfig.getInstance().getDropRate("ARROW"));
        }
    }

    // Ender Pearl drop from Endermen
    if (entity == EntityType.ENDERMAN) {
        int pearlChance = gameconfig.getInstance().getDropRate("ENDER_PEARL");
        if (random.nextInt(100) < pearlChance) {
            event.getDrops().add(new ItemStack(Material.ENDER_PEARL));
            Bukkit.getLogger().info("Ender Pearl drop chance: " + gameconfig.getInstance().getDropRate("ENDER_PEARL"));
        }
    }
}

@EventHandler
public void onEntityDeath1(EntityDeathEvent event) {
    int xpBoost = dropRates.getOrDefault("XP_BOTTLE", 0); // Get XP boost percentage
    int baseXp = event.getDroppedExp(); // Get normal XP drop
    int boostedXp = (int) (baseXp * (1 + (xpBoost / 100.0))); // Apply boost

    event.setDroppedExp(boostedXp); // Set new XP amount

    // Debugging
}
}

