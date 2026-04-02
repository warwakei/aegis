package fun.aegis.features.impl.combat;

import antidaunleak.api.annotation.Native;
import com.mojang.authlib.GameProfile;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.util.Hand;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.Instance;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.events.player.TickEvent;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AntiBot extends Module {
    public static AntiBot getInstance() {
        return Instance.get(AntiBot.class);
    }

    Set<UUID> suspectSet = new HashSet<>();
    static Set<UUID> botSet = new HashSet<>();
    SelectSetting mode = new SelectSetting("Режим", "Выберите режим обнаружения ботов")
            .value("Matrix", "ReallyWorld")
            .selected("ReallyWorld");

    public AntiBot() {
        super("AntiBot", "Anti Bot", ModuleCategory.COMBAT);
        setup(mode);
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
    @Native(type = Native.Type.VMProtectBeginUltra)
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

    private void checkPlayerAfterSpawn(PlayerListS2CPacket listS2CPacket) {
        listS2CPacket.getPlayerAdditionEntries().forEach(entry -> {
            GameProfile profile = entry.profile();
            if (profile == null || isRealPlayer(entry, profile)) {
                return;
            }
            if (isDuplicateProfile(profile)) {
                botSet.add(profile.getId());
            } else {
                suspectSet.add(profile.getId());
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
        return entry.latency() < 2 || (profile.getProperties() != null && !profile.getProperties().isEmpty());
    }

    private void evaluateSuspectPlayer(PlayerEntity player) {
        Iterable<ItemStack> armor = null;
        if (!isFullyEquipped(player)) {
            armor = player.getArmorItems();
        }
        if (isFullyEquipped(player) || hasArmorChanged(player, armor)) {
            botSet.add(player.getUuid());
        }
        suspectSet.remove(player.getUuid());
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
                for (ItemStack item : entity.getArmorItems()) {
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
                List<ItemStack> armorItems = StreamSupport.stream(entity.getArmorItems().spliterator(), false).toList();
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

    public boolean isDuplicateProfile(GameProfile profile) {
        return Objects.requireNonNull(mc.getNetworkHandler()).getPlayerList().stream()
                .filter(player -> player.getProfile().getName().equals(profile.getName()) && !player.getProfile().getId().equals(profile.getId()))
                .count() == 1;
    }

    public boolean isFullyEquipped(PlayerEntity entity) {
        return IntStream.rangeClosed(0, 3)
                .mapToObj(entity.getInventory()::getArmorStack)
                .allMatch(stack -> stack.getItem() instanceof ArmorItem && !stack.hasEnchantments());
    }

    public boolean hasArmorChanged(PlayerEntity entity, Iterable<ItemStack> prevArmor) {
        if (prevArmor == null) {
            return true;
        }
        List<ItemStack> currentArmorList = StreamSupport.stream(entity.getArmorItems().spliterator(), false).toList();
        List<ItemStack> prevArmorList = StreamSupport.stream(prevArmor.spliterator(), false).toList();
        return !IntStream.range(0, Math.min(currentArmorList.size(), prevArmorList.size()))
                .allMatch(i -> currentArmorList.get(i).equals(prevArmorList.get(i)))
                || currentArmorList.size() != prevArmorList.size();
    }

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
