# Walkthrough063 — отчёт изменений (не полный, будет дополняться)
status:
update in progress

## 1. EntityMixin.java — Silent Rotation Yaw Hook
**Файл:** `src/main/java/rich/mixin/EntityMixin.java`

Миксин подменял только `pitch` в `getRotationVector(FF)`, оставляя `yaw` клиентским. `isLookingAtTargetCenter()` в `StrikeManager` проверял направление взгляда по клиентской камере — silent-ротация не работала.

**Фикс:** Добавлен `@ModifyVariable` хук для `yaw` (ordinal = 1), подменяющий его на `AngleConnection.INSTANCE.getCurrentAngle().getYaw()`.

---

## 2. Aura.java — Jenro в rotateToTarget + getElytraRotateConstructor
**Файл:** `src/main/java/rich/modules/impl/combat/Aura.java`

**Баг 1:** Режим `"Jenro"` отсутствовал в `switch`-блоке `rotateToTarget()`. Камера не вращалась вообще.

**Фикс:** Добавлен `"Jenro"` к кейсу `"Matrix", "SpookyTime"`.

**Баг 2:** `getElytraRotateConstructor()` не обрабатывал `"Jenro"`, возвращая `MatrixAngle`.

**Фикс:** Добавлен `case "Jenro" -> new JenroAngle()`.

---

## 3. SPAngle.java — Скорость ротации при беге/прыжке
**Файл:** `src/main/java/rich/modules/impl/combat/aura/rotations/SPAngle.java`

SpookyTime был слишком плавным при беге+прыжке — ротация не успевала довестись.

**Фикс:**
- Детект `isMovingAndJumping = MoveUtil.hasPlayerMovement() && !mc.player.isOnGround()`
- `movementSpeedBoost += 0.25f` при прыжке+беге
- `minDistanceFactor` повышен с `0.4f` до `0.65f`
- `speedLerpFactor` поднимается до `0.96f` при движении, `0.98f` при атаке
- Базовая скорость при атаке: `1.0f` → `1.2f`

---

## 4. JenroAngle.java — Ускорение при полёте на элитрах
**Файл:** `src/main/java/rich/modules/impl/combat/aura/rotations/JenroAngle.java`

Ротация Jenro включала быстрый режим только если обе стороны летели на элитрах. В MaceTarget цель на земле — ротация работала на ~10% скорости.

**Фикс:** Введена `isElytraFlying = mc.player.isGliding()`. Все условия заменены на `(elytraTargetMode || isElytraFlying)`.

---

## 5. MaceTarget.java — Замена LinearConstructor
**Файл:** `src/main/java/rich/modules/impl/combat/MaceTarget.java`

`rotateTo()` использовал жёсткий `LinearConstructor` без anti-cheat обхода.

**Фикс:** Динамический выбор:
- Aura включена → `Aura.getInstance().getSmoothMode()`
- Aura выключена → `new JenroAngle()`

---

## 6. StageHandler.java — Атака без нагрудника
**Файл:** `src/main/java/rich/modules/impl/combat/macetarget/stage/StageHandler.java`

`handleAttacking()` требовал `!hasElytra`. Без нагрудника в инвентаре MaceTarget зависал.

**Фикс:** Условие: `(!hasElytra || InventoryUtils.findChestArmorSlot() == -1)`.

---

## 7. AttackHandler.java — Три улучшения [REVERTED]
**Файл:** `src/main/java/rich/modules/impl/combat/macetarget/attack/AttackHandler.java`

### 7а. Аварийный фоллбек [REVERTED]
Было: если `velocityY < -0.1` и разница по Y < 3.5 блоков — атака принудительная. Отменено.

### 7б. Tick Jitter Buffer [REVERTED]
Было: `currentAdaptiveDelay - 5` в `isAttackReady()`. Отменено, обратно `currentAdaptiveDelay`.

### 7в. Улучшенная проверка видимости [REVERTED]
Было: три точки (глаза, центр, ноги) + `hasClearPath()`. Отменено к оригиналу: один raycast глаза→глаза с проверкой `ENTITY` hit и `distToTarget < 1.5` для блоков.

---

## 8. TargetPredictor.java — Быстрая реакция
**Файл:** `src/main/java/rich/modules/impl/combat/macetarget/prediction/TargetPredictor.java`

`VELOCITY_SMOOTHING`: `0.65` → `0.4` — быстрее реакция на манёвры цели.

---

## 9. FlightController.java — Отключение кэша [REVERTED]
**Файл:** `src/main/java/rich/modules/impl/combat/macetarget/flight/FlightController.java`

`CACHE_DURATION_MS`: было `0` (без кэша), отменено обратно на `25`.

---

## 10. Небольшое предупреждение
Все изменения сделаны быстро, возможно есть баги, их нужно будет исправить при тестировании в игре.

Это обновление может сильно сказатся на производительности и требовать больше ресурсов компьютера.

Либо будет вторая версия из разряда "mini" с отключением некоторых улучшений для повышения производительности, либо в этом же обновлении будет много оптимизаций самого майнкрафта кактакового.

---

---

## 11. SplashScreen.java — Полный рефакторинг + анимации
**Файл:** `src/main/java/rich/client/splash/SplashScreen.java`

### 11а. Удалён мёртвый код
Поле `injectedLabel` — не использовалось, удалено.

### 11б. Анимация таймера — порядок инициализации
`animationTimer.start()` перенесён в конец конструктора (после создания всех компонентов), чтобы избежать potential NPE на `percentLabel`/`statusLabel`.

### 11в. Progress card — версия
Добавлена строка `Version.FULL_NAME` (Aegis 0.6.3 "Neo") под статусом загрузки.

### 11г. Ready card — полная переработка
- **Анимированная галочка**: окружность прорисовывается по дуге (`drawArc`), затем чекмарк анимируется через dash-array stroke
- **Пульсирующее свечение галочки** при полном появлении (`glow` second pass)
- **Подзаголовок**: "Модули и ресурсы успешно загружены" (появляется с задержкой 0.3 от readyAnim)
- **Версия**: `Version.FULL_NAME` внизу (появляется с задержкой 0.5)
- **Кнопка Launch**: увеличена до 140×42, шрифт 13px bold
- **Плавающие частицы**: 4 цвета (фиолетовый, синий, розовый, зелёный), спавнятся снизу, поднимаются вверх, затухают. Синхронизированы через `synchronized (readyParticles)`. Отображаются в `paintChildren()` readyCard позади контента

### 11д. Фон — плавная циклическая анимация цвета через HSB
- **Три перекрывающихся синуса** для оттенка (частоты 0.08, 0.13, 0.045 Гц) — никогда не повторяется
- **Насыщенность и яркость** тоже модулируются синусами
- **Bottom color** имеет смещённый оттенок для градиентной глубины
- Используется `Color.getHSBColor()` с `@SuppressWarnings("deprecation")`

### 11е. Неоновые орбы — циклическая смена оттенка
Каждый орб теперь медленно меняет hue (0.10 амплитуда через два синуса) вместо статичного цвета с пульсацией альфы:
- Орб A: фиолетовый → синий → бирюзовый
- Орб B: синий → голубой → сиреневый  
- Орб C: розовый → фиолетовый → coral
- Alpha-пульсация сохранена поверх hue-циклирования

### 11ж. Рамка контейнера — синхронизация с фоном
Цвет неоновой рамки и угловых акцентов теперь привязан к общему оттенку фона (`frameHue`, `dotHue`).

### 11з. Анимация прогресс-бара
Добавлен пульсирующий glow вокруг прогресс-бара (синусоида на `animationTime`).

### 11и. Пульсация процентов
`percentLabel` пульсирует между 85% и 100% яркостью (`Math.sin(animationTime * 2.5f)`).

### 11к. Дыхание статуса  
`statusLabel` дышит альфой 180→255→180 (`Math.sin(animationTime * 1.8f)`).

### 11л. FrostedPanel — усиленная анимация градиента
Градиент карточки теперь использует два наложенных синуса (периоды 1.5с и 2.2с) с амплитудой ±0.6 вместо ±0.1. Добавлен `Math.max(0, ...)` для защиты от отрицательных значений.

---

## 12. Esp.java — Улучшение боксов, fade, health bar
**Файл:** `src/main/java/rich/modules/impl/render/Esp.java`

### 12а. Градиентные corner box
Corner box теперь использует `ColorUtil.brightenColor` (0.3f) и `ColorUtil.darkenColorStatic` (0.4f) — внешние углы ярче, внутренние темнее.

### 12б. Distance fade
2D боксы: `distFade = max(0.15, min(1, 1 - (distance - 8) / 60))`. Alpha каждой линии модулируется.
3D боксы: `distFade = max(0.1, min(1, 1 - (distance - 5) / 50))` — alpha fill и outline синхронно затухают.

### 12в. Health bar под ником
Под блоком ника (после отрисовки текста) рисуется:
- Фон: `Render2D.rect(..., 0x60000000, 1f)`
- Заполнение: ширина `barWidth * hpRatio`, цвет зависит от HP (>60% зелёный, >30% жёлтый, иначе красный)

---

## 13. Шейдеры — Визуальные улучшения

### 13а. `glow_outline.fsh` — Multi-band glow
**Файл:** `src/main/resources/assets/rich/shaders/core/glow_outline.fsh`
- Три слоя свечения: sharp (0.105), soft (0.262), wide (0.525) с весами 0.6/0.3/0.1
- Shimmer-эффект: `sin(distToRay * 40 + progress * 20)`
- Sparkle: `hash12` + `step(0.97)`
- Цветовой сдвиг: теплее на переднем фронте (`+warmShift`), холоднее на хвосте

### 13б. `iridescent_outline.fsh` — Richer spectrum
**Файл:** `src/main/resources/assets/rich/shaders/core/iridescent_outline.fsh`
- Третья гармоника: `harmonicC = cos((p * 8.7 + t * 0.6) * 2.0) * 0.018`
- Третичный акцентный цвет: `hueC` blended at 15%
- `sparkleStreak`: второй слой sparkle с другим scaling
- Corner glow boost: усиление свечения в углах пропорционально расстоянию от центра

### 13в. `rect.fsh` — Subtle micro-grain
**Файл:** `src/main/resources/assets/rich/shaders/core/rect.fsh`
- Анимированный микро-шум: `hash12(pixelCoord + vec2(grainTime * 3.0, grainTime * 1.7)) * 0.022 - 0.011`
- Применяется с весом `rectColor.a` — не виден на прозрачных областях
- Время выведено из `screen.x` (чтобы не требовать дополнительного uniform)

### 13г. `outline.fsh` — Pulsing glow
**Файл:** `src/main/resources/assets/rich/shaders/core/outline.fsh`
- `glowPulse = 1.0 + 0.12 * sin(glowTime * 0.005 + perimeterPos * 8.0)` — пульсация движется вдоль периметра
- `effectiveRadius` пульсирует ±15%
- Время выведено из `screen.x + screen.y`

---

## 14. ClickGuiPalette.java — Расширенная палитра
**Файл:** `src/main/java/rich/screens/clickgui/theme/ClickGuiPalette.java`

Добавлены константы:
- `ACCENT_GLOW = 0xFF6A9CFF`
- `ACCENT_WARM = 0xFFB877D8`  
- `ACCENT_TEAL = 0xFF3ECFBA`
- `SURFACE_DARK = 0xFF0A0B10`
- `SURFACE_CARD = 0xFF141620`

Методы: `accentGlow()`, `accentWarm()`, `accentTeal()`.

---

## 15. HudStyle.java — Новый вариант GLOW
**Файл:** `src/main/java/rich/screens/hud/HudStyle.java`

- В `Variant` enum добавлен `GLOW`
- `panel()` с variant `GLOW`: tint-смешение к ACCENT/ACCENT_WARM/ACCENT_TEAL, border tinted к ACCENT_GLOW
- Дополнительный внешний glow outline: `Render2D.outline(x-1, y-1, w+2, h+2, 2.5f, accentGlow(alphaMul*0.5f), radius+1)`
- `inset()` с variant `GLOW`: tint-смещение к ACCENT, border tinted к ACCENT_GLOW

---

## 16. AMOLED ClickGui — Монохромный рестайл
**Цель:** Pure black AMOLED-стиль с микро-зернистостью (roughness), минимальными границами и акцентным свечением `#6A9CFF`.

### 16а. ClickGuiPalette.java — AMOLED-цвета
**Файл:** `src/main/java/rich/screens/clickgui/theme/ClickGuiPalette.java`

Все базовые цвета сдвинуты в сторону чистого чёрного с холодным отливом:
- `bgMain`: `(5, 6, 12)` (было `12, 13, 18`)
- `bgElevated`: `(10, 11, 18)` (было `16, 18, 26`)
- `border`: `(28, 32, 46, α180)` (было `42, 46, 56, α230`)
- `borderSubtle`: `(20, 24, 38, α110)` (было `34, 38, 48, α150`)
- `panelInset`: `(14, 16, 26, α55)` (было `22, 24, 32, α42`)
- `panelList`: `(8, 9, 16, α40)` (было `20, 22, 30, α32`)
- `textMuted`: `(110, 117, 135)` — холоднее (было `120, 125, 138`)

### 16б. HudStyle.java — Variant.AMOLED
**Файл:** `src/main/java/rich/screens/hud/HudStyle.java`

Добавлен enum `AMOLED`:
- `panel()`: фон pure black `bgMain`, лёгкий `4%` tint к `ACCENT_GLOW`/`ACCENT_TEAL`, border tinted `18%` к `ACCENT_GLOW`
- `inset()`: `6%` tint к `ACCENT`, border `14%` tint

### 16в. BackgroundRenderer.java — Чёрный холст + акцентный аутлайн
**Файл:** `src/main/java/rich/screens/clickgui/impl/background/render/BackgroundRenderer.java`

- Основной градиент: `(3,4,10)`→`(6,7,14)` — почти чистый чёрный
- `holoSheen` ослаблен до `0.06f`, цвет заменён на холодный синий `(100,156,255)`
- Чёрный аутлайн заменён на тонкий `(28,38,60)` + внешнее свечение `(40,70,120,α25)`
- В sidebar: разделитель `(16,20,34)`, иконки X/Y/Z в `#6A9CFF`, "Soon..." в акцентном синем

### 16г. ModuleListRenderer.java — Тёмные модули с акцентным глоу
**Файл:** `src/main/java/rich/screens/clickgui/impl/module/render/ModuleListRenderer.java`

- **Фон модуля (selected)**: пульсирует от `(12,14,24)` до `(32,42,72)` — холодный синий подтон
- **Фон модуля (hover)**: `(8,9,16)`→`(16,19,32)` — минимальное осветление
- **Аутлайн selected**: `#385AA0→#6A9CFF` с пульсацией (было `#3A5278→#4A6FA5`)
- **Нижний бар selected**: `#5282DC` с пульсацией
- **Аутлайн hover**: `#324C73` (было `#464C5C`)
- **State ball**: teal `#3EB4DC`→`#6AF0FF` с внешним glow-ореолом
- **Bind box**: фон `(30,34,50)`, аутлайн `#375078`, текст `#8291AA`
- **Scroll fade**: чисто чёрный `(0,0,0)` (было `(15,15,18)`)

### 16д. SettingsPanelRenderer.java — Акцентный разделитель
**Файл:** `src/main/java/rich/screens/clickgui/impl/module/render/SettingsPanelRenderer.java`

- Separator: пульсирующий `#3C82DC→#6A9CFF` (был серый `#262A34`)
- Scroll fade: чисто чёрный `(0,0,0)`
- "This module doesn't has settings" → `ClickGuiPalette.textMuted()`

### 16е. CategoryRenderer.java — Пульсирующий акцентный бар
**Файл:** `src/main/java/rich/screens/clickgui/impl/background/render/CategoryRenderer.java`

- **Текст категории**: при выборе подмешивается синий канал `+36`, пульсация `+18/+14/+36`
- **Акцентный бар**: `#4264B4→#6A9CFF` с собственной пульсацией (был статичный `ACCENT`)
- **Внешнее glow бара**: `3px` ореол `#4282DC` при animation > 0.3
- **Section header**: линии и текст пульсируют с холодным оттенком (период 3s)

### 16ж. HeaderRenderer.java — AMOLED-хедер
**Файл:** `src/main/java/rich/screens/clickgui/impl/background/render/HeaderRenderer.java`

- Header panel: `(7,8,16)`→`(10,11,20)` — чище чёрный
- Search box: фон `(7,8,14)`, outline пульсирует от `#385AA0` до `#6A9CFF` при фокусе
- Без фокуса: outline `#1A1E2E` (почти невидимый)
- Иконка поиска: `#5282C8`
- Курсор: синий `#649CE6` (был белый `#B4B4B9`)
- Текст поиска: ярко-белый `#E4E8F8`
- Placeholder: `#505A73`
- Selection highlight: `#3864B4`
- Категория (старая): `#5A5F73` (было `#6E7078`)
- Категория (новая): `#E2E6F8` (было `#DADCE4`)

---

## Статус сборки
Все изменения успешно компилируются через `./gradlew build` (Java 21, Fabric Loom 1.15).
