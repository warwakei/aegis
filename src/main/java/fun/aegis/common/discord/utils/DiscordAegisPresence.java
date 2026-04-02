package fun.aegis.common.discord.utils;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sun.jna.Structure;
import fun.aegis.common.discord.utils.RPCButton;

public class DiscordAegisPresence extends Structure {
    public String largeImageKey;
    public String largeImageText;
    public String smallImageText;
    public String smallImageKey;
    public boolean instance;
    public String state;
    public String details;
    public long startTimestamp;
    public long endTimestamp;
    public String partyId;
    public int partySize;
    public int partyMax;
    public int partyPrivacy;
    public String matchSecret;
    public String joinSecret;
    public String spectateSecret;
    public String button_label_1;
    public String button_url_1;
    public String button_label_2;
    public String button_url_2;

    public DiscordAegisPresence() {
        this.setStringEncoding("UTF-8");
    }

    protected List<String> getFieldOrder() {
        return Arrays.asList("state", "details", "startTimestamp", "endTimestamp", "largeImageKey", "largeImageText", "smallImageKey", "smallImageText", "partyId", "partySize", "partyMax", "partyPrivacy", "matchSecret", "joinSecret", "spectateSecret", "button_label_1", "button_url_1", "button_label_2", "button_url_2", "instance");
    }

    public static class Builder {
        private final DiscordAegisPresence AegisPresence = new DiscordAegisPresence();

        public Builder setSmallImage(String var1) {
            return this.setSmallImage(var1, "");
        }

        public Builder setDetails(String var1) {
            if (var1 != null && !var1.isEmpty()) {
                this.AegisPresence.details = var1.substring(0, Math.min(var1.length(), 128));
            }

            return this;
        }

        public Builder setLargeImage(String var1, String var2) {
            this.AegisPresence.largeImageKey = var1;
            this.AegisPresence.largeImageText = var2;
            return this;
        }

        public Builder setState(String var1) {
            if (var1 != null && !var1.isEmpty()) {
                this.AegisPresence.state = var1.substring(0, Math.min(var1.length(), 128));
            }

            return this;
        }

        public Builder setInstance(boolean var1) {
            if ((this.AegisPresence.button_label_1 == null || !this.AegisPresence.button_label_1.isEmpty()) && (this.AegisPresence.button_label_2 == null || !this.AegisPresence.button_label_2.isEmpty())) {
                this.AegisPresence.instance = var1;
            }
            return this;
        }

        public Builder setPartyId(String var1) {
            this.AegisPresence.partyId = var1;
            return this;
        }

        public Builder setPartySize(int var1) {
            this.AegisPresence.partySize = var1;
            return this;
        }

        public Builder setPartyMax(int var1) {
            this.AegisPresence.partyMax = var1;
            return this;
        }

        public Builder setPartyPrivacy(int var1) {
            this.AegisPresence.partyPrivacy = var1;
            return this;
        }

        public Builder setSmallImage(String var1, String var2) {
            this.AegisPresence.smallImageKey = var1;
            this.AegisPresence.smallImageText = var2;
            return this;
        }

        public Builder setButtons(List<RPCButton> buttons) {
            if (buttons != null && !buttons.isEmpty()) {
                int var2 = Math.min(buttons.size(), 2);
                this.AegisPresence.button_label_1 = buttons.get(0).getLabel();
                this.AegisPresence.button_url_1 = buttons.get(0).getUrl();
                if (var2 == 2) {
                    this.AegisPresence.button_label_2 = buttons.get(1).getLabel();
                    this.AegisPresence.button_url_2 = buttons.get(1).getUrl();
                }
            }

            return this;
        }

        public Builder setStartTimestamp(OffsetDateTime var1) {
            this.AegisPresence.startTimestamp = var1.toEpochSecond();
            return this;
        }

        public Builder setSecrets(String var1, String var2, String var3) {
            if ((this.AegisPresence.button_label_1 == null || !this.AegisPresence.button_label_1.isEmpty()) && (this.AegisPresence.button_label_2 == null || !this.AegisPresence.button_label_2.isEmpty())) {
                this.AegisPresence.matchSecret = var1;
                this.AegisPresence.joinSecret = var2;
                this.AegisPresence.spectateSecret = var3;
            }
            return this;
        }

        public Builder setStartTimestamp(long var1) {
            this.AegisPresence.startTimestamp = var1;
            return this;
        }

        public Builder setSecrets(String var1, String var2) {
            if ((this.AegisPresence.button_label_1 == null || !this.AegisPresence.button_label_1.isEmpty()) && (this.AegisPresence.button_label_2 == null || !this.AegisPresence.button_label_2.isEmpty())) {
                this.AegisPresence.joinSecret = var1;
                this.AegisPresence.spectateSecret = var2;
            }
            return this;
        }

        public Builder setEndTimestamp(long var1) {
            this.AegisPresence.endTimestamp = var1;
            return this;
        }

        public Builder setEndTimestamp(OffsetDateTime var1) {
            this.AegisPresence.endTimestamp = var1.toEpochSecond();
            return this;
        }

        public Builder setLargeImage(String var1) {
            return this.setLargeImage(var1, "");
        }

        public DiscordAegisPresence build() {
            return this.AegisPresence;
        }
    }
}
