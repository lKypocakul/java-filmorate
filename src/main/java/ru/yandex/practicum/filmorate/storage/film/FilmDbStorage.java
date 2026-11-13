package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FilmDbStorage implements FilmStorage {

    private static final String SELECT_FILMS =
            "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name AS mpa_name "
                    + "FROM films f LEFT JOIN mpa m ON m.id = f.mpa_id";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Film add(Film film) {
        Map<String, Object> values = new HashMap<>();
        values.put("name", film.getName());
        values.put("description", film.getDescription());
        values.put("release_date", Date.valueOf(film.getReleaseDate()));
        values.put("duration", film.getDuration());
        values.put("mpa_id", mpaId(film));
        long id = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("films")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(values)
                .longValue();
        saveGenres(id, film.getGenres());
        log.debug("Фильм сохранён в БД с id={}", id);
        return getById(id).orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    @Override
    public Film update(Film film) {
        int rows = jdbcTemplate.update(
                "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? "
                        + "WHERE id = ?",
                film.getName(), film.getDescription(), Date.valueOf(film.getReleaseDate()),
                film.getDuration(), mpaId(film), film.getId());
        if (rows == 0) {
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        saveGenres(film.getId(), film.getGenres());
        return getById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + film.getId() + " не найден"));
    }

    @Override
    public void delete(long id) {
        if (jdbcTemplate.update("DELETE FROM films WHERE id = ?", id) == 0) {
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }
    }

    @Override
    public Optional<Film> getById(long id) {
        List<Film> films = jdbcTemplate.query(SELECT_FILMS + " WHERE f.id = ?", this::mapFilm, id);
        attachGenres(films);
        return films.stream().findFirst();
    }

    @Override
    public Collection<Film> getAll() {
        List<Film> films = jdbcTemplate.query(SELECT_FILMS + " ORDER BY f.id", this::mapFilm);
        attachGenres(films);
        return films;
    }

    @Override
    public void addLike(long filmId, long userId) {
        jdbcTemplate.update(
                "MERGE INTO film_likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)",
                filmId, userId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ? AND user_id = ?", filmId, userId);
    }

    @Override
    public List<Film> getPopular(int count) {
        List<Film> films = jdbcTemplate.query(
                SELECT_FILMS + " ORDER BY (SELECT COUNT(*) FROM film_likes l WHERE l.film_id = f.id) DESC, f.id"
                        + " LIMIT ?",
                this::mapFilm, count);
        attachGenres(films);
        return films;
    }

    private Integer mpaId(Film film) {
        return film.getMpa() == null ? null : film.getMpa().getId();
    }

    private void saveGenres(long filmId, Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }
        List<Integer> genreIds = genres.stream().map(Genre::getId).distinct().toList();
        jdbcTemplate.batchUpdate(
                "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)",
                genreIds, genreIds.size(),
                (ps, genreId) -> {
                    ps.setLong(1, filmId);
                    ps.setInt(2, genreId);
                });
    }

    private void attachGenres(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }
        Map<Long, Film> filmsById = films.stream().collect(Collectors.toMap(Film::getId, Function.identity()));
        String placeholders = String.join(", ", Collections.nCopies(filmsById.size(), "?"));
        jdbcTemplate.query(
                "SELECT fg.film_id, g.id, g.name FROM film_genres fg JOIN genres g ON g.id = fg.genre_id "
                        + "WHERE fg.film_id IN (" + placeholders + ") ORDER BY g.id",
                (RowCallbackHandler) rs -> filmsById.get(rs.getLong("film_id"))
                        .getGenres()
                        .add(new Genre(rs.getInt("id"), rs.getString("name"))),
                filmsById.keySet().toArray());
    }

    private Film mapFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        int mpaId = rs.getInt("mpa_id");
        if (!rs.wasNull()) {
            film.setMpa(new Mpa(mpaId, rs.getString("mpa_name")));
        }
        return film;
    }
}
