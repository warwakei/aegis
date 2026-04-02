package rich.util.config.impl.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Identifier;
import rich.screens.account.AccountEntry;
import rich.util.config.impl.consolelogger.Logger;
import rich.util.session.SessionChanger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AccountConfig {

    private static AccountConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;
    private final List<AccountEntry> accounts = new ArrayList<>();
    private String activeAccountName = "";
    private String activeAccountDate = "";
    private String activeAccountSkin = "";

    private AccountConfig() {
        Path configDir = Paths.get("AegisNeo", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("accounts.json");
    }

    public static AccountConfig getInstance() {
        if (instance == null) {
            instance = new AccountConfig();
        }
        return instance;
    }

    public void save() {
        try {
            JsonObject root = new JsonObject();

            JsonArray accountsArray = new JsonArray();
            for (AccountEntry entry : accounts) {
                JsonObject accountObj = new JsonObject();
                accountObj.addProperty("name", entry.getName());
                accountObj.addProperty("date", entry.getDate());
                accountObj.addProperty("skin", entry.getSkin() != null ? entry.getSkin().toString() : "");
                accountObj.addProperty("pinned", entry.isPinned());
                accountObj.addProperty("originalIndex", entry.getOriginalIndex());
                accountsArray.add(accountObj);
            }
            root.add("accounts", accountsArray);

            JsonObject activeObj = new JsonObject();
            activeObj.addProperty("name", activeAccountName);
            activeObj.addProperty("date", activeAccountDate);
            activeObj.addProperty("skin", activeAccountSkin);
            root.add("active", activeObj);

            Files.writeString(configPath, gson.toJson(root), StandardCharsets.UTF_8);
            Logger.success("AccountConfig: accounts.json saved successfully!");
        } catch (IOException e) {
            Logger.error("AccountConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                Logger.info("AccountConfig: No config file found, using defaults.");
                return;
            }

            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            if (json == null || json.trim().isEmpty()) {
                Logger.error("AccountConfig: Config file is empty.");
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            accounts.clear();
            if (root.has("accounts")) {
                JsonArray accountsArray = root.getAsJsonArray("accounts");
                for (int i = 0; i < accountsArray.size(); i++) {
                    JsonObject accountObj = accountsArray.get(i).getAsJsonObject();
                    String name = accountObj.has("name") ? accountObj.get("name").getAsString() : "";
                    String date = accountObj.has("date") ? accountObj.get("date").getAsString() : "";
                    String skinStr = accountObj.has("skin") ? accountObj.get("skin").getAsString() : "";
                    boolean pinned = accountObj.has("pinned") && accountObj.get("pinned").getAsBoolean();
                    int originalIndex = accountObj.has("originalIndex") ? accountObj.get("originalIndex").getAsInt() : i;

                    Identifier skin = null;
                    if (!skinStr.isEmpty()) {
                        try {
                            skin = Identifier.of(skinStr);
                        } catch (Exception ignored) {}
                    }

                    AccountEntry entry = new AccountEntry(name, date, skin, pinned, originalIndex);
                    accounts.add(entry);
                }
            }

            if (root.has("active")) {
                JsonObject activeObj = root.getAsJsonObject("active");
                activeAccountName = activeObj.has("name") ? activeObj.get("name").getAsString() : "";
                activeAccountDate = activeObj.has("date") ? activeObj.get("date").getAsString() : "";
                activeAccountSkin = activeObj.has("skin") ? activeObj.get("skin").getAsString() : "";
            }

            if (!activeAccountName.isEmpty()) {
                SessionChanger.changeUsername(activeAccountName);
            }

            Logger.success("AccountConfig: accounts.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("AccountConfig: Load failed! " + e.getMessage());
        }
    }

    public List<AccountEntry> getAccounts() {
        return accounts;
    }

    public List<AccountEntry> getSortedAccounts() {
        List<AccountEntry> sorted = new ArrayList<>(accounts);
        sorted.sort((a, b) -> {
            if (a.isPinned() && !b.isPinned()) return -1;
            if (!a.isPinned() && b.isPinned()) return 1;
            return Integer.compare(a.getOriginalIndex(), b.getOriginalIndex());
        });
        return sorted;
    }

    public void addAccount(AccountEntry entry) {
        entry.setOriginalIndex(accounts.size());
        accounts.add(entry);
        save();
    }

    public void removeAccount(AccountEntry entry) {
        accounts.remove(entry);
        updateOriginalIndices();
        save();
    }

    public void removeAccountByIndex(int sortedIndex) {
        List<AccountEntry> sorted = getSortedAccounts();
        if (sortedIndex >= 0 && sortedIndex < sorted.size()) {
            AccountEntry toRemove = sorted.get(sortedIndex);
            accounts.remove(toRemove);
            updateOriginalIndices();
            save();
        }
    }

    public void clearAllAccounts() {
        accounts.clear();
        activeAccountName = "";
        activeAccountDate = "";
        activeAccountSkin = "";
        save();
    }

    public AccountEntry getAccountBySortedIndex(int sortedIndex) {
        List<AccountEntry> sorted = getSortedAccounts();
        if (sortedIndex >= 0 && sortedIndex < sorted.size()) {
            return sorted.get(sortedIndex);
        }
        return null;
    }

    private void updateOriginalIndices() {
        List<AccountEntry> unpinned = new ArrayList<>();
        for (AccountEntry entry : accounts) {
            if (!entry.isPinned()) {
                unpinned.add(entry);
            }
        }
        for (int i = 0; i < unpinned.size(); i++) {
            unpinned.get(i).setOriginalIndex(i);
        }
    }

    public void togglePin(int sortedIndex) {
        List<AccountEntry> sorted = getSortedAccounts();
        if (sortedIndex >= 0 && sortedIndex < sorted.size()) {
            AccountEntry entry = sorted.get(sortedIndex);
            entry.togglePinned();
            save();
        }
    }

    public String getActiveAccountName() {
        return activeAccountName;
    }

    public String getActiveAccountDate() {
        return activeAccountDate;
    }

    public Identifier getActiveAccountSkin() {
        if (activeAccountSkin.isEmpty()) {
            return null;
        }
        try {
            return Identifier.of(activeAccountSkin);
        } catch (Exception e) {
            return null;
        }
    }

    public void setActiveAccount(String name, String date, Identifier skin) {
        this.activeAccountName = name;
        this.activeAccountDate = date;
        this.activeAccountSkin = skin != null ? skin.toString() : "";
        SessionChanger.changeUsername(name);
        save();
    }
}