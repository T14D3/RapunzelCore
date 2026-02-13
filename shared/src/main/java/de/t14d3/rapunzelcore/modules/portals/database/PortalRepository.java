package de.t14d3.rapunzelcore.modules.portals.database;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.PortalEntity;
import de.t14d3.rapunzelcore.database.sync.DbEntitySync;
import de.t14d3.rapunzelcore.modules.portals.Portal;
import de.t14d3.spool.cache.CacheEvent;
import de.t14d3.spool.repository.EntityRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Repository for managing Portal entities with caching and synchronization support.
 *
 *
 * This repository provides a singleton instance for managing portal data with the following features:
 *
 *
 * - In-memory caching for fast portal lookups by ID and name
 *
 * - Automatic synchronization across servers via database events
 *
 * - Change listeners for reactive updates
 *
 * - Thread-safe operations using concurrent collections
 *
 *
 * Usage example:
 *
 * <pre>
 * PortalRepository repository = PortalRepository.getInstance();
 *
 * // Get portal by ID
 * Portal portal = repository.getPortal(uuid);
 *
 * // Get portal by name
 * Portal portal = repository.getPortalByName("spawn");
 *
 * // Save a portal
 * repository.save(new PortalEntity(portal));
 *
 * // Delete a portal
 * repository.deleteById(uuid);
 *
 * // Register for changes
 * repository.registerChangeListener(() -> {
 *     // Handle portal changes
 * });
 * </pre>
 *
 * @see Portal
 * @see PortalEntity
 */
public class PortalRepository extends EntityRepository<PortalEntity> {

/**
 * Listener interface for portal change notifications.
 */
 public interface ChangeListener {
/**
 * Called when a portal is created, updated, or deleted.
 */
 void onPortalChanged();
 }

 private static final PortalRepository instance = new PortalRepository();
 private final Map<UUID, PortalEntity> portalsById = new ConcurrentHashMap<>();
 private final Map<String, PortalEntity> portalsByName = new ConcurrentHashMap<>();
 private final List<ChangeListener> listeners = new CopyOnWriteArrayList<>();
 private volatile boolean syncRegistered;

/**
 * Private constructor for singleton pattern.
 */
 private PortalRepository() {
 super(CoreDatabase.getEntityManager(), PortalEntity.class);
 registerSyncListenerIfAvailable();
 }

/**
 * Gets the singleton instance of the repository.
 *
 * @return the repository instance
 */
 @NotNull
 public static PortalRepository getInstance() {
 return instance;
 }

/**
 * Registers the database sync listener if available.
 * This enables automatic cache updates when portals change on other servers.
 */
 private void registerSyncListenerIfAvailable() {
 if (syncRegistered) return;
 DbEntitySync sync = CoreDatabase.entitySync();
 if (sync == null) return;
 synchronized (this) {
 if (syncRegistered) return;
 sync.register(this::onCacheEvent);
 syncRegistered = true;
 }
 }

/**
 * Handles cache events from the database synchronization system.
 * Updates the local cache when portals are modified on other servers.
 *
 * @param event the cache event
 * @param sourceServer the server that originated the change
 */
 private void onCacheEvent(CacheEvent event, String sourceServer) {
 if (event == null || event.key() == null) return;
 if (!PortalEntity.class.getName().equals(event.key().entityClassName())) return;
 
 CacheEvent.Operation operation = event.operation();
 String id = event.key().id();
 if (operation == null || id == null) return;

 UUID uuid = UUID.fromString(id);
 CoreDatabase.runLockedAsync(() -> {
 PortalEntity cached = portalsById.get(uuid);
 if (cached != null) {
 if (operation == CacheEvent.Operation.DELETE) {
 portalsById.remove(uuid, cached);
 portalsByName.remove(cached.getName().toLowerCase(), cached);
 CoreDatabase.getEntityManager().detach(cached);
 } else {
 CoreDatabase.getEntityManager().refresh(cached);
 }
 }
 notifyListeners();
 });
 }

/**
 * Adds a portal entity to the cache.
 *
 * @param entity the entity to cache
 */
 private void cache(@Nullable PortalEntity entity) {
 if (entity == null || entity.getId() == null) return;
 portalsById.put(entity.getId(), entity);
 if (entity.getName() != null) {
 portalsByName.put(entity.getName().toLowerCase(), entity);
 }
 }

/**
 * Removes a portal from the cache by ID.
 *
 * @param id the portal ID
 */
 private void removeFromCache(@Nullable UUID id) {
 if (id == null) return;
 PortalEntity removed = portalsById.remove(id);
 if (removed != null && removed.getName() != null) {
 portalsByName.remove(removed.getName().toLowerCase(), removed);
 }
 }

/**
 * Notifies all registered change listeners.
 */
 private void notifyListeners() {
 for (ChangeListener listener : listeners) {
 try {
 listener.onPortalChanged();
 } catch (Exception ignored) {
 // Listener exceptions should not break the notification chain
 }
 }
 }

/**
 * Registers a change listener to receive portal change notifications.
 *
 * @param listener the listener to register
 */
 public void registerChangeListener(@Nullable ChangeListener listener) {
 if (listener != null) {
 listeners.add(listener);
 }
 }

/**
 * Unregisters a change listener.
 *
 * @param listener the listener to unregister
 */
 public void unregisterChangeListener(@Nullable ChangeListener listener) {
 if (listener != null) {
 listeners.remove(listener);
 }
 }

/**
 * Gets a thread-safe snapshot of all portals by ID.
 *
 * @return an unmodifiable map of portal ID to entity
 */
 @NotNull
 public Map<UUID, PortalEntity> snapshotById() {
 return Map.copyOf(portalsById);
 }

/**
 * Gets a thread-safe snapshot of all portals by name.
 *
 * @return an unmodifiable map of lowercase portal name to entity
 */
 @NotNull
 public Map<String, PortalEntity> snapshotByName() {
 return Map.copyOf(portalsByName);
 }

/**
 * Gets a portal by its ID.
 * Returns from cache if available, otherwise loads from database.
 *
 * @param id the portal ID
 * @return the portal, or null if not found
 */
 @Nullable
 public Portal getPortal(@Nullable UUID id) {
 if (id == null) return null;
 instance.registerSyncListenerIfAvailable();
 
 PortalEntity cached = instance.portalsById.get(id);
 if (cached != null) return cached.toPortal();

 return CoreDatabase.locked(() -> instance.portalsById.computeIfAbsent(
 id,
 k -> instance.findById(id)
 )).toPortal();
 }

/**
 * Gets a portal by its name (case-insensitive).
 * Returns from cache if available, otherwise loads from database.
 *
 * @param name the portal name
 * @return the portal, or null if not found
 */
 @Nullable
 public Portal getPortalByName(@Nullable String name) {
 if (name == null || name.isBlank()) return null;
 instance.registerSyncListenerIfAvailable();
 
 String key = name.toLowerCase();
 PortalEntity cached = instance.portalsByName.get(key);
 if (cached != null) return cached.toPortal();

 return CoreDatabase.locked(() -> instance.portalsByName.computeIfAbsent(
 key,
 k -> instance.findOneBy("name", name.trim())
 )).toPortal();
 }

/**
 * Saves a portal entity to the database and cache.
 *
 * @param entity the entity to save
 * @return the saved entity
 */
 @Override
 @Nullable
 public PortalEntity save(@Nullable PortalEntity entity) {
 if (entity == null) return null;
 registerSyncListenerIfAvailable();
 
 super.save(entity);
 cache(entity);
 notifyListeners();
 return entity;
 }

/**
 * Saves a portal entity with optional immediate flush to database.
 *
 * @param entity the entity to save
 * @param flush whether to flush immediately
 * @return the saved entity
 */
 @Nullable
 public PortalEntity save(@Nullable PortalEntity entity, boolean flush) {
 if (entity == null) return null;
 registerSyncListenerIfAvailable();
 
 if (flush) {
 CoreDatabase.locked(() -> {
 super.save(entity);
 CoreDatabase.getEntityManager().flush();
 return entity;
 });
 } else {
 super.save(entity);
 }
 
 cache(entity);
 notifyListeners();
 return entity;
 }

/**
 * Deletes a portal entity from the database and cache.
 *
 * @param entity the entity to delete
 */
 @Override
 public void delete(@Nullable PortalEntity entity) {
 if (entity == null) return;
 registerSyncListenerIfAvailable();
 
 super.delete(entity);
 removeFromCache(entity.getId());
 notifyListeners();
 }

/**
 * Deletes a portal by its ID.
 *
 * @param id the portal ID to delete
 */
 public void deleteById(@Nullable UUID id) {
 if (id == null) return;
 PortalEntity entity = findById(id);
 if (entity != null) {
 delete(entity);
 }
 }
}
