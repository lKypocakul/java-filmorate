package ru.yandex.practicum.filmorate.storage.mpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

@Component
public class MpaDbStorage implements MpaStorage {

    private static final RowMapper<Mpa> MPA_MAPPER =
            (rs, rowNum) -> new Mpa(rs.getInt("id"), rs.getString("name"));

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Mpa> getAll() {
        return jdbcTemplate.query("SELECT id, name FROM mpa ORDER BY id", MPA_MAPPER);
    }

    @Override
    public Optional<Mpa> getById(int id) {
        return jdbcTemplate.query("SELECT id, name FROM mpa WHERE id = ?", MPA_MAPPER, id)
                .stream()
                .findFirst();
    }
}
