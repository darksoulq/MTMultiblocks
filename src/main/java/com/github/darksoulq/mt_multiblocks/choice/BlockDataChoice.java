package com.github.darksoulq.mt_multiblocks.choice;

import com.github.darksoulq.mt_multiblocks.util.RotationUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class BlockDataChoice implements ComponentChoice {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<String> rawDataStrings;
    private transient List<BlockData> rawDataList;
    private transient List<BlockData[]> rotatedCacheList;

    public BlockDataChoice(List<BlockData> blockDataChoices) {
        this.rawDataStrings = new ArrayList<>();
        for (BlockData bd : blockDataChoices) {
            this.rawDataStrings.add(bd.getAsString());
        }
        initCache();
    }

    public BlockDataChoice(BlockData blockData) {
        this(List.of(blockData));
    }

    private void initCache() {
        this.rawDataList = new ArrayList<>();
        this.rotatedCacheList = new ArrayList<>();
        for (String rds : rawDataStrings) {
            BlockData data = Bukkit.createBlockData(rds);
            this.rawDataList.add(data);
            BlockData[] rotated = new BlockData[4];
            rotated[0] = RotationUtil.rotateBlockData(data, BlockFace.NORTH);
            rotated[1] = RotationUtil.rotateBlockData(data, BlockFace.EAST);
            rotated[2] = RotationUtil.rotateBlockData(data, BlockFace.SOUTH);
            rotated[3] = RotationUtil.rotateBlockData(data, BlockFace.WEST);
            this.rotatedCacheList.add(rotated);
        }
    }

    @Override
    public boolean matches(BlockData blockData, BlockFace facing) {
        if (rotatedCacheList == null) initCache();
        byte index = RotationUtil.getFaceIndex(facing);
        for (int i = 0; i < rawDataList.size(); i++) {
            BlockData targetData = (index != -1) ? rotatedCacheList.get(i)[index] : rawDataList.get(i);
            if (blockData.matches(targetData)) return true;
        }
        return false;
    }

    @Override
    public BlockData getDisplayData(BlockFace facing) {
        if (rotatedCacheList == null) initCache();
        byte index = RotationUtil.getFaceIndex(facing);
        return index != -1 ? rotatedCacheList.get(0)[index] : rawDataList.get(0);
    }
}