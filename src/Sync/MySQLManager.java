package Sync;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.bukkit.plugin.java.JavaPlugin;

public class MySQLManager {
    private final JavaPlugin plugin;
    private Connection connection;

    public MySQLManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupConfig();
        connect();
    }

    private void setupConfig() {
        plugin.getConfig().addDefault("mysql.host", "localhost");
        plugin.getConfig().addDefault("mysql.port", 3306);
        plugin.getConfig().addDefault("mysql.database", "uhc_network");
        plugin.getConfig().addDefault("mysql.user", "root");
        plugin.getConfig().addDefault("mysql.password", "password");
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    private void connect() {
        try {
            String host = plugin.getConfig().getString("mysql.host");
            int port = plugin.getConfig().getInt("mysql.port");
            String database = plugin.getConfig().getString("mysql.database");
            String user = plugin.getConfig().getString("mysql.user");
            String password = plugin.getConfig().getString("mysql.password");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");  // Use MariaDB driver if you're using that instead
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("MySQL Driver not found!");
                e.printStackTrace();
            }
            connection = DriverManager.getConnection(url, user, password);
            plugin.getLogger().info("MySQL connected.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to MySQL:");
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
