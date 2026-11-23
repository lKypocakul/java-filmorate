package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(UserDbStorage.class)
class UserDbStorageTest {
    private final UserDbStorage userStorage;

    private User newUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName("Name " + login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.add(user);
    }

    @Test
    void addAndGetById() {
        User created = newUser("alpha");

        assertThat(created.getId()).isNotNull();
        assertThat(userStorage.getById(created.getId()))
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getLogin()).isEqualTo("alpha");
                    assertThat(user.getEmail()).isEqualTo("alpha@mail.ru");
                    assertThat(user.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));
                });
    }

    @Test
    void getByIdUnknownReturnsEmpty() {
        assertThat(userStorage.getById(9999)).isEmpty();
    }

    @Test
    void updateChangesFields() {
        User created = newUser("beta");
        created.setName("Updated");
        created.setEmail("new@mail.ru");

        userStorage.update(created);

        assertThat(userStorage.getById(created.getId()))
                .hasValueSatisfying(user -> {
                    assertThat(user.getName()).isEqualTo("Updated");
                    assertThat(user.getEmail()).isEqualTo("new@mail.ru");
                });
    }

    @Test
    void updateUnknownThrowsNotFound() {
        User user = new User();
        user.setId(9999L);
        user.setEmail("a@b.ru");
        user.setLogin("ghost");

        assertThatThrownBy(() -> userStorage.update(user)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAllContainsCreatedUsers() {
        User first = newUser("gamma");
        User second = newUser("delta");

        assertThat(userStorage.getAll()).extracting(User::getId).contains(first.getId(), second.getId());
    }

    @Test
    void deleteRemovesUser() {
        User created = newUser("epsilon");

        userStorage.delete(created.getId());

        assertThat(userStorage.getById(created.getId())).isEmpty();
        assertThatThrownBy(() -> userStorage.delete(created.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void friendshipIsOneSided() {
        User first = newUser("zeta");
        User second = newUser("eta");

        userStorage.addFriend(first.getId(), second.getId());

        assertThat(userStorage.getFriends(first.getId())).extracting(User::getId).containsExactly(second.getId());
        assertThat(userStorage.getFriends(second.getId())).isEmpty();
    }

    @Test
    void removeFriendDeletesFriendship() {
        User first = newUser("theta");
        User second = newUser("iota");
        userStorage.addFriend(first.getId(), second.getId());

        userStorage.removeFriend(first.getId(), second.getId());

        assertThat(userStorage.getFriends(first.getId())).isEmpty();
    }

    @Test
    void addFriendTwiceDoesNotDuplicate() {
        User first = newUser("kappa");
        User second = newUser("lambda");

        userStorage.addFriend(first.getId(), second.getId());
        userStorage.addFriend(first.getId(), second.getId());

        assertThat(userStorage.getFriends(first.getId())).hasSize(1);
    }

    @Test
    void commonFriendsReturnsIntersection() {
        User first = newUser("mu");
        User second = newUser("nu");
        User common = newUser("xi");
        User onlyFirst = newUser("omicron");
        userStorage.addFriend(first.getId(), common.getId());
        userStorage.addFriend(first.getId(), onlyFirst.getId());
        userStorage.addFriend(second.getId(), common.getId());

        List<User> result = userStorage.getCommonFriends(first.getId(), second.getId());

        assertThat(result).extracting(User::getId).containsExactly(common.getId());
    }
}
