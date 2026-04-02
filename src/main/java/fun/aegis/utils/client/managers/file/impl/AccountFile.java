package fun.aegis.utils.client.managers.file.impl;

import antidaunleak.api.annotation.Native;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import fun.aegis.display.screens.mainmenu.altscreen.impl.AccountData;
import fun.aegis.display.screens.mainmenu.altscreen.impl.AccountRepository;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import fun.aegis.utils.client.managers.file.ClientFile;
import fun.aegis.utils.client.managers.file.exception.FileLoadException;
import fun.aegis.utils.client.managers.file.exception.FileSaveException;

import java.io.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountFile extends ClientFile {
    AccountRepository accountRepository;

    public AccountFile(AccountRepository accountRepository) {
        super("Accounts");
        this.accountRepository = accountRepository;
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(path, getName() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            AccountData data = new AccountData();
            data.accounts = accountRepository.accountList;
            data.currentAccount = accountRepository.currentAccount;
            gson.toJson(data, writer);
        } catch (JsonIOException | IOException e) {
            throw new FileSaveException("Failed to save accounts to file", e);
        }
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        Gson gson = new Gson();
        File file = new File(path, getName() + ".json");
        try (FileReader reader = new FileReader(file)) {
            AccountData data = gson.fromJson(reader, AccountData.class);
            accountRepository.accountList.clear();
            if (data.accounts != null) {
                accountRepository.accountList.addAll(data.accounts);
            }
            if (data.currentAccount != null) {
                accountRepository.currentAccount = data.currentAccount;
            }
        } catch (IOException e) {
            throw new FileLoadException("Failed to load accounts from file", e);
        } catch (JsonSyntaxException e) {
            throw new FileLoadException("JSON syntax error, accounts config cannot be loaded", e);
        } catch (JsonIOException e) {
            throw new FileLoadException("JSON IO error, accounts config cannot be loaded", e);
        }
    }
}
