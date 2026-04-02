package fun.aegis.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.aegis.events.player.TickEvent;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;

import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.event.types.EventType;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.math.time.StopWatch;
import fun.aegis.utils.math.script.Script;
import fun.aegis.events.item.UsingItemEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoSlow extends Module {
    public static NoSlow getInstance() {
        return Instance.get(NoSlow.class);
    }

    private final StopWatch notifWatch = new StopWatch();
    private final Script script = new Script();
    private boolean finish;

    public final SelectSetting itemMode = new SelectSetting("Режим предмета", "Выберите режим обхода").value("Grim Old", "SpookyTime", "Grim", "ReallyWorld", "SlothAC");

    public NoSlow() {
        super("NoSlow", "No Slow", ModuleCategory.MOVEMENT);
        setup(itemMode);
    }
    private int ticks = 0;

    @EventHandler
    public void onUpdate(TickEvent event) {
        if (mc.player.getActiveHand() == Hand.MAIN_HAND ||  mc.player.getActiveHand() == Hand.OFF_HAND) {
            ticks++;
        } else {
            ticks = 0;
        }
        
        // Логика для режима ReallyWorld
        if (itemMode.getSelected().equals("ReallyWorld")) {
            if (!mc.player.isGliding()) {
                if (mc.player.isUsingItem()) {
                    ticks++;
                } else {
                    ticks = 0;
                }
            }
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onUsingItem(UsingItemEvent e) {
        Hand first = mc.player.getActiveHand();
        Hand second = first.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;


        switch (e.getType()) {
            case EventType.ON -> {
                switch (itemMode.getSelected()) {
                    case "Grim Old" -> {
                        if (mc.player.getOffHandStack().getUseAction().equals(UseAction.NONE) || mc.player.getMainHandStack().getUseAction().equals(UseAction.NONE)) {
                            PlayerInteractionHelper.interactItem(first);
                            PlayerInteractionHelper.interactItem(second);
                            e.cancel();
                        }
                    }
                    case "SpookyTime" -> {
                        if (ticks > 1F && mc.player.getItemUseTime() > 1) {
                            e.cancel();
                            ticks = 0;
                        }
                    }
                    case "Grim" -> {
                        e.cancel();
                    }
                    case "ReallyWorld" -> {
                        if (ticks == 1 || ticks == 2) {
                            e.cancel();
                        }
                        if (ticks >= 2) {
                            ticks = 0;
                        }
                        if (ticks == 0) {
                            // Не отменяем событие
                        }
                    }
                    case "SlothAC" -> {
                        Hand active = mc.player.getActiveHand();
                        if (active != null) {
                            Hand opposite = (active == Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;
                            PlayerInteractionHelper.interactItem(opposite);
                            e.cancel();
                        }
                    }
                }
            }
            case EventType.POST -> {
                while (!script.isFinished()) script.update();
            }
        }
    }
}
