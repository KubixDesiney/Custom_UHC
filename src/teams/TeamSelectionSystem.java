package teams;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;

import Rules.gameconfig;
import gamemodes.Gamestatus;
import gamemodes.gamemode;

import java.util.*;

public class TeamSelectionSystem implements Listener {
    private final UHCTeamManager teamManager;
    private final JavaPlugin plugin;
    private final Map<Player, Integer> teamSelectionPages = new HashMap<>();
    private static final int TEAMS_PER_PAGE = 28; // 7 rows * 4 columns (excluding borders)

    public TeamSelectionSystem(UHCTeamManager teamManager, JavaPlugin plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    public void updateAllPlayersBanners() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            giveSelectionBanner(player);
        }
        }
    public void giveSelectionBanner(Player player) {
        // Don't give anything if game is in progress
        if (Gamestatus.getStatus() == 1) {
            removeTeamBanners(player);
            removeConfigComparators(player);
            return;
        }

        // Only give banner if team size > 1 and game hasn't started
        if (gameconfig.getTeamSize() > 1) {
            removeTeamBanners(player);
            ItemStack banner = createTeamSelectionBanner();
            ItemMeta meta = banner.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_PURPLE + "UHC_TEAM_BANNER");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            banner.setItemMeta(meta);
            
            // Find first empty hotbar slot
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getItem(i) == null) {
                    player.getInventory().setItem(i, banner);
                    break;
                }
            }
        } else {
            removeTeamBanners(player);
        }
        
        // Only give comparator to OPs before game starts
        if (player.isOp() && Gamestatus.getStatus() != 1) {
            giveConfigComparator(player);
        }
    }

    private void giveConfigComparator(Player player) {
        removeConfigComparators(player);
        ItemStack comparator = new ItemStack(Material.REDSTONE_COMPARATOR);
        ItemMeta meta = comparator.getItemMeta();
        meta.setDisplayName("§eGame Config");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_PURPLE + "UHC_CONFIG_COMPARATOR");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        comparator.setItemMeta(meta);
        
        // Find first empty hotbar slot
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i) == null) {
                player.getInventory().setItem(i, comparator);
                break;
            }
        }
    }

    private void removeTeamBanners(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isTeamBanner(item)) {
                player.getInventory().clear(i);
            }
        }
    }
    private void removeConfigComparators(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isConfigComparator(item)) {
                player.getInventory().clear(i);
            }
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (isTeamBanner(dropped) || isConfigComparator(dropped)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT_CLICK_BLOCK") && 
            (isTeamBanner(event.getItem()) || isConfigComparator(event.getItem()))) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        // Prevent moving special items from their slots
        if (clicked != null && (isTeamBanner(clicked) || isConfigComparator(clicked))) {
            int targetSlot = isConfigComparator(clicked) ? 0 : (player.isOp() ? 1 : 0);
            
            // Only allow moving if it's staying in its designated slot
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || 
                (event.getSlot() == targetSlot && event.getRawSlot() != targetSlot)) {
                event.setCancelled(true);
                player.updateInventory();
            }
        }
    }
    private boolean isTeamBanner(ItemStack item) {
        if (item == null || item.getType() != Material.BANNER || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasLore() && meta.getLore().contains(ChatColor.DARK_PURPLE + "UHC_TEAM_BANNER");
    }

    private boolean isConfigComparator(ItemStack item) {
        if (item == null || item.getType() != Material.REDSTONE_COMPARATOR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasLore() && meta.getLore().contains(ChatColor.DARK_PURPLE + "UHC_CONFIG_COMPARATOR");
    }


    private ItemStack createTeamSelectionBanner() {
        ItemStack banner = new ItemStack(Material.BANNER);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "Team Selection");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Right-click to select your team",
            ChatColor.GRAY + "Team size: " + ChatColor.AQUA + gameconfig.getTeamSize() + "v" + gameconfig.getTeamSize()
        ));
        meta.setBaseColor(DyeColor.WHITE);
        banner.setItemMeta(meta);
        return banner;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (Gamestatus.getStatus() != 1 && gameconfig.getTeamSize() > 1) { 
            giveSelectionBanner(event.getPlayer());
        }
    }

    @EventHandler
    public void onBannerRightClick(PlayerInteractEvent event) {
        if (event.getAction().name().contains("RIGHT_CLICK") && 
            event.getItem() != null && 
            event.getItem().getType() == Material.BANNER &&
            event.getItem().hasItemMeta() &&
            event.getItem().getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Team Selection")) {
            
            event.setCancelled(true); // Prevent banner placement
            openTeamSelectionMenu(event.getPlayer(), 0); // Open first page
        }
    }

    private void openTeamSelectionMenu(Player player, int page) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.BLUE + "Select Team - Page " + (page + 1));
        setupMenuBorders(menu);
        
        List<String> allTeams = teamManager.getAllTeams();
        int totalPages = (int) Math.ceil((double) allTeams.size() / TEAMS_PER_PAGE);
        
        // Add teams to current page
        int startIndex = page * TEAMS_PER_PAGE;
        int endIndex = Math.min(startIndex + TEAMS_PER_PAGE, allTeams.size());
        
        int slot = 10; // Start position (second row, second column)
        for (int i = startIndex; i < endIndex; i++) {
            String teamName = allTeams.get(i);
            menu.setItem(slot, createTeamBanner(teamName, player));
            
            slot++;
            if ((slot + 1) % 9 == 0) { // Skip border columns
                slot += 2;
            }
        }
        
        addNavigationButtons(menu, page, totalPages);
        teamSelectionPages.put(player, page);
        player.openInventory(menu);
    }

    private void setupMenuBorders(Inventory menu) {
        ItemStack border = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 0);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        border.setItemMeta(meta);
        
        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, border);
            menu.setItem(i + 45, border);
        }
        
        // Side borders
        for (int i = 0; i < 54; i += 9) {
            menu.setItem(i, border);
            menu.setItem(i + 8, border);
        }
    }

    private ItemStack createTeamBanner(String teamName, Player viewingPlayer) {
        ItemStack banner = new ItemStack(Material.BANNER);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        
        meta.setBaseColor(DyeColor.WHITE);
        meta.setDisplayName(ChatColor.WHITE + teamName);
        
        List<Player> members = teamManager.getPlayersInTeam(teamName);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Team Members (" + members.size() + "/" + gameconfig.getTeamSize() + "):");
        lore.add(" ");
        
        // Add team members with special highlighting for the viewing player
        for (Player member : members) {
            if (member.equals(viewingPlayer)) {
                lore.add(ChatColor.GREEN + "→ " + member.getName() + " (You)");
            } else {
                lore.add(ChatColor.WHITE + member.getName());
            }
        }
        
        // Add empty slots
        int emptySlots = gameconfig.getTeamSize() - members.size();
        for (int i = 0; i < emptySlots; i++) {
            lore.add(ChatColor.ITALIC + "[Empty slot]");
        }
        
        meta.setLore(lore);
        banner.setItemMeta(meta);
        return banner;
    }

    private void addNavigationButtons(Inventory menu, int currentPage, int totalPages) {
        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Material.PAPER);
            ItemMeta meta = prev.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "Previous Page");
            prev.setItemMeta(meta);
            menu.setItem(48, prev);
        }
        
        if (currentPage < totalPages - 1) {
            ItemStack next = new ItemStack(Material.PAPER);
            ItemMeta meta = next.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "Next Page");
            next.setItemMeta(meta);
            menu.setItem(50, next);
        }
    }

    @EventHandler
    public void onTeamSelectionClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().startsWith(ChatColor.BLUE + "Select Team - Page ")) return;
        
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Handle page navigation
        if (clicked.getType() == Material.PAPER && clicked.hasItemMeta()) {
            String displayName = clicked.getItemMeta().getDisplayName();
            int currentPage = teamSelectionPages.getOrDefault(player, 0);
            
            if (displayName.equals(ChatColor.GREEN + "Previous Page")) {
                openTeamSelectionMenu(player, currentPage - 1);
            } else if (displayName.equals(ChatColor.GREEN + "Next Page")) {
                openTeamSelectionMenu(player, currentPage + 1);
            }
        }
        // Handle team selection
        else if (clicked.getType() == Material.BANNER && clicked.hasItemMeta()) {
            String teamName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            
            // Check if team exists (case-insensitive)
            Optional<String> matchingTeam = teamManager.getAllTeams().stream()
                .filter(t -> t.equalsIgnoreCase(teamName))
                .findFirst();
                
            if (matchingTeam.isPresent()) {
                String actualTeamName = matchingTeam.get();
                
                // Execute the team join command
                player.performCommand("team join " + actualTeamName);
                player.closeInventory();
            }
        }
    }
}