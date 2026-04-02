package rich.util.repository.friend;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import rich.util.config.impl.friend.FriendConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@UtilityClass
public class FriendUtils {
    @Getter
    private final List<Friend> friends = new ArrayList<>();

    public void addFriend(PlayerEntity player) {
        addFriend(player.getName().getString());
    }

    public void addFriend(String name) {
        if (!isFriend(name)) {
            friends.add(new Friend(name));
        }
    }

    public void addFriendAndSave(String name) {
        addFriend(name);
        FriendConfig.getInstance().save();
    }

    public void removeFriend(PlayerEntity player) {
        removeFriend(player.getName().getString());
    }

    public void removeFriend(String name) {
        friends.removeIf(friend -> friend.getName().equalsIgnoreCase(name));
    }

    public void removeFriendAndSave(String name) {
        removeFriend(name);
        FriendConfig.getInstance().save();
    }

    public boolean isFriend(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            return isFriend(player.getName().getString());
        }
        return false;
    }

    public boolean isFriend(String friend) {
        return friends.stream().anyMatch(isFriend -> isFriend.getName().equalsIgnoreCase(friend));
    }

    public void clear() {
        friends.clear();
    }

    public void clearAndSave() {
        clear();
        FriendConfig.getInstance().save();
    }

    public List<String> getFriendNames() {
        return friends.stream().map(Friend::getName).collect(Collectors.toList());
    }

    public int size() {
        return friends.size();
    }

    public void setFriends(List<String> names) {
        friends.clear();
        for (String name : names) {
            friends.add(new Friend(name));
        }
    }
}