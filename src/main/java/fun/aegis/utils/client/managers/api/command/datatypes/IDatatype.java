package fun.aegis.utils.client.managers.api.command.datatypes;

import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.display.interfaces.QuickImports;

import java.util.stream.Stream;

public interface IDatatype extends QuickImports {
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException;
}
