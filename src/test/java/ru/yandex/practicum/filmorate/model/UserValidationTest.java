package ru.yandex.practicum.filmorate.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private User validUser() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    @Test
    void validUserHasNoViolations() {
        assertTrue(validator.validate(validUser()).isEmpty());
    }

    @Test
    void emailMustBeNonEmptyAndContainAt() {
        User user = validUser();
        user.setEmail("");
        assertTrue(validator.validate(user).size() >= 1);
        user.setEmail(null);
        assertTrue(validator.validate(user).size() >= 1);
        user.setEmail("usermail.ru");
        assertEquals(1, validator.validate(user).size());
    }

    @Test
    void loginMustBeNonEmptyWithoutSpaces() {
        User user = validUser();
        user.setLogin("");
        assertTrue(validator.validate(user).size() >= 1);
        user.setLogin(null);
        assertTrue(validator.validate(user).size() >= 1);
        user.setLogin("my login");
        assertEquals(1, validator.validate(user).size());
    }

    @Test
    void nameMayBeEmpty() {
        User user = validUser();
        user.setName("");
        assertTrue(validator.validate(user).isEmpty());
        user.setName(null);
        assertTrue(validator.validate(user).isEmpty());
    }

    @Test
    void birthdayBoundary() {
        User user = validUser();
        user.setBirthday(LocalDate.now());
        assertTrue(validator.validate(user).isEmpty());
        user.setBirthday(LocalDate.now().plusDays(1));
        assertEquals(1, validator.validate(user).size());
    }
}
