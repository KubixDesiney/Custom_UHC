package utilities;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class CustomBossBar {
    private Player player;
    private String title;
    private float progress;
    private BarColor color;
    private BarStyle style;
    private UUID uuid;
    private boolean visible;

    public enum BarColor {
        PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
    }

    public enum BarStyle {
        PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20
    }

    public CustomBossBar(Player player, String title, BarColor color, BarStyle style) {
        this.player = player;
        this.title = title;
        this.progress = 1.0f;
        this.color = color;
        this.style = style;
        this.uuid = UUID.randomUUID();
        this.visible = false;
    }

    public void show() {
        if (visible) return;
        
        // Create add bossbar packet
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BOSS);
        packet.getUUIDs().write(0, uuid);
        packet.getIntegers().write(0, 0); // Action 0 = add
        packet.getChatComponents().write(0, WrappedChatComponent.fromText(title));
        packet.getFloat().write(0, progress);
        packet.getIntegers().write(1, color.ordinal());
        packet.getIntegers().write(2, style.ordinal());
        packet.getBytes().write(0, (byte) 0); // Flags
        
        sendPacket(packet);
        visible = true;
    }

    public void hide() {
        if (!visible) return;
        
        // Create remove bossbar packet
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BOSS);
        packet.getUUIDs().write(0, uuid);
        packet.getIntegers().write(0, 1); // Action 1 = remove
        
        sendPacket(packet);
        visible = false;
    }

    public void destroy() {
        hide();
    }

    public void setTitle(String title) {
        this.title = title;
        if (!visible) return;
        
        // Create update title packet
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BOSS);
        packet.getUUIDs().write(0, uuid);
        packet.getIntegers().write(0, 3); // Action 3 = update title
        packet.getChatComponents().write(0, WrappedChatComponent.fromText(title));
        
        sendPacket(packet);
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(1, progress));
        if (!visible) return;
        
        // Create update progress packet
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BOSS);
        packet.getUUIDs().write(0, uuid);
        packet.getIntegers().write(0, 2); // Action 2 = update progress
        packet.getFloat().write(0, this.progress);
        
        sendPacket(packet);
    }

    public void setColor(BarColor color) {
        this.color = color;
        updateStyle();
    }

    public void setStyle(BarStyle style) {
        this.style = style;
        updateStyle();
    }

    private void updateStyle() {
        if (!visible) return;
        
        // Create update style packet
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BOSS);
        packet.getUUIDs().write(0, uuid);
        packet.getIntegers().write(0, 4); // Action 4 = update style
        packet.getIntegers().write(1, color.ordinal());
        packet.getIntegers().write(2, style.ordinal());
        
        sendPacket(packet);
    }

    private void sendPacket(PacketContainer packet) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (InvocationTargetException e) {
            Bukkit.getLogger().warning("Failed to send boss bar packet to " + player.getName());
        }
    }
}