# Публикация и deployment-артефакты

Этот документ фиксирует только публичный контракт репозитория. Домены, имена хостов, топология сети, TLS,
секреты и конкретный deployment-контроллер принадлежат окружению владельца и не должны появляться здесь.

## Принятые решения

- `flake.nix` собирает согласованную пару пакетов `gungame-frontend` и `gungame-server` для
  `x86_64-linux` и `aarch64-linux`.
- `nixosModules.gungame` предоставляет переиспользуемый systemd-сервис авторитетного backend. Модуль ничего не
  знает о внешнем reverse proxy, доменах и доставке артефактов.
- Production-подобный профиль `devenv` использует тот же контракт, что и обычное развёртывание: Nginx раздаёт
  собранную статику, а `/ws` проксируется на отдельный loopback-порт backend.
- Поддерживаемые release-теги имеют строгий вид `vMAJOR.MINOR.PATCH` без ведущих нулей и суффиксов.
- Гарантированная история deployment-сборок начинается с коммита
  `75dc118083ad0ac017837c68df1c76ccafa5845c`, в котором закреплён переносимый manifest schema 2 и полный
  механизм сборки и публикации артефактов. Коммиты должны быть его потомками.
- При вливании ветки этот baseline-коммит нужно сохранить в истории через fast-forward или merge commit. Squash и
  rebase меняют SHA; после такого вливания baseline необходимо отдельным коммитом обновить одновременно в build
  script и доверенном publisher workflow.
- Один артефакт всегда содержит именованные Nix-outputs `frontend` и `backend` одного commit SHA для
  `x86_64-linux`. Формат manifest намеренно не привязан к языку или структуре каталогов проекта.
- Формат артефакта — сжатый файловый Nix binary cache. Внутри находятся cache и `manifest.json`; рядом публикуется
  SHA-256-файл.
- Теговые сборки публикуются в GitHub Release соответствующего тега. Веточные сборки публикуются как неизменяемые
  SHA-именованные assets технического prerelease `gungame-build-cache`.
- Publisher и потребитель должны получать список assets постранично. GitHub ограничивает один Release тысячей
  assets, поэтому один технический release вмещает примерно 500 сборок коммитов (архив и checksum на коммит).
  Для первого этапа очистка и TTL намеренно не вводятся; до достижения лимита потребуется шардирование release
  либо согласованная политика удаления старых assets.
- Сборочный workflow имеет только `contents: read`. Отдельный доверенный workflow из default branch проверяет SHA,
  ancestry, ref, manifest и checksum, и только после этого получает `contents: write` для GitHub Releases.
- Ручной `workflow_dispatch` принимает полный commit SHA, тип ref и имя ref. Это позволяет внешнему контроллеру
  восстановить отсутствующий артефакт без выполнения workflow на production-хосте.

## Контракт manifest

`manifest.json` содержит:

- `schemaVersion` — сейчас `2`;
- `projectId` — стабильный идентификатор проекта, сейчас `gungame`;
- `commitSha` — полный lowercase SHA;
- `refType` — `branch` или `tag`;
- `refName` — исходное имя ветки либо тега;
- `system` — сейчас release workflow поддерживает `x86_64-linux`;
- `outputs` — объект с согласованными путями Nix store; для GunGame обязательны ключи `frontend` и `backend`.

Потребитель обязан проверить checksum архива, поля manifest, deployment-baseline и соответствие тега коммиту до
импорта binary cache. Значения из HTTP-запроса или manifest нельзя вставлять в shell-команду строковой
конкатенацией.

## Граница универсального CI/CD-сервиса

Публичный репозиторий реализует только контракт производителя: собирает именованные Nix-outputs и публикует
manifest schema 2. Slug домена, production ref, преобразование тегов, pathname статического сайта и backend,
WebSocket-режим, команды запуска и тексты служебных страниц задаются у потребителя декларативно. Эти параметры не
должны попадать в код контроллера. Благодаря этому контроллер можно перенести в отдельный репозиторий без переноса
GunGame-конфигурации; проекту потребуется оставить лишь свой build adapter и спецификацию.

## Проверки

```bash
npm run check
bash -n scripts/build_nix_release.sh
actionlint
nix flake check --print-build-logs
```

Локальная упаковка release-артефакта рассчитана на `x86_64-linux` и требует подходящего Linux builder:

```bash
GUNGAME_COMMIT_SHA="$(git rev-parse HEAD)" \
GUNGAME_REF_TYPE=branch \
GUNGAME_REF_NAME="$(git branch --show-current)" \
bash scripts/build_nix_release.sh release-artifacts
```

## Текущее состояние

- [x] Воспроизводимые frontend/backend Nix-пакеты.
- [x] Переиспользуемый NixOS-модуль backend.
- [x] Production-подобный профиль Nginx + WebSocket в `devenv`.
- [x] Разделение недоверенной сборки и доверенной публикации GitHub Release assets.
- [x] Строгая проверка SemVer-тегов и deployment-baseline.
- [ ] Влить механизм в default branch без изменения baseline SHA: до этого `workflow_run` не сможет использовать
      доверенный publisher из default branch.
- [ ] Проверить первую веточную публикацию и первый тег на GitHub после слияния.
