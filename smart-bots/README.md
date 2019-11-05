## Добавление нового бота

##### В модуле `smart-bots`
1. Скопировать json-файлы и модель в папку `assets`
1. Добавить новый тип бота в `domain/model/NeuroBotType.kt`
1. Прописать новые ресурсы в имплементацию фабрики ресурсов в `data/factory/ActualResourceFactory.kt`

##### В модуле `smart-panel-view`
1. Добавить drawable для новой иконки бота
1. Добавить объявление иконки в `mapper/SmartContentMapper.kt`
