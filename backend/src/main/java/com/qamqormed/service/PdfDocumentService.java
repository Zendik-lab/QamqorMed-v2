package com.qamqormed.service;

import com.qamqormed.entity.MedicalDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfDocumentService {
    private static final String DEMO_MARK = "ДЕМОНСТРАЦИОННЫЙ ДОКУМЕНТ. НЕ ЯВЛЯЕТСЯ ДЕЙСТВИТЕЛЬНЫМ МЕДИЦИНСКИМ ДОКУМЕНТОМ";
    private final String configuredFontPath;

    public PdfDocumentService(@Value("${app.pdf.font-path:}") String configuredFontPath) {
        this.configuredFontPath = configuredFontPath;
    }

    public byte[] render(MedicalDocument document) {
        // Patient PDFs must never fall back to editable draft content. Legacy rows with a
        // null status are treated as issued documents, matching the patient read model.
        String content = document.getStatus() == com.qamqormed.entity.MedicalDocumentStatus.DRAFT
                ? document.getDraftContent() : document.getFinalizedContent();
        if (content == null || content.isBlank()) content = document.getDescription() == null ? "" : document.getDescription();
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            FontChoice font = loadFont(pdf);
            float margin = 48;
            float width = page.getMediaBox().getWidth() - margin * 2;
            float y = page.getMediaBox().getHeight() - margin;
            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                stream.beginText();
                stream.setFont(font.font(), 10);
                stream.newLineAtOffset(margin, y);
                List<String> header = List.of(
                        document.getStatus() == com.qamqormed.entity.MedicalDocumentStatus.DRAFT ? "ЧЕРНОВИК" : DEMO_MARK,
                        "Документ № " + safe(document.getDocumentNumber()),
                        safe(document.getTitle()),
                        "Пациент: " + patientName(document),
                        "Врач: " + fullName(document.getDoctorUser()),
                        "Клиника: " + safe(document.getMedicalVisit().getClinic() == null ? null : document.getMedicalVisit().getClinic().getName()),
                        "Дата выдачи: " + safe((document.getApprovedAt() == null ? document.getCreatedAt() : document.getApprovedAt()) == null
                                ? null : (document.getApprovedAt() == null ? document.getCreatedAt() : document.getApprovedAt()).toString()), "");
                for (String line : header) { writeWrapped(stream, font, line, width, 12); }
                for (String line : content.replace("\r", "").split("\n", -1)) writeWrapped(stream, font, line, width, 14);
                stream.endText();
            }
            pdf.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать PDF", exception);
        }
    }

    private void writeWrapped(PDPageContentStream stream, FontChoice font, String value, float maxWidth, float leading) throws IOException {
        String normalized = value == null ? "" : value;
        if (normalized.isBlank()) { stream.newLineAtOffset(0, -leading); return; }
        StringBuilder line = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate, 10) > maxWidth && !line.isEmpty()) {
                stream.showText(line.toString()); stream.newLineAtOffset(0, -leading); line = new StringBuilder(word);
            } else line = new StringBuilder(candidate);
        }
        if (!line.isEmpty()) stream.showText(line.toString());
        stream.newLineAtOffset(0, -leading);
    }

    private FontChoice loadFont(PDDocument document) throws IOException {
        for (String candidate : fontCandidates()) {
            if (candidate == null || candidate.isBlank()) continue;
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path)) return new FontChoice(PDType0Font.load(document, Files.newInputStream(path)));
        }
        return new FontChoice(new PDType1Font(Standard14Fonts.FontName.HELVETICA));
    }

    private List<String> fontCandidates() {
        List<String> candidates = new ArrayList<>();
        candidates.add(configuredFontPath);
        candidates.add("C:/Windows/Fonts/arial.ttf");
        candidates.add("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");
        candidates.add("/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf");
        return candidates;
    }

    private String safe(String value) { return value == null ? "" : value; }

    private String patientName(MedicalDocument document) {
        if (document.getDependent() != null) {
            return fullName(document.getDependent().getLastName(), document.getDependent().getFirstName(), document.getDependent().getMiddleName());
        }
        if (document.getMedicalVisit().getPatientProfile() != null) {
            var profile = document.getMedicalVisit().getPatientProfile();
            return fullName(profile.getLastName(), profile.getFirstName(), profile.getMiddleName());
        }
        return fullName(document.getPatientUser());
    }

    private String fullName(com.qamqormed.entity.User user) {
        return user == null ? "" : fullName(user.getLastName(), user.getFirstName(), user.getMiddleName());
    }

    private String fullName(String lastName, String firstName, String middleName) {
        return String.join(" ", lastName == null ? "" : lastName, firstName == null ? "" : firstName, middleName == null ? "" : middleName)
                .trim().replaceAll(" +", " ");
    }

    private record FontChoice(org.apache.pdfbox.pdmodel.font.PDFont font) {
        float width(String value, float size) throws IOException { return font.getStringWidth(value) / 1000 * size; }
    }
}
