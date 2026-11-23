package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class, UserDbStorage.class, GenreDbStorage.class, MpaDbStorage.class})
class FilmDbStorageTest {
    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;

    private Film newFilm(String name, int mpaId, int... genreIds) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание " + name);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        film.setMpa(new Mpa(mpaId, null));
        Set<Genre> genres = new LinkedHashSet<>();
        for (int genreId : genreIds) {
            genres.add(new Genre(genreId, null));
        }
        film.setGenres(genres);
        return filmStorage.add(film);
    }

    private User newUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName(login);
        return userStorage.add(user);
    }

    @Test
    void addReturnsFilmWithMpaAndGenresNames() {
        Film created = newFilm("Фильм", 3, 2, 1);

        Film loaded = filmStorage.getById(created.getId()).orElseThrow();

        assertThat(loaded.getName()).isEqualTo("Фильм");
        assertThat(loaded.getMpa().getName()).isEqualTo("PG-13");
        assertThat(loaded.getGenres()).extracting(Genre::getId).containsExactly(1, 2);
        assertThat(loaded.getGenres()).extracting(Genre::getName).containsExactly("Комедия", "Драма");
    }

    @Test
    void getByIdUnknownReturnsEmpty() {
        assertThat(filmStorage.getById(9999)).isEmpty();
    }

    @Test
    void updateChangesFieldsAndGenres() {
        Film created = newFilm("Старое", 1, 1, 2);
        created.setName("Новое");
        created.setMpa(new Mpa(4, null));
        created.setGenres(new LinkedHashSet<>(List.of(new Genre(6, null))));

        Film updated = filmStorage.update(created);

        assertThat(updated.getName()).isEqualTo("Новое");
        assertThat(updated.getMpa().getName()).isEqualTo("R");
        assertThat(updated.getGenres()).extracting(Genre::getId).containsExactly(6);
    }

    @Test
    void updateUnknownThrowsNotFound() {
        Film film = newFilm("Фильм", 1);
        film.setId(9999L);

        assertThatThrownBy(() -> filmStorage.update(film)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAllContainsCreatedFilms() {
        Film first = newFilm("Первый", 1, 1);
        Film second = newFilm("Второй", 2);

        assertThat(filmStorage.getAll()).extracting(Film::getId).contains(first.getId(), second.getId());
    }

    @Test
    void deleteRemovesFilm() {
        Film created = newFilm("Удаляемый", 1, 1);

        filmStorage.delete(created.getId());

        assertThat(filmStorage.getById(created.getId())).isEmpty();
        assertThatThrownBy(() -> filmStorage.delete(created.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void popularSortsByLikesDescending() {
        Film unpopular = newFilm("Без лайков", 1);
        Film middle = newFilm("Один лайк", 1);
        Film top = newFilm("Два лайка", 1);
        User first = newUser("fan1");
        User second = newUser("fan2");
        filmStorage.addLike(middle.getId(), first.getId());
        filmStorage.addLike(top.getId(), first.getId());
        filmStorage.addLike(top.getId(), second.getId());

        List<Film> popular = filmStorage.getPopular(3);

        assertThat(popular.get(0).getId()).isEqualTo(top.getId());
        assertThat(popular.get(1).getId()).isEqualTo(middle.getId());
        assertThat(popular).extracting(Film::getId).contains(unpopular.getId());
    }

    @Test
    void popularRespectsCount() {
        newFilm("Один", 1);
        newFilm("Два", 1);

        assertThat(filmStorage.getPopular(1)).hasSize(1);
    }

    @Test
    void sameLikeTwiceCountsOnce() {
        Film film = newFilm("Фильм", 1);
        Film other = newFilm("Другой", 1);
        User user = newUser("fan3");
        User second = newUser("fan5");
        filmStorage.addLike(film.getId(), user.getId());
        filmStorage.addLike(film.getId(), user.getId());
        filmStorage.addLike(other.getId(), user.getId());
        filmStorage.addLike(other.getId(), second.getId());

        List<Film> popular = filmStorage.getPopular(2);

        assertThat(popular.get(0).getId()).isEqualTo(other.getId());
        assertThat(popular.get(1).getId()).isEqualTo(film.getId());
    }

    @Test
    void removeLikeLowersRating() {
        Film liked = newFilm("Понравился", 1);
        Film other = newFilm("Обычный", 1);
        User user = newUser("fan4");
        User second = newUser("fan6");
        filmStorage.addLike(liked.getId(), user.getId());
        filmStorage.addLike(liked.getId(), second.getId());
        filmStorage.addLike(other.getId(), user.getId());
        assertThat(filmStorage.getPopular(1).get(0).getId()).isEqualTo(liked.getId());

        filmStorage.removeLike(liked.getId(), user.getId());
        filmStorage.removeLike(liked.getId(), second.getId());

        assertThat(filmStorage.getPopular(1).get(0).getId()).isEqualTo(other.getId());
    }

    @Test
    void mpaReferenceDataIsLoaded() {
        assertThat(mpaStorage.getAll()).extracting(Mpa::getName).containsExactly("G", "PG", "PG-13", "R", "NC-17");
        assertThat(mpaStorage.getById(1)).hasValueSatisfying(mpa -> assertThat(mpa.getName()).isEqualTo("G"));
        assertThat(mpaStorage.getById(99)).isEmpty();
    }

    @Test
    void genreReferenceDataIsLoaded() {
        assertThat(genreStorage.getAll()).hasSize(6);
        assertThat(genreStorage.getById(1)).hasValueSatisfying(genre -> assertThat(genre.getName()).isEqualTo("Комедия"));
        assertThat(genreStorage.getById(99)).isEmpty();
    }
}
