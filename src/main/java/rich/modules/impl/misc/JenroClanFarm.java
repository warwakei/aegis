package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import com.mojang.authlib.GameProfile;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.PlayerListEntry;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.string.chat.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Jenro ClanFarm — автоматическая рассылка инвайтов в клан всем игрокам
 * Логика: 5 инвайтов по 0.3с → пауза 2.3с → следующие 5 → и так далее
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JenroClanFarm extends ModuleStructure {

    // Паттерн для валидации никнеймов (3-16 символов, только буквы/цифры/подчёркивание)
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    final SliderSettings batchSize = new SliderSettings("Пакет", "Сколько инвайтов перед паузой")
            .range(3, 10)
            .setValue(5);

    final SliderSettings delayBetween = new SliderSettings("Задержка (мс)", "Задержка между инвайтами в пакете")
            .range(100, 1000)
            .setValue(300);

    final SliderSettings pauseAfterBatch = new SliderSettings("Пауза (мс)", "Пауза после каждого пакета")
            .range(1000, 10000)
            .setValue(2300);

    private final Random random = new Random();
    private List<String> availablePlayers = new ArrayList<>();
    private int batchCounter = 0;
    private long lastInviteTime = 0;
    private boolean isPaused = false;
    private long pauseStartTime = 0;
    private int totalInvitesSent = 0;

    public JenroClanFarm() {
        super("Jenro ClanFarm", "Автоматическая рассылка инвайтов в клан", ModuleCategory.MISC);
        settings(batchSize, delayBetween, pauseAfterBatch);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        refreshPlayerList();
        batchCounter = 0;
        lastInviteTime = 0;
        isPaused = false;
        totalInvitesSent = 0;
        ChatMessage.brandmessage("Jenro ClanFarm: запущен (" + availablePlayers.size() + " игроков доступно)");
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        ChatMessage.brandmessage("Jenro ClanFarm: остановлен. Отправлено " + totalInvitesSent + " инвайтов.");
        availablePlayers.clear();
        batchCounter = 0;
        isPaused = false;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            setState(false);
            return;
        }

        // Если пауза после пакета — ждём
        if (isPaused) {
            long elapsed = System.currentTimeMillis() - pauseStartTime;
            long pauseDuration = (long) pauseAfterBatch.getValue();
            if (elapsed >= pauseDuration) {
                isPaused = false;
                batchCounter = 0;
                // Обновляем список игроков (могли зайти новые)
                refreshPlayerList();
            }
            return;
        }

        // Если все игроки получили инвайты — обновляем список и начинаем заново
        if (availablePlayers.isEmpty()) {
            refreshPlayerList();
            if (availablePlayers.isEmpty()) {
                ChatMessage.brandmessage("Нет доступных игроков для инвайтов");
                setState(false);
                return;
            }
            batchCounter = 0;
        }

        // Проверяем задержку
        long elapsed = System.currentTimeMillis() - lastInviteTime;
        long delay = (long) delayBetween.getValue();
        if (elapsed < delay) return;

        // Отправляем инвайт
        sendInvite();
        lastInviteTime = System.currentTimeMillis();
        batchCounter++;

        // Проверяем нужно ли делать паузу
        int size = (int) batchSize.getValue();
        if (batchCounter >= size) {
            isPaused = true;
            pauseStartTime = System.currentTimeMillis();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void sendInvite() {
        if (availablePlayers.isEmpty()) return;

        // Выбираем случайного игрока
        int randomIndex = random.nextInt(availablePlayers.size());
        String targetPlayer = availablePlayers.remove(randomIndex);

        // Отправляем команду
        mc.player.networkHandler.sendChatCommand("clan invite " + targetPlayer);
        totalInvitesSent++;

        ChatMessage.brandmessage("Инвайт #" + totalInvitesSent + " → " + targetPlayer +
                " (осталось: " + availablePlayers.size() + ")");
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void refreshPlayerList() {
        if (mc.player == null || mc.player.networkHandler == null) return;

        List<String> players = mc.player.networkHandler.getPlayerList().stream()
                .map(PlayerListEntry::getProfile)
                .map(GameProfile::name)
                .filter(name -> !name.equals(mc.player.getGameProfile().name())) // Исключаем себя
                .filter(name -> NICKNAME_PATTERN.matcher(name).matches()) // Только валидные ники
                .collect(Collectors.toList());

        availablePlayers = new ArrayList<>(players);
    }
}
