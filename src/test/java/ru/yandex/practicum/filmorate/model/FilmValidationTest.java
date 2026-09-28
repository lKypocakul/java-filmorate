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

class FilmValidationTest {
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

    private Film validFilm() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }

    @Test
    void validFilmHasNoViolations() {
        assertTrue(validator.validate(validFilm()).isEmpty());
    }

    @Test
    void emptyNameIsInvalid() {
        Film film = validFilm();
        film.setName("");
        assertEquals(1, validator.validate(film).size());
        film.setName("   ");
        assertEquals(1, validator.validate(film).size());
        film.setName(null);
        assertEquals(1, validator.validate(film).size());
    }

    @Test
    void descriptionBoundary() {
        Film film = validFilm();
        film.setDescription("a".repeat(200));
        assertTrue(validator.validate(film).isEmpty());
        film.setDescription("a".repeat(201));
        assertEquals(1, validator.validate(film).size());
    }

    @Test
    void releaseDateBoundary() {
        Film film = validFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        assertTrue(validator.validate(film).isEmpty());
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        assertEquals(1, validator.validate(film).size());
    }

    @Test
    void durationBoundary() {
        Film film = validFilm();
        film.setDuration(1);
        assertTrue(validator.validate(film).isEmpty());
        film.setDuration(0);
        assertEquals(1, validator.validate(film).size());
        film.setDuration(-1);
        assertEquals(1, validator.validate(film).size());
    }
}
