package com.github.darksoulq.mt_multiblocks.port;

import com.MT.xxxtrigger50xxx.Devices.Device;

public interface RedstonePort extends Port {
    boolean canReceive();
    boolean canEmit();
    int getEmissionLevel(Device coreDevice);
    void onRedstoneReceive(Device coreDevice, int powerLevel);
}