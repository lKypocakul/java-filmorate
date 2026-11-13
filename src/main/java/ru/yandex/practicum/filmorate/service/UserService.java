package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
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

    private void applyDefaultName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    public Collection<User> getAll() {
        return userStorage.getAll();
    }

    public User getById(long id) {
        return userStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }

    public void addFriend(long id, long friendId) {
        if (id == friendId) {
            throw new ValidationException("Нельзя добавить в друзья самого себя");
        }
        User user = getById(id);
        User friend = getById(friendId);
        user.getFriends().add(friendId);
        friend.getFriends().add(id);
        log.info("Пользователи {} и {} теперь друзья", id, friendId);
    }

    public void removeFriend(long id, long friendId) {
        User user = getById(id);
        User friend = getById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(id);
        log.info("Пользователи {} и {} больше не друзья", id, friendId);
    }

    public List<User> getFriends(long id) {
        User user = getById(id);
        log.debug("Запрошен список друзей пользователя {}", id);
        return user.getFriends().stream()
                .map(this::getById)
                .toList();
    }

    public List<User> getCommonFriends(long id, long otherId) {
        User user = getById(id);
        User other = getById(otherId);
        Set<Long> otherFriends = other.getFriends();
        log.debug("Поиск общих друзей пользователей {} и {}", id, otherId);
        return user.getFriends().stream()
                .filter(otherFriends::contains)
                .map(this::getById)
                .toList();
    }
}
