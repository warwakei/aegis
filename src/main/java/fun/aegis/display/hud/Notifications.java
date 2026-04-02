package fun.aegis.display.hud;

import fun.aegis.common.animation.implement.OutBack;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.glow.GlowEffect;
import fun.aegis.utils.client.sound.SoundManager;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.client.Instance;
import fun.aegis.events.container.SetScreenEvent;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.features.impl.render.Hud;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Notifications extends AbstractDraggable {
    public static Notifications getInstance() {
        return Instance.getDraggable(Notifications.class);
    }

    private final List<Notification> list = new ArrayList<>();
    private final List<Stack> stacks = new ArrayList<>();

    public Notifications() {
        super("Уведомления", 0, 0, 100, 15, true);
    }

    @Override
    public void tick() {
        list.forEach(notif -> {
            if (System.currentTimeMillis() > notif.removeTime || (notif.text.getString().contains("Пример Увидомления") && !PlayerInteractionHelper.isChat(mc.currentScreen)))
                notif.anim.setDirection(Direction.BACKWARDS);
        });
        list.removeIf(notif -> notif.anim.isFinished(Direction.BACKWARDS));
        while (!stacks.isEmpty()) {
            addTextIfNotEmpty(TypePickUp.INVENTORY, "Items raised: ");
            addTextIfNotEmpty(TypePickUp.SHULKER_INVENTORY, "Items placed in shulker: ");
            addTextIfNotEmpty(TypePickUp.SHULKER, "Raised shulker with: ");
        }
    }

    @Override
    public void packet(PacketEvent e) {
        if (!PlayerInteractionHelper.nullCheck()) switch (e.getPacket()) {
            case ItemPickupAnimationS2CPacket item when Hud.getInstance().notificationSettings.isSelected("Item Pick Up") && item.getCollectorEntityId() == Objects.requireNonNull(mc.player).getId() && Objects.requireNonNull(mc.world).getEntityById(item.getEntityId()) instanceof ItemEntity entity -> {
                ItemStack itemStack = entity.getStack();
                ContainerComponent component = itemStack.get(DataComponentTypes.CONTAINER);
                if (component == null) {
                    Text itemText = itemStack.getName();
                    if (itemText.getContent().toString().equals("empty")) {
                        MutableText text = Text.empty().append(itemText);
                        if (itemStack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + itemStack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
                        stacks.add(new Stack(TypePickUp.INVENTORY, text));
                    }
                } else component.stream().filter(s -> s.getName().getContent().toString().equals("empty")).forEach(stack -> {
                    MutableText text = Text.empty().append(stack.getName());
                    if (stack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
                    stacks.add(new Stack(TypePickUp.SHULKER, text));
                });
            }
            case ScreenHandlerSlotUpdateS2CPacket slot when Hud.getInstance().notificationSettings.isSelected("Item Pick Up") -> {
                int slotId = slot.getSlot();
                ContainerComponent updatedContainer = slot.getStack().get(DataComponentTypes.CONTAINER);
                if (updatedContainer != null && slotId < Objects.requireNonNull(mc.player).currentScreenHandler.slots.size() && slot.getSyncId() == 0) {
                    ContainerComponent currentContainer = mc.player.currentScreenHandler.getSlot(slotId).getStack().get(DataComponentTypes.CONTAINER);
                    if (currentContainer != null) updatedContainer.stream().filter(stack -> currentContainer.stream().noneMatch(s -> Objects.equals(s.getComponents(), stack.getComponents()) && s.toString().equals(stack.toString()))).forEach(stack -> {
                        MutableText text = Text.empty().append(stack.getName());
                        stacks.add(new Stack(TypePickUp.SHULKER_INVENTORY, text));
                    });
                }
            }
            default -> {}
        }
    }

    @Override
    public void setScreen(SetScreenEvent e) {
        if (e.getScreen() instanceof ChatScreen) {
            addList("Пример Увидомления", 99999999);
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(12, Fonts.Type.DEFAULT);

        int windowHeight = mc.getWindow().getScaledHeight();
        int windowWidth = mc.getWindow().getScaledWidth();
        int crosshairY = windowHeight / 2;
        int crosshairX = windowWidth / 2;

        // Устанавливаем начальную позицию только если она еще не установлена (первый рендер)
        if (this.getX() == 0) {
            this.setX(crosshairX);
        }
        if (this.getY() == 0) {
            this.setY(crosshairY + 100);
        }

        float offsetY = 0;
        float offsetX = 5;
        for (Notification notification : list) {
            float anim = notification.anim.getOutput().floatValue();
            float width = font.getStringWidth(notification.text) + offsetX * 2;
            float startY = this.getY() + offsetY;
            // Центрируем каждый notification относительно его ширины
            float startX = this.getX() - width / 2;
            Calculate.setAlpha(anim, () -> {
                blur.render(ShapeProperties.create(matrix, startX, startY, width + 10, getHeight() + 1).round(6).softness(10F).thickness(0).color(ColorAssist.getRect(0.4F)).build());

font.drawText(matrix, notification.text, (int) (startX + offsetX) + 6, startY + 7F);
            });
            offsetY += (getHeight() + 3) * anim;
        }
    }

    private void addTextIfNotEmpty(TypePickUp type, String prefix) {
        MutableText text = Text.empty();
        List<Stack> list = stacks.stream().filter(stack -> stack.type.equals(type)).toList();
        for (int i = 0, size = list.size(); i < size; i++) {
            Stack stack = list.get(i);
            if (stack.type != type) continue;
            text.append(stack.text);
            stacks.remove(stack);
            if (text.getString().length() > 150) break;
            if (i + 1 != size) text.append(" , ");
        }
        if (!text.equals(Text.empty())) addList(Text.empty().append(prefix).append(text), 8000);
    }

    public void addList(String text, long removeTime) {
        addList(text, removeTime, null);
    }

    public void addList(Text text, long removeTime) {
        addList(text, removeTime, null);
    }

    public void addList(String text, long removeTime, SoundEvent sound) {
        addList(Text.empty().append(text), removeTime, sound);
    }

    public void addList(Text text, long removeTime, SoundEvent sound) {
        list.add(new Notification(text, new OutBack().setMs(400).setValue(1), System.currentTimeMillis(), System.currentTimeMillis() + removeTime));
        if (list.size() > 12) list.removeFirst();
        list.sort(Comparator.comparingDouble(notif -> -notif.removeTime));
        if (sound != null) SoundManager.playSound(sound);
    }

    public record Notification(Text text, Animation anim, long startTime, long removeTime) {
        public boolean isExpired() {
            return System.currentTimeMillis() > removeTime;
        }
    }

    public record Stack(TypePickUp type, MutableText text) {}

    public enum TypePickUp {
        INVENTORY, SHULKER, SHULKER_INVENTORY
    }
}
