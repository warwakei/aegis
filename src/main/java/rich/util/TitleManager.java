package rich.util;

public class TitleManager {
    private static TitleManager instance;

    public enum TitleMode {
        DEFAULT,
        AEGIS
    }

    private TitleMode mode = TitleMode.AEGIS;

    private TitleManager() {}

    public static TitleManager getInstance() {
        if (instance == null) {
            instance = new TitleManager();
        }
        return instance;
    }

    public TitleMode getMode() {
        return mode;
    }

    public void setMode(TitleMode mode) {
        this.mode = mode;
    }

    public String getWindowTitle() {
        if (mode == TitleMode.DEFAULT) {
            return null; // родной тайтл майнкрафта
        }

        antidaunleak.api.UserProfile userProfile = antidaunleak.api.UserProfile.getInstance();
        String username = userProfile.profile("username");
        String role = userProfile.profile("role");
        return String.format("%s (%s - %s)", Version.FULL_NAME, role, username);
    }
}
