package fun.aegis.features.module;

import fun.aegis.features.impl.combat.*;
import fun.aegis.features.impl.misc.*;
import fun.aegis.features.impl.movement.*;
import fun.aegis.features.impl.player.*;
import fun.aegis.features.impl.render.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<Module> modules = new ArrayList<>();

    public void setup() {
        register(
                new JumpCircle(),
                new BetterMinecraft(),
                new ProjectileHelper(),
                new TargetStrafe(),
                new Strafe(),
                new AirStuck(),
                new NoEntityTrace(),
                new NoFallDamage(),
                new ElytraMotion(),
                new LongJump(),
                new ShiftTap(),
                new AspectRatio(),
                new FreeLook(),
                new ClickPearl(),
                // new HitEffect(),
                new ClickFriend(),
                new TabParser(),
                new WindJump(),
                new TargetESP(),
                new NoWeb(),
                new ServerHelper(),
                new WaterSpeed(),
                new ItemScroller(),
                new Hud(),
                new AuctionHelper(),
                new ProjectilePrediction(),
                new WorldParticles(),
                new ElytraTarget(),
                new XRay(),
                new TriggerBot(),
                new Aura(),
                new AimBot(),
                new AutoBuff(),
                new AutoSwap(),
                new NoFriendDamage(),
                new HitBoxModule(),
                new AntiBot(),
                new AutoCrystal(),
                new AutoSprint(),
                new Speed(),
                new NoPush(),
                new ElytraHelper(),
                new NoDelay(),
                new Velocity(),
                new AutoRespawn(),
                new NoSlow(),
                new InventoryMove(),
                new Blink(),
                new AutoTool(),
                new Fly(),
                new ElytraFly(),
                new CameraSettings(),
                new Phase(),
                new SwingAnimation(),
                new ViewModel(),
                new BlockOverlay(),
                new Jesus(),
                new AutoTotem(),
                new FastBow(),
                new Esp(),
                new BlockESP(),
                new FreeCam(),
                new ChestStealer(),
                new AutoTpAccept(),
                new Arrows(),
                new AutoLeave(),
                new WorldTweaks(),
                new NoRender(),
                new NameProtect(),
                new SelfDestruct(),
                new SeeInvisible(),
                new TargetPearl(),
                new AutoArmor(),
                new AutoUse(),
                new NoInteract(),
                new CrossHair(),
                new SuperFireWork(),
                new Spider(),
                new ServerRPSpoofer(),
                // new ChinaHat(),
                new KillEffect(),
                new FakeLag(),
                new SantaHatModule(),
                new Trails(),
                new FireFly(),
                // new LineGlyphs(),
                new ItemPhysic(),
                new ChatSpammer()

        );
    }

    public void register(Module... module) {
        modules.addAll(List.of(module));
    }

    public List<Module> modules() {
        return modules;
    }
}
