package rich.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.regex.Pattern;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");

    @Inject(method = "collectPlayerEntries", at = @At("RETURN"), cancellable = true)
    private void addVanishedEntries(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<PlayerListEntry> originalList = cir.getReturnValue();
        List<PlayerListEntry> vanishedList = new ArrayList<>();

        Scoreboard scoreboard = client.world.getScoreboard();
        List<Team> teams = new ArrayList<>(scoreboard.getTeams());
        teams.sort(Comparator.comparing(Team::getName));

        Collection<PlayerListEntry> online = client.player.networkHandler.getPlayerList();

        for (Team team : teams) {
            Collection<String> members = team.getPlayerList();
            if (members.size() != 1) continue;
            String name = members.iterator().next();
            if (!NAME_PATTERN.matcher(name).matches()) continue;

            boolean present = online.stream().anyMatch(e -> e.getProfile() != null && name.equals(e.getProfile().name()));
            if (present) continue;

            MutableText displayName = Text.empty()
                    .append(Text.literal("[").formatted(Formatting.GRAY))
                    .append(Text.literal("V").formatted(Formatting.RED))
                    .append(Text.literal("] ").formatted(Formatting.GRAY))
                    .append(team.getPrefix())
                    .append(Text.literal(name).formatted(Formatting.GRAY));

            GameProfile fakeProfile = new GameProfile(UUID.randomUUID(), name);
            PlayerListEntry fake = new PlayerListEntry(fakeProfile, client.isInSingleplayer());
            fake.setDisplayName(displayName);
            fake.setListOrder(Integer.MIN_VALUE);
            vanishedList.add(fake);
        }

        List<PlayerListEntry> finalList = new ArrayList<>();
        finalList.addAll(vanishedList);
        finalList.addAll(originalList);

        cir.setReturnValue(finalList);
    }
}
