# GunGame — AGENTS.md

## Что это
Top-down шутер от третьего лица на libGDX с физикой Box2D, освещением box2dlights и поддержкой геймпада/клавиатуры+мыши.

## Язык и стиль
- **Java**, Java 25 (toolchain, использует `record`, текстовые блоки `"""`)
- Lombok (`@Getter` и др.) — плагин Lombok обязателен в IDE
- Комментарии исключительно на русском (за исключением стороннего `BodyEditorLoader.java` и единичных секционных заголовков типа `// ───── helpers ─────` в `Laser.java`)
- Именование: camelCase, классы PascalCase; префиксы/суффиксы по ответственности (`Manager`, `Utils`, `Controller`)
- Отступы: табы (как в существующих файлах)

## Структура проекта
```
gungame/
├── build.gradle.kts          # Корневой Gradle: версии, модули :core и :desktop
├── settings.gradle.kts
├── gradle.properties         # org.gradle.jvmargs, версия 0.1-dev
├── assets/                   # Ресурсы: font/, sound/, texture/, ui/
├── core/src/com/gungame/     # Основная логика игры
│   ├── GunGame.java          # ApplicationAdapter — главный цикл (create/render/dispose)
│   ├── assets/               # TextureManager, SoundManager
│   ├── controller/           # Input: ControllersManager, HeroController (+ joystick/keyboard)
│   ├── ui/                   # UI: UiEngine, HealthBar, StaminaBar, Ammo, CameraShaker
│   └── world/                # Игровой мир
│       ├── GameWorld.java    # Центральный orchestrator: физика, рендер, рестарт
│       ├── GameWorldConfig.java
│       ├── collision/        # Box2D contact listener, категории коллизий
│       ├── explosion/        # Анимации взрывов
│       └── objects/
│           ├── meta/         # GameObject базовый класс, фабрики, типы
│           ├── phisical/     # Hero, Bullet, Grenade, Box, Barrel, FirePoint
│           ├── imaginary/    # GroundContainer, генерация земли
│           └── weapon/       # Gun, Laser, GrenadeThrower, GunData, BulletData
├── desktop/src/com/gungame/  # DesktopLauncher.java — точка входа
└── docs/                     # startup.md, controls.md
```

## Ключевые зависимости
| Dependency | Версия | Назначение |
|---|---|---|
| libGDX | 1.14.0 | Core, freetype, box2d |
| box2dlights | 1.5 | Динамическое освещение (RayHandler) |
| gdx-controllers | 2.2.4 | Абстракция геймпада |
| Lombok | 9.2.0 (plugin) | @Getter и др. |

## Архитектура
- **GunGame** (`ApplicationAdapter`) — создаёт камеры, загружает ресурсы, делегирует в `GameWorld`.
- **GameWorld** — owns Box2D `World`, `RayHandler`, `ControllersManager`, двух героев (`hero`, `hero2`). Обновляет физику, рендерит мир и UI каждый кадр.
- **GameObject** — абстрактная обёртка над Box2D `Body`. Подклассы: `Hero`, `Bullet`, `Grenade`, `Box`, `Barrel`, `StaticGameObject` и др.
- **GameObjectFactoryManager** — фабричный паттерн для создания объектов мира.
- **ControllersManager** → **HeroController** (abstract) → конкретные реализации для джойстика и клавиатуры/мыши.
- Два игрока: `hero` (P1) и `hero2` (P2/bot). Поле `isWorldToRestart` триггерит рестарт уровня.

## Как запускать
- Точка входа: `com.gungame.DesktopLauncher` (модуль `desktop`)
- В IDEA: запустить `DesktopLauncher.main()` напрямую
- Fat-JAR: `./gradlew :desktop:shadowJar` → `gungame.jar` в корне проекта

## Градл
```bash
./gradlew :desktop:shadowJar   # Сборка fat-jar
./gradlew clean                # Очистка
```

## Управление (для контекста)
- Геймпад: левый стик — движение, правый — прицел, RB — выстрел, X — перезарядка, Y — смена оружия, LB — граната, A — бег, B — рывок
- Клавиатура+мышь: WASD, ЛКМ, R, G, Shift, Space, 1/2/3, Esc

## Правила разработки
1. Новые игровые объекты наследуют `GameObject` (или `VisibleGameObject` / `StaticGameObject`) и регистрируются через `GameObjectFactoryManager`
2. Коллизии настраиваются через `CollisionCategory` и `setupCollisionFilter()`
3. Ресурсы загружаются через `TextureManager` / `SoundManager` — не использовать `Gdx.files` напрямую без менеджеров
4. Все игровые константы — в `GameWorldConfig`
5. Не коммить секреты, `.gradle/`, `build/`, `.idea/`

## Правила для ассистента
1. Явные ошибки и несоответствия (устаревшая документация, опечатки, неверные версии) исправлять немедленно, а не просто фиксировать их наличие
2. Инструкции и предпочтения пользователя запоминать в этот файл (AGENTS.md)
3. При обнаружении существенных ошибок (например, на этапе code review или самопроверки), которые можно исправить оптимальным образом без архитектурных деградаций, исправлять их сразу, не запрашивая разрешение у пользователя.
