#### Установка

* [Скачать](https://developer.android.com/ndk/downloads/older_releases) архив с Android NDK v16, извлечь в `/Users/<User>/Library/Android/sdk/` (путь указан для MacOS), переименовать папку в `ndk-bundle`
    * Итоговый путь к папке: `/Users/<User>/Library/Android/sdk/ndk-bundle`
* Клонировать проект командой `git clone --recursive`
    * Либо выполнить `git submodule update --init`, если исходники уже скачаны
* Переключиться на ветку `develop` и собрать проект в конфигурации `x86_SDK23Debug`

#### Настройка remote-репозиториев

```
origin  https://github.com/DrKLO/Telegram.git (fetch)
origin  https://github.com/DrKLO/Telegram.git (push)
sml     https://gitlab.smedialink.com/TelegramClient/android.git (fetch)
sml     https://gitlab.smedialink.com/TelegramClient/android.git (push)
```

#### Боты
Добавление новых ботов описано [здесь](smart-bots/README.md)
