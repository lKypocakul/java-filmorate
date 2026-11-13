package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class UserDbStorage implements UserStorage {

    private static final String SELECT_USERS = "SELECT u.id, u.email, u.login, u.name, u.birthday FROM users u";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User add(User user) {
        Map<String, Object> values = new HashMap<>();
        values.put("email", user.getEmail());
        values.put("login", user.getLogin());
        values.put("name", user.getName());
        values.put("birthday", toSqlDate(user));
        long id = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(values)
                .longValue();
        user.setId(id);
        log.debug("Пользователь сохранён в БД с id={}", id);
        return user;
    }

    @Override
    public User update(User user) {
        int rows = jdbcTemplate.update(
                "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?",
                user.getEmail(), user.getLogin(), user.getName(), toSqlDate(user), user.getId());
        if (rows == 0) {
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }
        return user;
    }

    @Override
    public void delete(long id) {
        if (jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) == 0) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
    }

    @Override
    public Optional<User> getById(long id) {
        return jdbcTemplate.query(SELECT_USERS + " WHERE u.id = ?", this::mapUser, id)
                .stream()
                .findFirst();
    }

    @Override
    public Collection<User> getAll() {
        return jdbcTemplate.query(SELECT_USERS + " ORDER BY u.id", this::mapUser);
    }

    @Override
    public void addFriend(long userId, long friendId) {
        jdbcTemplate.update(
                "MERGE INTO friendships (user_id, friend_id) KEY (user_id, friend_id) VALUES (?, ?)",
                userId, friendId);
    }

    @Override
    public void removeFriend(long userId, long friendId) {
        jdbcTemplate.update("DELETE FROM friendships WHERE user_id = ? AND friend_id = ?", userId, friendId);
    }

    @Override
    public List<User> getFriends(long userId) {
        return jdbcTemplate.query(
                SELECT_USERS + " JOIN friendships f ON f.friend_id = u.id WHERE f.user_id = ? ORDER BY u.id",
                this::mapUser, userId);
    }

    @Override
    public List<User> getCommonFriends(long userId, long otherId) {
        return jdbcTemplate.query(
                SELECT_USERS
                        + " JOIN friendships f1 ON f1.friend_id = u.id AND f1.user_id = ?"
                        + " JOIN friendships f2 ON f2.friend_id = u.id AND f2.user_id = ?"
                        + " ORDER BY u.id",
                this::mapUser, userId, otherId);
    }

    private Date toSqlDate(User user) {
        return user.getBirthday() == null ? null : Date.valueOf(user.getBirthday());
    }

    private User mapUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        Date birthday = rs.getDate("birthday");
        user.setBirthday(birthday == null ? null : birthday.toLocalDate());
        return user;
    }
}
