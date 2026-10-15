package com.qamqormed.service;

import com.qamqormed.entity.Referral;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Renders the existing Referral entity; no document row is created for a download. */
@Service
public class ReferralPdfService {
    private final String configuredFontPath;

    public ReferralPdfService(@Value("${app.pdf.font-path:}") String configuredFontPath) { this.configuredFontPath = configuredFontPath; }

    public byte[] render(Referral referral) {
        var visit = referral.getMedicalVisit();
        var appointment = visit.getAppointment();
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4); document.addPage(page);
            Font font = font(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText(); stream.setFont(font.value(), 10);
                stream.newLineAtOffset(48, page.getMediaBox().getHeight() - 48);
                List<String> lines = List.of(
                        "QamqorMed — НАПРАВЛЕНИЕ",
                        "Пациент: " + subjectName(referral),
                        "Дата приёма: " + safe(appointment.getAppointmentAt() == null ? null : appointment.getAppointmentAt().toString()),
                        "Тип: " + safe(referral.getReferralType() == null ? null : referral.getReferralType().name()),
                        "Специалист: " + safe(referral.getTargetSpecialty() == null ? null : referral.getTargetSpecialty().getName()),
                        "Медицинская организация: " + safe(referral.getTargetClinic() == null ? null : referral.getTargetClinic().getName()),
                        "Причина: " + safe(referral.getReason()),
                        "Рекомендации: " + safe(referral.getNotes()),
                        "Статус: " + safe(referral.getStatus() == null ? null : referral.getStatus().name()),
                        "Оформлено: " + safe(referral.getIssuedAt() == null ? null : referral.getIssuedAt().toString()));
                for (String line : lines) { stream.showText(line.replace("\n", " ")); stream.newLineAtOffset(0, -18); }
                stream.endText();
            }
            document.save(output); return output.toByteArray();
        } catch (IOException exception) { throw new IllegalStateException("Не удалось сформировать PDF направления", exception); }
    }

    private Font font(PDDocument document) throws IOException {
        for (String candidate : List.of(configuredFontPath, "C:/Windows/Fonts/arial.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")) {
            if (candidate != null && !candidate.isBlank() && Files.isRegularFile(Path.of(candidate))) {
                return new Font(PDType0Font.load(document, Files.newInputStream(Path.of(candidate))));
            }
        }
        return new Font(new PDType1Font(Standard14Fonts.FontName.HELVETICA));
    }

    private String subjectName(Referral referral) {
        if (referral.getDependent() != null) return fullName(referral.getDependent().getLastName(), referral.getDependent().getFirstName(), referral.getDependent().getMiddleName());
        var profile = referral.getMedicalVisit().getPatientProfile();
        if (profile != null) return fullName(profile.getLastName(), profile.getFirstName(), profile.getMiddleName());
        return fullName(referral.getPatientUser().getLastName(), referral.getPatientUser().getFirstName(), referral.getPatientUser().getMiddleName());
    }
    private String fullName(String last, String first, String middle) { return String.join(" ", safe(last), safe(first), safe(middle)).trim().replaceAll(" +", " "); }
    private String safe(String value) { return value == null ? "" : value; }
    private record Font(org.apache.pdfbox.pdmodel.font.PDFont value) {}
}
