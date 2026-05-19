package com.araziarabasi.yildizavi;

/**
 * Orijinal Dune Buggy CGP.as içinden çıkarılan levelCoords tablosu.
 *
 * Bu dosya şimdilik gerçek bölüm başlangıç/bitiş/mesafe/süre bilgisini kullanır.
 * Bir sonraki adımda symGround SVG path verileri de buraya eklenerek zemin 1:1 yapılacak.
 */
public final class DuneGroundData {
    private DuneGroundData() {}

    public static final float[][] LEVEL_COORDS = new float[][] {
            {370f, 200f, 11070f, 100f, 11170f, 700f, 240000f, 2f, 50000f},
            {350f, 200f, 9071f, 100f, 9171f, 700f, 240000f, 3f, 5150f},
            {330f, 100f, 7803f, 700f, 7903f, 700f, 240000f, 1f, 4887f, -400f},
            {330f, 200f, 9105f, 100f, 9205f, 700f, 240000f, 2f, 4035f, 200f},
            {350f, 220f, 9710f, 100f, 9810f, 700f, 240000f, 3f, 4530f, 200f},
            {500f, -100f, 10615f, 100f, 10715f, 700f, 180000f, 2f, 4808f, -60f},
            {560f, -150f, 9347f, 100f, 9447f, 700f, 180000f, 3f, 6118f, -150f},
            {400f, 150f, 10469f, 100f, 10569f, 700f, 180000f, 2f, 3834f, -260f},
            {400f, 150f, 12000f, 100f, 12100f, 700f, 180000f, 3f, 5882f, -100f},
            {400f, 0f, 9750f, 100f, 9850f, 700f, 180000f, 2f, 4520f, 50f},
            {400f, -350f, 12750f, 100f, 12850f, 700f, 120000f, 1f, 7345f, 100f},
            {400f, -250f, 13500f, 100f, 13600f, 700f, 120000f, 2f, 6583.5f, 200f},
            {400f, -110f, 10769f, 100f, 10869f, 700f, 120000f, 1f, 5110f, 70f},
            {400f, -50f, 10560f, 100f, 10660f, 700f, 120000f, 3f, 4729f, 30f},
            {500f, 150f, 11160f, 100f, 11260f, 700f, 120000f, 1f, 3626.5f, -20f}
    };

    public static int indexForLevel(int levelNo) {
        if (LEVEL_COORDS.length == 0) return 0;
        int i = (levelNo - 1) % LEVEL_COORDS.length;
        if (i < 0) i = 0;
        return i;
    }

    public static float startX(int levelNo) {
        return LEVEL_COORDS[indexForLevel(levelNo)][0];
    }

    public static float startY(int levelNo) {
        return LEVEL_COORDS[indexForLevel(levelNo)][1];
    }

    public static float endX(int levelNo) {
        return LEVEL_COORDS[indexForLevel(levelNo)][2];
    }

    public static float finishY(int levelNo) {
        return LEVEL_COORDS[indexForLevel(levelNo)][3];
    }

    public static float totalDistance(int levelNo) {
        return LEVEL_COORDS[indexForLevel(levelNo)][4];
    }

    public static float baseY(int levelNo) {
        return LEVEL_COORDS[indexForLevel(levelNo)][5];
    }

    public static int timeSeconds(int levelNo) {
        return Math.max(60, (int)(LEVEL_COORDS[indexForLevel(levelNo)][6] / 1000f));
    }

    public static int maxLevel() {
        return LEVEL_COORDS.length;
    }

    /**
     * Şimdilik symGround verisi parse edilene kadar, Dune Buggy'ye daha yakın
     * hafif iniş-çıkışlı zemin üretir. Büyük sinüs dağları yok.
     * Bir sonraki paket gerçek SVG shape pathlerinden heightMap üretecek.
     */
    public static float approximateGroundY(int levelNo, float worldX) {
        int idx = indexForLevel(levelNo);
        float[] c = LEVEL_COORDS[idx];

        float startX = c[0];
        float endX = c[2];
        float base = c[5] - 160f; // Android ekranına oturtma

        float t = (worldX - startX) / Math.max(1f, endX - startX);
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;

        // Orijinal Dune Buggy gibi: uzun düz alan + küçük rampalar + az engebe.
        float y = base;

        y += softBump(t, 0.05f, 0.16f, 28f);
        y += softBump(t, 0.16f, 0.28f, -36f);
        y += softBump(t, 0.28f, 0.42f, 30f);
        y += softBump(t, 0.42f, 0.54f, -42f);
        y += softBump(t, 0.54f, 0.68f, 38f);
        y += softBump(t, 0.68f, 0.80f, -30f);
        y += softBump(t, 0.80f, 0.92f, 34f);

        y += (float)Math.sin(worldX * 0.011f + levelNo * 0.4f) * 10f;
        y += (float)Math.sin(worldX * 0.027f + levelNo) * 5f;

        return y;
    }

    private static float softBump(float t, float start, float end, float height) {
        if (t < start || t > end) return 0f;
        float p = (t - start) / (end - start);
        return (float)Math.sin(p * Math.PI) * height;
    }
}
