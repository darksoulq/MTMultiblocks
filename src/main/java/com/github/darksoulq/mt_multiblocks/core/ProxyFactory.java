package com.github.darksoulq.mt_multiblocks.core;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.StructureComponent;
import org.bukkit.Location;

public interface ProxyFactory {
    Device create(Location location, Device coreDevice, StructureComponent component);
}