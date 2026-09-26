# Darkness Client — обновление и запуск

Исходник: https://github.com/BelyakDima/dc
Папка проекта: `D:\Desktop\Дима\Darkness Client`

## Полный цикл одной командой

Скачать свежий исходник с GitHub, поставить версию, собрать, заменить jar
в mods лаунчера и запустить игру:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "D:\Desktop\Дима\Darkness Client\scripts\update.ps1" -Version 1.6.0
```

Варианты:
- `-Version 1.6.0` — какая версия будет прошита в клиент (выводится в вотермарке
  и в `fabric.mod.json`). Если не указать — останется версия из репозитория.
- `-SkipLaunch` — обновить и собрать, но игру не запускать.
- `-LocalOnly` — не ходить на GitHub, собрать из того, что уже лежит в папке.

## Отдельные шаги

| Скрипт | Что делает |
|---|---|
| `scripts\update.ps1` | весь конвейер (GitHub → версия → сборка → mods → запуск) |
| `scripts\build.ps1` | только сборка jar через Java 21 из лаунчера |
| `scripts\install.ps1 -Jar <путь> -Version 1.6.0` | убить игру, удалить старые darkness*.jar, положить новый в mods |
| `scripts\launch.ps1` | запустить Legacy Launcher, нажать «Запустить», дождаться игры |

## Куда ставится мод

- Основная папка Legacy Launcher: `%APPDATA%\..\.tlauncher\legacy\Minecraft\game\mods`
  (полный путь: `C:\Users\user\AppData\Roaming\.tlauncher\legacy\Minecraft\game\mods`)
- Дополнительно: `C:\Users\user\AppData\Roaming\.minecraft\mods` (TLauncher)

## Детали

- Java 21 берётся из рантайма лаунчера (`java-runtime-delta`), ставить JDK не нужно.
- Сборка: `gradlew build`, итоговый jar — `build\libs\darkness-client.jar`,
  в mods кладётся как `darkness-client-<версия>.jar`.
- Версия клиента меняется в двух местах (update.ps1 делает это сам):
  `src\main\java\dev\darkness\client\DarknessClient.java` (`VERSION = "..."`)
  и `gradle.properties` (`mod_version=...`).
- Проверка, что мод загрузился: строка `Darkness Client <версия> loaded: N modules`
  в `C:\Users\user\AppData\Roaming\.tlauncher\legacy\Minecraft\game\logs\latest.log`.
- Кнопка «Запустить» в лаунчере нажимается по координатам, откалиброванным для
  развёрнутого окна 1600x900 (~30.6% ширины, ~56.6% высоты). При другом
  разрешении координаты в `scripts\launch.ps1` надо перекалибровать.
