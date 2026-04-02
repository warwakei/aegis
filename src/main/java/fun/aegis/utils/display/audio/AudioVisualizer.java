package fun.aegis.utils.display.audio;

import java.util.Arrays;

public class AudioVisualizer {
    private static final int BAND_COUNT = 8;
    private float[] bandHeights = new float[BAND_COUNT];
    private float[] targetHeights = new float[BAND_COUNT];
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 30;

    public AudioVisualizer() {
        Arrays.fill(bandHeights, 0f);
        Arrays.fill(targetHeights, 0f);
    }

    public void update() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            for (int i = 0; i < BAND_COUNT; i++) {
                bandHeights[i] = lerp(bandHeights[i], targetHeights[i], 0.15f);
            }
            return;
        }
        lastUpdateTime = currentTime;

        for (int i = 0; i < BAND_COUNT; i++) {
            targetHeights[i] = 1f + (float) Math.random() * 9f;
        }

        for (int i = 0; i < BAND_COUNT; i++) {
            bandHeights[i] = lerp(bandHeights[i], targetHeights[i], 0.15f);
        }
    }

    public float[] getBandHeights() {
        return bandHeights;
    }

    public float getBandHeight(int index) {
        if (index < 0 || index >= BAND_COUNT) return 0f;
        return bandHeights[index];
    }

    public int getBandCount() {
        return BAND_COUNT;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public void reset() {
        Arrays.fill(bandHeights, 0f);
        Arrays.fill(targetHeights, 0f);
    }
}
