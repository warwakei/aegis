package rich.command.helpers;

import rich.command.Command;
import rich.command.CommandManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 *  © 2026 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

public class TabCompleteHelper {
    private final List<String> completions;
    private String prefix = "";
    private boolean sorted = false;

    public TabCompleteHelper() {
        this.completions = new ArrayList<>();
    }

    public TabCompleteHelper append(String... strings) {
        completions.addAll(Arrays.asList(strings));
        return this;
    }

    public TabCompleteHelper addCommands(CommandManager manager) {
        String filter = this.prefix.toLowerCase();

        for (Command cmd : manager.getCommands()) {
            String mainName = cmd.getName();

            if (filter.isEmpty()) {
                completions.add(mainName);
            } else if (mainName.toLowerCase().startsWith(filter)) {
                completions.add(mainName);
            } else {
                for (String alias : cmd.getAliases()) {
                    if (alias.toLowerCase().startsWith(filter)) {
                        completions.add(alias);
                        break;
                    }
                }
            }
        }
        return this;
    }

    public TabCompleteHelper addCommands(List<Command> commands) {
        String filter = this.prefix.toLowerCase();

        for (Command cmd : commands) {
            String mainName = cmd.getName();

            if (filter.isEmpty()) {
                completions.add(mainName);
            } else if (mainName.toLowerCase().startsWith(filter)) {
                completions.add(mainName);
            } else {
                for (String alias : cmd.getAliases()) {
                    if (alias.toLowerCase().startsWith(filter)) {
                        completions.add(alias);
                        break;
                    }
                }
            }
        }
        return this;
    }

    public TabCompleteHelper filterPrefix(String prefix) {
        this.prefix = prefix.toLowerCase();
        return this;
    }

    public TabCompleteHelper sortAlphabetically() {
        this.sorted = true;
        return this;
    }

    public TabCompleteHelper prepend(String... strings) {
        List<String> temp = new ArrayList<>(Arrays.asList(strings));
        temp.addAll(completions);
        completions.clear();
        completions.addAll(temp);
        return this;
    }

    public Stream<String> stream() {
        Stream<String> stream = completions.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix));

        if (sorted) {
            stream = stream.sorted();
        }

        return stream;
    }

    public List<String> toList() {
        return stream().toList();
    }

    public String[] toArray() {
        return stream().toArray(String[]::new);
    }
}