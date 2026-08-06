package com.github.darksoulq.mt_multiblocks.util;

import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.StructureComponent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatternBuilder {
    private final Map<Character, StructureComponent> mapping = new HashMap<>();
    private final List<String[]> layers = new ArrayList<>();
    private char coreChar = 'C';

    public PatternBuilder setCoreChar(char c) {
        this.coreChar = c;
        return this;
    }

    public PatternBuilder addMapping(char c, StructureComponent component) {
        this.mapping.put(c, component);
        return this;
    }

    public PatternBuilder addLayer(String... rows) {
        layers.add(rows);
        return this;
    }

    public MultiblockBlueprint build() {
        MultiblockBlueprint blueprint = new MultiblockBlueprint();
        
        int coreX = -1, coreY = -1, coreZ = -1;
        
        for (int y = 0; y < layers.size(); y++) {
            String[] rows = layers.get(y);
            for (int z = 0; z < rows.length; z++) {
                String row = rows[z];
                for (int x = 0; x < row.length(); x++) {
                    if (row.charAt(x) == coreChar) {
                        coreX = x; coreY = y; coreZ = z;
                        break;
                    }
                }
            }
        }

        if (coreX == -1) throw new IllegalStateException("Core character not found in pattern");

        for (int y = 0; y < layers.size(); y++) {
            String[] rows = layers.get(y);
            for (int z = 0; z < rows.length; z++) {
                String row = rows[z];
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    if (c == ' ' || c == coreChar) continue;
                    StructureComponent comp = mapping.get(c);
                    if (comp != null) {
                        Vector rel = new Vector(x - coreX, y - coreY, z - coreZ);
                        blueprint.addComponentObj(rel, comp);
                    }
                }
            }
        }
        return blueprint;
    }
}