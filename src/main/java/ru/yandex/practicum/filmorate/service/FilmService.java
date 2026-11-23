package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       MpaStorage mpaStorage,
                       GenreStorage genreStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaStorage = mpaStorage;
        this.genreStorage = genreStorage;
    }

    public Film create(Film film) {
        checkReferences(film);
        Film created = filmStorage.add(film);
        log.info("Добавлен фильм id={}", created.getId());
        return created;
    }

    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("Id фильма должен быть указан");
        }
        checkReferences(film);
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
        getById(filmId);
        checkUserExists(userId);
        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(long filmId, long userId) {
        getById(filmId);
        checkUserExists(userId);
        filmStorage.removeLike(filmId, userId);
        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
    }

    public List<Film> getPopular(int count) {
        if (count <= 0) {
            throw new ValidationException("Параметр count должен быть положительным");
        }
        log.debug("Запрошено {} популярных фильмов", count);
        return filmStorage.getPopular(count);
    }

    private void checkReferences(Film film) {
        if (film.getMpa() != null) {
            Integer mpaId = film.getMpa().getId();
            if (mpaId == null) {
                throw new ValidationException("Не указан id рейтинга");
            }
            if (mpaStorage.getById(mpaId).isEmpty()) {
                throw new NotFoundException("Рейтинг с id = " + mpaId + " не найден");
            }
        }
        if (film.getGenres() == null) {
            return;
        }
        for (Genre genre : film.getGenres()) {
            if (genre.getId() == null) {
                throw new ValidationException("Не указан id жанра");
            }
            if (genreStorage.getById(genre.getId()).isEmpty()) {
                throw new NotFoundException("Жанр с id = " + genre.getId() + " не найден");
            }
        }
    }

    private void checkUserExists(long userId) {
        if (userStorage.getById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }
}
