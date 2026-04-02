package rich.command.helpers;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import rich.command.CommandManager;

import java.util.List;
import java.util.function.Function;

import static rich.command.impl.HelpCommand.getLine;

/**
 *  © 2026 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

public class Paginator<T> {

    private final List<T> items;
    private final int itemsPerPage;
    private int currentPage;

    public Paginator(List<T> items) {
        this(items, 8);
    }

    public Paginator(List<T> items, int itemsPerPage) {
        this.items = items;
        this.itemsPerPage = itemsPerPage;
        this.currentPage = 1;
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) items.size() / itemsPerPage));
    }

    public List<T> getCurrentPageItems() {
        int start = (currentPage - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());
        return items.subList(start, end);
    }

    public void setPage(int page) {
        this.currentPage = Math.max(1, Math.min(page, getTotalPages()));
    }

    public void display(
            Runnable header,
            Function<T, MutableText> itemFormatter,
            String commandPrefix
    ) {
        CommandManager manager = CommandManager.getInstance();

        if (header != null) {
            header.run();
        }

        for (T item : getCurrentPageItems()) {
            MutableText formatted = itemFormatter.apply(item);
            manager.sendRaw(formatted);
        }

        if (getTotalPages() > 1) {
            displayNavigation(manager, commandPrefix);
        } else {
            manager.sendRaw(Text.literal(getLine()));
        }
    }

    private void displayNavigation(CommandManager manager, String commandPrefix) {
        manager.sendRaw(Text.literal(getLine()));

        MutableText navigation = Text.literal("");

        if (currentPage > 1) {
            MutableText prevButton = Text.literal("§8[§b◄ Назад§8]");
            String prevCommand = commandPrefix + " " + (currentPage - 1);
            prevButton.setStyle(prevButton.getStyle()
                    .withHoverEvent(new HoverEvent.ShowText(
                            Text.literal("§7Страница " + (currentPage - 1))))
                    .withClickEvent(new ClickEvent.RunCommand(prevCommand)));
            navigation.append(prevButton);
        } else {
            navigation.append(Text.literal("§8[§7◄ Назад§8]"));
        }

        navigation.append(Text.literal(" §7Страница §b" + currentPage + "§7/§b" + getTotalPages() + " "));

        if (currentPage < getTotalPages()) {
            MutableText nextButton = Text.literal("§8[§bВперёд ►§8]");
            String nextCommand = commandPrefix + " " + (currentPage + 1);
            nextButton.setStyle(nextButton.getStyle()
                    .withHoverEvent(new HoverEvent.ShowText(
                            Text.literal("§7Страница " + (currentPage + 1))))
                    .withClickEvent(new ClickEvent.RunCommand(nextCommand)));
            navigation.append(nextButton);
        } else {
            navigation.append(Text.literal("§8[§7Вперёд ►§8]"));
        }

        manager.sendRaw(navigation);
    }

    public static <T> void paginate(
            String[] args,
            Paginator<T> paginator,
            Runnable header,
            Function<T, MutableText> itemFormatter,
            String commandPrefix
    ) {
        if (args.length > 0) {
            try {
                int page = Integer.parseInt(args[0]);
                paginator.setPage(page);
            } catch (NumberFormatException ignored) {
            }
        }

        paginator.display(header, itemFormatter, commandPrefix);
    }
}