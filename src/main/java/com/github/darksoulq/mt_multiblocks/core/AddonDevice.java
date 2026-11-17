package com.github.darksoulq.mt_multiblocks.core;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.MT.xxxtrigger50xxx.Devices.DeviceDataBase;
import org.bukkit.Location;

public abstract class AddonDevice extends Device {

    public AddonDevice(Location loc) {
        super(loc);
        this.setSave(false);
        this.setDelete(true);
        if (DeviceDataBase.db != null) {
            DeviceDataBase.db.removeData(this);
        }

        AddonDeviceDatabase.track(this);
    }

    @Override
    public void removeDevice() {
        super.removeDevice();
        AddonDeviceDatabase.untrack(this);
    }
}