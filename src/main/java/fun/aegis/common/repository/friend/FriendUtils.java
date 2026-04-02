package fun.aegis.common.repository.friend;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.*;
import java.util.List;


@Getter
@UtilityClass
public class FriendUtils {
    @Getter
    public final List<Friend> friends = new ArrayList<>();

    public void addFriend(PlayerEntity player) {
        addFriend(player.getName().getString());
    }

    public void addFriend(String name) {
        friends.add(new Friend(name));
    }

    public void removeFriend(PlayerEntity player) {
        removeFriend(player.getName().getString());
    }

    public void removeFriend(String name) {
        friends.removeIf(friend -> friend.getName().equalsIgnoreCase(name));
    }



    public boolean isFriend(Entity entity) {
        if (entity instanceof PlayerEntity player) return isFriend(player.getName().getString());
        return false;
    }
    public boolean isFriend(String friend) {
        return friends.stream().anyMatch(isFriend -> isFriend.getName().equals(friend));
    }

    public void clear() {
        friends.clear();
    }
}
