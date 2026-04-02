package fun.aegis.features.impl.misc;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.aegis.utils.features.aura.point.MultiPoint;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.TurnsConfig;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BindSetting;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.display.hud.CoolDowns;
import fun.aegis.display.hud.Notifications;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.event.types.EventType;
import fun.aegis.utils.features.aura.rotations.constructor.LinearConstructor;
import fun.aegis.features.impl.render.ProjectilePrediction;
import fun.aegis.common.repository.friend.FriendUtils;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.color.GradientAssist;
import fun.aegis.utils.interactions.inv.InventoryFlowManager;
import fun.aegis.utils.interactions.simulate.PlayerSimulation;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.math.projection.Projection;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.math.time.StopWatch;
import fun.aegis.utils.client.chat.StringHelper;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.utils.display.geometry.Render3D;
import fun.aegis.utils.math.task.TaskPriority;
import fun.aegis.utils.math.script.Script;
import fun.aegis.utils.client.packet.network.Network;
import fun.aegis.events.container.SetScreenEvent;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.events.player.RotationUpdateEvent;
import fun.aegis.events.render.DrawEvent;
import fun.aegis.events.render.WorldRenderEvent;
import java.util.*;
import java.util.stream.StreamSupport;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerHelper extends Module {

    public static ServerHelper getInstance() {
        return Instance.get(ServerHelper.class);
    }

    Map<BlockPos, BlockState> blockStateMap = new HashMap<>();
    List<ServerEvent> serverEvents = new ArrayList<>();
    List<Structure> structures = new ArrayList<>();
    List<KeyBind> keyBindings = new ArrayList<>();
    MultiPoint pointFinder = new MultiPoint();
    StopWatch itemsWatch = new StopWatch();
    StopWatch shulkerWatch = new StopWatch();
    StopWatch repairWatch = new StopWatch();
    Script script = new Script();
    Script script2 = new Script();
    @NonFinal UUID entityUUID;
    Map<Integer, Item> stacks = new HashMap<>();
    SelectSetting mode = new SelectSetting("Тип сервера", "Позволяет выбрать тип сервера")
            .value("ReallyWorld", "HolyWorld", "FunTime")
            .selected("FunTime");
    BooleanSetting autoLootSetting = new BooleanSetting("Авто лут", "Кража лута с ботов на ивенте")
            .setValue(true)
            .visible(() -> mode.isSelected("HolyWorld"));
    BooleanSetting autoShulkerSetting = new BooleanSetting("Авто шалкер", "Автоматически кладет лут в шалкер")
            .setValue(true)
            .visible(() -> mode.isSelected("HolyWorld"));
    BooleanSetting autoRepairSetting = new BooleanSetting("Авто ремонт", "Авто ремонтирует броню пузырем опыта при низкой прочности")
            .setValue(true)
            .visible(() -> mode.isSelected("HolyWorld"));
    BooleanSetting consumablesSetting = new BooleanSetting("Таймер расходников", "Отображает время до окончания расходников")
            .setValue(true)
            .visible(() -> mode.isSelected("FunTime"));
    BooleanSetting autoPointSetting = new BooleanSetting("Авто поинт", "Отображает информацию об ивенте")
            .setValue(true)
            .visible(() -> mode.isSelected("FunTime"));
    List<java.lang.String> potionQueue = new ArrayList<>();
    StopWatch potionTimer = new StopWatch();
    Map<java.lang.String, ItemInfo> itemConfig = new HashMap<>();
    Map<java.lang.String, Boolean> itemStates = new HashMap<>();
    Map<java.lang.String, Boolean> lastKeyStates = new HashMap<>();
    Map<java.lang.String, Boolean> keyPressedThisTick = new HashMap<>();
    @NonFinal int originalSlot = -1;
    @NonFinal int targetSlot = -1;
    @NonFinal ActionState actionState = ActionState.IDLE;
    @NonFinal long actionTimer = 0;
    @NonFinal java.lang.String pendingItemKey = null;
    @NonFinal long stopMovementUntil = 0;
    @NonFinal boolean keysOverridden = false;
    @NonFinal boolean wasForwardPressed, wasBackPressed, wasLeftPressed, wasRightPressed;
    @NonFinal int originalSourceSlot = -1;

    private enum ActionState {
        IDLE, SLOWING_DOWN, WAITING_STOP, SWAP_TO_ITEM, USE_ITEM, SWAP_BACK, SPEEDING_UP
    }

    private static class ItemInfo {
        java.lang.String searchName;
        Item item;
        java.lang.String displayName;

        ItemInfo(java.lang.String searchName, Item item, java.lang.String displayName) {
            this.searchName = searchName;
            this.item = item;
            this.displayName = displayName;
        }
    }

    public ServerHelper() {
        super("Server Assist", "Server Assist", ModuleCategory.MISC);
        initialize();
    }

    public void initialize() {
        setup(mode, autoLootSetting, consumablesSetting, autoPointSetting, autoShulkerSetting, autoRepairSetting);
        keyBindings.add(new KeyBind(Items.FIREWORK_STAR, new BindSetting("Анти полет", "Клавиша анти полета")
                .visible(() -> mode.isSelected("ReallyWorld")), 0));
        keyBindings.add(new KeyBind(Items.FLOWER_BANNER_PATTERN, new BindSetting("Свиток опыта", "Клавиша свитка опыта")
                .visible(() -> mode.isSelected("ReallyWorld")), 0));
        keyBindings.add(new KeyBind(Items.PRISMARINE_SHARD, new BindSetting("Взрывная трапка", "Клавиша взрывной трапки")
                .visible(() -> mode.isSelected("HolyWorld")), 5));
        keyBindings.add(new KeyBind(Items.POPPED_CHORUS_FRUIT, new BindSetting("Обычная трапка", "Клавиша обычной трапки")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.NETHER_STAR, new BindSetting("Стан", "Клавиша стана")
                .visible(() -> mode.isSelected("HolyWorld")), 30));
        keyBindings.add(new KeyBind(Items.FIRE_CHARGE, new BindSetting("Взрывная штучка", "Клавиша взрывной штучки")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.SNOWBALL, new BindSetting("Снежок заморозка", "Клавиша снежка")
                .visible(() -> mode.isSelected("HolyWorld") || mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.PHANTOM_MEMBRANE, new BindSetting("Божья аура", "Клавиша божьей ауры")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.NETHERITE_SCRAP, new BindSetting("Трапка", "Клавиша трапки")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.DRIED_KELP, new BindSetting("Пласт", "Клавиша пласта")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SUGAR, new BindSetting("Явная пыль", "Клавиша явной пыли")
                .visible(() -> mode.isSelected("FunTime")), 10));
        keyBindings.add(new KeyBind(Items.FIRE_CHARGE, new BindSetting("Огненный смерч", "Клавиша огненного смерча")
                .visible(() -> mode.isSelected("FunTime")), 10));
        keyBindings.add(new KeyBind(Items.ENDER_EYE, new BindSetting("Дезориентация", "Клавиша дезориентации")
                .visible(() -> mode.isSelected("FunTime")), 10));
        keyBindings.add(new KeyBind(Items.JACK_O_LANTERN, new BindSetting("Светильник Джека", "Клавиша светильника Джека")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.EXPERIENCE_BOTTLE, new BindSetting("Пузырь опыта", "Клавиша пузыря опыта")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.PINK_SHULKER_BOX, new BindSetting("Рюкзак 1 уровня", "Клавиша рюкзака 1 уровня")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.BLUE_SHULKER_BOX, new BindSetting("Рюкзак 2 уровня", "Клавиша рюкзака 2 уровня")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.RED_SHULKER_BOX, new BindSetting("Рюкзак 3 уровня", "Клавиша рюкзака 3 уровня")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.PINK_SHULKER_BOX, new BindSetting("Рюкзак 4 уровня", "Клавиша рюкзака 4 уровня")
                .visible(() -> mode.isSelected("HolyWorld")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье отрыжки", "Клавиша зелья отрыжки")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье серной кислоты", "Клавиша зелья серной кислоты")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье вспышки", "Клавиша зелья вспышки")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье мочи Флеша", "Клавиша зелья мочи Флеша")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье победителя", "Клавиша зелья победителя")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье агента", "Клавиша зелья агента")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье медика", "Клавиша зелья медика")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Зелье киллера", "Клавиша зелья киллера")
                .visible(() -> mode.isSelected("FunTime")), 0));
        keyBindings.forEach(bind -> setup(bind.setting));
        itemConfig.put("disorientation", new ItemInfo("дезориентация", Items.ENDER_EYE, "Дезориентация"));
        itemConfig.put("sugar", new ItemInfo("явная", Items.SUGAR, "Явная пыль"));
        itemConfig.put("bojaura", new ItemInfo("божья аура", Items.PHANTOM_MEMBRANE, "Божья аура"));
        itemConfig.put("snow", new ItemInfo("Снежок заморозка", Items.SNOWBALL, "Снежок заморозка"));
        itemConfig.put("plast", new ItemInfo("пласт", Items.DRIED_KELP, "Пласт"));
        itemConfig.put("trap", new ItemInfo("трапка", Items.NETHERITE_SCRAP, "Трапка"));
        itemConfig.put("fireSwirl", new ItemInfo("огненный смерч", Items.FIRE_CHARGE, "Огненный смерч"));
        itemConfig.put("otriga", new ItemInfo("отрыжки", Items.SPLASH_POTION, "Зелье отрыжки"));
        itemConfig.put("serka", new ItemInfo("серная", Items.SPLASH_POTION, "Зелье серной кислоты"));
        itemConfig.put("vspihka", new ItemInfo("вспышка", Items.SPLASH_POTION, "Зелье вспышки"));
        itemConfig.put("mochaflesha", new ItemInfo("моча флеша", Items.SPLASH_POTION, "Зелье мочи Флеша"));
        itemConfig.put("pobedilka", new ItemInfo("победителя", Items.SPLASH_POTION, "Зелье победителя"));
        itemConfig.put("agent", new ItemInfo("агента", Items.SPLASH_POTION, "Зелье агента"));
        itemConfig.put("medik", new ItemInfo("медика", Items.SPLASH_POTION, "Зелье медика"));
        itemConfig.put("killer", new ItemInfo("киллера", Items.SPLASH_POTION, "Зелье киллера"));
        itemConfig.put("antiflight", new ItemInfo("анти полет", Items.FIREWORK_STAR, "Анти полет"));
        itemConfig.put("expscroll", new ItemInfo("свиток опыта", Items.FLOWER_BANNER_PATTERN, "Свиток опыта"));
        itemConfig.put("dtrap", new ItemInfo("взрывная трапка", Items.PRISMARINE_SHARD, "Взрывная трапка"));
        itemConfig.put("trap_holy", new ItemInfo("трапка", Items.POPPED_CHORUS_FRUIT, "Обычная трапка"));
        itemConfig.put("stan", new ItemInfo("стан", Items.NETHER_STAR, "Стан"));
        itemConfig.put("ditem", new ItemInfo("взрывная штучка", Items.FIRE_CHARGE, "Взрывная штучка"));
        itemConfig.put("tikva", new ItemInfo("светильник джейка", Items.JACK_O_LANTERN, "Светильник Джека"));
        itemConfig.put("exp", new ItemInfo("пузырь опыта", Items.EXPERIENCE_BOTTLE, "Пузырь опыта"));
        itemConfig.put("shulker1", new ItemInfo("рюкзак (i уровень)", Items.PINK_SHULKER_BOX, "Рюкзак 1 уровня"));
        itemConfig.put("shulker2", new ItemInfo("рюкзак (ii уровень)", Items.BLUE_SHULKER_BOX, "Рюкзак 2 уровня"));
        itemConfig.put("shulker3", new ItemInfo("рюкзак (iii уровень)", Items.RED_SHULKER_BOX, "Рюкзак 3 уровня"));
        itemConfig.put("shulker4", new ItemInfo("рюкзак (iv уровень)", Items.PINK_SHULKER_BOX, "Рюкзак 4 уровня"));
        itemConfig.keySet().forEach(key -> {
            itemStates.put(key, false);
            lastKeyStates.put(key, false);
            keyPressedThisTick.put(key, false);
        });
    }

    @Override
    public void activate() {
        script2.cleanup();
        stacks.clear();
        potionQueue.clear();
        potionTimer.reset();
        itemStates.replaceAll((k, v) -> false);
        lastKeyStates.replaceAll((k, v) -> false);
        keyPressedThisTick.replaceAll((k, v) -> false);
        actionState = ActionState.IDLE;
        originalSlot = -1;
        targetSlot = -1;
        originalSourceSlot = -1;
        pendingItemKey = null;
        stopMovementUntil = 0;
        keysOverridden = false;
    }

    @Override
    public void deactivate() {
        itemStates.replaceAll((k, v) -> false);
        lastKeyStates.replaceAll((k, v) -> false);
        keyPressedThisTick.replaceAll((k, v) -> false);
        potionQueue.clear();
        potionTimer.reset();
        actionState = ActionState.IDLE;
        originalSlot = -1;
        targetSlot = -1;
        originalSourceSlot = -1;
        pendingItemKey = null;
        stopMovementUntil = 0;
        if (keysOverridden) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        }
        keysOverridden = false;
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!PlayerInteractionHelper.nullCheck()) {
            switch (e.getPacket()) {
                case ItemPickupAnimationS2CPacket item when autoShulkerSetting.isValue() && autoShulkerSetting.isVisible() && item.getCollectorEntityId() == mc.player.getId() && mc.world.getEntityById(item.getEntityId()) instanceof ItemEntity entity -> {
                    ItemStack stack = entity.getStack();
                    if (stack.get(DataComponentTypes.CONTAINER) == null) {
                        stacks.put(-Calculate.getRandom(1, 999999999), stack.getItem());
                        shulkerWatch.reset();
                    }
                }
                case ScreenHandlerSlotUpdateS2CPacket slot -> {
                    if (slot.getSyncId() == 0) {
                        Item item = slot.getStack().getItem();
                        stacks.entrySet().stream()
                                .filter(entry -> entry.getKey() < 0 && entry.getValue().equals(item))
                                .findFirst()
                                .ifPresent(entry -> {
                                    stacks.put(slot.getSlot() + 18, item);
                                    stacks.remove(entry.getKey());
                                });
                    }
                }
                case ChunkDeltaUpdateS2CPacket chunkDelta when consumablesSetting.isValue() && consumablesSetting.isVisible() -> {
                    chunkDelta.visitUpdates((pos, state) -> blockStateMap.put(pos.add(0, 0, 0), state));
                    script.addTickStep(0, () -> chunkDelta.visitUpdates((pos, state) -> {
                        Vec3d vec = pos.add(0, 0, 0).toCenterPos();
                        if (blockStateMap.size() > 50 && blockStateMap.size() < 600) {
                            if (isTrap(pos.up(2))) {
                                addStructure(Items.NETHERITE_SCRAP, vec, System.currentTimeMillis() + 15000);
                            } else if (isBigTrap(pos.up(3))) {
                                addStructure(Items.NETHERITE_SCRAP, vec, System.currentTimeMillis() + 30000);
                            }
                        }
                    }));
                }
                case GameMessageS2CPacket gameMessage when autoPointSetting.isValue() && autoPointSetting.isVisible() -> {
                    Text content = gameMessage.content();
                    java.lang.String contentString = content.toString();
                    java.lang.String message = content.getString();
                    java.lang.String name = StringUtils.substringBetween(message, "||| [", "] ");
                    if (name != null) {
                        java.lang.String position = StringUtils.substringBetween(contentString, "value='/gps ", "'");
                        java.lang.String lvl = StringUtils.substringBetween(message, "Уровень лута: ", "\n ║");
                        java.lang.String owner = StringUtils.substringBetween(message, "Призван игроком: ", "\n ║");
                        if (position != null) {
                            java.lang.String[] pose = position.split(" ");
                            Vec3d center = BlockPos.ofFloored(Integer.parseInt(pose[0]), Integer.parseInt(pose[1]), Integer.parseInt(pose[2])).toCenterPos();
                            switch (name) {
                                case "Мистический сундук" -> addEvent(name, lvl, owner, center, "overworld", 300, 0);
                                case "Вулкан" -> addEvent(name, lvl, owner, center, "overworld", 300, 120);
                                case "Метеоритный дождь", "Маяк убийца", "Мистический Алтарь" -> addEvent(name, lvl, owner, center, "overworld", 360, 0);
                                case "Загадочный маяк" -> addEvent(name, lvl, owner, center, "overworld", 60, 180);
                            }
                        } else {
                            switch (name) {
                                case "Сундук смерти" -> addEvent(name, lvl, owner, BlockPos.ofFloored(-155, 64, 205).toCenterPos(), "lobby", 300, 0);
                                case "Адская резня" -> addEvent(name, lvl, owner, BlockPos.ofFloored(48, 87, 73).toCenterPos(), "lobby", 180, 120);
                            }
                        }
                    }
                }
                case GameMessageS2CPacket gameMessage -> {
                    java.lang.String message = gameMessage.content().getString();
                    if (message.contains("▶ Повторно активировать Пузырь опыта возможно через")) {
                        java.lang.String subString = StringUtils.substringBetween(message, "через ", " секунд");
                        if (subString != null && !subString.isEmpty()) {
                            int duration = Integer.parseInt(subString) * 20;
                            ItemCooldownManager manager = mc.player.getItemCooldownManager();
                            manager.set(Items.EXPERIENCE_BOTTLE.getDefaultStack(), duration);
                            CoolDowns.getInstance().packet(new PacketEvent(new CooldownUpdateS2CPacket(manager.getGroup(Items.EXPERIENCE_BOTTLE.getDefaultStack()), duration), PacketEvent.Type.RECEIVE));
                        }
                    }
                }
                case OpenScreenS2CPacket openScreen when openScreen.getName().getString().contains("Рюкзак") && !stacks.isEmpty() -> script.cleanup().addTickStep(0, script2::update);
                default -> {}
            }
        }
    }

    @EventHandler
    public void onSetScreen(SetScreenEvent e) {
        if (e.getScreen() instanceof GenericContainerScreen screen && screen.getTitle().getString().contains("Рюкзак") && !script2.isFinished()) {
            e.setScreen(null);
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() != EventType.PRE || mc.currentScreen != null) {
            return;
        }

        boolean noMoveOrAction = System.currentTimeMillis() < stopMovementUntil || (actionState != ActionState.IDLE && actionState != ActionState.SPEEDING_UP);
        if (noMoveOrAction) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            if (mc.player.input != null) {
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
            }
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
        }

        for (KeyBind bind : keyBindings) {
            java.lang.String key = switch (bind.setting.getName()) {
                case "Анти полет" -> "antiflight";
                case "Свиток опыта" -> "expscroll";
                case "Взрывная трапка" -> "dtrap";
                case "Обычная трапка" -> "trap_holy";
                case "Стан" -> "stan";
                case "Взрывная штучка" -> "ditem";
                case "Снежок заморозка" -> "snow";
                case "Божья аура" -> "bojaura";
                case "Трапка" -> "trap";
                case "Пласт" -> "plast";
                case "Явная пыль" -> "sugar";
                case "Огненный смерч" -> "fireSwirl";
                case "Дезориентация" -> "disorientation";
                case "Светильник Джека" -> "tikva";
                case "Пузырь опыта" -> "exp";
                case "Рюкзак 1 уровня" -> "shulker1";
                case "Рюкзак 2 уровня" -> "shulker2";
                case "Рюкзак 3 уровня" -> "shulker3";
                case "Рюкзак 4 уровня" -> "shulker4";
                case "Зелье отрыжки" -> "otriga";
                case "Зелье серной кислоты" -> "serka";
                case "Зелье вспышки" -> "vspihka";
                case "Зелье мочи Флеша" -> "mochaflesha";
                case "Зелье победителя" -> "pobedilka";
                case "Зелье агента" -> "agent";
                case "Зелье медика" -> "medik";
                case "Зелье киллера" -> "killer";
                default -> null;
            };
            if (key != null && bind.setting.isVisible()) {
                boolean currentKey = false;
                if (bind.setting.getKey() != -1) {
                    if (bind.setting.getKey() >= GLFW.GLFW_MOUSE_BUTTON_1 && bind.setting.getKey() <= GLFW.GLFW_MOUSE_BUTTON_8) {
                        currentKey = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), bind.setting.getKey()) == GLFW.GLFW_PRESS;} else {
                        currentKey = InputUtil.isKeyPressed(mc.getWindow().getHandle(), bind.setting.getKey());
                    }
                }
                boolean wasPressedLastTick = lastKeyStates.getOrDefault(key, false);
                if (currentKey && !wasPressedLastTick) {
                    ItemInfo info = itemConfig.get(key);
                    if (info != null) {
                        Slot slot = InventoryTask.getSlot(s -> s.getStack().getItem().equals(info.item) && InventoryTask.getCleanName(s.getStack().getName()).contains(info.searchName.toLowerCase()));
                        boolean addStarPrefix = info.displayName.equals("Дезориентация")|| info.displayName.equals("Божья аура") || info.displayName.equals("Пласт") || info.displayName.equals("Трапка") || info.displayName.equals("Огненный смерч") || info.displayName.equals("Снежок заморозка") || info.displayName.equals("Явная пыль") || info.displayName.equals("Зелье отрыжки") || info.displayName.equals("Зелье серной кислоты") || info.displayName.equals("Зелье вспышки") || info.displayName.equals("Зелье мочи Флеша") || info.displayName.equals("Зелье победителя") || info.displayName.equals("Зелье агента") || info.displayName.equals("Зелье медика") || info.displayName.equals("Зелье киллера");
                        if (slot != null) {
                            ItemStack stack = slot.getStack();
                            if (mc.player.getItemCooldownManager().isCoolingDown(stack)) {
                                CoolDowns.getInstance().list.stream()
                                        .filter(c -> c.item().equals(info.item))
                                        .findFirst()
                                        .ifPresent(coolDown -> {
                                            int time = Math.toIntExact(-coolDown.time().elapsedTime() / 1000);
                                            java.lang.String duration = StringHelper.getDuration(time);
                                            MutableText text = Text.empty()
                                                    .append(GradientAssist.applyGradientToText(info.displayName, GradientAssist.getGradientColors(info.displayName), addStarPrefix))
                                                    .append("  будет  доступен  через ")
                                                    .append(Text.literal(duration).formatted(Formatting.GRAY));
                                            Notifications.getInstance().addList(text, 4000);
                                        });
                            } else {
                                if (!potionQueue.contains(key)) {
                                    potionQueue.add(key);
                                }
                            }
                        } else {
                            MutableText text = Text.empty()
                                    .append(GradientAssist.applyGradientToText(info.displayName, GradientAssist.getGradientColors(info.displayName), addStarPrefix))
                                    .append("  не  найдено");
                            Notifications.getInstance().addList(text, 4000);
                        }
                    }
                }
                lastKeyStates.put(key, currentKey);
                keyPressedThisTick.put(key, currentKey);
            }
        }
        if (actionState != ActionState.IDLE) {
            processItemAction();
        }
        if (actionState == ActionState.IDLE && !potionQueue.isEmpty() && potionTimer.finished(150)) {
            java.lang.String potionKey = potionQueue.remove(0);
            ItemInfo info = itemConfig.get(potionKey);
            if (info != null) {
                Slot slot = InventoryTask.getSlot(s -> s.getStack().getItem().equals(info.item) && InventoryTask.getCleanName(s.getStack().getName()).contains(info.searchName.toLowerCase()));
                boolean addStarPrefix = info.displayName.equals("Дезориентация") || info.displayName.equals("Божья аура") || info.displayName.equals("Пласт") || info.displayName.equals("Трапка") || info.displayName.equals("Огненный смерч") || info.displayName.equals("Снежок заморозка") || info.displayName.equals("Явная пыль") || info.displayName.equals("Зелье отрыжки") || info.displayName.equals("Зелье серной кислоты") || info.displayName.equals("Зелье вспышки") || info.displayName.equals("Зелье мочи Флеша") || info.displayName.equals("Зелье победителя") || info.displayName.equals("Зелье агента") || info.displayName.equals("Зелье медика") || info.displayName.equals("Зелье киллера");
                if (slot != null) {
                    ItemStack stack = slot.getStack();
                    if (!mc.player.getItemCooldownManager().isCoolingDown(stack)) {
                        startItemUse(slot, info, addStarPrefix);
                    } else {
                        CoolDowns.getInstance().list.stream()
                                .filter(c -> c.item().equals(info.item))
                                .findFirst()
                                .ifPresent(coolDown -> {
                                    int time = Math.toIntExact(-coolDown.time().elapsedTime() / 1000);
                                    java.lang.String duration = StringHelper.getDuration(time);
                                    MutableText text = Text.empty()
                                            .append(GradientAssist.applyGradientToText(info.displayName, GradientAssist.getGradientColors(info.displayName), addStarPrefix))
                                            .append("  будет  доступен  через ")
                                            .append(Text.literal(duration).formatted(Formatting.GRAY));
                                    Notifications.getInstance().addList(text, 4000);
                                });
                    }
                } else {
                    MutableText text = Text.empty()
                            .append(GradientAssist.applyGradientToText(info.displayName, GradientAssist.getGradientColors(info.displayName), addStarPrefix))
                            .append("  не  найдено");
                    Notifications.getInstance().addList(text, 4000);
                }
                potionTimer.reset();
            }
        }
        if (autoRepairSetting.isValue() && autoRepairSetting.isVisible() && StreamSupport.stream(mc.player.getArmorItems().spliterator(), false)
                .anyMatch(stack -> {
                    if ((double) stack.getDamage() / stack.getMaxDamage() < 0.94) return false;
                    RegistryEntry<Enchantment> mendingEntry = mc.world.getRegistryManager()
                            .getOrThrow(RegistryKeys.ENCHANTMENT)
                            .getEntry(Enchantments.MENDING.getValue())
                            .orElse(null);
                    return mendingEntry != null && EnchantmentHelper.getLevel(mendingEntry, stack) > 0;
                })) {
            InventoryTask.slots()
                    .filter(slot -> {
                        ItemStack stack = slot.getStack();
                        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
                        return !mc.player.getItemCooldownManager().isCoolingDown(stack) && stack.getItem().equals(Items.EXPERIENCE_BOTTLE) && component != null && component.toString().contains("\"text\":\" - при нажатие ПКМ, полностью ремонтирует\"") && repairWatch.every(5000);
                    })
                    .findFirst()
                    .ifPresent(slot -> InventoryFlowManager.addTask(() -> InventoryTask.swapAndUse(slot, MathAngle.cameraAngle())));
        }
        if (!InventoryTask.isServerScreen() && !stacks.isEmpty() && script2.isFinished() && shulkerWatch.finished(300)) {
            InventoryTask.slots()
                    .filter(s -> s.getStack().get(DataComponentTypes.CONTAINER) != null)
                    .max(Comparator.comparingDouble(s -> s.getStack().getOrDefault(DataComponentTypes.CONTAINER, null).stacks.stream().filter(item -> !item.isEmpty()).toList().size()))
                    .ifPresent(shulker -> {
                        InventoryTask.swapHand(shulker, Hand.MAIN_HAND, false);
                        InventoryTask.closeScreen(false);
                        PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
                        script2.cleanup().addTickStep(0, () -> {
                            List<Integer> integers = new ArrayList<>();
                            InventoryTask.slots().forEach(slot -> stacks.entrySet().stream()
                                    .filter(entry -> slot.inventory.equals(mc.player.getInventory()) && entry.getValue().equals(slot.getStack().getItem()) && entry.getKey() == slot.id)
                                    .forEach(entry -> {
                                        InventoryTask.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, false);
                                        integers.add(slot.id);
                                    }));
                            integers.forEach(stacks::remove);
                            InventoryTask.closeScreen(false);
                            InventoryTask.swapHand(shulker, Hand.MAIN_HAND, false);
                            InventoryTask.closeScreen(false);
                            shulkerWatch.reset();
                        });
                    });
        }
        if (autoLootSetting.isValue() && autoLootSetting.isVisible()) {
            PlayerInteractionHelper.streamEntities()
                    .filter(MerchantEntity.class::isInstance)
                    .map(MerchantEntity.class::cast)
                    .filter(m -> m.hasStackEquipped(EquipmentSlot.MAINHAND) || m.hasStackEquipped(EquipmentSlot.OFFHAND))
                    .findFirst()
                    .ifPresent(merchant -> {
                        Vec3d attackVector = pointFinder.computeVector(merchant, 6, TurnsConnection.INSTANCE.getRotation(), new LinearConstructor().randomValue(), true).getLeft();
                        Turns angle = MathAngle.calculateAngle(attackVector);
                        itemsWatch.reset();
                        entityUUID = merchant.getUuid();
                        if (mc.player.getEyePos().distanceTo(merchant.getBoundingBox().getCenter()) <= 6) {
                            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(merchant, false, Hand.MAIN_HAND, merchant.getBoundingBox().getCenter()));
                            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(merchant, false, Hand.MAIN_HAND));
                            TurnsConnection.INSTANCE.rotateTo(angle, TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_3, this);
                        }
                    });
        }
        script.cleanupIfFinished().update();
        blockStateMap.clear();
        structures.removeIf(cons -> cons.time - System.currentTimeMillis() <= 0);
        serverEvents.removeIf(event -> event.timeEnd + 90000 - System.currentTimeMillis() <= 0);
    }

    private void startItemUse(Slot slot, ItemInfo info, boolean addStarPrefix) {
        originalSlot = mc.player.getInventory().selectedSlot;
        originalSourceSlot = slot.id;
        targetSlot = slot.id;
        pendingItemKey = info.searchName;

        boolean needsSwap = !(slot.id >= 0 && slot.id < 9) && !(slot.id >= 36 && slot.id < 45);

        wasForwardPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode());
        wasBackPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.backKey.getDefaultKey().getCode());
        wasLeftPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.leftKey.getDefaultKey().getCode());
        wasRightPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.rightKey.getDefaultKey().getCode());

        if (needsSwap) {
            actionState = ActionState.SLOWING_DOWN;
            actionTimer = System.currentTimeMillis();
            stopMovementUntil = System.currentTimeMillis() + 95;
            keysOverridden = true;
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        } else {
            actionState = ActionState.SWAP_TO_ITEM;
            actionTimer = System.currentTimeMillis();
            stopMovementUntil = System.currentTimeMillis() + 95;
            keysOverridden = true;
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        }

        MutableText text = Text.empty()
                .append(GradientAssist.applyGradientToText(info.displayName, GradientAssist.getGradientColors(info.displayName), addStarPrefix))
                .append("  использована");
        Notifications.getInstance().addList(text, 4000);
    }

    private void processItemAction() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - actionTimer;

        switch (actionState) {
            case SLOWING_DOWN -> {
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
                if (mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                }
                if (elapsed > 1) {
                    actionState = ActionState.WAITING_STOP;
                }
            }
            case WAITING_STOP -> {
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
                double velocityX = Math.abs(mc.player.getVelocity().x);
                double velocityZ = Math.abs(mc.player.getVelocity().z);
                if ((velocityX < 0.001 && velocityZ < 0.001) || elapsed > 75) {
                    actionState = ActionState.SWAP_TO_ITEM;
                    actionTimer = currentTime;
                }
            }
            case SWAP_TO_ITEM -> {
                if (elapsed > 25) {
                    if (targetSlot >= 0 && targetSlot < 9) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(targetSlot));
                        mc.player.getInventory().selectedSlot = targetSlot;
                    } else if (targetSlot >= 36 && targetSlot < 45) {
                        int hotbarSlot = targetSlot - 36;
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                        mc.player.getInventory().selectedSlot = hotbarSlot;
                        targetSlot = hotbarSlot;
                    } else {
                        int swapSlot = 8;
                        InventoryTask.clickSlot(targetSlot, swapSlot, SlotActionType.SWAP, false);
                        targetSlot = swapSlot;
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(swapSlot));
                        mc.player.getInventory().selectedSlot = swapSlot;
                    }

                    actionState = ActionState.USE_ITEM;
                    actionTimer = currentTime;
                }
            }
            case USE_ITEM -> {
                if (elapsed > 40) {
                    mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
                    mc.player.swingHand(Hand.MAIN_HAND);
                    actionState = ActionState.SWAP_BACK;
                    actionTimer = currentTime;
                }
            }
            case SWAP_BACK -> {
                if (elapsed > 25) {
                    boolean wasFromInventory = !(originalSourceSlot >= 0 && originalSourceSlot < 9) && !(originalSourceSlot >= 36 && originalSourceSlot < 45);

                    if (wasFromInventory) {
                        if (targetSlot >= 0 && targetSlot < 9) {
                            InventoryTask.clickSlot(originalSourceSlot, targetSlot, SlotActionType.SWAP, false);
                        }
                    } else {
                        if (originalSourceSlot >= 36 && originalSourceSlot < 45) {
                            int hotbarSlot = originalSourceSlot - 36;
                            if (targetSlot != hotbarSlot) {
                                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                                mc.player.getInventory().selectedSlot = hotbarSlot;
                            }
                        } else if (originalSourceSlot >= 0 && originalSourceSlot < 9) {
                            if (targetSlot != originalSourceSlot) {
                                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSourceSlot));
                                mc.player.getInventory().selectedSlot = originalSourceSlot;
                            }
                        }
                    }

                    if (mc.player.getInventory().selectedSlot != originalSlot) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
                        mc.player.getInventory().selectedSlot = originalSlot;
                    }

                    restoreKeyStates();
                    actionState = ActionState.SPEEDING_UP;
                    actionTimer = currentTime;
                }
            }
            case SPEEDING_UP -> {
                long speedupElapsed = currentTime - actionTimer;

                if (speedupElapsed > 75) {
                    actionState = ActionState.IDLE;
                    originalSlot = -1;
                    targetSlot = -1;
                    originalSourceSlot = -1;
                    pendingItemKey = null;
                }
            }
        }
    }

    private void restoreKeyStates() {
        if (!keysOverridden) return;

        mc.options.forwardKey.setPressed(wasForwardPressed);
        mc.options.backKey.setPressed(wasBackPressed);
        mc.options.leftKey.setPressed(wasLeftPressed);
        mc.options.rightKey.setPressed(wasRightPressed);

        if (mc.player.input != null) {
            if (wasForwardPressed) {
                mc.player.input.movementForward = 1.0f;
                if (!mc.player.isSprinting()) {
                    mc.player.setSprinting(true);
                }
            }
            if (wasBackPressed) {
                mc.player.input.movementForward = -1.0f;
            }
            if (wasLeftPressed) {
                mc.player.input.movementSideways = 1.0f;
            }
            if (wasRightPressed) {
                mc.player.input.movementSideways = -1.0f;
            }
        }

        keysOverridden = false;
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        MatrixStack matrix = e.getStack();
        keyBindings.stream()
                .filter(bind -> PlayerInteractionHelper.isKey(bind.setting) && InventoryTask.getSlot(bind.item) != null)
                .forEach(bind -> {
                    BlockPos playerPos = mc.player.getBlockPos();
                    Vec3d smooth = Calculate.interpolate(Vec3d.of(BlockPos.ofFloored(mc.player.prevX, mc.player.prevY, mc.player.prevZ)), Vec3d.of(playerPos))
                            .subtract(Vec3d.of(playerPos));
                    int[] gradientColors = GradientAssist.getGradientColors(bind.setting.getName());
                    int color = gradientColors.length > 1 ? ColorAssist.gradient(10, 0, gradientColors) : gradientColors[0];
                    switch (bind.setting.getName()) {
                        case "Трапка", "Обычная трапка" -> drawItemCube(playerPos, smooth, 1.99F, color);
                        case "Дезориентация", "Огненный смерч", "Явная пыль" -> drawItemRadius(matrix, bind.distance, color);
                        case "Взрывная штучка" -> drawItemRadius(matrix, 5, color);
                        case "Пласт" -> {
                            float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
                            if (Math.abs(mc.player.getPitch()) > 60) {
                                BlockPos blockPos = playerPos.up().offset(mc.player.getFacing(), 3);
                                Vec3d pos1 = Vec3d.of(blockPos.east(3).south(3).down()).add(smooth);
                                Vec3d pos2 = Vec3d.of(blockPos.west(2).north(2).up()).add(smooth);
                                Render3D.drawBox(new Box(pos1, pos2), color, 3, true, true, true);
                            } else if (yaw <= -157.5F || yaw >= 157.5F) {
                                BlockPos blockPos = playerPos.north(3).up();
                                Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
                                Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
                                Render3D.drawBox(new Box(pos1, pos2), color, 3, true, true, true);
                            } else if (yaw <= -112.5F) {
                                drawSidePlast(playerPos.east(5).south().down(), smooth, color, -1, true);
                            } else if (yaw <= -67.5F) {
                                BlockPos blockPos = playerPos.east(2).up();
                                Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
                                Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
                                Render3D.drawBox(new Box(pos1, pos2), color, 3, true, true, true);
                            } else if (yaw <= -22.5F) {
                                drawSidePlast(playerPos.east(5).down(), smooth, color, 1, false);
                            } else if (yaw >= -22.5 && yaw <= 22.5) {
                                BlockPos blockPos = playerPos.south(2).up();
                                Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
                                Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
                                Render3D.drawBox(new Box(pos1, pos2), color, 3, true, true, true);
                            } else if (yaw <= 67.5F) {
                                drawSidePlast(playerPos.west(4).down(), smooth, color, 1, true);
                            } else if (yaw <= 112.5F) {
                                BlockPos blockPos = playerPos.west(3).up();
                                Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
                                Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
                                Render3D.drawBox(new Box(pos1, pos2), color, 3, true, true, true);
                            } else if (yaw <= 157.5F) {
                                drawSidePlast(playerPos.west(4).south().down(), smooth, color, -1, false);
                            }
                        }
                        case "Взрывная трапка" -> drawItemCube(playerPos, smooth, 3.99F, color);
                        case "Стан" -> drawItemCube(playerPos, smooth, 15.01F, color);
                        case "Снежок заморозка" -> ProjectilePrediction.getInstance().drawPredictionInHand(matrix, List.of(Items.SNOWBALL.getDefaultStack()), MathAngle.cameraAngle());
                    }
                });
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();
        structures.forEach(cons -> {
            double time = (cons.time - System.currentTimeMillis()) / 1000;
            Vec3d vec3d = Projection.worldSpaceToScreenSpace(cons.vec);
            java.lang.String text = Calculate.round(time, 0.1F) + "с";
            FontRenderer font = Fonts.getSize(14);
            float width = font.getStringWidth(text);
            float posX = (float) (vec3d.x - width / 2);
            float posY = (float) vec3d.y;
            float padding = 2;
            if (Projection.canSee(cons.vec) && cons.anarchy == Network.getAnarchy() && Network.getWorldType().equals(cons.world)) {
                blur.render(ShapeProperties.create(matrix, posX - padding, posY - padding, width + padding * 2, 10)
                        .round(1.5F)
                        .color(ColorAssist.HALF_BLACK)
                        .build());
                font.drawString(matrix, text, posX, posY + 1, ColorAssist.getText());
                Render2D.defaultDrawStack(context, cons.item.getDefaultStack(), posX - 14, posY - 2.5F, true, false, 0.5F);
            }
        });
        serverEvents.forEach(event -> {
            Vec3d vec3d = Projection.worldSpaceToScreenSpace(event.vec);
            double timeOpen = (event.timeOpen - System.currentTimeMillis()) / 1000;
            double timeEnd = (event.timeEnd - System.currentTimeMillis()) / 1000;
            java.lang.String distance = " [" + Calculate.round(mc.getEntityRenderDispatcher().camera.getPos().distanceTo(event.vec), 0.1) + "m" + "]";
            java.lang.String time = timeOpen > 0 ? ("До начала: " + Calculate.round(timeOpen, timeOpen < 30 ? 0.1F : 1) + "с").replace(".0", "") : timeEnd > 0 ? ("До конца: " + Calculate.round(timeEnd, timeEnd < 30 ? 0.1F : 1) + "с").replace(".0", "") : "Конец ивента!";
            if (Projection.canSee(event.vec) && event.anarchy == Network.getAnarchy() && Network.getWorldType().equals(event.world)) {
                List<java.lang.String> list = new ArrayList<>(Collections.singletonList(event.name + distance));
                if (event.owner != null) list.add("Призван: " + Formatting.GOLD + event.owner);
                list.add(time);
                if (event.lvl != null) list.add(event.lvl);
                draw(matrix, Fonts.getSize(14), list, vec3d);
            }
        });
        PlayerInteractionHelper.streamEntities()
                .filter(ent -> ent.getUuid().equals(entityUUID))
                .forEach(ent -> {
                    Vec3d pos = ent.getBlockPos().down().toCenterPos();
                    Vec3d vec = Projection.worldSpaceToScreenSpace(pos);
                    java.lang.String text = !itemsWatch.finished(200) ? "Можно забрать" : !itemsWatch.finished(20000) ? Calculate.round(20 - itemsWatch.elapsedTime() / 1000F, 0.1F) + "с" : "Скоро";
                    FontRenderer font = Fonts.getSize(14);
                    float height = 4;
                    float width = font.getStringWidth(text);
                    float padding = 3;
                    double x = vec.getX() - width / 2;
                    double y = vec.getY() - height / 2;
                    Formatting formatting = mc.player.getEyePos().distanceTo(ent.getEyePos()) < 5F ? Formatting.GREEN : Formatting.RED;
                    if (Projection.canSee(pos)) {
                        blur.render(ShapeProperties.create(matrix, x - padding, y - padding, width + padding * 2, height + padding * 2)
                                .round(2)
                                .color(ColorAssist.HALF_BLACK)
                                .build());
                        font.drawString(matrix, formatting + text, x, y, ColorAssist.getText());
                    }
                });
    }

    private void drawItemCube(BlockPos playerPos, Vec3d smooth, float size, int color) {
        Box box = new Box(playerPos.up()).offset(smooth).expand(size);
        boolean inBox = mc.world.getPlayers().stream()
                .map(player -> PlayerSimulation.simulateOtherPlayer(player, 2))
                .anyMatch(simulated -> simulated.player != mc.player && box.intersects(simulated.boundingBox) && !FriendUtils.isFriend(simulated.player));
        Render3D.drawBox(box, inBox ? ColorAssist.getFriendColor() : color, 3, true, true, true);
    }

    private void drawItemRadius(MatrixStack matrix, float distance, int color) {
        float playerHalfWidth = mc.player.getWidth() / 2;
        int finalColor = validDistance(distance) ? ColorAssist.getFriendColor() : color;
        Vec3d pos = Calculate.interpolate(mc.player).add(playerHalfWidth, 0.02, playerHalfWidth);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0, size = 90; i <= size; i++) {
            Vec3d cosSin = Calculate.cosSin(i, size, distance);
            Vec3d nextCosSin = Calculate.cosSin(i + 1, size, distance);
            Render3D.vertexLine(matrix, buffer, pos.add(cosSin), pos.add(cosSin.x, cosSin.y + 2, cosSin.z), ColorAssist.multAlpha(finalColor, 0.2F), ColorAssist.multAlpha(finalColor, 0));
            Render3D.drawLine(pos.add(cosSin), pos.add(nextCosSin), finalColor, 2, true);
        }
        for (int i = 0, size = 90; i <= size; i++) {
            Vec3d cosSin = Calculate.cosSin(i, size, distance);
            Render3D.vertexLine(matrix, buffer, pos.add(cosSin), pos.add(cosSin.x, cosSin.y - 2, cosSin.z), ColorAssist.multAlpha(finalColor, 0.2F), ColorAssist.multAlpha(finalColor, 0));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    }

    private void draw(MatrixStack matrix, FontRenderer font, List<java.lang.String> list, Vec3d vec3d) {
        float offsetY = 0;
        for (int i = 0; i < list.size(); i++) {
            java.lang.String string = list.get(i);
            float width = font.getStringWidth(string);
            float posX = (float) (vec3d.x - width / 2);
            blur.render(ShapeProperties.create(matrix, posX - 2, vec3d.y - 2 + offsetY, width + 2 * 2, 10)
                    .softness(3)
                    .round(getRound(font, list, i, width))
                    .color(ColorAssist.HALF_BLACK)
                    .build());
            font.drawString(matrix, string, posX, vec3d.y + 1 + offsetY, ColorAssist.getText());
            offsetY += 10;
        }
    }

    private void drawSidePlast(BlockPos blockPos, Vec3d smooth, int color, int i, boolean ff) {
        Vec3d vec3d = Vec3d.of(blockPos).add(smooth);
        float width = 2;
        int quadColor = ColorAssist.multAlpha(color, 0.15F);
        drawHorizontalLines(vec3d, color, width, i, ff);
        drawHorizontalLines(vec3d, color, width, i, ff);
        drawVerticalLines(vec3d, color, width, i, ff);
        drawHorizontalQuads(vec3d, quadColor, i, ff);
        drawHorizontalQuads(vec3d, quadColor, i, ff);
        drawVerticalQuads(vec3d, quadColor, i, ff);
    }

    private void drawHorizontalLines(Vec3d vec3d, int color, float width, int i, boolean ff) {
        float x = ff ? i : -i;
        Render3D.drawLine(vec3d, vec3d = vec3d.add(x, 0, 0), color, width, true);
        for (int f = 0; f < 4; f++) {
            Render3D.drawLine(vec3d, vec3d = vec3d.add(0, 0, i), color, width, true);
            Render3D.drawLine(vec3d, vec3d = vec3d.add(x, 0, 0), color, width, true);
        }
        Render3D.drawLine(vec3d, vec3d = vec3d.add(0, 0, i), color, width, true);
        Render3D.drawLine(vec3d, vec3d = vec3d.add(x * -2, 0, 0), color, width, true);
        for (int f = 0; f < 3; f++) {
            Render3D.drawLine(vec3d, vec3d = vec3d.add(0, 0, i * -1), color, width, true);
            Render3D.drawLine(vec3d, vec3d = vec3d.add(x * -1, 0, 0), color, width, true);
        }
        Render3D.drawLine(vec3d, vec3d.add(0, 0, i * -2), color, width, true);
    }

    private void drawVerticalLines(Vec3d vec3d, int color, float width, int i, boolean ff) {
        float x = ff ? i : -i;
        Render3D.drawLine(vec3d, vec3d = vec3d.add(x, 0, 0), color, width, true);
        for (int f = 0; f < 4; f++) {
            Render3D.drawLine(vec3d, vec3d = vec3d.add(0, 0, i), color, width, true);
            Render3D.drawLine(vec3d, vec3d = vec3d.add(x, 0, 0), color, width, true);
        }
        Render3D.drawLine(vec3d, vec3d = vec3d.add(0, 0, i), color, width, true);
        Render3D.drawLine(vec3d, vec3d = vec3d.add(x * -2, 0, 0), color, width, true);
        for (int f = 0; f < 3; f++) {
            Render3D.drawLine(vec3d, vec3d = vec3d.add(0, 0, i* -1), color, width, true);
            Render3D.drawLine(vec3d, vec3d = vec3d.add(x * -1, 0, 0), color, width, true);
        }
        Render3D.drawLine(vec3d, vec3d.add(0, 0, i * -2), color, width, true);
    }

    private void drawHorizontalQuads(Vec3d vec3d, int color, int i, boolean ff) {
        vec3d = vec3d.add(0, 1e-3, 0);
        float x = ff ? i : -i;
        Render3D.drawQuad(vec3d, vec3d.add(x, 0, 0), vec3d.add(x, 0, i * 2), vec3d.add(0, 0, i * 2), color, true);
        for (int f = 0; f < 3; f++) {
            Render3D.drawQuad(vec3d = vec3d.add(x, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 0, i * 2), vec3d.add(0, 0, i * 2), color, true);
        }
        Render3D.drawQuad(vec3d = vec3d.add(x, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 0, i), vec3d.add(0, 0, i), color, true);
    }

    private void drawVerticalQuads(Vec3d vec3d, int color, int i, boolean ff) {
        float x = ff ? i : -i;
        Render3D.drawQuad(vec3d, vec3d.add(x, 0, 0), vec3d.add(x, 5, 0), vec3d.add(0, 5, 0), color, true);
        for (int f = 0; f < 4; f++) {
            Render3D.drawQuad(vec3d = vec3d.add(x, 0, 0), vec3d.add(0, 0, i), vec3d.add(0, 5, i), vec3d.add(0, 5, 0), color, true);
            Render3D.drawQuad(vec3d = vec3d.add(0, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 5, 0), vec3d.add(0, 5, 0), color, true);
        }
        Render3D.drawQuad(vec3d = vec3d.add(x, 0, 0), vec3d.add(0, 0, i), vec3d.add(0, 5, i), vec3d.add(0, 5, 0), color, true);
        Render3D.drawQuad(vec3d = vec3d.add(0, 0, i), vec3d.add(x * -2, 0, 0), vec3d.add(x * -2, 5, 0), vec3d.add(0, 5, 0), color, true);
        vec3d = vec3d.add(x * -1, 0, 0);
        for (int f = 0; f < 3; f++) {
            Render3D.drawQuad(vec3d = vec3d.add(x * -1, 0, 0), vec3d.add(0, 0, i * -1), vec3d.add(0, 5, i * -1), vec3d.add(0, 5, 0), color, true);
            Render3D.drawQuad(vec3d = vec3d.add(0, 0, i * -1), vec3d.add(x * -1, 0, 0), vec3d.add(x * -1, 5, 0), vec3d.add(0, 5, 0), color, true);
        }
        Render3D.drawQuad(vec3d = vec3d.add(x * -1, 0, 0), vec3d.add(0, 0, i * -2), vec3d.add(0, 5, i * -2), vec3d.add(0, 5, 0), color, true);
    }

    private void addEvent(java.lang.String name, java.lang.String lvl, java.lang.String owner, Vec3d vec3d, java.lang.String world, int timeOpen, int timeLoot) {
        if (serverEvents.stream().noneMatch(server -> server.vec.equals(vec3d))) {
            long open = System.currentTimeMillis() + timeOpen * 1000L;
            long loot = open + timeLoot * 1000L;
            serverEvents.add(new ServerEvent(name, lvl, owner, vec3d, world, Network.getAnarchy(), open, loot));
        }
    }

    private void addStructure(Item item, Vec3d vec, double time) {
        if (structures.stream().noneMatch(str -> str.vec.equals(vec))) {
            structures.add(new Structure(item, vec, Network.getWorldType(), Network.getAnarchy(), time));
        }
    }

    private Vector4f getRound(FontRenderer font, List<java.lang.String> list, int i, float width) {
        if (i == 0) {
            float next = font.getStringWidth(list.get(i + 1));
            return next >= width ? new Vector4f(2, 0, 2, 0) : new Vector4f(2);
        }
        if (i == list.size() - 1) {
            float prev = font.getStringWidth(list.get(i - 1));
            return prev >= width ? new Vector4f(0, 2, 0, 2) : new Vector4f(2);
        }
        float prev = font.getStringWidth(list.get(i - 1));
        float next = font.getStringWidth(list.get(i + 1));
        return prev >= width ? next >= width ? new Vector4f() : new Vector4f(0, 2, 0, 2) : new Vector4f(2);
    }

    private boolean validDistance(float dist) {
        return dist == 0 || mc.world.getPlayers().stream()
                .anyMatch(p -> p != mc.player && !FriendUtils.isFriend(p) && mc.player.distanceTo(p) <= dist);
    }

    private boolean isTrap(BlockPos center) {
        int inconsistencies = 0;
        for (BlockPos pos : PlayerInteractionHelper.getCube(center, 2)) {
            if (pos.toCenterPos().distanceTo(center.toCenterPos()) < 2) {
                BlockState state = blockStateMap.get(pos);
                if (state != null && !state.isAir()) inconsistencies++;
            } else if (!pos.equals(center.up(2).north().east()) && !pos.equals(center.up(2).north().west()) && !pos.equals(center.up(2).south().east()) && !pos.equals(center.up(2).south().west())) {
                BlockState state = blockStateMap.get(pos);
                if (state == null || state.isAir()) inconsistencies++;
            }
            if (inconsistencies > 1) return false;
        }
        return true;
    }

    private boolean isBigTrap(BlockPos center) {
        int inconsistencies = 0;
        for (BlockPos pos : PlayerInteractionHelper.getCube(center, 3)) {
            if (Math.abs(pos.getX() - center.getX()) <= 2 && Math.abs(pos.getY() - center.getY()) <= 2 && Math.abs(pos.getZ() - center.getZ()) <= 2) {
                BlockState state = blockStateMap.get(pos);
                if (state != null && !state.isAir()) inconsistencies++;
            } else if (!pos.equals(center.up(3))) {
                BlockState state = blockStateMap.get(pos);
                if (state == null || state.isAir()) inconsistencies++;
            }
            if (inconsistencies > 1) return false;
        }
        return true;
    }

    public List<KeyBind> getKeyBindings() {
        return keyBindings;
    }

    public BindSetting getSetting(java.lang.String name) {
        return keyBindings.stream()
                .filter(bind -> bind.setting().getName().equals(name))
                .map(ServerHelper.KeyBind::setting)
                .findFirst()
                .orElse(null);
    }

    public record KeyBind(Item item, BindSetting setting, float distance) {
    }

    public record Structure(Item item, Vec3d vec, java.lang.String world, int anarchy, double time) {
    }

    public record ServerEvent(java.lang.String name, java.lang.String lvl, java.lang.String owner, Vec3d vec, java.lang.String world, int anarchy, double timeOpen, double timeEnd) {
    }
}
