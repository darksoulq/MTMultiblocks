package com.github.darksoulq.mt_multiblocks.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiblockModel implements Serializable {
    private final List<ModelPart> parts = new ArrayList<>();
    private final Map<String, ModelAnimation> animations = new HashMap<>();

    public MultiblockModel addPart(ModelPart part) {
        parts.add(part);
        return this;
    }

    public MultiblockModel addAnimation(ModelAnimation animation) {
        animations.put(animation.getName(), animation);
        return this;
    }

    public List<ModelPart> getParts() {
        return parts;
    }

    public ModelAnimation getAnimation(String name) {
        return animations.get(name);
    }
}