package fun.aegis.mixins.client.screen.mainmenu;

import com.llamalad7.mixinextras.sugar.Local;
import fun.aegis.features.impl.misc.SelfDestruct;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Mixin(ServerList.class)
public class ServerListMixin {
    @Unique
    private final List<ServerInfo> sponsorServers = List.of(
            new ServerInfo("FunTime", "mc.funtime.su", ServerInfo.ServerType.LAN),
            new ServerInfo("HolyWorld", "mc.holyworld.ru", ServerInfo.ServerType.LAN),
            new ServerInfo("ReallyWorld", "mc.reallyworld.ru", ServerInfo.ServerType.LAN),
            new ServerInfo("SpookyTime", "mc.spookytime.net", ServerInfo.ServerType.LAN),
            new ServerInfo("AresMine", "mc.aresmine.ru", ServerInfo.ServerType.LAN)
    );

    @Shadow
    @Final
    private List<ServerInfo> servers;

    @Inject(method = "loadFile", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/ServerList;hiddenServers:Ljava/util/List;", ordinal = 0))
    private void loadFileHook(CallbackInfo ci) {
        if (SelfDestruct.unhooked) return;

        removeDuplicateSponsors();
        addMissingSponsors();
    }

    @Redirect(method = "saveFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtList;add(Ljava/lang/Object;)Z", ordinal = 0))
    private boolean saveFileHook(NbtList instance, Object o, @Local(ordinal = 0) ServerInfo info) {
        if (SelfDestruct.unhooked) {
            return instance.add((NbtElement) o);
        }

        if (isSponsorServer(info)) {
            return true;
        }

        return instance.add((NbtElement) o);
    }

    @Unique
    private void removeDuplicateSponsors() {
        Set<String> sponsorAddresses = new HashSet<>();
        for (ServerInfo sponsor : sponsorServers) {
            sponsorAddresses.add(sponsor.address.toLowerCase());
        }

        Iterator<ServerInfo> iterator = servers.iterator();
        Set<String> seenAddresses = new HashSet<>();

        while (iterator.hasNext()) {
            ServerInfo server = iterator.next();
            String address = server.address.toLowerCase();

            if (sponsorAddresses.contains(address)) {
                if (seenAddresses.contains(address)) {
                    iterator.remove();
                } else {
                    seenAddresses.add(address);
                }
            }
        }
    }

    @Unique
    private void addMissingSponsors() {
        Set<String> existingAddresses = new HashSet<>();
        for (ServerInfo server : servers) {
            existingAddresses.add(server.address.toLowerCase());
        }

        for (ServerInfo sponsor : sponsorServers) {
            if (!existingAddresses.contains(sponsor.address.toLowerCase())) {
                servers.add(sponsor);
            }
        }
    }

    @Unique
    private boolean isSponsorServer(ServerInfo info) {
        for (ServerInfo sponsor : sponsorServers) {
            if (sponsor.address.equalsIgnoreCase(info.address)) {
                return true;
            }
        }
        return false;
    }
}
