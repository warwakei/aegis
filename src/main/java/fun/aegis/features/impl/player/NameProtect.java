package fun.aegis.features.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.TextSetting;
import fun.aegis.features.module.setting.implement.MultiSelectSetting;
import fun.aegis.common.repository.friend.FriendUtils;
import fun.aegis.events.render.TextFactoryEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NameProtect extends Module {
    TextSetting nameSetting = new TextSetting("Имя", "Никнейм, который будет заменен на ваш").setText("Protected").setMax(32);
    BooleanSetting friendsSetting = new BooleanSetting("Друзья", "Скрывает никнеймы друзей").setValue(true);
    MultiSelectSetting protectMode = new MultiSelectSetting("Режим", "Что скрывать")
            .value("Chat", "Nametags", "Scoreboard", "Tab List")
            .selected("Chat", "Nametags", "Scoreboard", "Tab List");
    BooleanSetting randomName = new BooleanSetting("Случайное имя", "Использовать случайное имя вместо заданного").setValue(false);

    public NameProtect() {
        super("NameProtect","Name Protect", ModuleCategory.PLAYER);
        setup(nameSetting, friendsSetting, protectMode, randomName);
    }

    @EventHandler
    public void onTextFactory(TextFactoryEvent e) {
        String replaceName = randomName.isValue() ? generateRandomName() : nameSetting.getText();
        
        if (protectMode.isSelected("Chat") || protectMode.isSelected("Nametags") || 
            protectMode.isSelected("Scoreboard") || protectMode.isSelected("Tab List")) {
            e.replaceText(mc.getSession().getUsername(), replaceName);
            if (friendsSetting.isValue()) {
                FriendUtils.getFriends().forEach(friend -> e.replaceText(friend.getName(), replaceName));
            }
        }
    }

    private String generateRandomName() {
        String[] names = {"Player", "User", "Gamer", "Pro", "Noob", "Admin", "Mod", "Dev", "Bot", "NPC"};
        int randomIndex = (int) (System.currentTimeMillis() / 5000) % names.length;
        return names[randomIndex] + (int)(Math.random() * 9999);
    }
}
