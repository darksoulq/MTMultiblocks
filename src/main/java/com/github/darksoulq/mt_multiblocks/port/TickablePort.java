package com.github.darksoulq.mt_multiblocks.port;

import org.bukkit.Location;

public interface TickablePort extends Port {
    void onTick(Location portLocation);
}