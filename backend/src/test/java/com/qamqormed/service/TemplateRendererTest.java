package com.qamqormed.service;

import com.qamqormed.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateRendererTest {
    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void rendersAllowListedFieldsAndMarksMissingValues() {
        TemplateRenderer.Rendered rendered = renderer.render(
                "Пациент: {{ patient.fullName }}; диагноз: {{diagnosis.code}}",
                Map.of("patient.fullName", "Иван Иванов"));

        assertEquals("Пациент: Иван Иванов; диагноз: [ТРЕБУЕТ ЗАПОЛНЕНИЯ: diagnosis.code]", rendered.content());
        assertEquals(java.util.Set.of("diagnosis.code"), rendered.missingFields());
    }

    @Test
    void rejectsUnknownFieldsAndExecutableMarkup() {
        assertThrows(BadRequestException.class, () -> renderer.render("{{patient.secret}}", Map.of()));
        assertThrows(BadRequestException.class, () -> renderer.render("<script>alert(1)</script>", Map.of()));
        assertThrows(BadRequestException.class, () -> renderer.render("{{patient.fullName", Map.of()));
    }
}
