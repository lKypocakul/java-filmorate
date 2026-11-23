package ru.yandex.practicum.filmorate.storage.genre;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

@Component
public class GenreDbStorage implements GenreStorage {

    private static final RowMapper<Genre> GENRE_MAPPER =
            (rs, rowNum) -> new Genre(rs.getInt("id"), rs.getString("name"));

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Genre> getAll() {
        return jdbcTemplate.query("SELECT id, name FROM genres ORDER BY id", GENRE_MAPPER);
    }

    @Override
    public Optional<Genre> getById(int id) {
        return jdbcTemplate.query("SELECT id, name FROM genres WHERE id = ?", GENRE_MAPPER, id)
                .stream()
                .findFirst();
    }
}
