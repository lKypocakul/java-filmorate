package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User create(User user) {
        applyDefaultName(user);
        User created = userStorage.add(user);
        log.info("Создан пользователь id={}", created.getId());
        return created;
    }

    public User update(User user) {
        if (user.getId() == null) {
            throw new ValidationException("Id пользователя должен быть указан");
        }
        applyDefaultName(user);
        User updated = userStorage.update(user);
        log.info("Обновлён пользователь id={}", updated.getId());
        return updated;
    }

    public Collection<User> getAll() {
        return userStorage.getAll();
    }

    public User getById(long id) {
        return userStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }

    /**
     * Дружба односторонняя: пользователь id добавляет friendId в свой список друзей,
     * а в списке друзей friendId пользователь id не появляется.
     */
    public void addFriend(long id, long friendId) {
        if (id == friendId) {
            throw new ValidationException("Нельзя добавить в друзья самого себя");
        }
        getById(id);
        getById(friendId);
        userStorage.addFriend(id, friendId);
        log.info("Пользователь {} добавил в друзья пользователя {}", id, friendId);
    }

    public void removeFriend(long id, long friendId) {
        getById(id);
        getById(friendId);
        userStorage.removeFriend(id, friendId);
        log.info("Пользователь {} удалил из друзей пользователя {}", id, friendId);
    }

    public List<User> getFriends(long id) {
        getById(id);
        log.debug("Запрошен список друзей пользователя {}", id);
        return userStorage.getFriends(id);
    }

    public List<User> getCommonFriends(long id, long otherId) {
        getById(id);
        getById(otherId);
        log.debug("Поиск общих друзей пользователей {} и {}", id, otherId);
        return userStorage.getCommonFriends(id, otherId);
    }

    private void applyDefaultName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
