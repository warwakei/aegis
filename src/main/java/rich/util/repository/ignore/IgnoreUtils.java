package rich.util.repository.ignore;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import rich.util.config.impl.ignore.IgnoreConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@UtilityClass
public class IgnoreUtils {
    @Getter
    private final List<Ignore> ignores = new ArrayList<>();
    
    @Getter
    private final List<ChatFilter> chatFilters = new ArrayList<>();

    public void addIgnore(PlayerEntity player) {
        addIgnore(player.getName().getString());
    }

    public void addIgnore(String name) {
        if (!isIgnore(name)) {
            ignores.add(new Ignore(name));
        }
    }

    public void addIgnoreAndSave(String name) {
        addIgnore(name);
        IgnoreConfig.getInstance().save();
    }

    public void removeIgnore(PlayerEntity player) {
        removeIgnore(player.getName().getString());
    }

    public void removeIgnore(String name) {
        ignores.removeIf(ignore -> ignore.getName().equalsIgnoreCase(name));
    }

    public void removeIgnoreAndSave(String name) {
        removeIgnore(name);
        IgnoreConfig.getInstance().save();
    }

    public boolean isIgnore(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            return isIgnore(player.getName().getString());
        }
        return false;
    }

    public boolean isIgnore(String ignore) {
        return ignores.stream().anyMatch(isIgnore -> isIgnore.getName().equalsIgnoreCase(ignore));
    }
    
    // Новые методы для фильтров
    public void addChatFilter(ChatFilter filter) {
        chatFilters.add(filter);
    }
    
    public void addChatFilterAndSave(ChatFilter filter) {
        addChatFilter(filter);
        IgnoreConfig.getInstance().save();
    }
    
    public void removeChatFilter(int index) {
        if (index >= 0 && index < chatFilters.size()) {
            chatFilters.remove(index);
        }
    }
    
    public void removeChatFilterAndSave(int index) {
        removeChatFilter(index);
        IgnoreConfig.getInstance().save();
    }
    
    public void clearChatFilters() {
        chatFilters.clear();
    }
    
    public void clearChatFiltersAndSave() {
        clearChatFilters();
        IgnoreConfig.getInstance().save();
    }
    
    public boolean shouldFilterMessage(String message, String sender) {
        return chatFilters.stream().anyMatch(filter -> filter.matches(message, sender));
    }

    public void clear() {
        ignores.clear();
    }

    public void clearAndSave() {
        clear();
        IgnoreConfig.getInstance().save();
    }

    public List<String> getIgnoreNames() {
        return ignores.stream().map(Ignore::getName).collect(Collectors.toList());
    }

    public int size() {
        return ignores.size();
    }

    public void setIgnores(List<String> names) {
        ignores.clear();
        for (String name : names) {
            ignores.add(new Ignore(name));
        }
    }
    
    public void setChatFilters(List<ChatFilter> filters) {
        chatFilters.clear();
        chatFilters.addAll(filters);
    }
}
