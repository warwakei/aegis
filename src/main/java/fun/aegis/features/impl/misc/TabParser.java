package fun.aegis.features.impl.misc;

import fun.aegis.events.player.TickEvent;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.MultiSelectSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.utils.client.chat.ChatMessage;
import fun.aegis.utils.math.time.TimerUtil;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.message.LastSeenMessageList;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.text.Text;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.BitSet;

public class TabParser extends Module {
    SelectSetting versionSelect = new SelectSetting("Версия", "1.16.5").value("1.16.5", "1.21.4");
    MultiSelectSetting donateSelect = new MultiSelectSetting("Донат префиксы", "").value("Герцог", "Князь", "Принц", "Титан", "Элита", "Глава");

    List<String> anarchyServers165 = new ArrayList<>();
    List<String> anarchyServers214 = new ArrayList<>();
    Map<String, Set<String>> parsedPlayers = new ConcurrentHashMap<>();
    Map<String, Set<String>> initialPlayers = new ConcurrentHashMap<>();
    TimerUtil switchTimer = TimerUtil.create();
    TimerUtil scanTimer = TimerUtil.create();
    int currentServerIndex = 0;
    boolean parsing = false;
    boolean waitingForServerLoad = false;

    static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");

    public TabParser() {
        super("Tab Parser", "Tab Parser", ModuleCategory.MISC);
        setup(versionSelect, donateSelect);

        anarchyServers165.addAll(List.of("/an102", "/an103", "/an104", "/an105", "/an106", "/an107"));
        for (int i = 203; i <= 221; i++) {
            anarchyServers165.add("/an" + i);
        }
        for (int i = 302; i <= 313; i++) {
            anarchyServers165.add("/an" + i);
        }
        anarchyServers165.addAll(List.of("/an502", "/an503", "/an504", "/an505", "/an506", "/an507", "/an602"));

        anarchyServers214.addAll(List.of("/an11", "/an12", "/an21", "/an23", "/an31", "/an32", "/an51", "/an52"));
    }

    @Override
    public void activate() {
        super.activate();
        loadExistingData();
        saveInitialState();
        currentServerIndex = 0;
        parsing = true;
        waitingForServerLoad = false;
        switchTimer.resetCounter();
        scanTimer.resetCounter();

        for (String donate : donateSelect.getSelected()) {
            if (!parsedPlayers.containsKey(donate)) {
                parsedPlayers.put(donate, ConcurrentHashMap.newKeySet());
            }
        }

        switchToNextServer();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        parsing = false;
        ChatMessage.brandmessage("Парсинг остановлен.");
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null || !parsing) return;

        if (waitingForServerLoad) {
            if (scanTimer.hasTimeElapsed(3000)) {
                waitingForServerLoad = false;
                scanTimer.resetCounter();
            }
            return;
        }

        if (scanTimer.hasTimeElapsed(2000)) {
            scanCurrentServer();
            scanTimer.resetCounter();
        }

        if (switchTimer.hasTimeElapsed(5000)) {
            switchToNextServer();
            switchTimer.resetCounter();
        }
    }

    private void saveInitialState() {
        initialPlayers.clear();
        for (Map.Entry<String, Set<String>> entry : parsedPlayers.entrySet()) {
            initialPlayers.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
    }

    private int calculateNewPlayers() {
        int newCount = 0;
        for (Map.Entry<String, Set<String>> entry : parsedPlayers.entrySet()) {
            String donate = entry.getKey();
            Set<String> currentPlayers = entry.getValue();
            Set<String> oldPlayers = initialPlayers.getOrDefault(donate, Collections.emptySet());

            for (String player : currentPlayers) {
                if (!oldPlayers.contains(player)) {
                    newCount++;
                }
            }
        }
        return newCount;
    }

    private void loadExistingData() {
        parsedPlayers.clear();

        for (String donate : donateSelect.getSelected()) {
            parsedPlayers.put(donate, ConcurrentHashMap.newKeySet());
        }

        File outputFile = getOutputFile();
        if (!outputFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(outputFile))) {
            String line;
            String currentDonate = null;
            int loadedCount = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("====") && line.contains("донатом")) {
                    String[] parts = line.split("донатом");
                    if (parts.length > 1) {
                        currentDonate = parts[1].replace("====", "").trim();
                        if (!parsedPlayers.containsKey(currentDonate)) {
                            parsedPlayers.put(currentDonate, ConcurrentHashMap.newKeySet());
                        }
                    }
                } else if (!line.isEmpty() && currentDonate != null && NAME_PATTERN.matcher(line).matches()) {
                    parsedPlayers.get(currentDonate).add(line);
                    loadedCount++;
                }
            }
            ChatMessage.brandmessage("Загружено " + loadedCount + " существующих записей");
        } catch (IOException e) {
            ChatMessage.brandmessage("Ошибка при загрузке данных: " + e.getMessage());
        }
    }

    private void scanCurrentServer() {
        if (mc.getNetworkHandler() == null || mc.world == null) return;

        Collection<PlayerListEntry> playerList = mc.getNetworkHandler().getPlayerList();
        if (playerList == null || playerList.isEmpty()) return;

        for (PlayerListEntry entry : playerList) {
            String name = entry.getProfile().getName();
            if (!NAME_PATTERN.matcher(name).matches()) continue;

            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;

            String displayText = displayName.getString().toLowerCase();

            for (String donate : donateSelect.getSelected()) {
                String donateLower = donate.toLowerCase();
                if (displayText.contains(donateLower)) {
                    Set<String> players = parsedPlayers.get(donate);
                    if (players != null) {
                        players.add(name);
                    }
                    break;
                }
            }
        }
    }

    private void switchToNextServer() {
        List<String> servers = versionSelect.isSelected("1.21.4") ? anarchyServers214 : anarchyServers165;

        if (currentServerIndex >= servers.size()) {
            parsing = false;
            int newPlayers = calculateNewPlayers();
            saveToFile();
            ChatMessage.brandmessage("Успешно спарсил все анархии!");
            if (newPlayers > 0) {
                ChatMessage.brandmessage("Было записано " + newPlayers + " новых ников");
            }
            ChatMessage.brandmessage("Чтобы открыть файл с никнеймами введите .tabparser dir");
            setState(false);
            return;
        }

        String nextServer = servers.get(currentServerIndex);
        currentServerIndex++;

        ChatMessage.brandmessage("Переключаюсь на сервер: " + nextServer + " (" + currentServerIndex + "/" + servers.size() + ")");

        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket(nextServer, Instant.now(), 0L, null, new LastSeenMessageList.Acknowledgment(0, new BitSet())));
            waitingForServerLoad = true;
            scanTimer.resetCounter();
        }
    }

    private void saveToFile() {
        File outputFile = getOutputFile();

        try {
            outputFile.getParentFile().mkdirs();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {
                List<String> orderedDonates = Arrays.asList("Герцог", "Князь", "Принц", "Титан", "Элита", "Глава");

                int totalPlayers = 0;

                for (String donate : orderedDonates) {
                    if (parsedPlayers.containsKey(donate)) {
                        Set<String> players = parsedPlayers.get(donate);
                        if (players != null && !players.isEmpty()) {
                            writer.write("==== Аккаунты с донатом " + donate + " ====");
                            writer.newLine();

                            List<String> sortedPlayers = new ArrayList<>(players);
                            Collections.sort(sortedPlayers);

                            for (String player : sortedPlayers) {
                                writer.write(player);
                                writer.newLine();
                            }

                            writer.newLine();
                            totalPlayers += players.size();
                        }
                    }
                }

                writer.flush();

                ChatMessage.brandmessage("Сохранено " + totalPlayers + " игроков в файл");
            }
        } catch (IOException e) {
            ChatMessage.brandmessage("Ошибка при сохранении файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static File getOutputFile() {
        return new File(mc.runDirectory, "tabparser_results.txt");
    }
}
