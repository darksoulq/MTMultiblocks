package com.github.darksoulq.mt_multiblocks.model;

import java.io.Serializable;

public enum Easing implements Serializable {
    LINEAR,
    EASE_IN_QUAD,
    EASE_OUT_QUAD,
    EASE_IN_OUT_QUAD,
    EASE_IN_OUT_SINE;

    public float apply(float t) {
        return switch (this) {
            case EASE_IN_QUAD -> t * t;
            case EASE_OUT_QUAD -> t * (2 - t);
            case EASE_IN_OUT_QUAD -> t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
            case EASE_IN_OUT_SINE -> (float) (-(Math.cos(Math.PI * t) - 1) / 2);
            default -> t;
        };
    }
}