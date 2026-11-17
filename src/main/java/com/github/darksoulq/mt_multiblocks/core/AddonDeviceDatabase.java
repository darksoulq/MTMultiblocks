package com.github.darksoulq.mt_multiblocks.core;

import com.MT.triggersUtility.TUMaths;
import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.MTMultiblocks;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddonDeviceDatabase {

    private static final Map<Class<?>, AddonDeviceDatabase> dbMap = new HashMap<>();

    private final JavaPlugin plugin;
    private final Map<Location, AddonDevice> activeDevices = new HashMap<>();

    public AddonDeviceDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void registerClass(Class<?> clazz, AddonDeviceDatabase db) {
        dbMap.put(clazz, db);
    }

    public static void track(AddonDevice device) {
        AddonDeviceDatabase db = dbMap.get(device.getClass());
        if (db != null) {
            db.trackDevice(device);
        } else if (device instanceof PortProxyDevice proxy) {
            Device core = proxy.getCoreDevice();
            if (core != null) {
                AddonDeviceDatabase coreDb = dbMap.get(core.getClass());
                if (coreDb != null) {
                    coreDb.trackDevice(device);
                }
            }
        }
    }

    public static void untrack(AddonDevice device) {
        AddonDeviceDatabase db = dbMap.get(device.getClass());
        if (db != null) {
            db.untrackDevice(device);
        } else if (device instanceof PortProxyDevice proxy) {
            Device core = proxy.getCoreDevice();
            if (core != null) {
                AddonDeviceDatabase coreDb = dbMap.get(core.getClass());
                if (coreDb != null) {
                    coreDb.untrackDevice(device);
                }
            }
        }
    }

    public void trackDevice(AddonDevice device) {
        if (device.getLocationREADONLY() != null) {
            activeDevices.put(device.getLocationREADONLY(), device);
        }
    }

    public void untrackDevice(AddonDevice device) {
        if (device.getLocationREADONLY() != null) {
            activeDevices.remove(device.getLocationREADONLY());
        }
    }

    public void saveAll() {
        File file = new File(plugin.getDataFolder(), "devices_data.dat");
        file.getParentFile().mkdirs();

        List<SavedDevice> savedDevices = new ArrayList<>();

        for (Map.Entry<Location, AddonDevice> entry : activeDevices.entrySet()) {
            AddonDevice device = entry.getValue();
            if (device == null || device.isRemoved()) continue;

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeObject(device);
                oos.flush();
                byte[] data = baos.toByteArray();

                savedDevices.add(new SavedDevice(TUMaths.locationString(entry.getKey()), device.getClass().getName(), plugin.getName(), data));
            } catch (Exception e) {
                JavaPlugin.getPlugin(MTMultiblocks.class).getLogger().severe("Failed to serialize device at " + TUMaths.locationString(entry.getKey()));
                e.printStackTrace();
            }
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(savedDevices);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadAll() {
        File file = new File(plugin.getDataFolder(), "devices_data.dat");
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<SavedDevice> savedDevices = (List<SavedDevice>) ois.readObject();

            Field allDevicesField = Device.class.getDeclaredField("allDevices");
            allDevicesField.setAccessible(true);
            HashMap<Location, Device> allDevices = (HashMap<Location, Device>) allDevicesField.get(null);

            Field readDevicesField = Device.class.getDeclaredField("readDevices");
            readDevicesField.setAccessible(true);
            ArrayList<Device> readDevices = (ArrayList<Device>) readDevicesField.get(null);

            for (SavedDevice sd : savedDevices) {
                if (sd.getClassName().equals(PortProxyDevice.class.getName())) continue;
                loadSingleDevice(sd, allDevices, readDevices);
            }

            for (SavedDevice sd : savedDevices) {
                if (!sd.getClassName().equals(PortProxyDevice.class.getName())) continue;
                loadSingleDevice(sd, allDevices, readDevices);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSingleDevice(SavedDevice sd, HashMap<Location, Device> allDevices, ArrayList<Device> readDevices) {
        Location loc = TUMaths.getStringLocation(sd.getLocation());
        if (loc == null) return;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(sd.getData());

            ObjectInputStream deviceOis = new ObjectInputStream(bais) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    try {
                        return Class.forName(desc.getName(), false, plugin.getClass().getClassLoader());
                    } catch (ClassNotFoundException ex) {
                        return super.resolveClass(desc);
                    }
                }
            };

            AddonDevice loadedDevice = (AddonDevice) deviceOis.readObject();

            trackDevice(loadedDevice);
            allDevices.put(loc, loadedDevice);
            if (!readDevices.contains(loadedDevice)) {
                readDevices.add(loadedDevice);
            }

            loadedDevice.onCreate(false);
        } catch (Exception e) {
            JavaPlugin.getPlugin(MTMultiblocks.class).getLogger().severe("Failed to deserialize addon device: " + sd.getClassName() + " at " + sd.getLocation());
            e.printStackTrace();
        }
    }
}