# GunGame — AGENTS.md

## Что это
Top-down шутер в браузере на **TypeScript + Phaser 4** с физикой **Matter.js** (без гравитации).
Переписан со старой Java/libGDX-версии (Box2D) на веб. Старый Java-код и Gradle-файлы больше не актуальны.

## Стек
- **Язык:** TypeScript (`strict`, `noUnusedLocals`, `noUnusedParameters` — см. `tsconfig.json`)
- **Фреймворк:** Phaser `^4.2.1`, физика Matter.js (`gravity: 0`, top-down)
- **Бандлер:** Vite `^8`, точка входа `index.html` → `src/main.ts`
- **Тесты:** `@playwright/test` в devDependencies, готового набора тестов нет
- **Отступы:** 4 пробела. Комментарии в коде пока смешанные (англ./рус) — новые писать на русском
- **Тип:** `IEntity` (`src/types/interfaces.ts`) — контракт сущности для `EntityManager`

## Структура проекта
```
gungame/
├── index.html                # HTML-обёртка с div#game-container
├── src/
│   ├── main.ts               # Точка входа: Phaser.Game config (Matter, gamepad) + resize
│   ├── scenes/               # Phaser-сцены
│   │   ├── BootScene.ts      # запуск → PreloadScene
│   │   ├── PreloadScene.ts   # загрузка atlas/audio → GameScene
│   │   ├── GameScene.ts      # мир: физика, генерация окружения, коллизии пуль
│   │   └── UIScene.ts        # HUD (оружие, патроны, полоска урона)
│   ├── core/
│   │   ├── EntityManager.ts  # реестр IEntity: add/update/remove, чистка мёртвых
│   │   ├── InputManager.ts   # клавиатура + мышь + геймпад, координаты прицела
│   │   └── EventBus.ts       # EventDispatcher (Phaser EventEmitter) + GameEvents
│   ├── objects/
│   │   ├── Hero.ts           # игрок: движение, стамины, дэш, прицел, лазер
│   │   ├── Projectile.ts     # пуля: sensor-body, data (damage/piercing/ignoredBodies)
│   │   ├── Barrel.ts         # взрывная бочка (цепная реакция, урон по дистанции)
│   │   ├── OilTank.ts        # танк: shockwave + масляная лужа (slowed)
│   │   └── ThinWall.ts       # сегменты деревянной стены, пробиваются
│   ├── weapons/
│   │   ├── BaseWeapon.ts     # абстрактное оружие: патроны, rate, reload
│   │   ├── Rifle.ts / AssaultRifle.ts / Shotgun.ts
│   │   └── WeaponManager.ts  # текущее оружие, fire(), переключение
│   └── types/interfaces.ts   # IEntity = IUpdateable & IDestroyable & {id, gameObject}
├── public/assets/            # статика, которую отдаёт Vite (font/, sound/, texture/, ui/)
│                             #   texture/ui: .atlas (libGDX) + сгенерированные .json
├── assets/                   # ⚠️ легаси: исходники с Java-ветки, дублирует public/assets
├── scripts/convert_atlas.js  # libGDX .atlas → Phaser .json (писал в public/, см. ниже)
├── docs/dev/startup.md       # установка/запуск
└── docs/play/controls.md     # управление
```

## Ключевые зависимости
| Dependency | Версия | Назначение |
|---|---|---|
| phaser | ^4.2.1 | Core + Matter physics |
| matter-js | ^0.20.0 | типы MatterJS (физика внутри Phaser) |
| vite | ^8.2.1 | dev-сервер и сборка |
| typescript | ^7.0.2 | компилятор (`tsc --noEmit` в build) |
| @playwright/test | ^1.62.1 | E2E (набор тестов пока пуст) |

## Архитектура
- **Сцены (chain):** `BootScene` → `PreloadScene` → `GameScene` (+ `UIScene` — параллельно через `scene.launch`).
  Конфиг `Phaser.Game` — в `src/main.ts`.
- **GameScene** — оркестратор мира: `create()` строит границы, генерирует окружение (ящики/бочки/танк/стену),
  создаёт `Hero` и `UIScene`, вешает `collisionstart`/`collisionactive` на `matter.world`. `update()` дёргает
  `entityManager.update()` и `inputManager.update()`.
- **EntityManager** — реестр `IEntity`; каждый кадр обновляет живых и чистит `isDestroyed`.
- **InputManager** — клавиатура (WASD/Shift/Space/R/1-3), мышь (`pointerWorldX/Y`), геймпад
  (левый стик — движение, правый — прицел, R2 — огонь, A — бег, B — рывок, Y — перезарядка, D-pad — смена оружия).
- **Прицел и выстрел** — в `Hero.update()`: угол `fireAngle` считается из дула в курсор, **но** при
  дистанции курсора до героя `< AIM_DEADZONE` (120 px) стрельба идёт по текущему направлению взгляда —
  иначе при наведении на себя стреляет «сквозь спину». Лазерная линия (`laserGraphics`) и все пули
  используют этот же `fireAngle`, так что правка в одном месте чинит и laser, и projectiles.
- **Оружие:** `BaseWeapon` (патроны, `fireRate`, `reload`, звук) → `Rifle` (piercing), `AssaultRifle`,
  `Shotgun` (32 дроби, gaussian spread). `WeaponManager` держит список и текущее оружие,
  эмитит `weaponChanged`/`ammoChanged` в `game.events`.
- **Проектиль:** sensor-body (не толкает объекты), data-поля `isProjectile`/`damage`/`isPiercing`/
  `ignoredBodies`; авто-destroy через 3 с. Логика попаданий — в `GameScene.handleProjectileCollision`:
  piercing сквозь thin wall не уничтожает пулю (цель добавляется в `ignoredBodies`).
- **Окружение:** `Barrel` — взрывается с квадратичным спадом урона и цепной реакцией (`explode`),
  `OilTank` — создаёт
  масляные сенсоры `isPuddle`, в которых `Hero.setSlowed(true)`; `ThinWall` — сегменты 32px с health.
- **События:** между сценами — `scene.game.events` (weaponChanged, ammoChanged), между компонентами —
  глобальный `EventDispatcher` (`hero-damage`, `hero-death`).

## Запуск и сборка
```bash
npm install
npm run dev        # vite dev-сервер (http://localhost:5173)
npm run build      # tsc && vite build → dist/
```
- Атласы: в `public/assets/texture|ui` лежат `.atlas` (libGDX) и `.json` (Phaser).
  `.json` — сгенерированы `scripts/convert_atlas.js` (читает/пишет `public/assets/...`).
  При добавлении нового `.atlas` — перегенерировать `.json` и обновить `atlases` в скрипте + `PreloadScene`.
- Топ-уровневая `assets/` — легаси-копия, Vite её **не** отдаёт. Править ресурсы в `public/assets/`.

## Управление
- **Мышь+клавиатура:** WASD — движение, мышь — прицел, ЛКМ — огонь, R — перезарядка,
  1/2/3 или колесо — смена оружия, Shift — бег (стamina), Space — рывок.
- **Геймпад:** левый стик — движение, правый — прицел, R2 — огонь, A — бег, B — рывок, Y — перезарядка, D-pad — смена оружия.

## Правила разработки
1. Новая сущность: реализует `IEntity` (`src/types/interfaces.ts`), регистрируется через `EntityManager`
   (или остаётся обычным `Matter.Sprite` при разовой сцене — как `Barrel`/`OilTank`).
2. Новая пуля/оружие: наследовать `BaseWeapon` и добавить в `WeaponManager.weapons`.
3. Коллизии — через `matter.world.on('collisionstart' | 'collisionactive')` + data-поля на объектах.
4. Смена оружия/патроны/урон → события: `game.events` для UI, `EventDispatcher` для игровой логики.
5. Ресурсы только из `public/assets/` (Vite), не создавать новые пути в `assets/`.
6. Не коммитить: `.idea/`, `build/`, `node_modules/`, `dist/`, `server.log` (дописать `.gitignore`).

## Правила для ассистента
1. Явные ошибки и несоответствия (устаревшая документация, опечатки, неверные версии) —
   исправлять немедленно, а не просто фиксировать их наличие.
2. Инструкции и предпочтения пользователя запоминать в этот файл (AGENTS.md).
3. Существенные ошибки, которые можно закрыть без архитектурной деградации, — исправлять сразу.
4. Перед поиском/чтением кода — использовать `ast-index` (см. системный скилл `ast-index-search`).