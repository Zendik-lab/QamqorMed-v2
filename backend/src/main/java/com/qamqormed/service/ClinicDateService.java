package com.qamqormed.service;

import com.qamqormed.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One calendar for AI date handling. The application clock is injected so
 * production uses real time and tests can use a fixed instant.
 */
@Service
public class ClinicDateService {
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Almaty");

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DOT_DATE = DateTimeFormatter.ofPattern("d.M.uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern ISO_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern DOT_PATTERN = Pattern.compile("\\d{1,2}\\.\\d{1,2}\\.\\d{4}");
    private static final Pattern RU_DAYS = Pattern.compile("(?:в|во)\\s+(?:(?:следующ(?:ий|ую)|ближайш(?:ий|ую))\\s+)?(понедельник|вторник|среду|среда|четверг|пятницу|пятница|субботу|суббота|воскресенье)");
    private static final Pattern KK_DAYS = Pattern.compile("(?:келесі\\s+)?(дүйсенбі|сейсенбі|сәрсенбі|бейсенбі|жұма|сенбі|жексенбі)");
    private static final Pattern EN_DAYS = Pattern.compile("(?:(?:next|on)\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)");
    private static final Pattern RU_IN_DAYS = Pattern.compile("через\\s+(\\d+)\\s+д\\p{L}*");
    private static final Pattern KK_IN_DAYS = Pattern.compile("(\\d+)\\s*күннен\\s+(?:кейін|соң)");
    private static final Pattern EN_IN_DAYS = Pattern.compile("in\\s+(\\d+)\\s+days?");

    private static final Map<String, DayOfWeek> RU_DAY_NAMES = Map.ofEntries(
            Map.entry("понедельник", DayOfWeek.MONDAY), Map.entry("вторник", DayOfWeek.TUESDAY),
            Map.entry("среда", DayOfWeek.WEDNESDAY), Map.entry("среду", DayOfWeek.WEDNESDAY),
            Map.entry("четверг", DayOfWeek.THURSDAY), Map.entry("пятница", DayOfWeek.FRIDAY),
            Map.entry("пятницу", DayOfWeek.FRIDAY), Map.entry("суббота", DayOfWeek.SATURDAY),
            Map.entry("субботу", DayOfWeek.SATURDAY), Map.entry("воскресенье", DayOfWeek.SUNDAY));
    private static final Map<String, DayOfWeek> KK_DAY_NAMES = Map.ofEntries(
            Map.entry("дүйсенбі", DayOfWeek.MONDAY), Map.entry("сейсенбі", DayOfWeek.TUESDAY),
            Map.entry("сәрсенбі", DayOfWeek.WEDNESDAY), Map.entry("бейсенбі", DayOfWeek.THURSDAY),
            Map.entry("жұма", DayOfWeek.FRIDAY), Map.entry("сенбі", DayOfWeek.SATURDAY),
            Map.entry("жексенбі", DayOfWeek.SUNDAY));
    private static final Map<String, DayOfWeek> EN_DAY_NAMES = Map.ofEntries(
            Map.entry("monday", DayOfWeek.MONDAY), Map.entry("tuesday", DayOfWeek.TUESDAY),
            Map.entry("wednesday", DayOfWeek.WEDNESDAY), Map.entry("thursday", DayOfWeek.THURSDAY),
            Map.entry("friday", DayOfWeek.FRIDAY), Map.entry("saturday", DayOfWeek.SATURDAY),
            Map.entry("sunday", DayOfWeek.SUNDAY));

    private final Clock clock;

    public ClinicDateService(Clock clock) {
        this.clock = clock;
    }

    public ZoneId defaultZone() {
        return DEFAULT_ZONE;
    }

    public ZoneId zoneOrDefault(String configuredZone) {
        if (configuredZone == null || configuredZone.isBlank()) return DEFAULT_ZONE;
        try {
            return ZoneId.of(configuredZone.trim());
        } catch (RuntimeException ignored) {
            return DEFAULT_ZONE;
        }
    }

    public LocalDate today() {
        return today(DEFAULT_ZONE);
    }

    public LocalDate today(ZoneId zone) {
        return LocalDate.now(clock.withZone(zone == null ? DEFAULT_ZONE : zone));
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(DEFAULT_ZONE));
    }

    public LocalDateTime now(ZoneId zone) {
        return LocalDateTime.now(clock.withZone(zone == null ? DEFAULT_ZONE : zone));
    }

    public java.time.Instant nowInstant() {
        return clock.instant();
    }

    /** Resolve an AI date expression using the clinic calendar, never the model's clock. */
    public LocalDate resolve(String value, String label) {
        String normalized = normalize(value);
        if (normalized == null) return null;
        LocalDate base = today();

        try {
            if (ISO_PATTERN.matcher(normalized).matches()) return LocalDate.parse(normalized, ISO_DATE);
            if (DOT_PATTERN.matcher(normalized).matches()) return LocalDate.parse(normalized, DOT_DATE);
        } catch (DateTimeParseException exception) {
            throw invalid(label);
        }

        String expression = stripDatePreposition(normalized);
        if (isAny(expression, "сегодня", "бүгін", "бугин", "today")) return base;
        if (isAny(expression, "завтра", "ертең", "ертеңге", "tomorrow")) return base.plusDays(1);
        if (isAny(expression, "послезавтра", "бүрсігүні", "бурсигуни", "day after tomorrow")) return base.plusDays(2);
        if (isNextWeek(expression)) return base.with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        LocalDate inDays = resolveInDays(expression, base);
        if (inDays != null) return inDays;

        DayOfWeek weekday = resolveWeekday(expression);
        if (weekday != null) return base.with(TemporalAdjusters.next(weekday));

        throw invalid(label);
    }

    private LocalDate resolveInDays(String expression, LocalDate base) {
        Matcher matcher = RU_IN_DAYS.matcher(expression);
        if (matcher.matches()) return base.plusDays(parseDays(matcher.group(1)));
        matcher = KK_IN_DAYS.matcher(expression);
        if (matcher.matches()) return base.plusDays(parseDays(matcher.group(1)));
        matcher = EN_IN_DAYS.matcher(expression);
        if (matcher.matches()) return base.plusDays(parseDays(matcher.group(1)));
        return null;
    }

    private long parseDays(String value) {
        try {
            long days = Long.parseLong(value);
            if (days < 0 || days > 3660) throw new NumberFormatException();
            return days;
        } catch (NumberFormatException exception) {
            throw invalid("дата");
        }
    }

    private DayOfWeek resolveWeekday(String expression) {
        Matcher matcher = RU_DAYS.matcher(expression);
        if (matcher.matches()) return RU_DAY_NAMES.get(matcher.group(1));
        matcher = KK_DAYS.matcher(expression);
        if (matcher.matches()) return KK_DAY_NAMES.get(matcher.group(1));
        matcher = EN_DAYS.matcher(expression);
        if (matcher.matches()) return EN_DAY_NAMES.get(matcher.group(1));
        return null;
    }

    private boolean isNextWeek(String expression) {
        return expression.matches("(?:следующ(?:ая|ую|ей)\\s+недел(?:я|ю|е)|келесі\\s+апта(?:да)?|next\\s+week)");
    }

    private String stripDatePreposition(String value) {
        return value.replaceFirst("^(?:на\\s+дату|для\\s+даты|на|для|for|on)\\s+", "").trim();
    }

    private boolean isAny(String value, String... expected) {
        for (String item : expected) if (item.equals(value)) return true;
        return false;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("\\s+", " ");
    }

    private BadRequestException invalid(String label) {
        return new BadRequestException("Некорректная " + label + ". Уточните дату в формате ДД.ММ.ГГГГ или ГГГГ-ММ-ДД");
    }
}
