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
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.Potion;
import org.bukkit.scheduler.BukkitRunnable;

import decoration.ScoreboardHandler;
import events.GameStartEvent;
import events.TeamSizeChangedEvent;
import gamemodes.Gamestatus;
import gamemodes.SwitchUHC;
import gamemodes.gamemode;
import teams.TeamSelectionSystem;
import teams.UHCTeamManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class gameconfig implements Listener {
    private static String gameName = "HOST";
    private static gameconfig instance;
    private TeamSelectionSystem teamSelectionSystem;
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
    private SwitchUHC switchUHC;
    private static int staticSwitchTime = 0;
    private static int switchTime = 0;
    private static int teamSize = 1;
    private static int pvpTime = 0;
    private static int meetupTime=0;
    private static int Slot = Bukkit.getMaxPlayers();
    UHCTeamManager manager = ((main) Bukkit.getPluginManager().getPlugin("Custom_UHC")).getTeamManager();
    private final Map<Integer, String> actions = new HashMap<>();
    private final main plugin; 
    public gameconfig(main plugin) {
    	instance=this;
    	this.switchUHC = new SwitchUHC(plugin.getTeamManager());
        this.plugin = plugin;
        this.teamSelectionSystem = new TeamSelectionSystem(manager, plugin);
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
        addItem(menu, 3, Material.LAPIS_BLOCK, "§eInitial Border", "§7Set the §asize §7of the starting border.", "", "§e➢ §7Status: §a"+world.getWorldBorder().getSize()+" §7(+"+world.getWorldBorder().getSize()+"§7/-"+world.getWorldBorder().getSize(), " ", "§6§l➢ §eClick to proceed");
        addItem(menu, 4, Material.REDSTONE_BLOCK, "§eFinal Border", "§7Set the §asize §7of the final border.", "", "§e➢ §7Status: §a"+finalBorderSize+"§7(+"+finalBorderSize / 2+"§7/-"+finalBorderSize /2, "", "§6§l➢ §eClick to proceed");
        addItem(menu, 5, Material.WATCH, "§eBorder Speed", "§7Control the §aborder speed.", "", "§e➢ §7Status: §a"+borderSpeed+" §ablocs/sec", "", "§6§l➢ §eClick to adjust");
        addItem(menu, 6, Material.BARRIER, "§eBorder Start Time", "§7Control the time before the border shrinks.", "", "§e➢ §7Time: §e"+formatTime(meetupTime), "", "§6§l➢ §eClick to proceed");
        addItem(menu, 7, Material.DIAMOND_SWORD, "§ePvP Time", "§7Set the time before §aPvP §7is enabled.", "", "§e➢ §7Time: §e"+formatTime(pvpTime), "", "§6§l➢ §eClick to proceed");
        addItem(menu, 8, Material.NETHERRACK, "§eNether Access", "§7Toggle §aNether access.", "", "§e➢ §7Status: ", "", "§6§l➢ §eClick to change");
        addItem(menu, 9, Material.APPLE, "§eDrops Configuration", "§7Adjust item drop rates.", "", "§6§l➢ §eClick to change");
        addItem(menu, 10, Material.PAPER, "§eGame Rules", "§7Modify game rules like §aiPvP §7and §aFriendlyFire.", "", "§6§l➢ §eClick to proceed");
        addItem(menu, 11, Material.BREWING_STAND_ITEM, "§ePotions Configuration", "§7Manage the §acreation §7of certain potions and their §bupgrades.", "", 
                "§e➧ §7Potions activated:",
                "§e➧ §7Speed potion: " + (speedPotionEnabled ? "§aEnabled" : "§cDisabled"),
                "§e➧ §7Healing potion: " + (healingPotionEnabled ? "§aEnabled" : "§cDisabled"),
                "§e➧ §7Strength potion: " + (strengthPotionEnabled ? "§aEnabled" : "§cDisabled"),
                "§e➧ §7Poison potion: " + (poisonPotionEnabled ? "§aEnabled" : "§cDisabled"),
                "",
                "§e➧ §7Potion allongée: " + (extendedPotionsEnabled ? "§aEnabled" : "§cDisabled"),
                "§e➧ §7Level II potion: " + (levelTwoPotionsEnabled ? "§aEnabled" : "§cDisabled"),
                "",
                "§6§l➧ §r§eClick to access");
        addItem(menu, 12, Material.CHEST, "§eStarting Items", "§7Modify players' starting inventory.", "", "§6§l➢ §eClick to change");
        addItem(menu, 27, Material.BOOK, "§eScenarios", "§7View available game scenarios.", "", "§6§l➢ §eClick to proceed");
        addItem(menu, 28, Material.BOOK_AND_QUILL, "§eGame name");
        addItem(menu, 31, Material.EMERALD_BLOCK, "§2§l⮚ §a§lLet's go! §2§l⮘", "§7Start the game with selected settings!");

        player.openInventory(menu);
    }
    private boolean cutCleanEnabled = false;
    public void openScenariosMenu(Player player) {
        Inventory scenariosMenu = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Scenarios");

        // Wagons
        addItem1(scenariosMenu, 2, Material.STORAGE_MINECART, "§eComing soon...");
        addItem1(scenariosMenu, 6, Material.EXPLOSIVE_MINECART, "§eGamemode selection", "§7Click to open the Gamemode menu.");

        // Line 3
        addItem1(scenariosMenu, 18, Material.EXP_BOTTLE, "§eExperience Bottles");
        addItem1(scenariosMenu, 19, Material.ANVIL, "§eAnvil");
        addItem1(scenariosMenu, 20, Material.BOOKSHELF, "§eLibrary");
        addItem1(scenariosMenu, 21, Material.ENCHANTMENT_TABLE, "§eEnchanting Table");
        addItem1(scenariosMenu, 22, Material.REDSTONE, "§eRedstone");
        addItem1(scenariosMenu, 23, Material.REDSTONE_ORE, "§eRedstone Ore");
        addItem1(scenariosMenu, 24, Material.DIAMOND_ORE, "§eDiamond Ore");
        addItem1(scenariosMenu, 25, Material.DIAMOND, "§eDiamond");
        addItem1(scenariosMenu, 26, Material.IRON_INGOT, "§eIron");

        // Line 4
        addItem1(scenariosMenu, 27, Material.GOLD_CHESTPLATE, "§eGold Chestplate");
        addItem1(scenariosMenu, 28, Material.TNT, "§eTNT");
        addItem1(scenariosMenu, 29, Material.ENCHANTED_BOOK, "§eEnchanted Book");
        addItem1(scenariosMenu, 30, Material.DIAMOND_BOOTS, "§eDiamond Boots");
        addItem1(scenariosMenu, 31, Material.LADDER, "§eLadder");
        addItem1(scenariosMenu, 32, Material.WOOD, "§eWood");
        addItem1(scenariosMenu, 33, Material.CHEST, "§eChest");
        addItem2(scenariosMenu, 35, createCutCleanItem());

        // Line 5
        addItem(scenariosMenu, 36, Material.POTION, "§ePotion");
        addItem2(scenariosMenu, 37, createGoneFishinItem());
        addItem1(scenariosMenu, 38, Material.APPLE, "§eApple");
        addItem1(scenariosMenu, 39, Material.BOW, "§eBow");
        addItem1(scenariosMenu, 40, Material.IRON_AXE, "§eAxe");
        addItem1(scenariosMenu, 41, Material.BREAD, "§eBread");
        addItem1(scenariosMenu, 42, Material.IRON_SWORD, "§eIron Sword");
        addItem1(scenariosMenu, 43, Material.COOKIE, "§eCookie");
        addItem1(scenariosMenu, 44, Material.NETHER_STAR, "§eNether Star");

        // Navigation
        addItem1(scenariosMenu, 49, Material.ARROW, "§cReturn to Main Menu");
        addItem1(scenariosMenu, 50, Material.PAPER, "§eNext Page");

        player.openInventory(scenariosMenu);
    }
 // ✅ Fix addItem1 to Work for All Items
    private void addItem1(Inventory inv, int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    // ✅ Fix addItem1 to Accept Custom ItemStacks
    private void addItem2(Inventory inv, int slot, ItemStack item) {
        if (item != null) {
            inv.setItem(slot, item);
        }
    }
    public void openGamemodeMenu(Player player) {
        Inventory gamemodeMenu = Bukkit.createInventory(null, 9, ChatColor.RED + "Game Modes");

        // Werewolf UHC
        addItem(gamemodeMenu, 0, Material.MONSTER_EGG, "§c§lUHC WEREWOLF", 
                "§7Click to set gamemode",
                "§6➢ §eGamemode: §aUHC WEREWOLF");

        // Switch UHC - add enchantment if active
        ItemStack switchBow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = switchBow.getItemMeta();
        bowMeta.setDisplayName("§e§lUHC SWITCH");
        List<String> bowLore = new ArrayList<>();
        bowLore.add("§7Click to set gamemode");
        bowLore.add("§6➢ §eGamemode: §aUHC SWITCH");
        bowLore.add("§6➢ §eStatus: " + (gamemode.getMode() == 2 ? "§aACTIVE" : "§cINACTIVE"));
        bowMeta.setLore(bowLore);
        if (gamemode.getMode() == 2) {
            bowMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        }
        switchBow.setItemMeta(bowMeta);
        gamemodeMenu.setItem(1, switchBow);

        // Mole UHC
        addItem(gamemodeMenu, 2, Material.GOLDEN_APPLE, "§4MOLE UHC", 
                "§7Click to set gamemode",
                "§6➢ §eGamemode: §aMole UHC");

        // Return arrow
        addItem(gamemodeMenu, 8, Material.ARROW, "§cReturn to Scenarios");

        player.openInventory(gamemodeMenu);
    }
    public void openSwitchMenu(Player player) {
        Inventory SwitchMenu = Bukkit.createInventory(null, 9, "SwitchUHC Settings");

        // Enable Button (Emerald Block)
        ItemStack enableItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta enableMeta = enableItem.getItemMeta();
        enableMeta.setDisplayName("§aENABLE SWITCH");
        enableMeta.setLore(Arrays.asList(
            "§7Enable the player switching system",
            "",
            "§eStatus: " + (gamemode.getMode() == 2 ? "§aACTIVE" : "§cINACTIVE")
        ));
        enableItem.setItemMeta(enableMeta);
        SwitchMenu.setItem(0, enableItem);

        // Disable Button (Barrier)
        ItemStack disableItem = new ItemStack(Material.BARRIER);
        ItemMeta disableMeta = disableItem.getItemMeta();
        disableMeta.setDisplayName("§cDISABLE SWITCH");
        disableMeta.setLore(Arrays.asList(
            "§7Disable the player switching system",
            "",
            "§eCurrently: " + (gamemode.getMode() != 2 ? "§cDISABLED" : "§aENABLED")
        ));
        disableItem.setItemMeta(disableMeta);
        SwitchMenu.setItem(1, disableItem);

        // Switch Time Configuration (Compass) - Fixed lore preservation
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        compassMeta.setDisplayName("§eSwitch Timer");
        
        // Create lore list separately to prevent modification
        List<String> compassLore = new ArrayList<>();
        compassLore.add("§7Configure when switches occur");
        compassLore.add("");
        compassLore.add("§e➢ Current: " + formatTime(staticSwitchTime));
        compassLore.add("");
        compassLore.add("§6§l➢ §eLeft-Click: Add 5 minutes");
        compassLore.add("§6§l➢ §eRight-Click: Reset to 0");
        
        compassMeta.setLore(compassLore);
        
        // Only enchant if active
        if (gamemode.getMode() == 2) {
            compassMeta.addEnchant(Enchantment.LUCK, 1, true);
            compassMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS); // Hide "enchanted" text
        }
        
        compass.setItemMeta(compassMeta);
        SwitchMenu.setItem(4, compass);

        // Return Arrow
        addItem(SwitchMenu, 8, Material.ARROW, "§cBack to Gamemodes");

        player.openInventory(SwitchMenu);
    }
    @EventHandler
    public void onInventoryClick6(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("SwitchUHC Settings")) return;
        
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        switch (item.getType()) {
            case EMERALD_BLOCK:
                if (staticSwitchTime <= 0) {
                    player.sendMessage(ChatColor.RED + "You must set a switch time first!");
                    return;
                }
                gamemode.setMode(2);
                player.sendMessage(ChatColor.GREEN + "SwitchUHC enabled!");
                openSwitchMenu(player);
                break;
                
            case BARRIER:
                gamemode.setMode(-1);
                player.sendMessage(ChatColor.RED + "SwitchUHC disabled!");
                openSwitchMenu(player);
                break;
                
            case COMPASS:
                if (event.getClick().isRightClick()) {
                    staticSwitchTime = 0;
                    player.sendMessage(ChatColor.RED + "Switch timer reset to 0!");
                } else {
                    staticSwitchTime += 300; // Add 5 minutes
                    player.sendMessage(ChatColor.GREEN + "+5 minutes! Timer: " + formatTime(staticSwitchTime));
                }
                openSwitchMenu(player);
                break;
                
            case ARROW:
                openGamemodeMenu(player);
                break;
        }
    }
    public boolean isGoneFishinEnabled() {
        return goneFishinEnabled;
    }
    public boolean goneFishinEnabled = false;
    private ItemStack createGoneFishinItem() {
        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        meta.setDisplayName("§eGonefishin");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Categorie : Survival");
        lore.add(""); // Empty second line
        lore.add("§6Features:");
        lore.add("§7- Unlimited durability");
        lore.add("§7- Maximum Luck of the Sea");
        lore.add("§7- Instant catches");
        lore.add(""); // Empty line before status
        lore.add("§eStatus: " + (goneFishinEnabled ? "§aEnabled" : "§cDisabled"));
        
        meta.setLore(lore);
        
        if (goneFishinEnabled) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        rod.setItemMeta(meta);
        return rod;
    }
   
    @EventHandler
    public void onInventoryClick5(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        // Prevent moving items in menus
        if (event.getView().getTitle().contains("Game Configuration") || event.getView().getTitle().contains("Scenarios") || event.getView().getTitle().contains("Game Modes") || event.getView().getTitle().contains("SwitchUHC menu") ) {
            event.setCancelled(true);
        }

        // Handle scenario clicks
        if (event.getView().getTitle().contains("Scenarios")) {
            if (item.getType() == Material.EXPLOSIVE_MINECART) {
                openGamemodeMenu(player);
            } else if (item.getType() == Material.COAL) {
                cutCleanEnabled = !cutCleanEnabled;
                openScenariosMenu(player);
            } else if (item.getType() == Material.ARROW) {
                openMenu(player);
            } else if (item.getType() == Material.FISHING_ROD) {
                goneFishinEnabled = !goneFishinEnabled;
                openScenariosMenu(player);
                String status = goneFishinEnabled ? "§aenabled" : "§cdisabled";
                player.sendMessage("§eGonefishin scenario " + status);
            }
        }

        // Handle gamemode changes
        if (event.getView().getTitle().contains("Game Modes")) {
            if (item.getType() == Material.MONSTER_EGG) {
                gamemode.setMode(1);
            } else if (item.getType() == Material.BOW) {
                Bukkit.getScheduler().runTask(plugin, () -> openSwitchMenu(player));
                gamemode.setMode(2);
            } else if (item.getType() == Material.GOLDEN_APPLE) {
                gamemode.setMode(0);
            } else if (item.getType() == Material.ARROW) {
                openScenariosMenu(player);
            }
    }
    }
    public void onItemDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().getOpenInventory().getTitle().contains("Game Configuration") || event.getPlayer().getOpenInventory().getTitle().contains("Scenarios") || event.getPlayer().getOpenInventory().getTitle().contains("Game Modes") || event.getPlayer().getOpenInventory().getTitle().contains("SwitchUHC menu")) {
            event.setCancelled(true);
        }
    }
    private ItemStack createCutCleanItem() {
        // Always return 1 coal, even if cutClean is disabled
        ItemStack cutCleanItem = new ItemStack(Material.COAL, 1);  
        ItemMeta meta = cutCleanItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eCutClean");
            // Show whether it's enabled or not
            meta.setLore(Arrays.asList(cutCleanEnabled ? "§aEnabled" : "§cDisabled"));
            if (cutCleanEnabled) {
                meta.addEnchant(Enchantment.LUCK, 1, true);  // Adds enchantment if enabled
            }
            cutCleanItem.setItemMeta(meta);
        }
        return cutCleanItem;
    }

    

    private boolean strengthPotionEnabled = false;
    private boolean poisonPotionEnabled = false;
    private boolean healingPotionEnabled = false;
    private boolean speedPotionEnabled = false;
    private boolean levelTwoPotionsEnabled = false;
    private boolean extendedPotionsEnabled = false;
    private boolean allPotionsEnabled = false;

    public void openPotionMenu(Player player) {
        Inventory potionMenu = Bukkit.createInventory(null, 18, ChatColor.DARK_PURPLE + "Potion Configuration");

        addToggleItem(potionMenu, 0, Material.POTION, "§eStrength Potion", strengthPotionEnabled);
        addToggleItem(potionMenu, 1, Material.POTION, "§ePoison Potion", poisonPotionEnabled);
        addToggleItem(potionMenu, 2, Material.POTION, "§eHealing Potion", healingPotionEnabled);
        addToggleItem(potionMenu, 3, Material.POTION, "§eSpeed Potion", speedPotionEnabled);

        addToggleItem(potionMenu, 6, Material.GLOWSTONE_DUST, "§eLevel II Potions", levelTwoPotionsEnabled);
        addToggleItem(potionMenu, 7, Material.REDSTONE, "§eExtended Potions", extendedPotionsEnabled);
        addToggleItem(potionMenu, 8, Material.POTION, "§bAll Potions", allPotionsEnabled);

        addItem(potionMenu, 14, Material.ARROW, "§cReturn", "§7Go back to the previous menu.");

        player.openInventory(potionMenu);
    }

    private void addToggleItem(Inventory inv, int slot, Material material, String name, boolean enabled) {
        String status = enabled ? "§aEnabled" : "§cDisabled";
        addItem(inv, slot, material, name, "", "§e➢ §7Status: " + status, "", "§6§l➢ §eClick to toggle");
    }


    @EventHandler
    public void onInventoryClick3(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();

        if (inv.getTitle().equals(ChatColor.DARK_PURPLE + "Potion Configuration")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;
            String itemName = clickedItem.getItemMeta().getDisplayName();

            switch (itemName) {
                case "§eStrength Potion":
                    strengthPotionEnabled = !strengthPotionEnabled;
                    break;
                case "§ePoison Potion":
                    poisonPotionEnabled = !poisonPotionEnabled;
                    break;
                case "§eHealing Potion":
                    healingPotionEnabled = !healingPotionEnabled;
                    break;
                case "§eSpeed Potion":
                    speedPotionEnabled = !speedPotionEnabled;
                    break;
                case "§eLevel II Potions":
                    levelTwoPotionsEnabled = !levelTwoPotionsEnabled;
                    break;
                case "§eExtended Potions":
                    extendedPotionsEnabled = !extendedPotionsEnabled;
                    break;
                case "§bAll Potions":
                    allPotionsEnabled = !allPotionsEnabled;
                    strengthPotionEnabled = allPotionsEnabled;
                    poisonPotionEnabled = allPotionsEnabled;
                    healingPotionEnabled = allPotionsEnabled;
                    speedPotionEnabled = allPotionsEnabled;
                    levelTwoPotionsEnabled = allPotionsEnabled;
                    extendedPotionsEnabled = allPotionsEnabled;
                    break;
                case "§cReturn":
                    openMenu(player);
                    break;
            }
            openPotionMenu(player);
        }
    }

    @EventHandler
    public void onPotionBrew(BrewEvent event) {
        BrewerInventory inv = event.getContents();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == Material.POTION) {
                Potion potion = Potion.fromItemStack(item);
                if (potion != null) {
                    switch (potion.getType()) {
                        case STRENGTH:
                            if (!strengthPotionEnabled) event.setCancelled(true);
                            break;
                        case POISON:
                            if (!poisonPotionEnabled) event.setCancelled(true);
                            break;
                        case INSTANT_HEAL:
                            if (!healingPotionEnabled) event.setCancelled(true);
                            break;
                        case SPEED:
                            if (!speedPotionEnabled) event.setCancelled(true);
                            break;
					default:
						event.setCancelled(true);
						break;
                    }
                    if (!levelTwoPotionsEnabled && potion.getLevel() > 1) {
                        event.setCancelled(true);
                    }
                    if (!extendedPotionsEnabled && potion.hasExtendedDuration()) {
                        event.setCancelled(true);
                    }
                }
            }
        }
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

        if (inventory.getName().equals(ChatColor.GRAY + "Game Configuration")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta()) return;

            // Border Speed (Watch) handling
            if (item.getItemMeta().getDisplayName().equals("§eBorder Speed")) {
                if (event.getClick().isRightClick()) {
                    // Right-click: Decrease speed by 0.5 blocks per second
                    borderSpeed = Math.max(0.1, borderSpeed - 0.5); // Minimum speed 0.1
                    player.sendMessage("§aBorder speed decreased to " + borderSpeed + " blocks per second.");
                } else {
                    // Left-click: Increase speed by 0.5 blocks per second
                    borderSpeed += 0.5;
                    player.sendMessage("§aBorder speed increased to " + borderSpeed + " blocks per second.");
                }
                openMenu(player); // Refresh menu
            }
        }
    }
  
    
    public void openFinalBorderSizeMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, ChatColor.GRAY + "Final Border Size");

        // Create banner items with clear display names
        ItemStack size500 = createBannerItem(DyeColor.RED, "§c500 blocks", "Final size: 500x500");
        ItemStack size100 = createBannerItem(DyeColor.WHITE, "§f100 blocks", "Final size: 100x100");
        ItemStack size50 = createBannerItem(DyeColor.YELLOW, "§e50 blocks", "Final size: 50x50");
        ItemStack size25 = createBannerItem(DyeColor.LIME, "§a25 blocks", "Final size: 25x25");

        // Center item showing current selection
        ItemStack current = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta currentMeta = current.getItemMeta();
        currentMeta.setDisplayName("§6Current: " + finalBorderSize);
        current.setItemMeta(currentMeta);

        // Add items to menu
        menu.setItem(0, size500);
        menu.setItem(1, size100);
        menu.setItem(2, size50);
        menu.setItem(3, size25);
        menu.setItem(4, current); // Center position
        
        // Return arrow
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.setDisplayName("§cBack to Main Menu");
        arrow.setItemMeta(arrowMeta);
        menu.setItem(8, arrow);

        player.openInventory(menu);
    }

    private ItemStack createBannerItem(DyeColor color, String name, String lore) {
        ItemStack banner = new ItemStack(Material.BANNER);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        meta.setBaseColor(color);
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore, "", "§eClick to select"));
        banner.setItemMeta(meta);
        return banner;
    }
    @EventHandler
    public void onFinalBorderSizeMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTitle().equals(ChatColor.GRAY + "Final Border Size"))) {
            return;
        }
        
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.BANNER && clicked.hasItemMeta()) {
            String name = clicked.getItemMeta().getDisplayName();
            
            if (name.contains("500")) {
                finalBorderSize = 500;
            } else if (name.contains("100")) {
                finalBorderSize = 100;
            } else if (name.contains("50")) {
                finalBorderSize = 50;
            } else if (name.contains("25")) {
                finalBorderSize = 25;
            }
            
            player.sendMessage("§aFinal border size set to §e" + finalBorderSize + " blocks§a.");
            openFinalBorderSizeMenu(player); // Refresh menu
        }
        else if (clicked.getType() == Material.ARROW) {
            openMenu(player); // Return to main menu
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
        teamSelectionSystem.giveSelectionBanner(player);
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
            } else if (event.getSlot() == 7) {
                if (event.getClick().isRightClick()) {
                    pvpTime = 0;
                } else {
                    addPvPTime(5); 
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> openMenu(player), 1L);
            } else if (event.getSlot() == 6) {
                if (event.getClick().isRightClick()) {
                    meetupTime = 0;
                } else {
                	addMeetupTime(5);
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> openMenu(player), 1L);
            } else if (event.getSlot() == 0) {
            	openSlotManagementMenu(player);
            } else if (event.getSlot() == 3) {
            	openBorderInitial(player);
            } else if (event.getSlot() == 9) {
            	openDrop(player);
            } else if (event.getSlot() == 11) {
            	openPotionMenu(player);
            } else if (event.getSlot() == 27) {
            	openScenariosMenu(player);
            } else if (event.getSlot() == 31 ) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "start");
            } else if (event.getSlot() == 4) {
            	openFinalBorderSizeMenu(player);
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
    private void addSwitchTime(int seconds) {
        switchTime += seconds;
        staticSwitchTime = switchTime; // Update the static value when manually changed
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
     public void onGameStart4(GameStartEvent e) {     
         switchTime = staticSwitchTime; // Initialize from the configured value
         
         new BukkitRunnable() {
             @Override
             public void run() {
            	 if (gamemode.getMode() == 2) {
            		 if (Gamestatus.getStatus() != 1) {
                         cancel(); // Stop if game isn't running
                         return;
                     }
                     
                     if (switchTime > 0) {
                         switchTime--;
                         
                         // Optional: Add countdown announcements like PvP timer
                         if (switchTime == 300) { // 5 minutes left
                             Bukkit.broadcastMessage("§e§lUHC §r§8➢ §ePlayer switch will occur in §b5 §eminutes.");
                         } else if (switchTime == 60) { // 1 minute left
                             Bukkit.broadcastMessage("§e§lUHC §r§8➢ §ePlayer switch will occur in §b1 §eminute.");
                         } else if (switchTime <= 5 && switchTime > 0) { // 10-1 seconds left
                             Sound sound = Sound.valueOf("BLOCK_NOTE_PLING");
                             for (Player p : Bukkit.getOnlinePlayers()) {
                                 p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                             }
                             Bukkit.broadcastMessage("§e§lUHC §r§8➢ §ePlayer switch in §b" + switchTime + " §eseconds!");
                         }
                     } else if (switchTime == 0) {
                         // When timer reaches 0
                         switchUHC.executeSwitch();
                         Bukkit.broadcastMessage("§e§lUHC §r§8➢ §aPlayers have been switched!");              
                         if (staticSwitchTime > 0) {
                             switchTime = staticSwitchTime; // Reset timer to original value FIRST
                             switchUHC.startSwitchTimer(staticSwitchTime); // Then restart the timer
                         } else {
                             cancel(); // Stop the timer if no switch time is set
                         }
                     }
                 }
            	 }
                
         }.runTaskTimer(plugin, 0L, 20L); // Run every second
     }

     @EventHandler
     public void onGameStart2(GameStartEvent e) {
    	    e.getp();
    	    
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
    	            	meetupTime = meetupTime;
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
    	            	startBorderShrink();
    	            	
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
         player.getInventory().getItemInHand();
         
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
    	public void startBorderShrink() {
    	    World world = Bukkit.getWorld("world");
    	    if (world == null) return;
    	    WorldBorder worldBorder = world.getWorldBorder();
    	    
    	    double currentSize = worldBorder.getSize();
    	    double targetSize = finalBorderSize;
    	    
    	    
    	    double distanceToShrink = currentSize - targetSize;
    	    double duration = distanceToShrink / borderSpeed; 
    	    
    	    worldBorder.setSize(targetSize, (long) duration);
    	    Bukkit.broadcastMessage("§aBorder shrinking to §e" + finalBorderSize + "§a blocks at §e" + 
    	                          borderSpeed + " blocks/sec§a. ETA: §e" + 
    	                          formatTime((int)duration) + "§a.");
    	}
     @EventHandler
     public void onGameStart(GameStartEvent e) {
         e.getp();
         
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
	            	pvpTime = pvpTime;
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
    public static int getSwitchTime() {
    	return switchTime;
    }
    public static void setSwitchTime(int newSwitchTime) {
    	switchTime = newSwitchTime;
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
@EventHandler
public void onBlockBreak2(BlockBreakEvent event) {
    if (cutCleanEnabled) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        int xpBoost = dropRates.getOrDefault("XP_BOTTLE", 0);
        int boostedXp = (int) (1 * (1 + (xpBoost / 100.0)));
        Material blockType = block.getType();
        
        // Only handle specific blocks, don't cancel others
        if (blockType == Material.IRON_ORE || blockType == Material.GOLD_ORE) {
            // Cancel the event only for these blocks
            event.setCancelled(true);
            
            // Handle iron and gold ores
            if (blockType == Material.IRON_ORE) {
                player.giveExp(boostedXp);
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.IRON_INGOT));
            } else if (blockType == Material.GOLD_ORE) {
                player.giveExp(boostedXp);
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.GOLD_INGOT));
            }
            
            // Break the block manually
            block.setType(Material.AIR);
        }
        // Let all other blocks break normally
    }
}



@EventHandler
public void onEntityDeath2(EntityDeathEvent event) {
    if (cutCleanEnabled && event.getEntity() instanceof Player == false) {  // Exclude players
        // Get the entity type
        EntityType entityType = event.getEntityType();
        
        // Check if the entity is an animal
        if (entityType == EntityType.COW) {
            event.getDrops().clear();  // Remove all drops
            event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), new ItemStack(Material.COOKED_BEEF));
        } else if (entityType == EntityType.PIG) {
            event.getDrops().clear();  // Remove all drops
            event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), new ItemStack(Material.COOKED_BEEF));
        } else if (entityType == EntityType.CHICKEN) {
            event.getDrops().clear();  // Remove all drops
            event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), new ItemStack(Material.COOKED_CHICKEN));
        } else if (entityType == EntityType.SHEEP) {
            event.getDrops().clear();  // Remove all drops
            event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), new ItemStack(Material.COOKED_MUTTON));
        }
        // Add more animals if needed
    }
}

}

