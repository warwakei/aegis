package fun.aegis.features.impl.misc;

import antidaunleak.api.annotation.Native;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.features.aura.warp.TurnsConfig;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.utils.math.task.TaskPriority;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BindSetting;
import fun.aegis.events.keyboard.HotBarScrollEvent;
import fun.aegis.events.keyboard.KeyEvent;
import fun.aegis.events.player.HotBarUpdateEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.inv.InventoryTask;
import fun.aegis.utils.math.time.StopWatch;
import fun.aegis.utils.math.script.Script;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.features.impl.render.ProjectilePrediction;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WindJump extends Module {
    private final Turns rot = new Turns(0, 0);
    BindSetting windChargeBind = new BindSetting("Заряд ветра", "Бросить заряд ветра");
    StopWatch stopWatch = new StopWatch();
    Script script = new Script();

    public WindJump() {
        super("WindJump", "Wind Jump", ModuleCategory.MISC);
        setup(windChargeBind);
    }

    @EventHandler
    public void onHotBarUpdate(HotBarUpdateEvent e) {
        if (!script.isFinished()) e.cancel();
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (!script.isFinished()) e.cancel();
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyReleased(windChargeBind.getKey())) {
            if (stopWatch.finished(0)) {
                InventoryTask.swapAndUse(Items.WIND_CHARGE);
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (PlayerInteractionHelper.isKey(windChargeBind)) {
            rot.setYaw(mc.player.getYaw());
            rot.setPitch(90);
            TurnsConnection.INSTANCE.rotateTo(rot, TurnsConfig.DEFAULT, TaskPriority.LOW_PRIORITY, this);
            ItemStack stack = Items.WIND_CHARGE.getDefaultStack();
            ProjectilePrediction.getInstance().drawPredictionInHand(e.getStack(), List.of(stack), MathAngle.cameraAngle());
        }
    }

    @EventHandler

    public void onTick(TickEvent e) {
        if (!script.isFinished() && stopWatch.every(250)) {
            script.update();
        }
    }
}
