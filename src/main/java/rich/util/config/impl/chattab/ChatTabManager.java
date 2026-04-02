package rich.util.config.impl.chattab;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.network.message.SignedMessage;
import rich.util.repository.friend.FriendUtils;
import rich.util.repository.ignore.IgnoreUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ChatTabManager {
    private static ChatTabManager instance;
    
    private List<ChatTab> tabs;
    private ChatTab activeTab;
    private boolean tabsEnabled = true;

    private ChatTabManager() {
        this.tabs = new ArrayList<>();
        this.activeTab = null;
    }

    public static ChatTabManager getInstance() {
        if (instance == null) {
            instance = new ChatTabManager();
        }
        return instance;
    }

    public void init() {
        this.tabs = ChatTabConfig.getInstance().load();
        this.activeTab = null;
    }

    public void addTab(ChatTab tab) {
        tabs.add(tab);
        ChatTabConfig.getInstance().save(tabs);
    }

    public void moveTab(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < tabs.size() &&
            toIndex >= 0 && toIndex < tabs.size() && fromIndex != toIndex) {
            ChatTab tab = tabs.remove(fromIndex);
            tabs.add(toIndex, tab);
            ChatTabConfig.getInstance().save(tabs);
        }
    }

    public void removeTab(String name) {
        tabs.removeIf(tab -> tab.getName().equalsIgnoreCase(name));
        ChatTabConfig.getInstance().save(tabs);
    }

    public void clearTabs() {
        tabs.clear();
        ChatTabConfig.getInstance().save(tabs);
    }

    public ChatTab getTab(String name) {
        if (name.equalsIgnoreCase("Друзья")) {
            return new ChatTab("Друзья", ChatTab.FilterType.Friends, "");
        }
        if (name.equalsIgnoreCase("Игнор")) {
            return new ChatTab("Игнор", ChatTab.FilterType.Ignored, "");
        }
        for (ChatTab tab : tabs) {
            if (tab.getName().equalsIgnoreCase(name)) {
                return tab;
            }
        }
        return null;
    }

    public boolean shouldShowMessage(SignedMessage signedMessage, Text messageText) {
        if (!tabsEnabled) {
            return true;
        }

        if (activeTab == null) {
            return true;
        }

        String sender = getSenderName(signedMessage.getSender());
        String content = messageText.getString();

        return activeTab.matches(sender, content);
    }

    public boolean shouldShowMessage(String sender, String message) {
        if (!tabsEnabled) {
            return true;
        }

        if (activeTab == null) {
            return true;
        }

        return activeTab.matches(sender, message);
    }

    private String getSenderName(UUID senderUuid) {
        if (senderUuid == null) return null;
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(senderUuid);
            if (entry != null) {
                return entry.getProfile().name();
            }
        }
        return null;
    }

    public void setActiveTab(String name) {
        if (name == null || name.equalsIgnoreCase("all") || name.equalsIgnoreCase("все")) {
            this.activeTab = null;
            return;
        }
        this.activeTab = getTab(name);
    }

    public ChatTab getActiveTab() {
        return activeTab;
    }

    public boolean hasActiveTab() {
        return activeTab != null;
    }

    public String getActiveTabName() {
        return activeTab != null ? activeTab.getName() : "Полный чат";
    }

    public void save() {
        ChatTabConfig.getInstance().save(tabs);
    }
}
