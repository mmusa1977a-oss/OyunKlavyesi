package com.araziarabasi.yildizavi;

public class CSpringConstraint {
    public CParticle p1;
    public CParticle p2;
    public float koeffStiffPress;
    public float koeffStiffStretch;
    public float relaxLen;

    public CSpringConstraint(CParticle p1, CParticle p2, float koeffStiffPress, float koeffStiffStretch) {
        this.p1 = p1;
        this.p2 = p2;
        this.koeffStiffPress = koeffStiffPress;
        this.koeffStiffStretch = koeffStiffStretch;

        CVector2D d = new CVector2D(p1.pos.x - p2.pos.x, p1.pos.y - p2.pos.y);
        relaxLen = d.modul();
    }

    public void resolve() {
        CVector2D d = new CVector2D(p1.pos.x - p2.pos.x, p1.pos.y - p2.pos.y);
        float len = d.modul();
        if (len < 0.00001f) return;

        float diff = (len - relaxLen) / len;
        float stiff = len > relaxLen ? koeffStiffStretch : koeffStiffPress;
        float k = diff * stiff / (p1.mass + p2.mass);

        p1.pos.x -= d.x * p1.mass * k;
        p1.pos.y -= d.y * p1.mass * k;
        p2.pos.x += d.x * p2.mass * k;
        p2.pos.y += d.y * p2.mass * k;
    }
}
