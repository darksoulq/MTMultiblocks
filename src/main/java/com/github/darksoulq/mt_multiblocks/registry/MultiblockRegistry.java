package com.github.darksoulq.mt_multiblocks.registry;

import com.MT.triggersUtility.TUMaths;
import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.StructureComponent;
import com.github.darksoulq.mt_multiblocks.core.PortProxyDevice;
import com.github.darksoulq.mt_multiblocks.core.ProxyFactory;
import com.github.darksoulq.mt_multiblocks.port.Port;
import org.bukkit.Location;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiblockRegistry {
    public static MultiblockRegistry instance;

    public Map<String, String> secondaryToCoreMap;
    public Map<String, Port> secondaryToPortMap;
    public Map<Class<? extends Port>, ProxyFactory> proxyFactories;

    public MultiblockRegistry() {
        instance = this;
        this.secondaryToCoreMap = new ConcurrentHashMap<>();
        this.secondaryToPortMap = new ConcurrentHashMap<>();
        this.proxyFactories = new ConcurrentHashMap<>();
    }

    public void registerProxyFactory(Class<? extends Port> portClass, ProxyFactory factory) {
        proxyFactories.put(portClass, factory);
    }

    public void registerSecondaryBlock(Location secondaryLoc, Device coreDevice, StructureComponent component) {
        String secStr = TUMaths.locationString(secondaryLoc);
        Location coreLoc = coreDevice.getLocationREADONLY();
        secondaryToCoreMap.put(secStr, TUMaths.locationString(coreLoc));
        secondaryToPortMap.put(secStr, component.getPort());

        Device existing = Device.getDevice(secondaryLoc);
        if (!(existing instanceof PortProxyDevice)) {
            ProxyFactory factory = proxyFactories.get(component.getPort().getClass());
            if (factory != null) {
                factory.create(secondaryLoc, coreDevice, component);
            } else {
                new PortProxyDevice(secondaryLoc, coreDevice, component);
            }
        }
    }

    public void unregisterSecondaryBlock(Location secondaryLoc) {
        String secStr = TUMaths.locationString(secondaryLoc);
        secondaryToCoreMap.remove(secStr);
        secondaryToPortMap.remove(secStr);
    }

    public void unregisterCore(Device coreDevice) {
        String coreStr = TUMaths.locationString(coreDevice.getLocationREADONLY());
        secondaryToCoreMap.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(coreStr)) {
                secondaryToPortMap.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    public int getSecondaryBlockCount(Device coreDevice) {
        if (coreDevice == null || coreDevice.getLocationREADONLY() == null) return 0;
        String coreStr = TUMaths.locationString(coreDevice.getLocationREADONLY());
        int count = 0;
        for (String targetCore : secondaryToCoreMap.values()) {
            if (targetCore.equals(coreStr)) {
                count++;
            }
        }
        return count;
    }
}