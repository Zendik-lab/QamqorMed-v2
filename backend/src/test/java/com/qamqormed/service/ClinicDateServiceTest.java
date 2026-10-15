package com.qamqormed.service;

import com.qamqormed.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClinicDateServiceTest {
    private static final Instant NEW_YEAR_INSTANT = Instant.parse("2026-12-31T19:30:00Z");

    private final ClinicDateService dates = new ClinicDateService(
            Clock.fixed(NEW_YEAR_INSTANT, ZoneOffset.UTC));

    @Test
    void usesClinicDateWhenServerAndClinicDatesDiffer() {
        assertEquals(LocalDate.of(2027, 1, 1), dates.today());
        assertEquals(LocalDate.of(2027, 1, 2), dates.resolve("завтра", "дата поиска"));
        assertEquals(LocalDate.of(2027, 1, 1), dates.resolve("сегодня", "дата поиска"));
    }

    @Test
    void resolvesRelativeDatesAcrossRussianKazakhAndEnglish() {
        LocalDate today = LocalDate.of(2027, 1, 1);
        assertEquals(today.plusDays(2), dates.resolve("послезавтра", "дата"));
        assertEquals(today.plusDays(3), dates.resolve("через 3 дня", "дата"));
        assertEquals(today.plusDays(4), dates.resolve("4 күннен кейін", "дата"));
        assertEquals(today.plusDays(5), dates.resolve("in 5 days", "date"));
        assertEquals(today.plusDays(1), dates.resolve("ертең", "дата"));
        assertEquals(today.plusDays(1), dates.resolve("tomorrow", "date"));
        assertEquals(today.plusDays(1), dates.resolve("на завтра", "дата"));
    }

    @Test
    void resolvesWeekdaysNextWeekAndAbsoluteFormats() {
        assertEquals(LocalDate.of(2027, 1, 4), dates.resolve("в понедельник", "дата"));
        assertEquals(LocalDate.of(2027, 1, 4), dates.resolve("в следующий понедельник", "дата"));
        assertEquals(LocalDate.of(2027, 1, 5), dates.resolve("келесі сейсенбі", "дата"));
        assertEquals(LocalDate.of(2027, 1, 6), dates.resolve("next Wednesday", "date"));
        assertEquals(LocalDate.of(2027, 1, 4), dates.resolve("на следующей неделе", "дата"));
        assertEquals(LocalDate.of(2027, 2, 3), dates.resolve("03.02.2027", "дата"));
        assertEquals(LocalDate.of(2027, 2, 4), dates.resolve("2027-02-04", "дата"));
    }

    @Test
    void tomorrowRemainsFutureEvenAtLateSlot() {
        LocalDate tomorrow = dates.resolve("завтра", "дата");
        assertTrue(tomorrow.atTime(23, 59).isAfter(dates.now()));
    }

    @Test
    void invalidDateRequestsAskForClarification() {
        assertThrows(BadRequestException.class, () -> dates.resolve("31.02.2027", "дата поиска"));
        assertThrows(BadRequestException.class, () -> dates.resolve("когда-нибудь", "дата поиска"));
    }
}
