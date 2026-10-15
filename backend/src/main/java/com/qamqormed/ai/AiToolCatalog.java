package com.qamqormed.ai;

import com.qamqormed.entity.Role;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AiToolCatalog {
    private AiToolCatalog() {}

    public static List<OllamaClient.ToolDefinition> forRole(Role role) {
        List<OllamaClient.ToolDefinition> tools = new ArrayList<>();
        tools.add(new OllamaClient.ToolDefinition("search_doctors",
                "Find active real doctors using the QamqorMed database. Never invent doctors, clinics or specialties.",
                schema(Map.of(
                        "region", stringProperty("Region name"),
                        "city", stringProperty("City name"),
                        "clinic", stringProperty("Clinic or polyclinic name"),
                        "specialty", stringProperty("Medical specialty name"),
                        "doctorName", stringProperty("Part of doctor's name"),
                        "date", dateProperty("Optional date: YYYY-MM-DD, DD.MM.YYYY, today/tomorrow, in N days, weekday, or next week. Backend resolves it using clinic date."),
                        "timeFrom", timeProperty("Optional earliest time HH:mm"),
                        "timeTo", timeProperty("Optional latest time HH:mm")
                ), List.of())));
        tools.add(new OllamaClient.ToolDefinition("get_available_slots",
                "Return only appointment slots confirmed as available by QamqorMed for a real doctor and date.",
                schema(Map.of(
                        "doctorId", integerProperty("Doctor profile ID returned by search_doctors"),
                        "date", dateProperty("Date: YYYY-MM-DD, DD.MM.YYYY, today/tomorrow, in N days, weekday, or next week. Backend resolves it using clinic date."),
                        "timeFrom", timeProperty("Optional earliest time HH:mm"),
                        "timeTo", timeProperty("Optional latest time HH:mm")
                ), List.of("doctorId", "date"))));

        if (role == Role.PATIENT) {
            tools.add(new OllamaClient.ToolDefinition("list_patient_subjects",
                    "List the authenticated patient's own subject and active dependents. Use this before preparing a booking for a child or dependent.",
                    schema(Map.of(), List.of())));
            tools.add(new OllamaClient.ToolDefinition("prepare_appointment",
                    "Create a short-lived appointment offer only. Never claim that an appointment was created; the patient must press the confirmation button.",
                    schema(Map.of(
                            "doctorId", integerProperty("Doctor profile ID from a backend result"),
                            "timeSlotId", integerProperty("Available slot ID from get_available_slots"),
                            "dependentId", integerProperty("Active dependent ID from list_patient_subjects; omit for self")
                    ), List.of("doctorId", "timeSlotId"))));
            tools.add(new OllamaClient.ToolDefinition("prepare_waitlist",
                    "Create a short-lived waitlist offer when no slots are available. Never add the patient to waitlist without the confirmation button.",
                    schema(Map.of(
                            "dependentId", integerProperty("Active dependent ID; omit for self"),
                            "clinicId", integerProperty("Clinic ID from backend results"),
                            "specialtyId", integerProperty("Specialty ID from backend results"),
                            "doctorId", integerProperty("Optional doctor profile ID"),
                            "desiredDateFrom", dateProperty("First desired date; absolute or relative expression resolved by backend"),
                            "desiredDateTo", dateProperty("Last desired date; absolute or relative expression resolved by backend"),
                            "preferredTimeFrom", timeProperty("Optional earliest time HH:mm"),
                            "preferredTimeTo", timeProperty("Optional latest time HH:mm")
                    ), List.of("clinicId", "specialtyId", "desiredDateFrom", "desiredDateTo"))));
        }
        return List.copyOf(tools);
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("type", "object");
        value.put("properties", properties);
        value.put("required", required);
        value.put("additionalProperties", false);
        return value;
    }

    private static Map<String, Object> stringProperty(String description) { return Map.of("type", "string", "description", description); }
    private static Map<String, Object> dateProperty(String description) { return Map.of("type", "string", "description", description); }
    private static Map<String, Object> timeProperty(String description) { return Map.of("type", "string", "description", description, "pattern", "^\\d{2}:\\d{2}$"); }
    private static Map<String, Object> integerProperty(String description) { return Map.of("type", "integer", "description", description); }
}
