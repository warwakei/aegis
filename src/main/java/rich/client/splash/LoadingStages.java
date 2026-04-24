package rich.client.splash;

public enum LoadingStages {
    INITIALIZING(0, "Инициализация..."),
    LOADING_CONFIG(5, "Загрузка конфигурации..."),
    LOADING_REPOSITORIES(10, "Загрузка репозиториев..."),
    LOADING_FRIENDS_CONFIG(15, "Загрузка списка друзей..."),
    LOADING_STAFF_CONFIG(18, "Загрузка конфигурации персонала..."),
    COMPILING_CORE_SHADERS(20, "Компиляция основных шейдеров..."),
    COMPILING_RENDER_SHADERS(30, "Компиляция шейдеров рендера..."),
    COMPILING_EFFECT_SHADERS(40, "Компиляция шейдеров эффектов..."),
    COMPILING_UI_SHADERS(50, "Компиляция UI шейдеров..."),
    LOADING_FONTS(55, "Загрузка шрифтов..."),
    LOADING_TEXTURES(60, "Загрузка текстур..."),
    LOADING_MODELS(65, "Загрузка моделей..."),
    INITIALIZING_EVENT_SYSTEM(70, "Инициализация системы событий..."),
    INITIALIZING_HUD_ELEMENTS(75, "Инициализация HUD элементов..."),
    LOADING_MODULES(80, "Загрузка модулей..."),
    INITIALIZING_COMMANDS(85, "Инициализация команд..."),
    FINALIZING_CONFIGS(90, "Завершение конфигурации..."),
    FINALIZING(95, "Завершение загрузки..."),
    COMPLETE(100, "Готово!");

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
        if (this == COMPLETE) {
            SplashScreenManager.complete();
        }
    }
}
