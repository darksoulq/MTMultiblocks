package com.github.darksoulq.mt_multiblocks;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.choice.BlockDataChoice;
import com.github.darksoulq.mt_multiblocks.port.Port;
import com.github.darksoulq.mt_multiblocks.util.ValidationResult;
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
        components.put(relativeLocation, new RequiredComponent(data, port));
        return this;
    }

    public MultiblockBlueprint addOptionalComponent(Vector relativeLocation, BlockData data, Port port) {
        components.put(relativeLocation, new OptionalComponent(new BlockDataChoice(data), port));
        return this;
    }

    public MultiblockBlueprint addOptionalComponent(Vector relativeLocation, List<BlockData> dataChoices, Port port) {
        components.put(relativeLocation, new OptionalComponent(new BlockDataChoice(dataChoices), port));
        return this;
    }

    public MultiblockBlueprint addComponentObj(Vector relativeLocation, StructureComponent component) {
        components.put(relativeLocation, component);
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

    public ValidationResult canPlace(Location coreLoc, BlockFace facing, Player player) {
        if (placementConditions == null) return ValidationResult.success();
        for (PlacementCondition condition : placementConditions) {
            ValidationResult result = condition.canPlace(coreLoc, facing, player);
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.success();
    }
}