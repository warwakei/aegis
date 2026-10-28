package rich.modules.module.category;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ModuleCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    MISC("Misc"),
    AUTOBUY("AutoBuy"),
    JENRO("Jenro");

    final String readableName;
}