# UI/UX Finalization Plan (Detailed)

Дата обновления: 2026-02-27  
Проект: `mobile_tracker`  
Цель: довести UI/UX-ветку до состояния "готово к финальной приемке"

## 1. Изначальный план (с которого стартовали)

1. Довести экраны до общего scaffold/state-паттерна (`Login`, `ContextSelection`, `Summary`, `Settings`, `Upload`).
2. Подключить настоящий list/detail layout на планшете для `DeviceList`, `EmployeeSearch`, `Journal`, `Return`.
3. Добить очистку hardcoded UI-строк и пройтись по accessibility.

## 2. Статус по плану

### 2.1 Выполнено

- [x] Общая UI-база внедрена:
  - `presentation/common/AppScreenScaffold.kt`
  - `presentation/common/AppUiComponents.kt`
  - `presentation/common/ContentState.kt`
- [x] Back-навигация и top bar для вторичных экранов унифицированы через `AppNavGraph`.
- [x] Адаптивная навигация на главном экране:
  - телефон: `NavigationBar`
  - планшет: `NavigationRail`
- [x] Реальный list/detail на планшете подключен:
  - `DeviceListScreen`
  - `EmployeeSearchScreen`
  - `JournalScreen`
  - `ReturnScreen`
- [x] Экраны `Login`, `ContextSelection`, `Summary`, `Settings`, `Upload` приведены к общему scaffold/state подходу.
- [x] Большая часть UI-строк вынесена в `strings.xml`.
- [x] Убраны оставшиеся прямые `Color(0x...)` из `presentation/*Screen.kt` (переведено на theme/material tokens).
- [x] Для кликабельных карточек добавлены `onClickLabel`, `role` и `selected` semantics (TalkBack-friendly навигация в списках).
- [x] По последней проверке сборка `:app:assembleDebug` проходит успешно.

### 2.2 Частично выполнено / требует финализации

- [~] Полная консистентность theme tokens:
  - прямые `Color(0x...)` в `presentation/*Screen.kt` закрыты; нужна финальная валидация контраста в light/dark.
- [~] Полный accessibility-проход:
  - code-level правки внесены; нужен финальный аудит на устройствах (TalkBack/Font Scale/контраст).
- [~] UI smoke/regression:
  - требуется формализованная финальная проверка сценариев.

## 3. Что осталось, чтобы считать работу завершенной

### 3.1 Прямые цвета в screen-слое

Статус: выполнено для `presentation/*Screen.kt` (на текущем срезе).  
Остаток: только контроль контраста при ручной регрессии light/dark.

### 3.2 Финальный accessibility-проход

Что проверить:

1. `contentDescription` на всех интерактивных иконках и кнопках.
2. Touch targets не меньше `48dp`.
3. Font Scale `1.3` и `1.5` без обрезки ключевого контента.
4. TalkBack traversal order для основных flow:
   - Login -> ContextSelection -> Home
   - Issue/Return
   - Upload
   - Journal/Summary/Settings

### 3.3 Финальная UX регрессия (ручная)

Обязательные конфигурации:

1. Phone portrait.
2. Tablet portrait.
3. Tablet landscape.

Обязательные сценарии:

1. Navigation/back без тупиков.
2. Loading/Empty/Error/Content состояния на list/data экранах.
3. Выдача/возврат + переход в Upload.
4. Оффлайн баннер и поведение при восстановлении сети.

### 3.4 (Опционально, если требуется по ТЗ) англоязычная локаль

1. Создать `values-en/strings.xml`.
2. Закрыть минимум ключевые экраны и системные действия.

## 4. Порядок закрытия (рекомендуемый)

1. Провести accessibility-проход и исправить найденные дефекты.
2. Выполнить финальную ручную UX-регрессию на phone/tablet.
3. Валидировать контраст после перехода на theme tokens (light/dark).
4. Зафиксировать результат в чеклисте приемки.

Чеклист приемки: `app/UI_UX_ACCEPTANCE_CHECKLIST.md`.

## 5. Definition of Done (финальный)

Считать UI/UX-ветку завершенной можно, если одновременно выполнено:

- [ ] В `Screen.kt` не осталось прямых `Color(0x...)` и hardcoded UI-строк.
- [ ] Все критичные user flows проходят на phone и tablet.
- [ ] Нет критичных a11y-дефектов (touch target, контраст, TalkBack, font scale).
- [ ] `:app:assembleDebug` стабильно проходит.
- [ ] Финальный чеклист регрессии закрыт и подтвержден.

## 6. Быстрый operational checklist

- [x] Tokenize remaining screen colors.
- [ ] Run full a11y pass and fix findings.
- [ ] Run phone/tablet regression scenarios.
- [ ] Update this file with final status "DONE".
