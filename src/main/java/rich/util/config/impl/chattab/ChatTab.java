package rich.util.config.impl.chattab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import rich.util.repository.friend.FriendUtils;
import rich.util.repository.ignore.IgnoreUtils;

@Getter
@Setter
@AllArgsConstructor
public class ChatTab {
    private String name;
    private FilterType filterType;
    private String filterValue;

    public enum FilterType {
        StartingFrom,
        Contains,
        FromPlayer,
        Friends,
        Ignored
    }

    public boolean matches(String sender, String message) {
        if (filterValue == null || filterValue.isEmpty()) {
            return true;
        }

        switch (filterType) {
            case StartingFrom:
                return message.startsWith(filterValue);
            case Contains:
                return message.contains(filterValue);
            case FromPlayer:
                return sender != null && sender.equalsIgnoreCase(filterValue);
            case Friends:
                return sender != null && FriendUtils.isFriend(sender);
            case Ignored:
                return sender != null && IgnoreUtils.isIgnore(sender);
            default:
                return true;
        }
    }
}
