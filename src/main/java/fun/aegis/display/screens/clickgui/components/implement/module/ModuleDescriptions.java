package fun.aegis.display.screens.clickgui.components.implement.module;

import fun.aegis.features.module.Module;
import fun.aegis.features.impl.combat.*;
import fun.aegis.features.impl.misc.*;
import fun.aegis.features.impl.movement.*;
import fun.aegis.features.impl.player.*;
import fun.aegis.features.impl.render.*;

public class ModuleDescriptions {
    public static String getDescription(Module module) {
        if (module instanceof ServerHelper)
            return "Помогает взаимодействовать с сервером, предоставляя полезные функции.";
        if (module instanceof WaterSpeed)
            return "Увеличивает скорость передвижения в воде.";
        if (module instanceof ItemScroller)
            return "Настраивает поведение предметов для удобства.";
        if (module instanceof Hud)
            return "Отображает дополнительную информацию на экране.";
        if (module instanceof AuctionHelper)
            return "Помогает управлять аукционами на сервере.";
        if (module instanceof ProjectilePrediction)
            return "Предсказывает траекторию полета снарядов.";
        if (module instanceof Strafe)
            return "Помогает игроку при ходьбе.";
        if (module instanceof TargetStrafe)
            return "Крутится вокруг таргета в aura.";
        if (module instanceof Jesus)
            return "Дает возможность игроку ходить по воде.";
        if (module instanceof ProjectileHelper)
            return "Помогает игроку наводится на цель.";
        if (module instanceof XRay)
            return "Позволяет видеть сквозь блоки для поиска ресурсов.";
        if (module instanceof TriggerBot)
            return "Бьет сущность если игрок смотрит на нее.";
        if (module instanceof Aura)
            return "Автоматически атакует ближайших врагов.";
        if (module instanceof AimBot)
            return "Помогает плавно или мгновенно наводиться на цель.";
        if (module instanceof AutoBuff)
            return "Автоматически кидает зелья баффов в ноги.";
        if (module instanceof AutoSwap)
            return "Автоматически меняет предметы в руке.";
        if (module instanceof NoFriendDamage)
            return "Предотвращает урон по союзникам.";
        if (module instanceof SelfDestruct)
            return "Скрывает чит с игры, находится в разработке.";
        if (module instanceof HitBoxModule)
            return "Изменяет размер хитбоксов сущностей.";
        if (module instanceof AntiBot)
            return "Обнаруживает и игнорирует ботов на сервере.";
        if (module instanceof AutoSprint)
            return "Автоматически включает спринт при движении.";
        if (module instanceof NoSlow)
            return "Устраняет замедление при определенных действиях.";
        if (module instanceof InventoryMove)
            return "Позволяет двигаться при открытом интерфейсе.";
        if (module instanceof Blink)
            return "Создает иллюзию телепортации для других игроков.";
        if (module instanceof Fly)
            return "Позволяет летать в режиме выживания.";
        if (module instanceof AutoTotem)
            return "Автоматически экипирует тотем при низком здоровье.";
        if (module instanceof FreeCam)
            return "Предоставляет инструменты для отладки камеры.";
        if (module instanceof ChestStealer)
            return "Быстро забирает предметы из контейнеров.";
        if (module instanceof AutoTpAccept)
            return "Автоматически принимает запросы на телепортацию.";
        if (module instanceof AutoLeave)
            return "Автоматически покидает сервер при угрозе.";
        if (module instanceof AutoArmor)
            return "Автоматически надевает лучшую броню.";
        if (module instanceof NoInteract)
            return "Блокирует взаимодействие с объектами.";
        if (module instanceof ServerRPSpoofer)
            return "Подделывает данные для серверов.";
        if (module instanceof ShiftTap)
            return "Автоматически приседает при ударе.";
        if (module instanceof ClickPearl)
            return "Автоматически использует жемчужины при клике.";
        if (module instanceof ClickFriend)
            return "Добавляет игроков в список друзей по клику.";
        if (module instanceof WindJump)
            return "Усиливает прыжки с учетом направления ветра.";
        if (module instanceof AirStuck)
            return "Фризит игрока в воздухе, предотвращая движение.";
        if (module instanceof ElytraMotion)
            return "Улучшает управление и скорость полета на элитрах.";
        if (module instanceof ElytraHelper)
            return "Улучшает управление элитрами.";
        if (module instanceof TabParser)
            return "Парсит таблисту для получения информации.";
        if (module instanceof ElytraTarget)
            return "Помогает в полете на элитрах.";
        if (module instanceof FakeLag)
            return "Создает фейк лаг для обхода античитов.";
        return "Описание модуля отсутствует.";
    }
}
