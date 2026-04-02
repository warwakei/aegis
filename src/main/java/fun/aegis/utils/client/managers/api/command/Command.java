package fun.aegis.utils.client.managers.api.command;

import fun.aegis.utils.display.interfaces.QuickImports;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public abstract class Command implements ICommand, QuickImports {
    protected final List<String> names;

    protected Command(String... names) {
        this.names = Stream.of(names)
                .map(string -> string.toLowerCase(Locale.US))
                .toList();
    }

    @Override
    public final List<String> getNames() {
        return this.names;
    }
}
