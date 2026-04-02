package fun.aegis.utils.client.managers.api.command.datatypes;

import net.minecraft.scoreboard.Team;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.managers.api.command.helpers.TabCompleteHelper;

import java.util.Collection;
import java.util.stream.Stream;

public enum TabPlayerDataType implements IDatatypeFor<Team> {
    INSTANCE;

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return new TabCompleteHelper()
                .append(getTeam().stream()
                        .map(Team::getPlayerList)
                        .map(Object::toString)
                        .map(s -> s.replaceAll("[\\[\\]]", "")))
                .filterPrefix(ctx.getConsumer().getString())
                .sortAlphabetically()
                .stream();
    }

    @Override
    public Team get(IDatatypeContext datatypeContext) throws CommandException {
        final String username = datatypeContext.getConsumer().getString();
        return getTeam().stream()
                .filter(s -> s.getName().equalsIgnoreCase(username))
                .findFirst().orElse(null);
    }

    public Collection<Team> getTeam() {
        return mc.world.getScoreboard().getTeams();
    }
}
