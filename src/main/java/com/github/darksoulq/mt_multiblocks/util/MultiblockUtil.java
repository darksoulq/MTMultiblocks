package com.github.darksoulq.mt_multiblocks.util;

import com.MT.xxxtrigger50xxx.Devices.Device;
import com.github.darksoulq.mt_multiblocks.BlueprintContext;
import com.github.darksoulq.mt_multiblocks.ExternalMultiblocks;
import com.github.darksoulq.mt_multiblocks.MTMultiblock;
import com.github.darksoulq.mt_multiblocks.MultiblockBlueprint;

public class MultiblockUtil {

    public static boolean isCoreDevice(Device device) {
        if (device instanceof MTMultiblock) {
            return true;
        }
        return device != null && ExternalMultiblocks.hasBlueprint(device.getName());
    }

    public static MultiblockBlueprint getTargetBlueprint(Device device, BlueprintContext context) {
        if (device instanceof MTMultiblock mt) {
            return mt.getBlueprint(context);
        }
        if (device != null && ExternalMultiblocks.hasBlueprint(device.getName())) {
            return ExternalMultiblocks.getBlueprint(device.getName(), context);
        }
        return null;
    }
}