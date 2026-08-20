package com.github.darksoulq.mt_multiblocks.model;

import java.io.Serializable;

public class Keyframe implements Serializable {
    public final int durationTicks;
    public float translateX = 0, translateY = 0, translateZ = 0;
    public float rotX = 0, rotY = 0, rotZ = 0;
    public float scaleX = 1, scaleY = 1, scaleZ = 1;
    public Easing easing = Easing.LINEAR;

    public Keyframe(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public Keyframe translate(float x, float y, float z) {
        this.translateX = x; this.translateY = y; this.translateZ = z;
        return this;
    }

    public Keyframe rotate(float x, float y, float z) {
        this.rotX = x; this.rotY = y; this.rotZ = z;
        return this;
    }

    public Keyframe rotateX(float x) {
        this.rotX = x;
        return this;
    }

    public Keyframe rotateY(float y) {
        this.rotY = y;
        return this;
    }

    public Keyframe rotateZ(float z) {
        this.rotZ = z;
        return this;
    }

    public Keyframe scale(float x, float y, float z) {
        this.scaleX = x; this.scaleY = y; this.scaleZ = z;
        return this;
    }

    public Keyframe easing(Easing easing) {
        this.easing = easing;
        return this;
    }
}