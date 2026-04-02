package fun.aegis.common.discord.utils;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class RPCButton implements Serializable {
    private final String url;
    private final String label;

    public static RPCButton create(String label, String url) {
        label = label.substring(0, Math.min(label.length(), 31));
        return new RPCButton(label, url);
    }

    protected RPCButton(String label, String url) {
        this.label = label;
        this.url = url;
    }
}
