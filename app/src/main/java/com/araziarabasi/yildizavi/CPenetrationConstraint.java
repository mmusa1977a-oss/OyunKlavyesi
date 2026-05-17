package com.araziarabasi.yildizavi;

public class CPenetrationConstraint {
    public CParticle p0;
    public CParticle p1;
    public CParticle p2;
    public float sign;

    public CPenetrationConstraint(CParticle p0, CParticle p1, CParticle p2) {
        this.p0 = p0;
        this.p1 = p1;
        this.p2 = p2;

        sign = p0.pos.getDistanceTo(p1.pos, p2.pos);
        sign = sign < 0 ? -1f : 1f;
    }

    public void resolve() {
        float d = p0.pos.getDistanceTo(p1.pos, p2.pos);

        if (sign * d <= 2f) {
            CVector2D n = new CVector2D(p2.pos.x, p2.pos.y);
            n.minus(p1.pos);
            n.normalize();
            n.rotate((float)(Math.PI / 2f) * sign);
            n.mult(Math.abs(d + 1f));
            p1.pos.minus(n);
        }
    }
}

