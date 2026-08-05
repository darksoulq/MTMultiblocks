package com.github.darksoulq.mt_multiblocks.port;

import org.bukkit.configuration.ConfigurationSection;

public interface PortFactory {
    Port create(ConfigurationSection config);
}