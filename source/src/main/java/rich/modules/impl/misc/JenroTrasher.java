package rich.modules.impl.misc;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.TextSetting;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class JenroTrasher extends ModuleStructure {
    private final BooleanSetting compass = new BooleanSetting("Компас", "Выбрасывать обычный компас без привязки").setValue(true);
    private final BooleanSetting book = new BooleanSetting("Книга", "Выбрасывать подписанную книгу").setValue(true);
    private final TextSetting extra = new TextSetting("Другой мусор", "ID предметов через запятую, например minecraft:rotten_flesh")
            .setText("").setMax(256);
    private final TextSetting servers = new TextSetting("Серверы Jenro", "Домены серверов через запятую; поддомены учитываются")
            .setText("funnymc.su").setMax(256);

    private ClientPlayerEntity previousPlayer;
    private int delay;
    private int nextSlot;
    private String parsedText;
    private Set<Identifier> extraItems = Set.of();

    public JenroTrasher() {
        super("Jenro Trasher", "Выбрасывает выбранный мусор из инвентаря на Jenro", ModuleCategory.JENRO);
        settings(compass, book, extra, servers);
    }

    @Override public void deactivate() { previousPlayer = null; }

    @EventHandler
    public void onTick(TickEvent event) {
        var player = mc.player;
        if (player != previousPlayer) {
            previousPlayer = player;
            delay = 20;
            nextSlot = 0;
        }
        if (player == null || mc.interactionManager == null || mc.currentScreen != null
                || player.currentScreenHandler != player.playerScreenHandler || !isJenro()) return;
        if (delay > 0) {
            delay--;
            return;
        }

        updateExtraItems();
        var handler = player.playerScreenHandler;
        if (!handler.getCursorStack().isEmpty()) return;
        int size = handler.slots.size();
        for (int i = 0; i < size; i++) {
            Slot slot = handler.slots.get((nextSlot + i) % size);
            if (slot.inventory != player.getInventory() || slot.getIndex() < 0 || slot.getIndex() >= 36
                    || !slot.canTakeItems(player) || !isTrash(slot.getStack())) continue;
            nextSlot = (slot.id + 1) % size;
            delay = 7;
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 1, SlotActionType.THROW, player);
            return;
        }
        delay = 4;
    }

    private boolean isJenro() {
        var entry = mc.getCurrentServerEntry();
        if (entry == null || servers.getText() == null) return false;
        String host = ServerAddress.parse(entry.address).getAddress().toLowerCase(Locale.ROOT);
        for (String configured : servers.getText().split(",")) {
            String domain = configured.trim().toLowerCase(Locale.ROOT);
            if (!domain.isEmpty() && (host.equals(domain) || host.endsWith("." + domain))) return true;
        }
        return false;
    }

    private boolean isTrash(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (compass.isValue() && stack.isOf(Items.COMPASS) && !stack.contains(DataComponentTypes.LODESTONE_TRACKER)) return true;
        if (book.isValue() && stack.isOf(Items.WRITTEN_BOOK)) return true;
        return extraItems.contains(Registries.ITEM.getId(stack.getItem()));
    }

    private void updateExtraItems() {
        String text = extra.getText();
        if (Objects.equals(text, parsedText)) return;
        parsedText = text;
        Set<Identifier> ids = new HashSet<>();
        if (text != null) {
            for (String value : text.split(",")) {
                String candidate = value.trim();
                if (candidate.isEmpty()) continue;
                Identifier id = Identifier.tryParse(candidate);
                if (id != null) ids.add(id);
            }
        }
        extraItems = Set.copyOf(ids);
    }
}
