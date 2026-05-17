package com.araziarabasi.yildizavi;

import java.util.ArrayList;
import java.util.List;

public class CParticleEngine {
    public final List<CParticle> pArray = new ArrayList<>();
    public final List<CWheel> wArray = new ArrayList<>();
    public final List<CSpringConstraint> cArray = new ArrayList<>();
    public final List<CPenetrationConstraint> penArray = new ArrayList<>();

    public float gravity = 0.70f;
    public float koeffDamp = 0.985f;
    public float maxAccSpeed = 28.5f;
    public float timeMultiplier = 1f;

    public void clear() {
        pArray.clear();
        wArray.clear();
        cArray.clear();
        penArray.clear();
    }

    public void addParticle(CParticle p) {
        pArray.add(p);
    }

    public void addWheel(CWheel wheel, CParticle particle, float maxAccSpeed, float rad) {
        wheel.init(particle, maxAccSpeed, rad);
        wArray.add(wheel);
    }

    public void addSpringConstraint(CParticle p1, CParticle p2, float press, float stretch) {
        cArray.add(new CSpringConstraint(p1, p2, press, stretch));
    }

    public void addPenetrationConstraint(CParticle p0, CParticle p1, CParticle p2) {
        penArray.add(new CPenetrationConstraint(p0, p1, p2));
    }

    public void process(DuneLevel level, boolean gas, boolean brake, boolean jump) {
        float acc = 0;
        if (gas) acc += 1f;
        if (brake) acc -= 0.72f;

        for (CWheel w : wArray) {
            w.process(timeMultiplier, acc, koeffDamp);
        }

        for (CParticle p : pArray) {
            p.acc.reinit(0, gravity);
            p.verlet(timeMultiplier, koeffDamp);
        }

        if (jump) {
            boolean grounded = false;
            for (CWheel w : wArray) if (w.intersectionFlag) grounded = true;
            if (grounded) {
                for (CParticle p : pArray) p.addVelocity(0, -8.5f);
            }
        }

        for (int i = 0; i < 4; i++) {
            for (CSpringConstraint c : cArray) c.resolve();
            for (CPenetrationConstraint p : penArray) p.resolve();
        }

        for (CWheel w : wArray) {
            w.checkCollision(level);
        }

        for (int i = 0; i < 2; i++) {
            for (CSpringConstraint c : cArray) c.resolve();
        }
    }

    public boolean isGrounded() {
        for (CWheel w : wArray) if (w.intersectionFlag) return true;
        return false;
    }
}
