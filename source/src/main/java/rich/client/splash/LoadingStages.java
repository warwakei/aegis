package rich.client.splash;

public enum LoadingStages {
    INITIALIZING(0, "Initializing..."),
    LOADING_CONFIG(5, "Loading configuration..."),
    LOADING_REPOSITORIES(10, "Loading repositories..."),
    LOADING_FRIENDS_CONFIG(15, "Loading friend list..."),
    LOADING_STAFF_CONFIG(18, "Loading staff configuration..."),
    COMPILING_CORE_SHADERS(20, "Compiling core shaders..."),
    COMPILING_RENDER_SHADERS(30, "Compiling render shaders..."),
    COMPILING_EFFECT_SHADERS(40, "Compiling effect shaders..."),
    COMPILING_UI_SHADERS(50, "Compiling UI shaders..."),
    LOADING_FONTS(55, "Loading fonts..."),
    LOADING_TEXTURES(60, "Loading textures..."),
    LOADING_MODELS(65, "Loading models..."),
    INITIALIZING_EVENT_SYSTEM(70, "Initializing event system..."),
    INITIALIZING_HUD_ELEMENTS(75, "Initializing HUD elements..."),
    LOADING_MODULES(80, "Loading modules..."),
    INITIALIZING_COMMANDS(85, "Initializing commands..."),
    FINALIZING_CONFIGS(90, "Finalizing configuration..."),
    FINALIZING(95, "Finalizing startup..."),
    COMPLETE(100, "Done!");

    private final int progress;
    private final String message;

    LoadingStages(int progress, String message) {
        this.progress = progress;
        this.message = message;
    }

    public int getProgress() {
        return progress;
    }

    public String getMessage() {
        return message;
    }

    public void update() {
        SplashScreenManager.updateProgress(progress, message);
        // dramatic pacing for the splash (18-44s total across all stages)
        try {
            Thread.sleep(800 + (int)(Math.random() * 1400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (this == COMPLETE) {
            SplashScreenManager.complete();
        }
    }
}
