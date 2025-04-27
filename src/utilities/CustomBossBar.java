package utilities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomBossBar {
    private static final Map<UUID, CustomBossBar> playerBars = new HashMap<>();
    
    private final UUID playerId;
    private final UUID barId;
    private String title;
    private float progress;
    private BarColor color;
    private BarStyle style;
    private boolean visible;
    
    private final ProtocolManager protocolManager;
    
    public enum BarColor {
        PINK(0), BLUE(1), RED(2), GREEN(3), YELLOW(4), PURPLE(5), WHITE(6);
        
        private final int id;
        
        BarColor(int id) {
            this.id = id;
        }
        
        public int getId() {
            return id;
        }
    }
    
    public enum BarStyle {
        PROGRESS(0), NOTCHED_6(1), NOTCHED_10(2), NOTCHED_12(3), NOTCHED_20(4);
        
        private final int id;
        
        BarStyle(int id) {
            this.id = id;
        }
        
        public int getId() {
            return id;
        }
    }
    
    public CustomBossBar(Player player, String title, BarColor color, BarStyle style) {
        this.playerId = player.getUniqueId();
        this.barId = UUID.randomUUID();
        this.title = title;
        this.progress = 1.0f;
        this.color = color;
        this.style = style;
        this.visible = false;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        
        playerBars.put(playerId, this);
    }
    
    public void setTitle(String title) {
        this.title = title;
        update();
    }
    
    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(1, progress));
        update();
    }
    
    public void setColor(BarColor color) {
        this.color = color;
        update();
    }
    
    public void setStyle(BarStyle style) {
        this.style = style;
        update();
    }
    
    public void show() {
        this.visible = true;
        sendCreatePacket();
    }
    
    public void hide() {
        this.visible = false;
        sendRemovePacket();
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void destroy() {
        hide();
        playerBars.remove(playerId);
    }
    
    private void update() {
        if (visible) {
            sendUpdatePacket();
        }
    }
    
    private void sendCreatePacket() {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;
        
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BOSS);
        
        // 1.12.2 Packet Structure:
        // - UUID (unique bar ID)
        // - int (action: 0=ADD, 1=REMOVE, 2=UPDATE_PCT, 3=UPDATE_NAME, 4=UPDATE_STYLE, 5=UPDATE_PROPERTIES)
        // - IChatBaseComponent (title)
        // - float (progress)
        // - int (color)
        // - int (style)
        // - boolean (darken sky)
        // - boolean (play end music)
        // - boolean (create fog)
        
        packet.getUUIDs().write(0, barId);
        packet.getIntegers().write(0, 0); // ADD action
        
        packet.getChatComponents().write(0, WrappedChatComponent.fromText(title));
        packet.getFloat().write(0, progress);
        
        // For 1.12.2, we need to use specific field indexes
        packet.getIntegers().write(1, color.getId()); // Color
        packet.getIntegers().write(2, style.getId()); // Style
        
        // Flags
        packet.getBooleans().write(0, false); // darken sky
        packet.getBooleans().write(1, false); // play end music
        packet.getBooleans().write(2, false); // create fog
        
        sendPacket(player, packet);
    }
    
    private void sendUpdatePacket() {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;
        
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BOSS);
        
        packet.getUUIDs().write(0, barId);
        packet.getIntegers().write(0, 2); // UPDATE_PCT action
        
        packet.getFloat().write(0, progress);
        
        // Also update title in case it changed
        packet.getChatComponents().write(0, WrappedChatComponent.fromText(title));
        
        sendPacket(player, packet);
    }
    
    private void sendRemovePacket() {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;
        
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BOSS);
        packet.getUUIDs().write(0, barId);
        packet.getIntegers().write(0, 1); // REMOVE action
        
        sendPacket(player, packet);
    }
    
    
    private void sendPacket(Player player, PacketContainer packet) {
        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (InvocationTargetException e) {
            Bukkit.getLogger().warning("Failed to send boss bar packet to " + player.getName() + ": " + e.getMessage());
        }
    }
    
    public static CustomBossBar getBossBar(Player player) {
        return playerBars.get(player.getUniqueId());
    }
    
    public static void cleanup() {
        playerBars.values().forEach(CustomBossBar::destroy);
        playerBars.clear();
    }
}
