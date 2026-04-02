# Работа с аннотациями
## @Native куда кидать?
Ставите `@Native` на методы:
- **Главный класс инициализации**
- **Fonts**
- **На функции которые хотите**, но главное не визуальные
- **На Main**, НО ГЛАВНОЕ В МЕЙНЕ НА КЛАСС

⚠️ **Важно**: Желательно нативе кидать на методы, чем на классы - может быть проблема с оптимизацией.

⚠️**Важно**: Чем больше нативных методов чем лучше
так что не стесняйся налаживать)) 



# UserProfile API

## Описание

`UserProfile` - это синглтон класс для получения информации о профиле пользователя в защите AntiDaunLeak. Класс предоставляет доступ к основным данным пользователя через нативные методы.

## Структура класса

### Основные компоненты

- **Profile Access**: Предоставляет универсальный метод для доступа к данным профиля

## API Методы

### Получение экземпляра

```java
UserProfile profile = UserProfile.getInstance();
```

### Нативные методы

| Метод | Возвращаемый тип | Описание                                |
|-------|------------------|-----------------------------------------|
| `getUsername()` | `String` | Возвращает имя пользователя             |
| `getHwid()` | `String` | Возвращает HWID к примеру ID устройства |
| `getRole()` | `String` | Возвращает роль пользователя в системе  |
| `getUid()` | `String` | Возвращает уникальный ID пользователя   |
| `getSubsTime()` | `String` | Возвращает время действия подписки      |

### Универсальный метод доступа

```java
public String profile(String profile)
```

Позволяет получить данные профиля по ключу:

| Ключ | Описание          |
|------|-------------------|
| `"username"` | Имя пользователя  |
| `"hwid"` | Хвид пользователя |
| `"role"` | Роль пользователя |
| `"uid"` | UID пользователя  |
| `"subTime"` | Время подписки    |

## Примеры использования

### Прямое использование нативных методов что нельзя делать ❌

```java
UserProfile userProfile = UserProfile.getInstance();

String username = userProfile.getUsername();
String hwid = userProfile.getHwid();
String role = userProfile.getRole();
String uid = userProfile.getUid();
String subsTime = userProfile.getSubsTime();
```

### Вот пример правильного использования как гетать ✅

```java
UserProfile userProfile = UserProfile.getInstance();

String username = userProfile.profile("username");
String hwid = userProfile.profile("hwid");
String role = userProfile.profile("role");
String uid = userProfile.profile("uid");
String subsTime = userProfile.profile("subTime");
```

## Зависимости

- `antidaunleak.antidaunleak.api.annotation.Native` - кастомная аннотация для нативных методов

### Куда кидать аннотацию @Native?
