package com.github.darksoulq.mt_multiblocks.core;

import java.io.Serial;
import java.io.Serializable;

public class SavedDevice implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String location;
    private final String className;
    private final String pluginName;
    private final byte[] data;

    public SavedDevice(String location, String className, String pluginName, byte[] data) {
        this.location = location;
        this.className = className;
        this.pluginName = pluginName;
        this.data = data;
    }

    public String getLocation() {
        return location;
    }

    public String getClassName() {
        return className;
    }

    public String getPluginName() {
        return pluginName;
    }

    public byte[] getData() {
        return data;
    }
}