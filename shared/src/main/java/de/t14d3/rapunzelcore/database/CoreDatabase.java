package de.t14d3.rapunzelcore.database;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.database.entities.Channel;
import de.t14d3.rapunzelcore.database.entities.Home;
import de.t14d3.rapunzelcore.database.entities.Player;
import de.t14d3.rapunzelcore.database.entities.Warp;
import de.t14d3.spool.core.EntityManager;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

/**
 * Paper-specific database implementation that extends the shared CoreDatabase.
 * This class handles Paper/Bukkit-specific database operations and initialization.
 */
public class CoreDatabase{

    private static EntityManager entityManager;
    private static String connectionString;
    private static final ExecutorService FLUSH_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r, "RapunzelCore-DBFlush");
            t.setDaemon(true);
            return t;
        }
    });


    public CoreDatabase(String connectionString) {
        CoreDatabase.connectionString = connectionString;
        Connection conn;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            conn = DriverManager.getConnection(connectionString);
            entityManager = EntityManager.create(conn);

            // Run DB migrations
            RapunzelCore.getLogger().info("Running DB migrations...");
            entityManager.registerEntities(Player.class, Home.class, Warp.class, Channel.class);
            RapunzelCore.getLogger().info("Applying {} migrations", entityManager.updateSchema());
            RapunzelCore.getLogger().info("Schema valid: {}", entityManager.validateSchema());
        } catch (Exception e) {
            throw new RuntimeException("CoreDatabase initialization failed", e);
        }
    }


    public void close() {
        // Close database connection if needed
    }

    public static EntityManager getEntityManager() {
        return entityManager;
    }

    public static Connection openConnection() {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalStateException("Database connection string not initialized");
        }
        try {
            return DriverManager.getConnection(connectionString);
        } catch (Exception e) {
            throw new RuntimeException("Failed to open DB connection", e);
        }
    }

    public static void runLocked(Runnable runnable) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager not initialized");
        }
        synchronized (entityManager) {
            runnable.run();
        }
    }

    public static <T> T locked(Supplier<T> supplier) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager not initialized");
        }
        synchronized (entityManager) {
            return supplier.get();
        }
    }

    public static void flushAsync() {
        FLUSH_EXECUTOR.execute(() -> runLocked(() -> entityManager.flush()));
    }
}
