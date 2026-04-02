package fun.aegis.main.client;

import java.io.File;

public interface ClientInfoProvider {
    String userName();
    String clientName();
    String role();

    String getFullInfo();

    File clientDir();

    File filesDir();

    File configsDir();
}
