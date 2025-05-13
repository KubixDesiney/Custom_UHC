package Sync;


import Rules.gameconfig;
import gamemodes.Gamestatus;
import gamemodes.gamemode;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.List;

public class UHCStatusSQLSync {

    private final JavaPlugin plugin;
    private Connection connection;
    private final String table;
    private final String serverId;
    private final int interval;

    public UHCStatusSQLSync(JavaPlugin plugin) {
        this.plugin = plugin;

        ConfigurationSection sql = plugin.getConfig().getConfigurationSection("mysql");
        this.table = sql.getString("table", "uhc_servers");
        this.serverId = sql.getString("server_id", "uhc");
        this.interval = sql.getInt("sync_interval_seconds", 5);

        connectToDatabase(sql);
        createTableIfMissing();
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateDatabase();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, interval * 20L);
    }

    private void connectToDatabase(ConfigurationSection sql) {
        try {
            String host = sql.getString("host");
            int port = sql.getInt("port");
            String db = sql.getString("database");
            String user = sql.getString("username");
            String pass = sql.getString("password");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false";
            connection = DriverManager.getConnection(url, user, pass);
            Bukkit.getLogger().info("[UHCStatusSQLSync] Connected to MySQL.");
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[UHCStatusSQLSync] Failed to connect to MySQL: " + e.getMessage());
        }
    }

    private void createTableIfMissing() {
        String sql = "CREATE TABLE IF NOT EXISTS " + table + " (" +
                "id VARCHAR(50) PRIMARY KEY," +
                "hostname VARCHAR(50)," +
                "gamemode VARCHAR(50)," +
                "host VARCHAR(50)," +
                "players INT," +
                "max_players INT," +
                "teamsize VARCHAR(10)," +
                "meetup INT," +
                "pvp INT," +
                "scenarios TEXT," +
                "status INT," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[UHCStatusSQLSync] Failed to create table: " + e.getMessage());
        }
    }

    private void updateDatabase() {
        try {
            if (gameconfig.getInstance() == null) return;
            gameconfig gc = gameconfig.getInstance();

            String hostname = gc.getGameName();
            int gamemodeName = gamemode.getMode();
            String host = Bukkit.getOnlinePlayers().stream().filter(p -> p.isOp()).findFirst().map(p -> p.getName()).orElse("Unknown");
            int players = Bukkit.getOnlinePlayers().size();
            int maxPlayers = Bukkit.getMaxPlayers();
            String teamSize = gc.getTeamSize() == 1 ? "FFA" : "To" + gc.getTeamSize();
            int meetup = gc.getMeetupTime() / 60;
            int pvp = gc.getPvPTime() / 60;
            int status = Gamestatus.getStatus();
            List<String> scenarios = gc.getEnabledScenarioNames();
            String scenariosString = String.join(",", scenarios);

            PreparedStatement ps = connection.prepareStatement(
                "REPLACE INTO " + table + " (id, hostname, gamemode, host, players, max_players, teamsize, meetup, pvp, scenarios, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );
            ps.setString(1, serverId);
            ps.setString(2, hostname);
            ps.setInt(3, gamemodeName);
            ps.setString(4, host);
            ps.setInt(5, players);
            ps.setInt(6, maxPlayers);
            ps.setString(7, teamSize);
            ps.setInt(8, meetup);
            ps.setInt(9, pvp);
            ps.setString(10, scenariosString);
            ps.setInt(11, status);
            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            Bukkit.getLogger().warning("[UHCStatusSQLSync] MySQL update failed: " + e.getMessage());
        }
    }
}

