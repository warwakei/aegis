package fun.aegis.display.screens.clickgui.newgui.animation;

public enum Easing {
    LINEAR {
        @Override
        public float apply(float t) {
            return t;
        }
    },
    QUAD_IN {
        @Override
        public float apply(float t) {
            return t * t;
        }
    },
    QUAD_OUT {
        @Override
        public float apply(float t) {
            return t * (2 - t);
        }
    },
    QUAD_IN_OUT {
        @Override
        public float apply(float t) {
            return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
        }
    },
    CUBIC_IN_OUT {
        @Override
        public float apply(float t) {
            return t < 0.5f ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
        }
    },
    QUARTIC_OUT {
        @Override
        public float apply(float t) {
            return 1 - (float) Math.pow(1 - t, 4);
        }
    },
    BACK_OUT {
        @Override
        public float apply(float t) {
            float c1 = 1.70158f;
            float c3 = c1 + 1;
            return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
        }
    };

    public abstract float apply(float t);
}
