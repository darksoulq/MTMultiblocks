package com.github.darksoulq.mt_multiblocks;

import com.github.darksoulq.mt_multiblocks.util.ValidationResult;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.io.Serializable;

public interface PlacementCondition extends Serializable {
    ValidationResult canPlace(Location coreLoc, BlockFace facing, Player player);
}