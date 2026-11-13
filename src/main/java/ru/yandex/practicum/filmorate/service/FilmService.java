package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film create(Film film) {
        Film created = filmStorage.add(film);
        log.info("Добавлен фильм id={}", created.getId());
        return created;
    }

    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("Id фильма должен быть указан");
        }
        Film updated = filmStorage.update(film);
        log.info("Обновлён фильм id={}", updated.getId());
        return updated;
    }

    public Collection<Film> getAll() {
        return filmStorage.getAll();
    }

    public Film getById(long id) {
        return filmStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    public void addLike(long filmId, long userId) {
        Film film = getById(filmId);
        checkUserExists(userId);
        if (film.getLikes().add(userId)) {
            log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
        } else {
            log.debug("Пользователь {} уже ставил лайк фильму {}", userId, filmId);
        }
    }

    public void removeLike(long filmId, long userId) {
        Film film = getById(filmId);
        checkUserExists(userId);
        if (film.getLikes().remove(userId)) {
            log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
        } else {
            log.debug("У фильма {} не было лайка пользователя {}", filmId, userId);
        }
    }

    public List<Film> getPopular(int count) {
        if (count <= 0) {
            throw new ValidationException("Параметр count должен быть положительным");
        }
        log.debug("Запрошено {} популярных фильмов", count);
        return filmStorage.getAll().stream()
                .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                .limit(count)
                .toList();
    }

    private void checkUserExists(long userId) {
        if (userStorage.getById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }
}
