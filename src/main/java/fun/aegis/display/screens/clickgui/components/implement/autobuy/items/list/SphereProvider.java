package fun.aegis.display.screens.clickgui.components.implement.autobuy.items.list;

import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.AutoBuyableItem;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.customitem.CustomItem;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.defaultsetpricec.Defaultpricec;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SphereProvider {
    public static List<AutoBuyableItem> getSpheres() {
        List<AutoBuyableItem> spheres = new ArrayList<>();
        List<Text> andromedaLore = List.of(
                Text.literal("Сфера хранит взгляд"),
                Text.literal("Андромеды, ведущий"),
                Text.literal("сквозь мрак и звёзды")
        );
        spheres.add(createSphere("[★] Сфера Андромеды", "9d1ee31a-65ad-4d5c-850e-b8dda3875e1e", "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NTEwODQzNywKICAicHJvZmlsZUlkIiA6ICIzMjNiYjlkYzkwZWU0Nzk5YjUxYzE3NjRmZDRhNjI3OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJOcGllIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQ0ZmZlM2YzNThmMjA5YmFkOGZmZjRkYzQ4MjQ1ZDliYWYwYTAzMWIzYzFlZTZiNzU4NDYwYTMzOWIxNTE5ZTIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==", Defaultpricec.getPrice("Сфера Андромеды"), andromedaLore));
        List<Text> pandoraLore = List.of(
                Text.literal("Сфера таит секреты"),
                Text.literal("Пандоры, дарующие"),
                Text.literal("благодать её хозяину")
        );
        spheres.add(createSphere("[★] Сфера Пандоры", "812d254a-5d3b-41b6-93f8-bd8b08a0c07c", "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NTY2NTExNCwKICAicHJvZmlsZUlkIiA6ICJkNzJlNGJjZDIyZGI0NjQ4OTUxNTc0M2UyYTRmMWFjMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJhdnZheSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84ZTUxZTY1ZWI0MDUyNzcyMzgyYzllNTA3YTU0YmRlZDQzZTM5Zjc1NWI1ZGRmNTViM2YzOTQ0M2NlZDQ2N2Y0IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=", Defaultpricec.getPrice("Сфера Пандоры"), pandoraLore));
        List<Text> titanLore = List.of(
                Text.literal("Мифическая сфера древних"),
                Text.literal("Титанов, обладающая"),
                Text.literal("их мощью и прочностью")
        );
        spheres.add(createSphere("[★] Сфера Титана", "05c21710-125c-4738-a102-2e1a4cd577e1", "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM1NDQ1NTE5MiwKICAicHJvZmlsZUlkIiA6ICJkOTcwYzEzZTM4YWI0NzlhOTY1OGM1ZDQ1MjZkMTM0YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJDcmltcHlMYWNlODUxMjciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlOTY5ODQ1OGI3ODQxYzk2YWU0ZjI0ZWM4NGFlMDE3MjQxMDA2NDFjNTY0ZTJhN2IxODVmNDA2ZThlZDIzIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=", Defaultpricec.getPrice("Сфера Титана"), titanLore));
        List<Text> apolloLore = List.of(
                Text.literal("Святой свет Аполлона,"),
                Text.literal("переполняющий силой,"),
                Text.literal("таится в этой сфере")
        );
        spheres.add(createSphere("[★] Сфера Аполлона", "478bd194-bd00-4c33-b3df-31115657f9a3", "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjYyNTM0NywKICAicHJvZmlsZUlkIiA6ICJhMjk1ODZmYmU1ZDk0Nzk2OWZjOGQ4ZGE0NzlhNDNlZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJMZXZlMjQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQxMTdiNjAxOGZlZjBkNTE1NjcyMTczZTNiMjZlNjYwZDY1MWU1ODc2YmE2ZDAzZTUzNDIyNzBjNDliZWM4MCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", Defaultpricec.getPrice("Сфера Аполлона"), apolloLore));
        List<Text> astreaLore = List.of(
                Text.literal("Справедливость Астреи"),
                Text.literal("дарует жизнь каждому,"),
                Text.literal("кто её достоин")
        );
        spheres.add(createSphere("[★] Сфера Астрея", "89e3c3fb-65c0-4960-964a-62416b1b3f14", "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NTA2MjQwNywKICAicHJvZmlsZUlkIiA6ICJlMzcxMWU2Y2E0ZmY0NzA4YjY5ZjhiNGZlYzNhZjdhMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNckJ1cnN0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzFhNWFhZGQ1MmE1ZmFiOTcwODgxNDUxYWRmNTZmYmI0OTNhMzU4NTZlYTk2ZjU0ZTMyZWVhNjYyZDc4N2VkMjAiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==", Defaultpricec.getPrice("Сфера Астрея"), astreaLore));
        List<Text> osirisLore = List.of(
                Text.literal("Силы и мощь мёртвых,"),
                Text.literal("дарованные Осирисом,"),
                Text.literal("таятся в этой сфере")
        );
        spheres.add(createSphere("[★] Сфера Осириса", "5053c3bc-dda9-437f-8caf-e8517e0154ba", "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjY2Mzg3NiwKICAicHJvZmlsZUlkIiA6ICI3NGEwMzQxNWY1OTI0ZTA4YjMyMGM2MmU1NGE3ZjJhYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNZXp6aXIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDgxMzYzNWJkODZiMTcxYmJlMTQzYWQ3MWUwOTAyMjkyNjQ5Y2IzYWI4NDQwZWQwMGY4NWNhNmNhMzgyOTkzNiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", Defaultpricec.getPrice("Сфера Осириса"), osirisLore));
        List<Text> chimeraLore = List.of(
                Text.literal("Сфера рождена в"),
                Text.literal("божественно-жгучем"),
                Text.literal("пламени Химеры")
        );
        spheres.add(createSphere("[★] Сфера Химеры", "8ac3951d-c8f9-463c-be7a-f29b558f6376", "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjE4MTEwOSwKICAicHJvZmlsZUlkIiA6ICJiNzRiMGQzNTBkNTk0NTU4YmYyYjBlMDJlYmE4NjE4NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCcmFuZG9uYnBtMjg0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzlmYWJlZWQ0MjRiMjUyYTg5NDVhNjQ0MmI0NjJkNWYzMTQ3MDFhODE2ZGEyZDBhNjljY2RmY2ZkNzQ2ZTU4OGUiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==", Defaultpricec.getPrice("Сфера Химеры"), chimeraLore));
        return spheres;
    }

    private static AutoBuyableItem createSphere(String displayName, String headUuid, String texture, int price, List<Text> lore) {
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("HideFlags", true);
        nbt.putBoolean("Unbreakable", true);
        NbtCompound skullOwner = new NbtCompound();
        skullOwner.putUuid("Id", UUID.fromString(headUuid));
        NbtCompound properties = new NbtCompound();
        NbtList textures = new NbtList();
        NbtCompound textureNbt = new NbtCompound();
        textureNbt.putString("Value", texture);
        textures.add(textureNbt);
        properties.put("textures", textures);
        skullOwner.put("Properties", properties);
        nbt.put("SkullOwner", skullOwner);
        return new CustomItem(displayName, nbt, Items.PLAYER_HEAD, price, null, lore);
    }
}
