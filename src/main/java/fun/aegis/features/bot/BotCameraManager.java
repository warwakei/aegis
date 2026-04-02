package fun.aegis.features.bot;

import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import net.minecraft.util.math.Vec3d;

public class BotCameraManager {
    private static BotCameraManager instance;
    private int activeBotId;
    private Vec3d playerOriginalPos;
    private Turns playerOriginalRotation;
    private boolean isViewingBot;

    private BotCameraManager() {
        this.activeBotId = -1;
        this.isViewingBot = false;
    }

    public static BotCameraManager getInstance() {
        if (instance == null) {
            instance = new BotCameraManager();
        }
        return instance;
    }

    public void switchToBotView(int botId) {
        if (isViewingBot && activeBotId == botId) {
            switchToPlayerView();
            return;
        }

        if (isViewingBot) {
            switchToPlayerView();
        }

        this.activeBotId = botId;
        this.isViewingBot = true;
        
        Bot bot = BotManager.getInstance().getBot(botId);
        if (bot != null && bot.isConnected()) {
            TurnsConnection.INSTANCE.setRotation(new Turns(0, 0));
        }
    }

    public void switchToPlayerView() {
        if (!isViewingBot) return;

        this.isViewingBot = false;
        this.activeBotId = -1;
        TurnsConnection.INSTANCE.setRotation(null);
    }

    public boolean isViewingBot() {
        return isViewingBot;
    }

    public int getActiveBotId() {
        return activeBotId;
    }

    public void stopViewing() {
        switchToPlayerView();
    }
}
