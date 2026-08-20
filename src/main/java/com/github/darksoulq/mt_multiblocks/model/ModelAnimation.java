package com.github.darksoulq.mt_multiblocks.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModelAnimation implements Serializable {
    private final String name;
    private final Map<String, List<Keyframe>> timelines = new HashMap<>();

    public ModelAnimation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ModelAnimation addKeyframe(String partId, Keyframe kf) {
        timelines.computeIfAbsent(partId, k -> new ArrayList<>()).add(kf);
        return this;
    }

    public List<Keyframe> getTimeline(String partId) {
        return timelines.get(partId);
    }

    public Set<String> getPartIds() {
        return timelines.keySet();
    }
}