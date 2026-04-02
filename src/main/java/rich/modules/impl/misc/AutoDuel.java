package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.modules.module.setting.implement.TextSetting;
import rich.util.timer.TimerUtil;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AutoDuel extends ModuleStructure {

    private final Pattern pattern = Pattern.compile("^\\w{3,16}$");

    private final SelectSetting mode = new SelectSetting("Режим", "Режим дуэли")
            .value("Шары", "Щит", "Шипы 3", "Незеритка", "Читерский рай", "Лук", "Классик", "Тотемы", "Нодебафф")
            .selected("Шары");

    private final SliderSettings slowTime = new SliderSettings("Скорость отправки", "Задержка между запросами")
            .setValue(500F).range(300F, 1000F);

    private final BooleanSetting babki = new BooleanSetting("Играть на деньги", "Ставка монет")
            .setValue(false);

    private final TextSetting money = new TextSetting("Монет", "Количество монет для ставки")
            .setText("10000")
            .visible(() -> babki.isValue());

    private double lastPosX, lastPosY, lastPosZ;

    private final List<String> sent = Lists.newArrayList();
    private final TimerUtil counter = TimerUtil.create();
    private final TimerUtil counter2 = TimerUtil.create();
    private final TimerUtil counterChoice = TimerUtil.create();
    private final TimerUtil counterTo = TimerUtil.create();

    public AutoDuel() {
        super("AutoDuel", "Auto Duel", ModuleCategory.MISC);
        settings(mode, slowTime, babki, money);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        counter.resetCounter();
        counter2.resetCounter();
        counterChoice.resetCounter();
        counterTo.resetCounter();
        sent.clear();
        super.activate();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        handleDuelLogic();
        handleScreenInteraction();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onPacket(PacketEvent event) {
        if (event.getType() == PacketEvent.Type.RECEIVE && event.getPacket() instanceof GameMessageS2CPacket chat) {
            String text = chat.content().getString();
            if ((text.contains("начало") && text.contains("через") && text.contains("секунд!")) ||
                    (text.contains("дуэли » во время поединка запрещено использовать команды"))) {
                setState(false);
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleDuelLogic() {
        List<String> players = getOnlinePlayers();

        double distance = Math.sqrt(
                Math.pow(lastPosX - mc.player.getX(), 2) +
                        Math.pow(lastPosY - mc.player.getY(), 2) +
                        Math.pow(lastPosZ - mc.player.getZ(), 2)
        );

        if (distance > 500) {
            setState(false);
            return;
        }

        lastPosX = mc.player.getX();
        lastPosY = mc.player.getY();
        lastPosZ = mc.player.getZ();

        if (counter2.hasTimeElapsed(800L * players.size())) {
            sent.clear();
            counter2.resetCounter();
        }

        for (String player : players) {
            if (!sent.contains(player) && !player.equals(mc.player.getGameProfile().name())) {
                if (counter.hasTimeElapsed((long) slowTime.getValue())) {
                    sendDuelRequest(player);
                    sent.add(player);
                    counter.resetCounter();
                }
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void sendDuelRequest(String player) {
        if (babki.isValue()) {
            mc.player.networkHandler.sendChatCommand("duel " + player + " " + money.getText());
        } else {
            mc.player.networkHandler.sendChatCommand("duel " + player);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void handleScreenInteraction() {
        if (mc.currentScreen != null && mc.player.currentScreenHandler instanceof ScreenHandler chest) {
            String title = mc.currentScreen.getTitle().getString();

            if (title.contains("Выбор набора (1/1)")) {
                if (counterChoice.hasTimeElapsed(150)) {
                    int slotID = getKitSlot();
                    if (slotID >= 0) {
                        mc.interactionManager.clickSlot(
                                mc.player.currentScreenHandler.syncId,
                                slotID,
                                0,
                                SlotActionType.QUICK_MOVE,
                                mc.player
                        );
                    }
                    counterChoice.resetCounter();
                }
            } else if (title.contains("Настройка поединка")) {
                if (counterTo.hasTimeElapsed(150)) {
                    mc.interactionManager.clickSlot(
                            chest.syncId,
                            0,
                            0,
                            SlotActionType.QUICK_MOVE,
                            mc.player
                    );
                    counterTo.resetCounter();
                }
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private int getKitSlot() {
        return switch (mode.getSelected()) {
            case "Щит" -> 0;
            case "Шипы 3" -> 1;
            case "Лук" -> 2;
            case "Тотемы" -> 3;
            case "Нодебафф" -> 4;
            case "Шары" -> 5;
            case "Классик" -> 6;
            case "Читерский рай" -> 7;
            case "Незеритка" -> 8;
            default -> -1;
        };
    }

    private List<String> getOnlinePlayers() {
        return mc.player.networkHandler.getPlayerList().stream()
                .map(PlayerListEntry::getProfile)
                .map(GameProfile::name)
                .filter(profileName -> pattern.matcher(profileName).matches())
                .collect(Collectors.toList());
    }
}