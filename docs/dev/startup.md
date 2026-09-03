# Настройка проекта и разработка

## Требования

Для разработки и локального запуска необходимы **Node.js 24+** и **npm 11**. TypeScript 7 использует нативный
компилятор на Go.

### Воспроизводимая среда с Nix

На Linux и macOS с установленными Nix и devenv войдите в окружение проекта и установите зависимости:

```bash
devenv shell
gg-install
```

Devenv предоставляет Node.js 24, npm и Git. Для каждого скрипта из `package.json` автоматически создаётся команда
с префиксом `gg-`: например, `npm run typecheck` доступен как `gg-typecheck`, а `npm run format:check` — как
`gg-format-check`.

На системах без Nix используйте Node.js и npm указанных выше версий, затем установите зависимости через `npm ci`.

## Одиночная игра

1. Склонируйте репозиторий.
2. В корневой директории выполните команду для установки всех зависимостей:
    ```bash
    npm ci
    ```
3. Запустите локальный сервер разработки:
    ```bash
    npm run dev
    ```
4. Откройте предложенный адрес (например, `http://localhost:5173/`) в браузере.

## Мультиплеер

Мультиплеер использует отдельный авторитетный WebSocket-сервер. Запустите его до Vite-сервера в двух терминалах:

```bash
npm run server
```

```bash
npm run dev
```

Откройте `http://localhost:5173/` в двух вкладках браузера. Сервер слушает `ws://localhost:8080` и принимает до четырёх игроков.

Для игры по локальной сети откройте адрес Vite-сервера с другого устройства. Клиент подключится к тому же хосту на порту `8080`; убедитесь, что этот порт доступен в локальной сети.

## Сборка для продакшена

Для компиляции TypeScript-кода и сборки оптимизированного бандла выполните:

```bash
npm run build
```

Готовые статические файлы (HTML, JS, CSS, картинки) появятся в папке `dist/`. Их можно загрузить на любой хостинг (GitHub Pages, Vercel, Netlify или обычный Nginx/Apache).

### Воспроизводимая сборка через Nix

Корневой `flake.nix` экспортирует два пакета для `x86_64-linux` и `aarch64-linux`. Например, на ARM-сервере или настроенном Linux remote builder:

```bash
nix build .#packages.aarch64-linux.gungame-frontend
nix build .#packages.aarch64-linux.gungame-server
```

- `gungame-frontend` содержит готовую статику в `share/gungame`;
- `gungame-server` предоставляет команду `bin/gungame-server` с Node.js и runtime-зависимостями.

Flake также экспортирует `nixosModules.gungame` и `nixosModules.host`. Первый модуль запускает авторитетный сервер через systemd и раздаёт frontend через Caddy. Второй хранит версионируемую конфигурацию будущего NixOS-хоста. WebSocket Upgrade на том же домене проксируется на локальный игровой сервер, поэтому браузер использует единый HTTPS/WSS origin.

## Деплой на NixOS

Каталог `nix/host/` содержит версионируемую конфигурацию NixOS-хоста и минимальный bootstrap flake для `/etc/nixos`. По умолчанию шаблон использует `aarch64-linux`; для x86-сервера замените `system` на `x86_64-linux`. При обновлении input `gungame` машина получает и новую версию приложения, и изменения host-конфигурации из репозитория. Перед первым развёртыванием:

1. Скопируйте `nix/host/flake.nix` в `/etc/nixos/flake.nix`.
2. Скопируйте `nix/host/local.nix.example` в `/etc/nixos/local.nix`, затем задайте реальный домен и CI public key. Реальный `local.nix` не должен попадать в Git.
3. Создайте `/etc/nixos/hardware-configuration.nix` через `nixos-generate-config` или `nixos-anywhere`. Файл `hardware-configuration.nix.example` предназначен только как ориентир; загрузочное устройство и файловые системы нельзя безопасно угадать заранее.
4. Создайте отдельную SSH-пару для GitHub Actions и укажите её публичный ключ в `services.gungame.deploy.authorizedKeys`.
5. Выполните `nix flake lock --flake /etc/nixos`, затем первый `nixos-rebuild switch --flake /etc/nixos#gungame` вручную.

Host flake держит этот репозиторий как input с именем `gungame`. Установленная модулем команда `gungame-deploy`:

1. блокирует параллельные деплои;
2. копирует `/etc/nixos` во временный каталог;
3. выполняет `nix flake update gungame` только в этой копии;
4. проверяет flake и собирает новое поколение;
5. активирует поколение и проверяет `gungame-server.service`;
6. обновляет рабочий `flake.lock` только после успеха;
7. откатывает поколение при ошибке активации или запуска сервиса.

CI-job `deploy` запускается только после успешных quality- и Nix-проверок на push в `web`. Создайте GitHub Environment `production`, ограничьте его веткой `web` и, если тариф репозитория поддерживает это, включите обязательное ручное подтверждение. Добавьте в Environment:

- secret `GUNGAME_DEPLOY_SSH_KEY` — приватный CI-ключ;
- secret `GUNGAME_DEPLOY_KNOWN_HOSTS` — заранее проверенная строка `known_hosts`, полученная вне CI;
- variable `GUNGAME_DEPLOY_HOST` — домен или IP NixOS-машины;
- variable `GUNGAME_DEPLOY_USER` — `gungame-deploy`.

SSH-ключ на сервере получает forced command и не может передать deploy-скрипту произвольные аргументы. Не используйте `ssh-keyscan` внутри CI: сохранённый `known_hosts` защищает первый контакт от подмены.

Активация новой версии перезапускает авторитетный сервер и завершает текущий матч. До появления переноса состояния между поколениями production Environment рекомендуется защищать ручным подтверждением и выполнять деплой в согласованное окно.

## Проверки качества

Перед коммитом запустите общий набор проверок:

```bash
npm run check
```

Отдельные команды:

- `npm run typecheck` — проверка типов нативным компилятором TypeScript;
- `npm run lintcheck` — строгая проверка Oxlint без допустимых предупреждений;
- `npm run format:check` — проверка форматирования Oxfmt;
- `npm run lint` — автоматическое исправление доступных ошибок линтера;
- `npm run format` — форматирование поддерживаемых файлов.

## Архитектура

Игра построена на паттерне Entity-Manager:

- **Фреймворк:** Phaser 4
- **Физика:** Matter.js (настроена без гравитации для top-down режима)
- **Точка входа:** `src/main.ts`
- **Менеджеры:** `src/core/` (`EntityManager`, `InputManager`, `EventBus`)
- **Сцены:** `src/scenes/` (`BootScene`, `PreloadScene`, `GameScene`)
- **Сущности:** `src/objects/` (реализуют интерфейс `IEntity`)
- **Мультиплеер:** `server.js` (авторитетная Matter.js-физика) и `src/core/NetworkManager.ts` (WebSocket-клиент)
