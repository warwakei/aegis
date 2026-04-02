package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.*;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.AuctionUtils;
import rich.screens.clickgui.impl.autobuy.manager.AutoBuyManager;
import rich.util.modules.autobuy.BuyRequest;
import rich.util.modules.autobuy.NetworkManager;
import rich.util.modules.autobuy.ServerManager;
import rich.util.timer.TimerUtil;

import java.util.*;

@Getter
public class AutoBuy extends ModuleStructure {
    private static AutoBuy instance;

    private final SelectSetting mode = new SelectSetting("Режим", "Проверяющий").value("Проверяющий", "Покупающий");
    private final SelectSetting serverType = new SelectSetting("Сервера", "Выкл").value("Выкл", "1.16.5", "1.21.4");
    private final SliderSettings updateDelay = new SliderSettings("Задержка обновления", "").range(300, 1000).setValue(500);
    private final SliderSettings serverSwitchTime = new SliderSettings("Время смены сервера", "").range(30, 120).setValue(30);
    private final BooleanSetting notifications = new BooleanSetting("Уведомления", "").setValue(true);

    private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();
    private final NetworkManager network = new NetworkManager();
    private final ServerManager serverManager = new ServerManager();

    private final TimerUtil updateTimer = TimerUtil.create();
    private final TimerUtil ahOpenTimer = TimerUtil.create();
    private final TimerUtil serverSwitchTimer = TimerUtil.create();

    private boolean inAuction = false;
    private boolean notifiedEnter = false;
    private Set<String> sentItems = new HashSet<>();
    private Set<String> boughtItems = new HashSet<>();
    private volatile boolean pendingUpdate = false;

    public AutoBuy() {
        super("Auto Buy", "Автоматическая покупка на аукционе", ModuleCategory.MISC);
        instance = this;

        serverType.setVisible(() -> mode.isSelected("Покупающий"));
        serverSwitchTime.setVisible(() -> mode.isSelected("Покупающий") && !serverType.isSelected("Выкл"));
        updateDelay.setVisible(() -> mode.isSelected("Покупающий"));

        settings(mode, serverType, updateDelay, serverSwitchTime, notifications);
    }

    public static AutoBuy getInstance() {
        return instance;
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        super.activate();
        autoBuyManager.setEnabled(true);
        reset();

        if (mode.isSelected("Покупающий")) {
            network.startAsServer();
            if (!serverType.isSelected("Выкл")) {
                mc.options.pauseOnLostFocus = false;
            }
        } else {
            network.startAsClient();
        }

        msg("§aМодуль включён. Режим: §b" + mode.getSelected());
        if (mode.isSelected("Покупающий") && !serverType.isSelected("Выкл")) {
            msg("§7Сервера: §b" + serverType.getSelected() + " §7| Смена каждые §b" + (int)serverSwitchTime.getValue() + "с");
        }
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        super.deactivate();
        autoBuyManager.setEnabled(false);
        network.stop();
        serverManager.reset();
        reset();
        msg("§cМодуль выключен");
    }

    private void reset() {
        inAuction = false;
        notifiedEnter = false;
        sentItems.clear();
        boughtItems.clear();
        pendingUpdate = false;
        updateTimer.resetCounter();
        ahOpenTimer.resetCounter();
        serverSwitchTimer.resetCounter();
        serverManager.resetTimers();
    }

    public void sendPauseSync(boolean paused) {
        network.sendPauseState(paused);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (!isState()) return;

        handlePauseSync();

        if (!autoBuyManager.isEnabled()) return;

        if (mode.isSelected("Проверяющий")) {
            handleServerSwitchCommand();
            handleUpdateCommand();
        }

        if (mode.isSelected("Покупающий") && !serverType.isSelected("Выкл")) {
            handleServerLogic();
        }

        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            handleNotInScreen();
            return;
        }

        String title = screen.getTitle().getString().toLowerCase();
        int slots = screen.getScreenHandler().slots.size();

        if (isSuspiciousPriceScreen(title, slots)) {
            confirmSuspiciousPrice(screen);
            return;
        }

        if (!title.contains("аукцион") && !title.contains("поиск")) {
            handleNotInAuction();
            return;
        }

        handleInAuction(screen);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void handlePauseSync() {
        Boolean pauseState = network.pollPauseState();
        if (pauseState != null) {
            autoBuyManager.setEnabledSilent(!pauseState);
            if (pauseState) {
                msg("§e[СИНХРО] Пауза включена");
            } else {
                msg("§a[СИНХРО] Пауза выключена");
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void handleServerSwitchCommand() {
        String switchCmd = network.pollServerSwitch();
        if (switchCmd != null) {
            msg("§e[ПРОВЕРЯЮЩИЙ] Переключаюсь на сервер: " + switchCmd);
            mc.player.networkHandler.sendChatCommand(switchCmd.substring(1));
            serverManager.setWaitingForServerLoad(true);
            inAuction = false;
            notifiedEnter = false;
            sentItems.clear();
        }
    }

    private void handleUpdateCommand() {
        if (network.pollUpdateCommand()) {
            pendingUpdate = true;
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleServerLogic() {
        serverManager.updateHubStatus(mc.world);

        if (serverManager.shouldJoinAnarchy(serverType.getSelected())) {
            serverManager.joinAnarchyFromHub(mc.player, serverType.getSelected());
            msg("§e[ПОКУПАТЕЛЬ] Захожу на анархию...");
        }

        if (serverManager.isWaitingForServerLoad()) {
            if (!serverManager.isInHub()) {
                serverManager.setWaitingForServerLoad(false);
                serverSwitchTimer.resetCounter();
                ahOpenTimer.resetCounter();
                msg("§a[ПОКУПАТЕЛЬ] Загрузился на сервер");
            }
        }

        long switchInterval = (long) (serverSwitchTime.getValue() * 1000);
        if (!serverManager.isInHub() && serverSwitchTimer.hasTimeElapsed(switchInterval)) {
            serverManager.switchToNextServer(mc.player, network, serverType.getSelected());
            serverSwitchTimer.resetCounter();
            inAuction = false;
            sentItems.clear();
            boughtItems.clear();
        }
    }

    private void handleNotInScreen() {
        if (inAuction) {
            inAuction = false;
            sentItems.clear();
            boughtItems.clear();
            if (mode.isSelected("Проверяющий") && notifiedEnter) {
                network.sendLeaveAuction();
                notifiedEnter = false;
            }
        }

        if (ahOpenTimer.hasTimeElapsed(11000)) {
            mc.player.networkHandler.sendChatCommand("ah");
            ahOpenTimer.resetCounter();
        }
    }

    private void handleNotInAuction() {
        if (inAuction) {
            inAuction = false;
            sentItems.clear();
            boughtItems.clear();
            if (mode.isSelected("Проверяющий") && notifiedEnter) {
                network.sendLeaveAuction();
                notifiedEnter = false;
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleInAuction(GenericContainerScreen screen) {
        if (!inAuction) {
            inAuction = true;
            sentItems.clear();
            boughtItems.clear();
            msg("§aВ аукционе");

            if (mode.isSelected("Проверяющий") && !notifiedEnter) {
                network.sendEnterAuction();
                notifiedEnter = true;
            }
        }

        if (mode.isSelected("Покупающий")) {
            processBuyRequestsInstant(screen);

            if (updateTimer.hasTimeElapsed((long) updateDelay.getValue() - 200)) {
                int clientsInAuction = network.getClientsInAuctionCount();
                if (clientsInAuction > 0) {
                    network.sendUpdateCommand();
                }
                updateAuction(screen);
                updateTimer.resetCounter();
            }
        } else {
            if (pendingUpdate) {
                updateAuction(screen);
                pendingUpdate = false;
            }
            scanAndSendInstant(screen);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isSuspiciousPriceScreen(String title, int slots) {
        if (title.contains("подозрительн")) return true;
        if (title.contains("подтвер")) return true;
        if (title.contains("suspicious")) return true;
        if (title.contains("confirm")) return true;

        if (slots == 63 || slots == 36) {
            if (!title.contains("аукцион") && !title.contains("поиск") && !title.contains("инвентарь")) {
                return true;
            }
        }

        return false;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void confirmSuspiciousPrice(GenericContainerScreen screen) {
        int syncId = screen.getScreenHandler().syncId;
        mc.interactionManager.clickSlot(syncId, 1, 0, SlotActionType.PICKUP, mc.player);
        msg("§a✓ Подтвердил покупку");
    }

    private void updateAuction(GenericContainerScreen screen) {
        int syncId = screen.getScreenHandler().syncId;
        mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
    }

    private String generateLoreHash(ItemStack stack) {
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return "nolore";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Text line : lore.lines()) {
            if (count >= 3) break;
            String text = line.getString();
            if (!text.contains("$") && !text.contains("Прoдaвeц") && !text.contains("Истeкaeт") && !text.contains("Нажмите")) {
                sb.append(text.hashCode());
                count++;
            }
        }
        return sb.toString();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void scanAndSendInstant(GenericContainerScreen screen) {
        if (!network.isConnected()) return;

        List<AutoBuyableItem> items = autoBuyManager.getEnabledItems();
        if (items.isEmpty()) return;

        for (int i = 0; i < 45 && i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            if (AuctionUtils.isArmorItem(stack) && AuctionUtils.hasThornsEnchantment(stack)) continue;

            int price = AuctionUtils.getPrice(stack);
            if (price <= 0) continue;

            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            String loreHash = generateLoreHash(stack);
            String key = itemId + "|" + price + "|" + stack.getCount() + "|" + loreHash;

            if (sentItems.contains(key)) continue;

            processItemForSending(stack, items, key, itemId, price, loreHash);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void processItemForSending(ItemStack stack, List<AutoBuyableItem> items, String key, String itemId, int price, String loreHash) {
        for (AutoBuyableItem item : items) {
            int maxPrice = item.getSettings().getBuyBelow();
            int minQuantity = item.getSettings().isCanHaveQuantity() ? item.getSettings().getMinQuantity() : 1;

            if (price > maxPrice) continue;

            if (item.getSettings().isCanHaveQuantity()) {
                if (stack.getCount() < minQuantity) continue;
            }

            if (AuctionUtils.compareItem(stack, item.createItemStack())) {
                sentItems.add(key);
                String displayName = item.getDisplayName();
                network.sendBuyCommand(price, itemId, displayName, stack.getCount(), loreHash, maxPrice, minQuantity);
                break;
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processBuyRequestsInstant(GenericContainerScreen screen) {
        int syncId = screen.getScreenHandler().syncId;

        BuyRequest request;
        while ((request = network.pollBuyRequest()) != null) {
            String buyKey = request.itemId + "|" + request.price + "|" + request.count + "|" + request.loreHash;
            if (boughtItems.contains(buyKey)) {
                continue;
            }

            boolean found = tryExactMatch(screen, syncId, request, buyKey);

            if (!found) {
                tryFallbackMatch(screen, syncId, request, buyKey);
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean tryExactMatch(GenericContainerScreen screen, int syncId, BuyRequest request, String buyKey) {
        for (int i = 0; i < 45 && i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            String stackItemId = Registries.ITEM.getId(stack.getItem()).toString();
            if (!stackItemId.equals(request.itemId)) continue;

            int stackPrice = AuctionUtils.getPrice(stack);
            if (stackPrice != request.price) continue;

            if (stack.getCount() != request.count) continue;

            String stackLoreHash = generateLoreHash(stack);
            if (!stackLoreHash.equals(request.loreHash)) continue;

            if (AuctionUtils.isArmorItem(stack) && AuctionUtils.hasThornsEnchantment(stack)) continue;

            mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
            boughtItems.add(buyKey);
            msg("§a⚡ КУПИЛ: §f" + request.displayName + " §aза " + stackPrice + "$");
            return true;
        }
        return false;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean tryFallbackMatch(GenericContainerScreen screen, int syncId, BuyRequest request, String buyKey) {
        for (int i = 0; i < 45 && i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            String stackItemId = Registries.ITEM.getId(stack.getItem()).toString();
            if (!stackItemId.equals(request.itemId)) continue;

            int stackPrice = AuctionUtils.getPrice(stack);
            if (stackPrice <= 0 || stackPrice > request.maxPrice) continue;

            if (stack.getCount() < request.minQuantity) continue;

            if (AuctionUtils.isArmorItem(stack) && AuctionUtils.hasThornsEnchantment(stack)) continue;

            String fallbackKey = stackItemId + "|" + stackPrice + "|" + stack.getCount() + "|" + generateLoreHash(stack);
            if (boughtItems.contains(fallbackKey)) continue;

            mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
            boughtItems.add(fallbackKey);
            boughtItems.add(buyKey);
            msg("§a⚡ КУПИЛ: §f" + request.displayName + " §aза " + stackPrice + "$");
            return true;
        }
        return false;
    }

    private void msg(String text) {
        if (notifications.isValue() && mc.player != null) {
        }
    }

    public NetworkManager getNetworkManager() {
        return network;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public boolean isFullyEnabled() {
        return isState() && autoBuyManager.isEnabled();
    }
}