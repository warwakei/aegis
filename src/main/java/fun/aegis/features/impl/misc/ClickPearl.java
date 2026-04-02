package fun.aegis.features.impl.misc;

import antidaunleak.api.annotation.Native;
import fun.aegis.events.player.TickEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BindSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.utils.client.chat.ChatMessage;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.interactions.inv.InventoryResult;
import fun.aegis.utils.interactions.inv.InventoryToolkit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class ClickPearl extends Module {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private final SelectSetting modeSetting = new SelectSetting("Режим", "Способ броска")
            .value("Default", "Legit")
            .selected("Default");
    private final BindSetting keySetting = new BindSetting("Кнопка", "Кнопка для использования");

    private boolean prevKeyPressed = false;
    private long lastThrowTime = 0L;
    private int packetSequence = 0;

    public ClickPearl() {
        super("ClickPearl", "Click Pearl", ModuleCategory.MISC);
        setup(modeSetting, keySetting);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (MC.player == null || MC.world == null) return;

        boolean keyDown = isBindActive();
        if (!prevKeyPressed && keyDown && System.currentTimeMillis() - lastThrowTime > 100) {
            lastThrowTime = System.currentTimeMillis();
            
            if (modeSetting.getSelected().equals("Default")) {
                executeDefault();
            } else {
                executeLegit();
            }
        }
        prevKeyPressed = keyDown;
    }

    private void executeDefault() {
        // Default: пакетный бросок из любого места инвентаря
        int savedSlot = MC.player.getInventory().selectedSlot;
        
        // Сначала ищем в хотбаре
        InventoryResult hotbar = InventoryToolkit.findItemInHotBar(Items.ENDER_PEARL);
        if (hotbar.found()) {
            // Пакетно переключаем слот, бросаем, возвращаем
            MC.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbar.slot()));
            MC.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, packetSequence++, MC.player.getYaw(), MC.player.getPitch()));
            MC.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(savedSlot));
            MC.player.swingHand(Hand.MAIN_HAND);
            return;
        }
        
        // Ищем в инвентаре
        InventoryResult inv = InventoryToolkit.findItemInInventory(Items.ENDER_PEARL);
        if (inv.found()) {
            // Свапаем из инвентаря в текущий слот хотбара
            InventoryToolkit.clickSlot(inv.slot(), savedSlot, SlotActionType.SWAP);
            // Бросаем пакетом
            MC.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, packetSequence++, MC.player.getYaw(), MC.player.getPitch()));
            MC.player.swingHand(Hand.MAIN_HAND);
            // Свапаем обратно
            InventoryToolkit.clickSlot(inv.slot(), savedSlot, SlotActionType.SWAP);
        } else {
            ChatMessage.brandmessage("Нету жемчуга");
        }
    }

    private void executeLegit() {
        // Legit: перл должен быть в хотбаре, моментально берём, бросаем, возвращаем слот
        int savedSlot = MC.player.getInventory().selectedSlot;
        
        InventoryResult hotbar = InventoryToolkit.findItemInHotBar(Items.ENDER_PEARL);
        if (hotbar.found()) {
            // Моментально переключаем на слот с перлом
            MC.player.getInventory().selectedSlot = hotbar.slot();
            MC.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbar.slot()));
            
            // Бросаем
            MC.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, packetSequence++, MC.player.getYaw(), MC.player.getPitch()));
            MC.player.swingHand(Hand.MAIN_HAND);
            
            // Моментально возвращаем слот обратно
            MC.player.getInventory().selectedSlot = savedSlot;
            MC.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(savedSlot));
        } else {
            ChatMessage.brandmessage("Жемчуг должен быть в хотбаре");
        }
    }

    private boolean isBindActive() {
        long window = MC.getWindow().getHandle();
        int key = keySetting.getKey();

        if (key >= GLFW.GLFW_MOUSE_BUTTON_1 && key <= GLFW.GLFW_MOUSE_BUTTON_8) {
            return GLFW.glfwGetMouseButton(window, key) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(window, key);
    }
}
