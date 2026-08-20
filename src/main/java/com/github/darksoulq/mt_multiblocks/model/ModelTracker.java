package com.github.darksoulq.mt_multiblocks.model;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.MT.xxxtrigger50xxx.MineMain;
import com.github.darksoulq.mt_multiblocks.BlueprintContext;
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.util.MultiblockUtil;
import com.github.darksoulq.mt_multiblocks.util.Octree;
import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModelTracker {
    private static final Map<Location, ActiveModel> models = new ConcurrentHashMap<>();
    private static final Map<World, Octree<ActiveModel>> worldOctrees = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Location, VisibilityState>> playerTracking = new ConcurrentHashMap<>();

    private static int taskId = -1;
    private static int ticks = 0;

    public static void start() {
        if (taskId != -1) return;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MineMain.getPlugin(), () -> {
            ticks++;
            if (ticks % 20 == 0) tickLifecycle();
            tickAnimations();
            if (ticks % 4 == 0) tickTracking();
        }, 0, 1);
    }

    public static void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
            for (Location loc : new HashSet<>(models.keySet())) {
                unregister(loc);
            }
            playerTracking.clear();
            worldOctrees.clear();
        }
    }

    public static ActiveModel getModel(Device device) {
        return device != null ? models.get(device.getLocationREADONLY()) : null;
    }

    private static void tickLifecycle() {
        Set<Location> validLocations = new HashSet<>();

        for (Device device : Device.getDevicesREADONLY()) {
            Location loc = device.getLocationREADONLY();
            validLocations.add(loc);

            if (!models.containsKey(loc)) {
                BlueprintContext ctx = new BlueprintContext(device);
                MultiblockBlueprint bp = MultiblockUtil.getTargetBlueprint(device, ctx);

                if (bp != null && bp.getModel() != null) {
                    ActiveModel am = new ActiveModel(loc, RotationUtil.getFacingSafely(device), bp.getModel());
                    models.put(loc, am);

                    Octree<ActiveModel> octree = worldOctrees.computeIfAbsent(loc.getWorld(), w ->
                        new Octree<>(BoundingBox.of(new Vector(-30000000, -64, -30000000), new Vector(30000000, 320, 30000000)), 0, ActiveModel::getBoundingBox)
                    );
                    octree.insert(am);
                }
            }
        }

        for (Location loc : new HashSet<>(models.keySet())) {
            if (!validLocations.contains(loc)) {
                unregister(loc);
            }
        }
    }

    private static void tickAnimations() {
        for (ActiveModel model : models.values()) {
            model.getAnimationController().tick();
        }
    }

    private static void tickTracking() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            Map<Location, VisibilityState> tracking = playerTracking.computeIfAbsent(uuid, k -> new HashMap<>());
            Map<Location, VisibilityState> nowTracking = new HashMap<>();

            Octree<ActiveModel> octree = worldOctrees.get(p.getWorld());
            if (octree != null) {
                BoundingBox queryBox = BoundingBox.of(p.getLocation(), 45, 45, 45);
                for (ActiveModel model : octree.query(queryBox)) {
                    VisibilityState state = VisibilityCuller.getVisibility(p, model);
                    nowTracking.put(model.getCoreLocation(), state);

                    VisibilityState oldState = tracking.getOrDefault(model.getCoreLocation(), VisibilityState.HIDDEN);

                    if (state == VisibilityState.VISIBLE && oldState != VisibilityState.VISIBLE) {
                        model.spawnFor(p);
                    } else if (state != VisibilityState.VISIBLE && oldState == VisibilityState.VISIBLE) {
                        model.destroyFor(p);
                    }
                }
            }

            for (Map.Entry<Location, VisibilityState> entry : tracking.entrySet()) {
                if (!nowTracking.containsKey(entry.getKey())) {
                    if (entry.getValue() == VisibilityState.VISIBLE) {
                        ActiveModel model = models.get(entry.getKey());
                        if (model != null) {
                            model.destroyFor(p);
                        }
                    }
                }
            }
            playerTracking.put(uuid, nowTracking);
        }

        playerTracking.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    private static void unregister(Location loc) {
        ActiveModel model = models.remove(loc);
        if (model != null) {
            model.destroyAll();
            Octree<ActiveModel> octree = worldOctrees.get(loc.getWorld());
            if (octree != null) {
                octree.remove(model);
            }
        }
    }
}