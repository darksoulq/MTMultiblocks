package com.github.darksoulq.mt_multiblocks.core;

import com.MT.triggersUtility.Saveable;
import com.MT.triggersUtility.TUMaths;
import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.MTMultiblocks;
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;
import com.github.darksoulq.mt_multiblocks.StructureComponent;
import com.github.darksoulq.mt_multiblocks.port.Port;
import com.github.darksoulq.mt_multiblocks.util.MultiblockUtil;
import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiblockRegistry {
    public static MultiblockRegistry instance;

    public Map<String, String> secondaryToCoreMap;
    public Map<String, Port> secondaryToPortMap;
    public transient Map<Class<? extends Port>, ProxyFactory> proxyFactories;

    private static Field allDevicesField;

    static {
        try {
            allDevicesField = Device.class.getDeclaredField("allDevices");
            allDevicesField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public MultiblockRegistry() {
        instance = this;
        this.secondaryToCoreMap = new ConcurrentHashMap<>();
        this.secondaryToPortMap = new ConcurrentHashMap<>();
        this.proxyFactories = new ConcurrentHashMap<>();
        load();
        restoreProxies();
    }

    public void save() {
        File file = new File(MTMultiblocks.getInstance().getDataFolder(), "registry.dat");
        file.getParentFile().mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(new HashMap<>(secondaryToCoreMap));
            oos.writeObject(new HashMap<>(secondaryToPortMap));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void load() {
        File file = new File(MTMultiblocks.getInstance().getDataFolder(), "registry.dat");
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            this.secondaryToCoreMap = new ConcurrentHashMap<>((Map<String, String>) ois.readObject());
            this.secondaryToPortMap = new ConcurrentHashMap<>((Map<String, Port>) ois.readObject());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerProxyFactory(Class<? extends Port> portClass, ProxyFactory factory) {
        proxyFactories.put(portClass, factory);
    }

    public void registerSecondaryBlock(Location secondaryLoc, Device coreDevice, StructureComponent component) {
        String secStr = TUMaths.locationString(secondaryLoc);
        secondaryToCoreMap.put(secStr, TUMaths.locationString(coreDevice.getLocationREADONLY()));
        secondaryToPortMap.put(secStr, component.getPort());
        bindProxy(secondaryLoc, coreDevice, component);
    }

    public void unregisterSecondaryBlock(Location secondaryLoc) {
        String secStr = TUMaths.locationString(secondaryLoc);
        secondaryToCoreMap.remove(secStr);
        secondaryToPortMap.remove(secStr);
        unbindProxy(secondaryLoc);
    }

    public void unregisterCore(Device coreDevice) {
        String coreStr = TUMaths.locationString(coreDevice.getLocationREADONLY());
        secondaryToCoreMap.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(coreStr)) {
                Location secLoc = TUMaths.getStringLocation(entry.getKey());
                unbindProxy(secLoc);
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

    @SuppressWarnings("unchecked")
    private void bindProxy(Location secondaryLoc, Device coreDevice, StructureComponent component) {
        try {
            HashMap<Location, Device> allDevices = (HashMap<Location, Device>) allDevicesField.get(null);
            ProxyFactory factory = proxyFactories.get(component.getPort().getClass());
            Device proxy;
            if (factory != null) {
                proxy = factory.create(secondaryLoc, coreDevice, component);
            } else {
                proxy = new PortProxyDevice(secondaryLoc, coreDevice, component);
            }
            allDevices.put(secondaryLoc, proxy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void unbindProxy(Location secondaryLoc) {
        try {
            HashMap<Location, Device> allDevices = (HashMap<Location, Device>) allDevicesField.get(null);
            allDevices.remove(secondaryLoc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void removeAllProxies() {
        try {
            HashMap<Location, Device> allDevices = (HashMap<Location, Device>) allDevicesField.get(null);
            if (allDevices == null) return;

            List<Device> proxiesToRemove = new ArrayList<>();
            for (Device dev : allDevices.values()) {
                if (dev != null && dev.getClass().getName().contains("PortProxyDevice")) {
                    proxiesToRemove.add(dev);
                }
            }

            for (Device dev : proxiesToRemove) {
                if (dev instanceof PortProxyDevice proxy) {
                    proxy.prepareForShutdown();
                    try {
                        proxy.removeDevice();
                    } catch (Exception ignored) {}
                }
            }

            try {
                Field savedTypesField = Saveable.class.getDeclaredField("savedTypes");
                savedTypesField.setAccessible(true);
                HashMap<String, ArrayList<Saveable>> savedTypes =
                    (HashMap<String, ArrayList<Saveable>>) savedTypesField.get(null);

                if (savedTypes != null) {
                    savedTypes.remove(PortProxyDevice.class.getName());

                    for (ArrayList<Saveable> saveableList : savedTypes.values()) {
                        if (saveableList == null) continue;
                        for (Saveable saveable : saveableList) {
                            if (saveable == null) continue;

                            for (Field field : saveable.getClass().getDeclaredFields()) {
                                if (Collection.class.isAssignableFrom(field.getType())) {
                                    try {
                                        field.setAccessible(true);
                                        Collection<?> col = (Collection<?>) field.get(saveable);
                                        if (col != null) {
                                            col.removeIf(obj -> obj != null && obj.getClass().getName().contains("PortProxyDevice"));
                                        }
                                    } catch (Exception ignored) {}
                                }
                                else if (Map.class.isAssignableFrom(field.getType())) {
                                    try {
                                        field.setAccessible(true);
                                        Map<?, ?> map = (Map<?, ?>) field.get(saveable);
                                        if (map != null) {
                                            map.keySet().removeIf(obj -> obj != null && obj.getClass().getName().contains("PortProxyDevice"));
                                            map.values().removeIf(obj -> obj != null && obj.getClass().getName().contains("PortProxyDevice"));
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            try {
                Device.getDevicesREADONLY().removeIf(obj -> obj != null && obj.getClass().getName().contains("PortProxyDevice"));
                Device.getPylonsREADONLY().removeIf(obj -> obj != null && obj.getClass().getName().contains("PortProxyDevice"));
            } catch (Exception ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restoreProxies() {
        if (secondaryToCoreMap == null) return;
        for (Map.Entry<String, String> entry : secondaryToCoreMap.entrySet()) {
            Location secLoc = TUMaths.getStringLocation(entry.getKey());
            Location coreLoc = TUMaths.getStringLocation(entry.getValue());

            if (secLoc != null && coreLoc != null) {
                Device coreDevice = Device.getDevice(coreLoc);
                if (coreDevice != null) {
                    MultiblockBlueprint blueprint = MultiblockUtil.getTargetBlueprint(coreDevice);
                    if (blueprint != null) {
                        BlockFace facing = RotationUtil.getFacingSafely(coreDevice);
                        Map<Vector, StructureComponent> structure = blueprint.buildStructure(coreLoc, facing, coreDevice, null);
                        Vector relative = secLoc.clone().subtract(coreLoc).toVector();
                        Vector original = RotationUtil.rotateVector(relative, facing.getOppositeFace());

                        StructureComponent matchedComponent = null;
                        for (Map.Entry<Vector, StructureComponent> compEntry : structure.entrySet()) {
                            if (compEntry.getKey().distanceSquared(original) < 0.1) {
                                matchedComponent = compEntry.getValue();
                                break;
                            }
                        }

                        if (matchedComponent != null) {
                            bindProxy(secLoc, coreDevice, matchedComponent);
                        }
                    }
                }
            }
        }
    }
}