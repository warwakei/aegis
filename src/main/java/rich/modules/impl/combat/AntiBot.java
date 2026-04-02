package rich.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import com.mojang.authlib.GameProfile;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.util.Hand;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.Instance;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AntiBot extends ModuleStructure {
    @Native(type = Native.Type.VMProtectBeginUltra)
    public static AntiBot getInstance() {
        return Instance.get(AntiBot.class);
    }

    Set<UUID> suspectSet = new HashSet<>();
    static Set<UUID> botSet = new HashSet<>();
    SelectSetting mode = new SelectSetting("Режим", "Выберите режим обнаружения ботов")
            .value("Matrix", "ReallyWorld")
            .selected("ReallyWorld");

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public AntiBot() {
        super("AntiBot", "Anti Bot", ModuleCategory.COMBAT);
        settings(mode);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case PlayerListS2CPacket list -> checkPlayerAfterSpawn(list);
            case PlayerRemoveS2CPacket remove -> removePlayerBecauseLeftServer(remove);
            default -> {}
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!suspectSet.isEmpty()) {
            mc.world.getPlayers().stream()
                    .filter(p -> suspectSet.contains(p.getUuid()))
                    .forEach(this::evaluateSuspectPlayer);
        }
        if (mode.isSelected("Matrix")) {
            matrixMode();
        } else if (mode.isSelected("ReallyWorld")) {
            ReallyWorldMode();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void checkPlayerAfterSpawn(PlayerListS2CPacket listS2CPacket) {
        listS2CPacket.getPlayerAdditionEntries().forEach(entry -> {
            GameProfile profile = entry.profile();
            if (profile == null || isRealPlayer(entry, profile)) {
                return;
            }
            if (isDuplicateProfile(profile)) {
                botSet.add(profile.id());
            } else {
                suspectSet.add(profile.id());
            }
        });
    }

    private void removePlayerBecauseLeftServer(PlayerRemoveS2CPacket removeS2CPacket) {
        removeS2CPacket.profileIds().forEach(uuid -> {
            suspectSet.remove(uuid);
            botSet.remove(uuid);
        });
    }

    private boolean isRealPlayer(PlayerListS2CPacket.Entry entry, GameProfile profile) {
        return entry.latency() < 2 || (profile.properties() != null && !profile.properties().isEmpty());
    }

    private void evaluateSuspectPlayer(PlayerEntity player) {
        List<ItemStack> armor = null;
        if (!isFullyEquipped(player)) {
            armor = getArmorItems(player);
        }
        if (isFullyEquipped(player) || hasArmorChanged(player, armor)) {
            botSet.add(player.getUuid());
        }
        suspectSet.remove(player.getUuid());
    }

    private List<ItemStack> getArmorItems(PlayerEntity entity) {
        List<ItemStack> armorItems = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            armorItems.add(entity.getEquippedStack(slot));
        }
        return armorItems;
    }

    private ItemStack getArmorStack(PlayerEntity entity, int index) {
        if (index >= 0 && index < ARMOR_SLOTS.length) {
            return entity.getEquippedStack(ARMOR_SLOTS[index]);
        }
        return ItemStack.EMPTY;
    }

    private boolean isArmorItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) return false;
        EquipmentSlot slot = equippable.slot();
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST ||
                slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    private void matrixMode() {
        Iterator<UUID> iterator = suspectSet.iterator();
        while (iterator.hasNext()) {
            UUID susPlayer = iterator.next();
            PlayerEntity entity = mc.world.getPlayerByUuid(susPlayer);
            if (entity != null) {
                String playerName = entity.getName().getString();
                boolean isNameBot = playerName.startsWith("CIT-") && !playerName.contains("NPC") && !playerName.contains("[ZNPC]");
                int armorCount = 0;
                for (EquipmentSlot slot : ARMOR_SLOTS) {
                    ItemStack item = entity.getEquippedStack(slot);
                    if (!item.isEmpty()) armorCount++;
                }
                boolean isFullArmor = armorCount == 4;
                boolean isFakeUUID = !entity.getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes()));
                if (isFullArmor || isNameBot || isFakeUUID) {
                    botSet.add(susPlayer);
                }
            }
            iterator.remove();
        }
        if (mc.player.age % 100 == 0) {
            botSet.removeIf(uuid -> mc.world.getPlayerByUuid(uuid) == null);
        }
    }

    private void ReallyWorldMode() {
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (!entity.getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + entity.getName().getString()).getBytes()))
                    && !botSet.contains(entity.getUuid())
                    && !entity.getName().getString().contains("NPC")
                    && !entity.getName().getString().startsWith("[ZNPC]")) {
                botSet.add(entity.getUuid());
            }
        }
    }

    private void newMatrixMode() {
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity != mc.player) {
                List<ItemStack> armorItems = getArmorItems(entity);
                boolean allArmorValid = true;
                for (ItemStack item : armorItems) {
                    if (item.isEmpty() || !item.isEnchantable() || item.getDamage() > 0) {
                        allArmorValid = false;
                        break;
                    }
                }
                boolean hasSpecificArmor = false;
                for (ItemStack item : armorItems) {
                    if (item.getItem() == Items.LEATHER_BOOTS || item.getItem() == Items.LEATHER_LEGGINGS
                            || item.getItem() == Items.LEATHER_CHESTPLATE || item.getItem() == Items.LEATHER_HELMET
                            || item.getItem() == Items.IRON_BOOTS || item.getItem() == Items.IRON_LEGGINGS
                            || item.getItem() == Items.IRON_CHESTPLATE || item.getItem() == Items.IRON_HELMET) {
                        hasSpecificArmor = true;
                        break;
                    }
                }
                if (allArmorValid && hasSpecificArmor
                        && entity.getStackInHand(Hand.OFF_HAND).getItem() == Items.AIR
                        && entity.getStackInHand(Hand.MAIN_HAND).getItem() != Items.AIR
                        && entity.getHungerManager().getFoodLevel() == 20
                        && !entity.getName().getString().contains("NPC")
                        && !entity.getName().getString().startsWith("[ZNPC]")) {
                    botSet.add(entity.getUuid());
                } else {
                    botSet.remove(entity.getUuid());
                }
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    public boolean isDuplicateProfile(GameProfile profile) {
        return Objects.requireNonNull(mc.getNetworkHandler()).getPlayerList().stream()
                .filter(player -> player.getProfile().name().equals(profile.name()) && !player.getProfile().id().equals(profile.id()))
                .count() == 1;
    }

    public boolean isFullyEquipped(PlayerEntity entity) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getEquippedStack(slot);
            if (!isArmorItem(stack) || stack.hasEnchantments()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasArmorChanged(PlayerEntity entity, List<ItemStack> prevArmor) {
        if (prevArmor == null) {
            return true;
        }
        List<ItemStack> currentArmorList = getArmorItems(entity);
        if (currentArmorList.size() != prevArmor.size()) {
            return true;
        }
        for (int i = 0; i < currentArmorList.size(); i++) {
            if (!ItemStack.areEqual(currentArmorList.get(i), prevArmor.get(i))) {
                return true;
            }
        }
        return false;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public boolean isBot(PlayerEntity entity) {
        String playerName = entity.getName().getString();
        boolean isNameBot = playerName.startsWith("CIT-") && !playerName.contains("NPC") && !playerName.startsWith("[ZNPC]");
        boolean isMarkedBot = botSet.contains(entity.getUuid());
        isBotU(entity);
        return isNameBot || isMarkedBot;
    }

    public boolean isBot(UUID uuid) {
        return botSet.contains(uuid);
    }

    public boolean isBotU(Entity entity) {
        return !entity.getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + entity.getName().getString()).getBytes()))
                && entity.isInvisible()
                && !entity.getName().getString().contains("NPC")
                && !entity.getName().getString().startsWith("[ZNPC]");
    }

    public void reset() {
        suspectSet.clear();
        botSet.clear();
    }

    @Override
    public void deactivate() {
        reset();
        super.deactivate();
    }
}