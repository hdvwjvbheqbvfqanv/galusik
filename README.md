# Мод «Бабушка» (Fabric, Minecraft 26.2)

Добавляет **только одного персонажа** — бабушку в красном цветочном платье и тапочках.

## Поведение
- Как житель: ходит, открывает двери, ночью идёт в **свою кровать** (ищет свободную в радиусе 16 блоков, запоминает и остаётся жить рядом), утром просыпается. **Не торгует.**
- Если вокруг темно (уровень света < 8) — ищет ближайшее освещённое место и выдаёт случайную реплику, следующую — через **2 секунды после окончания** предыдущей из 8 ваших файлов (две подряд одинаковые не бывают).
- Нашла свет — **стоит на месте 60 секунд**, реплики продолжаются с тем же интервалом.
- Во сне молчит (`TALK_WHILE_SLEEPING` в `BabushkaEntity`).

Все числа (интервал, минута, порог света, радиусы) — константы в начале `BabushkaEntity.java`.

## Как получить
`/summon babushka:babushka` или `/give @s babushka:babushka_spawn_egg`.
Для дома поставьте рядом кровать.

## Сборка
Быстрее всего: `build.bat` (Windows) / `build.sh` (нужны JDK 25 и Gradle 9.3+), либо залейте проект в GitHub — workflow `.github/workflows/build.yml` соберёт jar сам (раздел Actions → Artifacts).
Нужна Java 25.
1. Если нет `gradlew` — выполните `gradle wrapper --gradle-version 9.3.0` (или откройте проект в IntelliJ IDEA).
2. `./gradlew build` → `build/libs/babushka-1.0.0.jar` в папку `mods` вместе с Fabric API.

Версии (Loom, Fabric API) указаны в `gradle.properties` — при необходимости поднимите до актуальных с fabricmc.net/develop.

## Если сборка ругается на API 26.2
Код написан на официальных именах Mojang. Места, которые чаще всего меняются между версиями:
- `BabushkaEntity.isNightTime()` — определение ночи (`level().isDarkOutside()`);
- `ModEntities` — `Item.Properties.spawnEgg(...)`;
- `BabushkaRenderer` — импорты `HumanoidModel`, `HumanoidRenderState`, `ModelLayers`;
- `BabushkaClient` — `EntityRendererRegistry`.

## Файлы
- `tools/make_skin.py` — генератор скина (`textures/entity/babushka.png`); можно перерисовать в любом редакторе.
- Звуки — `assets/babushka/sounds/line/*.ogg` (моно, чтобы громкость зависела от расстояния).
