package Rules;


import test.main;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class gameconfig implements Listener {
    private static String gameName = "HOST";
    private static gameconfig instance;
    private TeamSelectionSystem teamSelectionSystem;
    private final Map<String, Integer> dropRates = new HashMap<>();
    private final Set<UUID> playersInSetupMode = new HashSet<>();

    private final String MENU_TITLE = "Drop Rate Settings";
    private boolean isNetherAccessEnabled = false;
    private World mainWorld = Bukkit.getWorld("world");
    private final Random random = new Random();
    private final Map<Location, Long> lastDropTimes = new HashMap<>();
    private final Map<Location, Integer> treeDropCounts = new HashMap<>();
    private final Set<Location> activePortals = new HashSet<>();
    private static final long DROP_COOLDOWN_MS = 5000;
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
    private final main plugin; 
    public gameconfig(main plugin) {
    	instance=this;
    	this.switchUHC = new SwitchUHC(plugin.getTeamManager());
        this.plugin = plugin;
        this.teamSelectionSystem = new TeamSelectionSystem(manager, plugin);
        for (String key : new String[]{"APPLE", "GOLDEN_APPLE", "FLINT", "FEATHER", "ARROW", "XP_BOTTLE", "ENDER_PEARL"}) {
            int defaultRate = switch(key) {
                case "APPLE" -> 10;        // 10% chance (feels right for apples)
                case "GOLDEN_APPLE" -> 3;  // 3% chance (rare but obtainable)
                case "FLINT" -> 35;        // 35% chance
                case "FEATHER" -> 1;       // +1 feather always
                case "ARROW" -> 3;         // +3 arrows always
                case "XP_BOTTLE" -> 30;    // 30% XP boost
                case "ENDER_PEARL" -> 8;   // 8% chance
                default -> 0;
            };
            dropRates.put(key, plugin.getConfig().getInt("drop_rates." + key, defaultRate));
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
        addItem(menu, 8, Material.NETHERRACK, "§eNether Access", "§7Toggle §aNether access.", "", "§e➢ §7Status: " + (isNetherAccessEnabled ? "§aEnabled" : "§cDisabled"), "", "§6§l➢ §eClick to change");
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
    @EventHandler
    public void onPortalCreate(BlockIgniteEvent event) {
        if (!isNetherAccessEnabled && 
            event.getBlock().getType() == Material.OBSIDIAN && 
            event.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Nether access is disabled!");
        }
    }
    private void toggleNetherAccess(Player player) {
        isNetherAccessEnabled = !isNetherAccessEnabled;
        plugin.getConfig().set("nether_access", isNetherAccessEnabled);
        plugin.saveConfig();
        
        String status = isNetherAccessEnabled ? "§aENABLED" : "§cDISABLED";
        player.sendMessage("§eNether access: " + status);
        
        // If disabling, teleport players back from Nether
        if (!isNetherAccessEnabled) {
            teleportPlayersInNether();
        }
    }
    private boolean cutCleanEnabled = false;
    public void openScenariosMenu(Player player) {
        Inventory scenariosMenu = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Scenarios");

        // Wagons
        addItem1(scenariosMenu, 2, Material.STORAGE_MINECART, "§eComing soon...");
        addItem1(scenariosMenu, 6, Material.EXPLOSIVE_MINECART, "§eGamemode selection", "§7Click to open the Gamemode menu.");

        // Line 3
        addItem2(scenariosMenu, 18, createMasterLevelItem());
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
        addItem2(scenariosMenu, 34, createCatEyesItem());
        addItem2(scenariosMenu, 35, createCutCleanItem());

        // Line 5
        addItem2(scenariosMenu, 36, createDoubleHealthItem());
        addItem2(scenariosMenu, 37, createGoneFishinItem());
        addItem1(scenariosMenu, 38, Material.APPLE, "§eApple");
        addItem1(scenariosMenu, 39, Material.BOW, "§eBow");
        ItemStack noBreakAxe = new ItemStack(Material.IRON_AXE);
        ItemMeta noBreakMeta = noBreakAxe.getItemMeta();
        noBreakMeta.setDisplayName("§eNoBreak");
        List<String> lore = new ArrayList<>();
        lore.add("§7Category: §aQuality of Life");
        lore.add("");
        lore.add("§6Features:");
        lore.add("§7- Tools, weapons, and armor never break");
        lore.add("§7- All items become unbreakable");
        lore.add("");
        lore.add("§eStatus: " + (noBreakEnabled ? "§aEnabled" : "§cDisabled"));
        noBreakMeta.setLore(lore);

        // Add enchantment glow if enabled
        if (noBreakEnabled) {
            noBreakMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            noBreakMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        noBreakAxe.setItemMeta(noBreakMeta);
        addItem2(scenariosMenu, 40, noBreakAxe);

        // 2. Update the click handler (around line 540) to properly refresh the enchantment:
  
        addItem1(scenariosMenu, 41, Material.BREAD, "§eBread");
        addItem1(scenariosMenu, 42, Material.IRON_SWORD, "§eIron Sword");
        addItem1(scenariosMenu, 43, Material.COOKIE, "§eCookie");
        addItem2(scenariosMenu, 44, createSuperHeroesItem());

        // Navigation
        addItem1(scenariosMenu, 49, Material.ARROW, "§cReturn to Main Menu");
        addItem1(scenariosMenu, 50, Material.PAPER, "§eNext Page");

        player.openInventory(scenariosMenu);
    } 
    private boolean superHeroesEnabled = false;
    private ItemStack createSuperHeroesItem() {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();
        meta.setDisplayName("§dSuperHeroes");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Category: §5Other");
        lore.add("");
        lore.add("§6Features:");
        lore.add("§7- Grants each player random super powers");
        lore.add("§7- Various powerful effect combinations");
        lore.add("");
        lore.add("§eStatus: " + (superHeroesEnabled ? "§aEnabled" : "§cDisabled"));
        
        meta.setLore(lore);
        
        if (superHeroesEnabled) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        star.setItemMeta(meta);
        return star;
    }
    private boolean masterLevelEnabled = false;
    private int masterLevelAmount = 1000;
    private static final int MAX_MASTER_LEVEL = 1000;
    private ItemStack createMasterLevelItem() {
        ItemStack xpBottle = new ItemStack(Material.EXP_BOTTLE);
        ItemMeta meta = xpBottle.getItemMeta();
        meta.setDisplayName("§6MASTERLEVEL");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Category: §dEnchantment");
        lore.add("");
        lore.add("§6Features:");
        lore.add("§7- Grants all players a massive XP boost");
        lore.add("§7- Configurable starting level");
        lore.add("");
        lore.add("§eConfiguration: Level: §a" + masterLevelAmount);
        lore.add("");
        lore.add("§eStatus: " + (masterLevelEnabled ? "§aEnabled" : "§cDisabled"));
        lore.add("");
        lore.add("§7[i] §bEditable Scenario");
        
        meta.setLore(lore);
        
        if (masterLevelEnabled) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        xpBottle.setItemMeta(meta);
        return xpBottle;
    }
    private void openMasterLevelConfigMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, "§6MASTERLEVEL Configuration");
        
        // Current level display
        ItemStack current = new ItemStack(Material.EXP_BOTTLE);
        ItemMeta currentMeta = current.getItemMeta();
        currentMeta.setDisplayName("§aCurrent Level: §e" + masterLevelAmount);
        current.setItemMeta(currentMeta);
        menu.setItem(4, current);
        
        // Decrease button (-100)
        ItemStack decrease = new ItemStack(Material.REDSTONE);
        ItemMeta decreaseMeta = decrease.getItemMeta();
        decreaseMeta.setDisplayName("§cDecrease (-100)");
        decrease.setItemMeta(decreaseMeta);
        menu.setItem(0, decrease);
        
        // Increase button (+100)
        ItemStack increase = new ItemStack(Material.EMERALD);
        ItemMeta increaseMeta = increase.getItemMeta();
        increaseMeta.setDisplayName("§aIncrease (+100)");
        increase.setItemMeta(increaseMeta);
        menu.setItem(8, increase);
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§cBack to Scenarios");
        back.setItemMeta(backMeta);
        menu.setItem(1, back);
        
        player.openInventory(menu);
    }
    @EventHandler
    public void onMasterLevelConfigClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6MASTERLEVEL Configuration")) return;
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        switch (clicked.getType()) {
            case REDSTONE: // Decrease
                masterLevelAmount = Math.max(0, masterLevelAmount - 100);
                break;
            case EMERALD: // Increase
                masterLevelAmount = Math.min(MAX_MASTER_LEVEL, masterLevelAmount + 100);
                break;
            case ARROW: // Back
                openScenariosMenu(player);
                return;
            default:
                return;
        }
        
        // Save new value
        plugin.getConfig().set("scenarios.master_level.amount", masterLevelAmount);
        plugin.saveConfig();
        
        // Update menu
        openMasterLevelConfigMenu(player);
    }
    private boolean doubleHealthEnabled = false;
    private ItemStack createDoubleHealthItem() {
        ItemStack potion = new ItemStack(Material.POTION);
        ItemMeta meta = potion.getItemMeta();
        meta.setDisplayName("§cDoubleHealth");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Category: §aSurvival");
        lore.add("");
        lore.add("§6Features:");
        lore.add("§7- Doubles max health (20 → 40)");
        lore.add("§7- Full heal on activation");
        lore.add("");
        lore.add("§eStatus: " + (doubleHealthEnabled ? "§aEnabled" : "§cDisabled"));
        
        meta.setLore(lore);
        
        if (doubleHealthEnabled) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        potion.setItemMeta(meta);
        return potion;
    }
    private boolean catEyesEnabled = false;
    private ItemStack createCatEyesItem() {
        ItemStack lantern = new ItemStack(Material.SEA_LANTERN);
        ItemMeta meta = lantern.getItemMeta();
        meta.setDisplayName("§cCatEyes");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Category: §aSurvival");
        lore.add("");
        lore.add("§6Features:");
        lore.add("§7- Permanent Night Vision effect");
        lore.add("§7- No blindness in darkness");
        lore.add("");
        lore.add("§eStatus: " + (catEyesEnabled ? "§aEnabled" : "§cDisabled"));
        
        meta.setLore(lore);
        
        if (catEyesEnabled) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        lantern.setItemMeta(meta);
        return lantern;
    }
    private boolean noBreakEnabled = false;
 // 4. Add these new methods somewhere in the class
    private void makeAllItemsUnbreakable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Armor
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor != null) {
                    makeUnbreakable(armor);
                }
            }
            
            // Inventory items
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && isBreakableItem(item.getType())) {
                    makeUnbreakable(item);
                }
            }
        }
    }

    private void makeAllItemsBreakable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Armor
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor != null) {
                    makeBreakable(armor);
                }
            }
            
            // Inventory items
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && isBreakableItem(item.getType())) {
                    makeBreakable(item);
                }
            }
        }
    }

    private void makeUnbreakable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
    }

    private void makeBreakable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(false);
        item.setItemMeta(meta);
    }

    private boolean isBreakableItem(Material material) {
        return material.name().endsWith("_AXE") || 
               material.name().endsWith("_PICKAXE") || 
               material.name().endsWith("_SHOVEL") || 
               material.name().endsWith("_HOE") || 
               material.name().endsWith("_SWORD") || 
               material.name().endsWith("_HELMET") || 
               material.name().endsWith("_CHESTPLATE") || 
               material.name().endsWith("_LEGGINGS") || 
               material.name().endsWith("_BOOTS") || 
               material == Material.BOW || 
               material == Material.FISHING_ROD || 
               material == Material.SHEARS || 
               material == Material.FLINT_AND_STEEL;
    }

    // 5. Add this to the GameStartEvent handler (around line 1830)
    
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
                    
                    // Save to config file
                    plugin.getConfig().set("scenarios.gone_fishin", goneFishinEnabled);
                    plugin.saveConfig();
                    
                    Bukkit.getLogger().info("[CONFIG] Gone Fishin' set to: " + goneFishinEnabled);
                    
                    // Update the menu item
                    event.getInventory().setItem(event.getSlot(), createGoneFishinItem());
                    
                    String status = goneFishinEnabled ? "§aENABLED" : "§cDISABLED";
                    player.sendMessage("§eGone Fishin' scenario: " + status);
                    event.setCancelled(true);
                }  else  if (item.getType() == Material.IRON_AXE && item.getItemMeta().getDisplayName().equals("§eNoBreak")) {
                    noBreakEnabled = !noBreakEnabled;
                    
                    // Save to config
                    plugin.getConfig().set("scenarios.no_break", noBreakEnabled);
                    plugin.saveConfig();
                    
                    // Update the menu with new enchantment state
                    openScenariosMenu(player);
                    
                    // Apply/remove unbreakable to all items if game is running
                    if (Gamestatus.getStatus() == 1) {
                        if (noBreakEnabled) {
                            makeAllItemsUnbreakable();
                        } else {
                            makeAllItemsBreakable();
                        }
                    }
                    
                    String status = noBreakEnabled ? "§aENABLED" : "§cDISABLED";
                    player.sendMessage("§eNoBreak scenario: " + status);
                    event.setCancelled(true);
                } else if (item.getType() == Material.SEA_LANTERN && item.getItemMeta().getDisplayName().equals("§cCatEyes")) {
                    catEyesEnabled = !catEyesEnabled;
                    
                    // Save to config
                    plugin.getConfig().set("scenarios.cat_eyes", catEyesEnabled);
                    plugin.saveConfig();
                    
                    // Update the menu
                    event.getInventory().setItem(event.getSlot(), createCatEyesItem());
                    
                    String status = catEyesEnabled ? "§aENABLED" : "§cDISABLED";
                    player.sendMessage("§eCatEyes scenario: " + status);
                    event.setCancelled(true);
                } else if (item.getType() == Material.POTION && item.getItemMeta().getDisplayName().equals("§cDoubleHealth")) {
                    doubleHealthEnabled = !doubleHealthEnabled;
                    
                    // Save to config
                    plugin.getConfig().set("scenarios.double_health", doubleHealthEnabled);
                    plugin.saveConfig();
                    
                    // Update the menu
                    event.getInventory().setItem(event.getSlot(), createDoubleHealthItem());
                    
                    String status = doubleHealthEnabled ? "§aENABLED" : "§cDISABLED";
                    player.sendMessage("§eDoubleHealth scenario: " + status);
                    event.setCancelled(true);
                } else if (item.getType() == Material.EXP_BOTTLE && item.getItemMeta().getDisplayName().equals("§6MASTERLEVEL")) {
                    if (event.getClick().isRightClick()) {
                        openMasterLevelConfigMenu(player);
                    } else {
                        masterLevelEnabled = !masterLevelEnabled;
                        
                        // Save to config
                        plugin.getConfig().set("scenarios.master_level.enabled", masterLevelEnabled);
                        plugin.getConfig().set("scenarios.master_level.amount", masterLevelAmount);
                        plugin.saveConfig();
                        
                        // Update the menu
                        event.getInventory().setItem(event.getSlot(), createMasterLevelItem());
                        
                        String status = masterLevelEnabled ? "§aENABLED" : "§cDISABLED";
                        player.sendMessage("§eMASTERLEVEL scenario: " + status);
                    }
                    event.setCancelled(true);
                } else if (item.getType() == Material.NETHER_STAR && item.getItemMeta().getDisplayName().equals("§dSuperHeroes")) {
                    superHeroesEnabled = !superHeroesEnabled;
                    
                    // Save to config
                    plugin.getConfig().set("scenarios.super_heroes", superHeroesEnabled);
                    plugin.saveConfig();
                    
                    // Update the menu
                    event.getInventory().setItem(event.getSlot(), createSuperHeroesItem());
                    
                    String status = superHeroesEnabled ? "§aENABLED" : "§cDISABLED";
                    player.sendMessage("§eSuperHeroes scenario: " + status);
                    event.setCancelled(true);
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
    public boolean isSuperHeroesEnabled() {
        return superHeroesEnabled;
    }
    public boolean isMasterLevelEnabled() {
        return masterLevelEnabled;
    }

    public int getMasterLevelAmount() {
        return masterLevelAmount;
    }
    public boolean isDoubleHealthEnabled() {
        return doubleHealthEnabled;
    }
    public boolean isCatEyesEnabled() {
        return catEyesEnabled;
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
        // If all potions are enabled, skip individual checks (only enforce level/duration)
        if (allPotionsEnabled) {
            // Check level and duration restrictions
            for (ItemStack item : event.getContents().getContents()) {
                if (item != null && item.getType() == Material.POTION) {
                    Potion potion = Potion.fromItemStack(item);
                    if (potion != null) {
                        if (!levelTwoPotionsEnabled && potion.getLevel() > 1) {
                            event.setCancelled(true);
                            return;
                        }
                        if (!extendedPotionsEnabled && potion.hasExtendedDuration()) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
            return; // Allow brewing since all potions are enabled
        }

        // If "All Potions" is disabled, check individual potion permissions
        for (ItemStack item : event.getContents().getContents()) {
            if (item != null && item.getType() == Material.POTION) {
                Potion potion = Potion.fromItemStack(item);
                if (potion != null) {
                    // Check if the potion type is allowed
                    switch (potion.getType()) {
                        case STRENGTH:
                            if (!strengthPotionEnabled) {
                                event.setCancelled(true);
                                return;
                            }
                            break;
                        case POISON:
                            if (!poisonPotionEnabled) {
                                event.setCancelled(true);
                                return;
                            }
                            break;
                        case INSTANT_HEAL:
                            if (!healingPotionEnabled) {
                                event.setCancelled(true);
                                return;
                            }
                            break;
                        case SPEED:
                            if (!speedPotionEnabled) {
                                event.setCancelled(true);
                                return;
                            }
                            break;
                        default:
                            // If it's not a handled potion type, cancel by default (optional)
                            event.setCancelled(true);
                            return;
                    }

                    // Check level and duration restrictions
                    if (!levelTwoPotionsEnabled && potion.getLevel() > 1) {
                        event.setCancelled(true);
                        return;
                    }
                    if (!extendedPotionsEnabled && potion.hasExtendedDuration()) {
                        event.setCancelled(true);
                        return;
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
            } else if (event.getSlot() == 12) { // Starting Items slot
                openStartingItemsViewer(player);
            } else if (event.getSlot() == 8) { // Nether Access slot
                isNetherAccessEnabled = !isNetherAccessEnabled;
                plugin.getConfig().set("nether_access", isNetherAccessEnabled);
                plugin.saveConfig();
                
                if (!isNetherAccessEnabled) {
                    closeExistingPortals();
                    teleportPlayersInNether();
                }
                
                player.sendMessage(ChatColor.YELLOW + "Nether access: " + 
                    (isNetherAccessEnabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                openMenu(player);
            }
        }
    }
    private static ItemStack[] startingInventory = new ItemStack[36]; // Main inventory (0-35)
    private static ItemStack[] startingArmor = new ItemStack[4];     // Armor (helmet, chestplate, leggings, boots)
    public static ItemStack[] getStartingInventory() {
        return startingInventory;
    }

    public static ItemStack[] getStartingArmor() {
        return startingArmor;
    }

    public void openStartingItemsViewer(Player player) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Starting Items (Preview)");

        // Display saved armor (slots 36-39)
        if (startingArmor != null) {
            menu.setItem(36, startingArmor[0] != null ? startingArmor[0].clone() : null); // Helmet
            menu.setItem(37, startingArmor[1] != null ? startingArmor[1].clone() : null); // Chestplate
            menu.setItem(38, startingArmor[2] != null ? startingArmor[2].clone() : null); // Leggings
            menu.setItem(39, startingArmor[3] != null ? startingArmor[3].clone() : null); // Boots
        }

        // Display saved inventory (slots 0-35)
        if (startingInventory != null) {
            for (int i = 0; i < startingInventory.length; i++) {
                if (startingInventory[i] != null) {
                    menu.setItem(i, startingInventory[i].clone());
                }
            }
        }

        // Return arrow (Slot 50)
        ItemStack returnArrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = returnArrow.getItemMeta();
        arrowMeta.setDisplayName(ChatColor.YELLOW + "§cBack to Main Menu");
        returnArrow.setItemMeta(arrowMeta);
        menu.setItem(50, returnArrow);

        // "Edit" button (Slot 53 - replaces the door)
        ItemStack editButton = new ItemStack(Material.WOOD_DOOR);
        ItemMeta editMeta = editButton.getItemMeta();
        editMeta.setDisplayName(ChatColor.GREEN + "§aEdit Starting Items");
        editMeta.setLore(Arrays.asList(
            "§7Click to enter setup mode.",
            "§7You will be put in Creative Mode.",
            "§7Type §a/finish §7when done."
        ));
        editButton.setItemMeta(editMeta);
        menu.setItem(53, editButton);

        player.openInventory(menu);
    }
    public void openStartingItemsEditor(Player player) {
        // Clear the player's inventory and load saved items (if they exist)
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        if (startingInventory != null) {
            player.getInventory().setContents(startingInventory);
        }
        if (startingArmor != null) {
            player.getInventory().setArmorContents(startingArmor);
        }

        // Switch to Creative Mode and add to setup mode tracking
        player.setGameMode(GameMode.CREATIVE);
        playersInSetupMode.add(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "§aYou are now editing starting items!");
        player.sendMessage(ChatColor.GREEN + "§aModify your inventory and type §e/finish §awhen done.");
    }
    @EventHandler
    public void onStartingItemsViewerClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.GOLD + "Starting Items (Preview)")) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 50) { // Return to main menu
            openMenu(player);
        } else if (slot == 53) { // Edit button
            player.closeInventory();
            openStartingItemsEditor(player); // Switches to edit mode
        }
    }
    @EventHandler
    public void onStartingItemsMenuClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.GOLD + "Starting Items Setup")) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Return to main menu (Slot 50)
        if (slot == 49) {
            openMenu(player);
        }
        // Configure door (Slot 53)
        else if (slot == 53) {
            player.closeInventory();
            player.setGameMode(GameMode.CREATIVE);
            player.sendMessage(ChatColor.GREEN + "§aYou are now in setup mode!");
            player.sendMessage(ChatColor.GREEN + "§aEdit your inventory and type §e/finish §awhen done.");
        }
    }
    @EventHandler
    public void onBlockBreak1(BlockBreakEvent event) {
        if (playersInSetupMode.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (playersInSetupMode.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        if (command.equals("/finish")) {
            if (player.getGameMode() == GameMode.CREATIVE) {
                // Save the edited items
                startingInventory = player.getInventory().getContents();
                startingArmor = player.getInventory().getArmorContents();

                // Clear inventory and restore original tools
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);

                if (player.isOp()) {
                    // Give back config comparator
                    ItemStack comparator = new ItemStack(Material.REDSTONE_COMPARATOR);
                    ItemMeta meta = comparator.getItemMeta();
                    meta.setDisplayName("§eGame Config");
                    comparator.setItemMeta(meta);
                    player.getInventory().addItem(comparator);

                    // Give back team banner (if teams enabled)
                    if (teamSize > 1) {
                        teamSelectionSystem.giveSelectionBanner(player);
                    }
                }

                player.setGameMode(GameMode.SURVIVAL);
                playersInSetupMode.remove(player.getUniqueId()); // Remove from setup mode
                player.sendMessage(ChatColor.GREEN + "§aStarting items saved!");
            }
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onPlayerCommand1(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        if (command.equals("/enchant")) {
            if (player.getGameMode() == GameMode.CREATIVE) {
                openEnchantMenu(player);
            } else {
                player.sendMessage(ChatColor.RED + "§cYou must be in setup mode to use this!");
            }
            event.setCancelled(true);
        }
    }
    public void openEnchantMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 36, ChatColor.DARK_PURPLE + "Tool Enchanting");

        ItemStack tool = player.getInventory().getItemInHand();
        if (tool == null || tool.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Hold a tool/armor to enchant!");
            return;
        }

        // List all possible enchantments for the tool
        List<Enchantment> possibleEnchants = new ArrayList<>();
        for (Enchantment enchant : Enchantment.values()) {
            if (enchant.canEnchantItem(tool)) {
                possibleEnchants.add(enchant);
            }
        }

        // Fill the menu with enchantment books
        for (int i = 0; i < possibleEnchants.size() && i < 27; i++) {
            Enchantment enchant = possibleEnchants.get(i);
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = book.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + enchant.getName());
            
            // Check current level (if already applied)
            int currentLevel = tool.getEnchantmentLevel(enchant);
            
            meta.setLore(Arrays.asList(
                "§7Current: §e" + currentLevel,
                "§aLeft-Click: +1 Level",
                "§cRight-Click: -1 Level"
            ));
            book.setItemMeta(meta);
            menu.setItem(i, book);
        }

        // Add a "Back" button (Slot 35)
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backButton.setItemMeta(backMeta);
        menu.setItem(35, backButton);

        player.openInventory(menu);
    }
    @EventHandler
    public void onEnchantMenuClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Tool Enchanting")) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Back button (Slot 35)
        if (clicked.getType() == Material.ARROW && event.getSlot() == 35) {
            player.closeInventory();
            return;
        }

        // If an enchantment book is clicked
        if (clicked.getType() == Material.ENCHANTED_BOOK) {
            ItemStack tool = player.getInventory().getItemInHand();
            if (tool == null || tool.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "Hold a tool/armor to enchant!");
                return;
            }

            String enchantName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            Enchantment enchant = Enchantment.getByName(enchantName);
            if (enchant == null) return;

            int currentLevel = tool.getEnchantmentLevel(enchant);

            if (event.getClick().isLeftClick()) {
                currentLevel++;
                tool.addUnsafeEnchantment(enchant, currentLevel);
            } else if (event.getClick().isRightClick()) {
                if (currentLevel <= 1) {
                    tool.removeEnchantment(enchant); // Remove if level is 1 or 0
                    currentLevel = 0;
                } else {
                    currentLevel--;
                    tool.addUnsafeEnchantment(enchant, currentLevel);
                }
            }

            // Update the book's display
            ItemMeta meta = clicked.getItemMeta();
            meta.setLore(Arrays.asList(
                "§7Current: §e" + currentLevel,
                "§aLeft-Click: +1 Level",
                "§cRight-Click: -1 Level"
            ));
            clicked.setItemMeta(meta);

            player.updateInventory();
        }
    }
    private Enchantment getRandomEnchantForTool(Material toolType) {
        if (toolType == Material.DIAMOND_SWORD || toolType == Material.IRON_SWORD) {
            return Enchantment.DAMAGE_ALL; // Sharpness
        } else if (toolType == Material.DIAMOND_PICKAXE || toolType == Material.IRON_PICKAXE) {
            return Enchantment.DIG_SPEED; // Efficiency
        } else if (toolType == Material.DIAMOND_AXE || toolType == Material.IRON_AXE) {
            return Enchantment.DIG_SPEED; // Efficiency
        } else if (toolType == Material.BOW) {
            return Enchantment.ARROW_DAMAGE; // Power
        } else if (toolType.name().endsWith("_HELMET") || toolType.name().endsWith("_CHESTPLATE") 
                || toolType.name().endsWith("_LEGGINGS") || toolType.name().endsWith("_BOOTS")) {
            return Enchantment.PROTECTION_ENVIRONMENTAL; // Protection
        } else {
            return Enchantment.DURABILITY; // Unbreaking (fallback)
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
     public void onPortalUse(PlayerPortalEvent event) {
         if (!isNetherAccessEnabled && 
             event.getCause() == PlayerPortalEvent.TeleportCause.NETHER_PORTAL) {
             event.setCancelled(true);
             event.getPlayer().sendMessage(ChatColor.RED + "Nether access is disabled!");
         }
     }
     @EventHandler
     public void onPlayerInteract1(PlayerInteractEvent event) {
         if (!isNetherAccessEnabled && 
             event.getAction() == Action.RIGHT_CLICK_BLOCK && 
             event.getClickedBlock().getType() == Material.OBSIDIAN && 
             event.getItem() != null && 
             event.getItem().getType() == Material.FLINT_AND_STEEL) {
             event.setCancelled(true);
             event.getPlayer().sendMessage(ChatColor.RED + "Nether access is disabled!");
         }
     }
     public void closeExistingPortals() {
    	    for (World world : Bukkit.getWorlds()) {
    	        for (Chunk chunk : world.getLoadedChunks()) {
    	            for (BlockState blockState : chunk.getTileEntities()) {
    	                if (blockState.getBlock().getType() == Material.PORTAL) {
    	                    blockState.getBlock().setType(Material.AIR);
    	                }
    	            }
    	        }
    	    }
    	    activePortals.clear();
    	}


     // Teleport players in the Nether to the main world within the border
     private void teleportPlayersInNether() {
    	    World netherWorld = Bukkit.getWorld("world_nether");
    	    if (netherWorld == null) return;
    	    
    	    for (Player player : netherWorld.getPlayers()) {
    	        Location safeLocation = findSafeLocation(mainWorld);
    	        player.teleport(safeLocation);
    	        player.sendMessage("§aYou have been teleported back to the main world as the Nether is disabled.");
    	    }
    	    
    	    // Close all nether portals in the main world
    	    closeAllPortals(mainWorld);
    	}

    	private Location findSafeLocation(World world) {
    	    WorldBorder border = world.getWorldBorder();
    	    Location spawn = world.getSpawnLocation();
    	    
    	    // Ensure the location is within the border
    	    double x = Math.max(Math.min(spawn.getX(), border.getCenter().getX() + border.getSize()/2), 
    	                      border.getCenter().getX() - border.getSize()/2);
    	    double z = Math.max(Math.min(spawn.getZ(), border.getCenter().getZ() + border.getSize()/2), 
    	                      border.getCenter().getZ() - border.getSize()/2);
    	    
    	    // Find the highest block at this location
    	    int y = world.getHighestBlockYAt((int)x, (int)z) + 1;
    	    
    	    return new Location(world, x, y, z);
    	}
    	private void closeAllPortals(World world) {
    	    for (Chunk chunk : world.getLoadedChunks()) {
    	        for (BlockState blockState : chunk.getTileEntities()) {
    	            // In 1.12.2, we check for portal blocks directly
    	            if (blockState.getBlock().getType() == Material.PORTAL) {
    	                blockState.getBlock().setType(Material.AIR);
    	            }
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
	                Bukkit.broadcastMessage("§7§o⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯");
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

    public void onBlockBreak(BlockBreakEvent event) {
        Material block = event.getBlock().getType();

        // Apple drop from trees (both manual breaking and natural decay)
        if (event.getBlock().getType() == Material.LEAVES || event.getBlock().getType() == Material.LEAVES_2) {
            handleLeafDrops(event.getBlock());
        }

        // Flint drop from gravel
        if (block == Material.GRAVEL) {
            int flintChance = getDropRate("FLINT");
            if (random.nextInt(100) < flintChance) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.FLINT));
            }
        }
    }
    private void handleLeafDrops(Block block) {
        // Find the base log of the tree
        Block treeBase = findTreeBase(block);
        if (treeBase == null) return;
        
        // Use the tree base's location as the key
        Location treeLocation = treeBase.getLocation();
        
        // Get or initialize drop count for this tree
        int drops = treeDropCounts.getOrDefault(treeLocation, 0);
        
        // Golden apple check (max 1 per tree)
        if (drops < 1 && random.nextInt(100) < getDropRate("GOLDEN_APPLE")) {
            block.getWorld().dropItemNaturally(block.getLocation(), 
                new ItemStack(Material.GOLDEN_APPLE));
            treeDropCounts.put(treeLocation, drops + 1);
            return;
        }
        
        // Regular apple check (max 3 per tree)
        if (drops < 3 && random.nextInt(100) < getDropRate("APPLE")) {
            block.getWorld().dropItemNaturally(block.getLocation(), 
                new ItemStack(Material.APPLE));
            treeDropCounts.put(treeLocation, drops + 1);
        }
    }
    private boolean canDropFromTree(Block block) {
        // Find the base of the tree (log block)
        Block base = findTreeBase(block);
        if (base == null) return true;
        
        Long lastDrop = lastDropTimes.get(base.getLocation());
        if (lastDrop == null || System.currentTimeMillis() - lastDrop > DROP_COOLDOWN_MS) {
            lastDropTimes.put(base.getLocation(), System.currentTimeMillis());
            return true;
        }
        return false;
    }
    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        handleLeafDrops(event.getBlock());
    }

    private Block findTreeBase(Block leafBlock) {
        // Search downward from the leaf to find the log
        for (int y = leafBlock.getY(); y > 0; y--) {
            Block current = leafBlock.getWorld().getBlockAt(leafBlock.getX(), y, leafBlock.getZ());
            if (current.getType().toString().endsWith("_LOG")) {
                return current; // Found the tree's log
            }
        }
        return null; // No log found below this leaf
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
        
        if (blockType == Material.IRON_ORE || blockType == Material.GOLD_ORE) {
            event.setCancelled(true);
            
            if (blockType == Material.IRON_ORE) {
                player.giveExp(boostedXp);
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.IRON_INGOT));
            } else if (blockType == Material.GOLD_ORE) {
                player.giveExp(boostedXp);
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.GOLD_INGOT));
            }
            
            block.setType(Material.AIR);
        }
        // New: Coal ore to torches
        else if (blockType == Material.COAL_ORE) {
            event.setCancelled(true);
            player.giveExp(boostedXp);
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.TORCH, 4)); // 4 torches per coal ore
            block.setType(Material.AIR);
        }
        // New: Sugar cane to books
        if (block.getType() == Material.SUGAR_CANE_BLOCK) {
            event.setCancelled(true);
            
            // Count how many sugar canes are in this column
            int caneCount = 1;
            Block above = block.getRelative(BlockFace.UP);
            while (above.getType() == Material.SUGAR_CANE_BLOCK) {
                caneCount++;
                above.setType(Material.AIR);
                above = above.getRelative(BlockFace.UP);
            }
            
            // Drop books equal to the number of canes broken
            block.getWorld().dropItemNaturally(block.getLocation(), 
                new ItemStack(Material.BOOK, caneCount));
            block.setType(Material.AIR);
            
            return;
        }
    }
}

private boolean shouldDrop(String itemType) {
    int rate = dropRates.getOrDefault(itemType, 0);
    
    // For percentage-based drops (apples, flint, etc.)
    if (rate > 100) {  // Rates stored as 100 = 1%
        // Scale the random check appropriately
        return random.nextInt(10000) < rate;
    }
    // For absolute counts (feathers, arrows)
    return true; // The count is handled separately
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
    }
}

}


