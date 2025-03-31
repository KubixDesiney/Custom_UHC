package utilities;
import org.bukkit.ChatColor;

public class ColorUtil {
    public static ChatColor getChatColorFromConfig(String colorCode) {
        if (colorCode.startsWith("&")) {
            // Replace the & with §
            colorCode = "§" + colorCode.substring(1);
        }
        return ChatColor.valueOf(colorCode);
    }
}