package com.github.darksoulq.mt_multiblocks;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.port.Port;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiblockBlueprint implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<Vector, StructureComponent> components = new HashMap<>();
    private transient List<PlacementCondition> placementConditions;

    public MultiblockBlueprint addComponent(Vector relativeLocation, BlockData data, Port port) {
        components.put(relativeLocation, new StructureComponent(data, port, true));
        return this;
    }

    public MultiblockBlueprint addOptionalComponent(Vector relativeLocation, BlockData data, Port port) {
        components.put(relativeLocation, new StructureComponent(data, port, false));
        return this;
    }

    public MultiblockBlueprint addPlacementCondition(PlacementCondition condition) {
        if (placementConditions == null) {
            placementConditions = new ArrayList<>();
        }
        placementConditions.add(condition);
        return this;
    }

    public Map<Vector, StructureComponent> getComponents() {
        return components;
    }

    public Map<Vector, StructureComponent> buildStructure(Location coreLoc, BlockFace facing, Device coreDevice, ItemStack coreItem) {
        return new HashMap<>(components);
    }

    public boolean canPlace(Location coreLoc, BlockFace facing, Player player) {
        if (placementConditions == null) return true;
        for (PlacementCondition condition : placementConditions) {
            if (!condition.canPlace(coreLoc, facing, player)) {
                return false;
            }
        }
        return true;
    }
}