package com.github.darksoulq.mt_multiblocks;

import com.github.darksoulq.mt_multiblocks.choice.BlockDataChoice;
import com.github.darksoulq.mt_multiblocks.model.MultiblockModel;
import com.github.darksoulq.mt_multiblocks.port.Port;
import com.github.darksoulq.mt_multiblocks.util.ValidationResult;
import org.bukkit.block.data.BlockData;
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
    private transient MultiblockModel model;

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

    public MultiblockBlueprint setModel(MultiblockModel model) {
        this.model = model;
        return this;
    }

    public MultiblockModel getModel() {
        return model;
    }

    public Map<Vector, StructureComponent> getComponents() {
        return components;
    }

    public Map<Vector, StructureComponent> buildStructure(BlueprintContext context) {
        return new HashMap<>(components);
    }

    public ValidationResult canPlace(BlueprintContext context) {
        if (placementConditions == null) return ValidationResult.success();
        for (PlacementCondition condition : placementConditions) {
            ValidationResult result = condition.canPlace(context);
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.success();
    }
}