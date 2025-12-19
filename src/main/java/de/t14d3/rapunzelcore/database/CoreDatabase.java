package de.t14d3.rapunzelcore.database;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.entities.Home;
import de.t14d3.rapunzelcore.entities.Player;
import de.t14d3.rapunzelcore.entities.Warp;
import de.t14d3.spool.core.EntityManager;
import de.t14d3.spool.migration.MigrationManager;
import de.t14d3.spool.migration.SchemaIntrospector;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DriverManager;

public class CoreDatabase {
    private static EntityManager entityManager;

    public CoreDatabase(Main plugin) {
        String jdbc = plugin.getConfig().getString("database.jdbc", "jdbc:sqlite:plugins/RapunzelCore/rapunzelcore.db");
        Connection conn;
        try {
            conn = DriverManager.getConnection(jdbc);
        } catch (Exception e) {
            throw new RuntimeException("CoreDatabase initialization failed", e);
        }
        entityManager = EntityManager.create(conn);
        plugin.getLogger().info("Connected to database with dialect " + entityManager.getDialect());

        // Run DB migrations
        plugin.getLogger().info("Running DB migrations...");
        entityManager.registerEntities(Player.class, Home.class, Warp.class);
        MigrationManager migrationManager = entityManager.getMigrationManager();
        SchemaIntrospector introspector = entityManager.getIntrospector();
        try {
            migrationManager.getSchemaDiff();
            plugin.getLogger().info("Applying " + entityManager.updateSchema() + " migrations");
            plugin.getLogger().info("Schema valid: " + entityManager.validateSchema());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static EntityManager getEntityManager() {
        return entityManager;
    }

    public static void flushAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            synchronized (entityManager) {
                entityManager.flush();
            }
        });
    }
}
