package com.qamqormed.service;

import com.qamqormed.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deliberately small, non-executable template renderer. It supports only the
 * allow-listed dotted fields below; no expressions, conditionals or HTML are
 * evaluated.
 */
@Component
public class TemplateRenderer {
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9]*(?:\\.[a-zA-Z][a-zA-Z0-9]*)?)\\s*}}"
    );
    private static final Set<String> ALLOWED = Set.of(
            "patient.fullName", "patient.birthDate", "patient.iin",
            "doctor.fullName", "doctor.specialty",
            "clinic.name", "clinic.address",
            "appointment.date", "appointment.time",
            "diagnosis.code", "diagnosis.name", "diagnosis.secondary", "prescriptions.summary", "referrals.summary", "visit.nurseAssessment",
            "visit.complaints", "visit.anamnesis", "visit.examination", "visit.recommendations",
            "document.number", "document.issueDate");

    public Rendered render(String template, Map<String, String> values) {
        if (template == null || template.isBlank()) throw new BadRequestException("Шаблон не содержит текста");
        if (template.toLowerCase(java.util.Locale.ROOT).contains("<script") || template.toLowerCase(java.util.Locale.ROOT).contains("javascript:")) {
            throw new BadRequestException("Шаблон содержит запрещённое содержимое");
        }
        Matcher matcher = VARIABLE.matcher(template);
        StringBuffer output = new StringBuffer();
        Set<String> missing = new LinkedHashSet<>();
        int cursor = 0;
        while (matcher.find()) {
            String field = matcher.group(1);
            if (!ALLOWED.contains(field)) throw new BadRequestException("Недопустимая переменная шаблона: " + field);
            String value = values.get(field);
            if (value == null || value.isBlank()) {
                missing.add(field);
                value = "[ТРЕБУЕТ ЗАПОЛНЕНИЯ: " + field + "]";
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(value));
            cursor = matcher.end();
        }
        matcher.appendTail(output);
        if (template.substring(cursor).contains("{{") || output.toString().contains("{{")) {
            throw new BadRequestException("Шаблон содержит неизвестную или незакрытую переменную");
        }
        return new Rendered(output.toString(), missing);
    }

    public Set<String> allowedFields() { return ALLOWED; }

    public record Rendered(String content, Set<String> missingFields) {}
}
