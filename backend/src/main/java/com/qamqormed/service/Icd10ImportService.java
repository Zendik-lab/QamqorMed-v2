package com.qamqormed.service;

import com.qamqormed.dto.Icd10ImportResponse;
import com.qamqormed.entity.ICD10Code;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.repository.ICD10CodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class Icd10ImportService {
    private final ICD10CodeRepository codes;

    public Icd10ImportService(ICD10CodeRepository codes) { this.codes = codes; }

    @Transactional
    public Icd10ImportResponse importCsv(MultipartFile file, String source, String sourceVersion) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Передайте CSV-файл справочника МКБ-10");
        if (file.getSize() > 50L * 1024 * 1024) throw new BadRequestException("Файл справочника слишком большой");
        if (source == null || source.isBlank() || sourceVersion == null || sourceVersion.isBlank()) {
            throw new BadRequestException("Укажите источник и версию справочника");
        }
        int imported = 0, updated = 0, skipped = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new BadRequestException("CSV-файл пуст");
            Map<String, Integer> header = columns(parseCsv(headerLine));
            if (!header.containsKey("code") || !header.containsKey("title_ru")) {
                throw new BadRequestException("CSV должен содержать колонки code и title_ru");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                var row = parseCsv(line);
                String codeValue = value(row, header, "code").toUpperCase(Locale.ROOT);
                String titleRu = value(row, header, "title_ru");
                if (!codeValue.matches("[A-Z][0-9]{2}(?:\\.[0-9]{1,4})?") || titleRu.isBlank()) { skipped++; continue; }
                ICD10Code code = codes.findByCodeIgnoreCase(codeValue).orElseGet(ICD10Code::new);
                boolean existing = code.getId() != null;
                code.setCode(codeValue); code.setTitleRu(titleRu); code.setTitleKk(emptyToNull(value(row, header, "title_kk"))); code.setTitleEn(emptyToNull(value(row, header, "title_en")));
                code.setDescription(emptyToNull(value(row, header, "description"))); code.setChapter(emptyToNull(value(row, header, "chapter"))); code.setParentCode(emptyToNull(value(row, header, "parent_code")));
                String selectable = value(row, header, "selectable"); code.setSelectable(selectable.isBlank() || Boolean.parseBoolean(selectable)); code.setActive(true); code.setSource(source.trim()); code.setSourceVersion(sourceVersion.trim());
                codes.save(code); if (existing) updated++; else imported++;
            }
        } catch (IOException exception) { throw new BadRequestException("Не удалось прочитать CSV-файл"); }
        return new Icd10ImportResponse(imported, updated, skipped, source.trim(), sourceVersion.trim());
    }

    private Map<String, Integer> columns(java.util.List<String> values) {
        Map<String, Integer> result = new HashMap<>();
        for (int index = 0; index < values.size(); index++) result.put(values.get(index).trim().toLowerCase(Locale.ROOT), index);
        return result;
    }
    private String value(java.util.List<String> row, Map<String, Integer> columns, String name) { Integer index = columns.get(name); return index == null || index >= row.size() ? "" : row.get(index).trim(); }
    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private java.util.List<String> parseCsv(String line) {
        java.util.List<String> result = new java.util.ArrayList<>(); StringBuilder value = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < line.length(); i++) { char c = line.charAt(i); if (c == '"') { if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { value.append('"'); i++; } else quoted = !quoted; } else if (c == ',' && !quoted) { result.add(value.toString()); value.setLength(0); } else value.append(c); }
        result.add(value.toString()); return result;
    }
}
