package com.github.darksoulq.mt_multiblocks.preview;

import com.MT.xxxtrigger50xxx.MineItems;
import com.MT.xxxtrigger50xxx.MineMain;
import com.github.darksoulq.mt_multiblocks.ExternalMultiblocks;
import com.github.darksoulq.mt_multiblocks.MTMultiblock;
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.StructureComponent;
import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MultiblockPreviewManager {
    public static final Map<String, MTMultiblock> multiblockDummies = new HashMap<>();

    private static final Map<UUID, Display[]> activePreviews = new HashMap<>();
    private static final Map<UUID, Location> lastPreviewLocations = new HashMap<>();
    private static final Map<UUID, BlockFace> lastPreviewFacings = new HashMap<>();
    private static final Map<UUID, ItemStack> lastPreviewItems = new HashMap<>();
    private static int taskId = -1;

    public static void start() {
        if (taskId != -1) return;

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MineMain.getPlugin(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                handlePlayerPreview(p);
            }
        }, 0L, 2L);
    }

    public static void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
            for (UUID uuid : activePreviews.keySet().toArray(new UUID[0])) {
                clearPreview(uuid);
            }
        }
    }

    private static void handlePlayerPreview(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();

        if (hand.getType().isAir() || !hand.hasItemMeta()) {
            clearPreview(p.getUniqueId());
            return;
        }

        String deviceName;
        try {
            deviceName = MineItems.getMTItemKey(hand);
        } catch (Exception e) {
            clearPreview(p.getUniqueId());
            return;
        }

        MultiblockBlueprint blueprint = getTargetBlueprint(deviceName);

        if (blueprint == null) {
            clearPreview(p.getUniqueId());
            return;
        }

        RayTraceResult ray = p.rayTraceBlocks(5.0);
        if (ray == null || ray.getHitBlock() == null || ray.getHitBlockFace() == null) {
            clearPreview(p.getUniqueId());
            return;
        }

        Location coreLoc = ray.getHitBlock().getLocation().add(ray.getHitBlockFace().getDirection());

        if (!coreLoc.getWorld().isChunkLoaded(coreLoc.getBlockX() >> 4, coreLoc.getBlockZ() >> 4)) {
            clearPreview(p.getUniqueId());
            return;
        }

        BlockFace facing = getPlacementFacingHorizontal(p).getOppositeFace();

        if (lastPreviewLocations.containsKey(p.getUniqueId())) {
            Location lastLoc = lastPreviewLocations.get(p.getUniqueId());
            BlockFace lastFace = lastPreviewFacings.get(p.getUniqueId());
            ItemStack lastItem = lastPreviewItems.get(p.getUniqueId());

            if (lastLoc.equals(coreLoc) && lastFace == facing && isSameHandItem(hand, lastItem)) {
                updateGlowColors(p.getUniqueId(), blueprint, coreLoc, facing, hand);
                return;
            }
        }

        buildPreview(p.getUniqueId(), blueprint, hand, coreLoc, facing);
        lastPreviewLocations.put(p.getUniqueId(), coreLoc);
        lastPreviewFacings.put(p.getUniqueId(), facing);
        lastPreviewItems.put(p.getUniqueId(), hand.clone());
    }

    private static boolean isSameHandItem(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.getType() == b.getType() && a.hasItemMeta() == b.hasItemMeta() &&
            (!a.hasItemMeta() || Bukkit.getItemFactory().equals(a.getItemMeta(), b.getItemMeta()));
    }

    private static MultiblockBlueprint getTargetBlueprint(String deviceName) {
        if (deviceName == null) return null;
        if (multiblockDummies.containsKey(deviceName)) {
            return multiblockDummies.get(deviceName).getBlueprint();
        }
        if (ExternalMultiblocks.hasBlueprint(deviceName)) {
            return ExternalMultiblocks.getBlueprint(deviceName);
        }
        return null;
    }

    private static void buildPreview(UUID uuid, MultiblockBlueprint blueprint, ItemStack hand, Location coreLoc, BlockFace facing) {
        clearPreview(uuid);
        Map<Vector, StructureComponent> structure = blueprint.buildStructure(coreLoc, facing, null, hand);
        Display[] displays = new Display[structure.size() + 1];
        boolean isValid = isValidPlacement(structure, coreLoc, facing);
        Color coreGlowColor = isValid ? Color.LIME : Color.RED;

        Display coreDisplay;
        if (hand.getType() == Material.PLAYER_HEAD) {
            Location headLoc = coreLoc.clone().add(0.5, 0.5, 0.5);
            headLoc.setYaw(RotationUtil.getYawFromFace(facing));
            ItemDisplay id = (ItemDisplay) coreLoc.getWorld().spawnEntity(headLoc, EntityType.ITEM_DISPLAY);
            id.setItemStack(hand);
            id.setItemDisplayTransform(ItemDisplayTransform.HEAD);
            coreDisplay = id;
        } else {
            BlockData coreData = hand.getType().createBlockData();
            if (coreData instanceof Directional dir) {
                dir.setFacing(facing);
            } else if (coreData instanceof Rotatable rot) {
                rot.setRotation(facing);
            }
            BlockDisplay bd = (BlockDisplay) coreLoc.getWorld().spawnEntity(coreLoc, EntityType.BLOCK_DISPLAY);
            bd.setBlock(coreData);
            coreDisplay = bd;
        }

        coreDisplay.setGlowing(true);
        coreDisplay.setGlowColorOverride(coreGlowColor);
        displays[0] = coreDisplay;

        int index = 1;
        for (Map.Entry<Vector, StructureComponent> entry : structure.entrySet()) {
            Vector rotated = RotationUtil.rotateVector(entry.getKey(), facing);
            Location target = coreLoc.clone().add(rotated);

            if (!target.getWorld().isChunkLoaded(target.getBlockX() >> 4, target.getBlockZ() >> 4)) {
                continue;
            }

            StructureComponent component = entry.getValue();
            BlockDisplay display = (BlockDisplay) target.getWorld().spawnEntity(target, EntityType.BLOCK_DISPLAY);
            display.setBlock(component.getRotatedData(facing));
            display.setGlowing(true);

            Color componentColor = component.isRequired() ? coreGlowColor : Color.AQUA;
            display.setGlowColorOverride(componentColor);
            displays[index++] = display;
        }

        activePreviews.put(uuid, displays);
    }

    private static void updateGlowColors(UUID uuid, MultiblockBlueprint blueprint, Location coreLoc, BlockFace facing, ItemStack hand) {
        Display[] displays = activePreviews.get(uuid);
        if (displays == null) return;

        Map<Vector, StructureComponent> structure = blueprint.buildStructure(coreLoc, facing, null, hand);
        boolean isValid = isValidPlacement(structure, coreLoc, facing);
        Color baseGlowColor = isValid ? Color.LIME : Color.RED;

        if (displays[0] != null && displays[0].isValid() && (displays[0].getGlowColorOverride() == null || !displays[0].getGlowColorOverride().equals(baseGlowColor))) {
            displays[0].setGlowColorOverride(baseGlowColor);
        }

        int index = 1;
        for (StructureComponent component : structure.values()) {
            if (index < displays.length) {
                Display display = displays[index];
                Color expectedColor = component.isRequired() ? baseGlowColor : Color.AQUA;
                if (display != null && display.isValid() && (display.getGlowColorOverride() == null || !display.getGlowColorOverride().equals(expectedColor))) {
                    display.setGlowColorOverride(expectedColor);
                }
                index++;
            }
        }
    }

    public static void clearPreview(UUID uuid) {
        Display[] displays = activePreviews.remove(uuid);
        if (displays != null) {
            for (Display d : displays) {
                if (d != null && d.isValid()) d.remove();
            }
        }
        lastPreviewLocations.remove(uuid);
        lastPreviewFacings.remove(uuid);
        lastPreviewItems.remove(uuid);
    }

    private static boolean isValidPlacement(Map<Vector, StructureComponent> structure, Location core, BlockFace facing) {
        for (Map.Entry<Vector, StructureComponent> entry : structure.entrySet()) {
            if (!entry.getValue().isRequired()) continue;
            Vector rotated = RotationUtil.rotateVector(entry.getKey(), facing);
            Location target = core.clone().add(rotated);

            if (!target.getWorld().isChunkLoaded(target.getBlockX() >> 4, target.getBlockZ() >> 4)) {
                return false;
            }

            if (!target.getBlock().getType().isAir() && !target.getBlock().isReplaceable()) {
                return false;
            }
        }
        return true;
    }

    private static BlockFace getPlacementFacingHorizontal(Player player) {
        Vector dir = player.getLocation().getDirection().normalize();
        if (Math.abs(dir.getX()) > Math.abs(dir.getZ())) {
            return dir.getX() > 0.0 ? BlockFace.EAST : BlockFace.WEST;
        } else {
            return dir.getZ() > 0.0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }
}