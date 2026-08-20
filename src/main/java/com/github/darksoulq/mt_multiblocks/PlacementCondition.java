package com.github.darksoulq.mt_multiblocks;

import com.github.darksoulq.mt_multiblocks.util.ValidationResult;
import java.io.Serializable;

public interface PlacementCondition extends Serializable {
    ValidationResult canPlace(BlueprintContext context);
}