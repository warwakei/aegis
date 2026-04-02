package fun.aegis.utils.client.managers.api.command.datatypes;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.util.InputUtil;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.managers.api.command.helpers.TabCompleteHelper;
import fun.aegis.utils.client.chat.StringHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public enum KeyDataType implements IDatatypeFor<Map.Entry<java.lang.String, Integer>> {
    INSTANCE;

    @Override
    public Stream<java.lang.String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
        Stream<java.lang.String> keys = getKeys()
                .keySet()
                .stream();

        java.lang.String context = datatypeContext
                .getConsumer()
                .getString();

        return new TabCompleteHelper()
                .append(keys)
                .filterPrefix(context)
                .sortAlphabetically()
                .stream();
    }

    @Override
    public Map.Entry<java.lang.String, Integer> get(IDatatypeContext datatypeContext) throws CommandException {
        java.lang.String key = datatypeContext
                .getConsumer()
                .getString();

        return getKeys()
                .entrySet()
                .stream()
                .filter(s -> s.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }

    private static Map<java.lang.String, Integer> getKeys() {
        Map<java.lang.String, Integer> keys = new HashMap<>();
        for (Int2ObjectMap.Entry<InputUtil.Key> entry : InputUtil.Type.KEYSYM.map.int2ObjectEntrySet()) {
            int keyCode = entry.getIntKey();
            java.lang.String bindName = StringHelper.getBindName(keyCode).toLowerCase();
            keys.put(bindName, keyCode);
        }
        return keys;
    }
}
