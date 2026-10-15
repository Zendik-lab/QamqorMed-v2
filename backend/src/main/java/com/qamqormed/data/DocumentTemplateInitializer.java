package com.qamqormed.data;

import com.qamqormed.entity.DocumentTemplate;
import com.qamqormed.entity.DocumentTemplateVersion;
import com.qamqormed.entity.MedicalDocumentType;
import com.qamqormed.entity.TemplateLanguage;
import com.qamqormed.entity.TemplateScope;
import com.qamqormed.repository.DocumentTemplateRepository;
import com.qamqormed.repository.DocumentTemplateVersionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Seeds only neutral, non-clinical text templates. No diagnosis or treatment is invented here. */
@Component
@Profile("dev")
public class DocumentTemplateInitializer implements CommandLineRunner {
    private final DocumentTemplateRepository templates;
    private final DocumentTemplateVersionRepository versions;

    public DocumentTemplateInitializer(DocumentTemplateRepository templates, DocumentTemplateVersionRepository versions) {
        this.templates = templates; this.versions = versions;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (templates.count() > 0) return;
        List<Seed> seeds = List.of(
                new Seed("Общая медицинская справка", MedicalDocumentType.CERTIFICATE, "Справка\nПациент: {{patient.fullName}}\nДата рождения: {{patient.birthDate}}\nДата посещения: {{appointment.date}}\nСодержание справки заполнено врачом после проверки данных приёма."),
                new Seed("Подтверждение посещения", MedicalDocumentType.CERTIFICATE, "Подтверждение посещения\nПациент: {{patient.fullName}}\nДата посещения: {{appointment.date}}\nВрач: {{doctor.fullName}}\nФактические сведения подтверждаются врачом."),
                new Seed("Направление к специалисту", MedicalDocumentType.REFERRAL, "Направление\nПациент: {{patient.fullName}}\nОсновной диагноз: {{diagnosis.code}} — {{diagnosis.name}}\nЦель направления и дополнительные рекомендации: заполнить врачу."),
                new Seed("Направление на анализы", MedicalDocumentType.LAB_REQUEST, "Направление на лабораторное исследование\nПациент: {{patient.fullName}}\nОснование: {{visit.complaints}}\nНазначаемое исследование и цель: заполнить врачу."),
                new Seed("Направление на обследование", MedicalDocumentType.REFERRAL, "Направление на обследование\nПациент: {{patient.fullName}}\nОснование: {{diagnosis.name}}\nВид обследования и цель: заполнить врачу."),
                new Seed("Лекарственное назначение", MedicalDocumentType.PRESCRIPTION, "Лекарственное назначение\nПациент: {{patient.fullName}}\nПрепарат, форма, дозировка, способ применения и продолжительность: заполнить врачу.\nНазначение не является рецептом без предусмотренного законом оформления."),
                new Seed("Консультационное заключение", MedicalDocumentType.CONCLUSION, "Консультационное заключение\nПациент: {{patient.fullName}}\nЖалобы: {{visit.complaints}}\nДанные осмотра: {{visit.examination}}\nДиагноз: {{diagnosis.code}} — {{diagnosis.name}}\nРекомендации: {{visit.recommendations}}"),
                new Seed("Повторная консультация", MedicalDocumentType.CONCLUSION, "Повторная консультация\nПациент: {{patient.fullName}}\nДиагноз: {{diagnosis.code}} — {{diagnosis.name}}\nДинамика и план наблюдения: заполнить врачу."),
                new Seed("Результат медицинского осмотра", MedicalDocumentType.REPORT, "Результат медицинского осмотра\nПациент: {{patient.fullName}}\nДата: {{appointment.date}}\nЖалобы: {{visit.complaints}}\nОсмотр: {{visit.examination}}\nИтог: заполнить врачу."),
                new Seed("Итоговое заключение по приёму", MedicalDocumentType.CONCLUSION, "Итоговое заключение по приёму\nПациент: {{patient.fullName}}\nКлиника: {{clinic.name}}\nВрач: {{doctor.fullName}}\nДата приёма: {{appointment.date}}\nЖалобы: {{visit.complaints}}\nАнамнез: {{visit.anamnesis}}\nДанные осмотра: {{visit.examination}}\nДанные подготовки медсестры: {{visit.nurseAssessment}}\nОсновной диагноз: {{diagnosis.code}} — {{diagnosis.name}}\nСопутствующие диагнозы: {{diagnosis.secondary}}\nНазначения: {{prescriptions.summary}}\nНаправления: {{referrals.summary}}\nРекомендации: {{visit.recommendations}}\nИтог приёма: заполнить врачу.")
        );
        // Do not label Russian copy as Kazakh or English. Those locales remain
        // available through the admin/import workflow for curated translations.
        for (Seed seed : seeds) {
            DocumentTemplate template = new DocumentTemplate();
            template.setName(seed.name());
            template.setDocumentType(seed.type()); template.setScope(TemplateScope.SYSTEM); template.setLanguage(TemplateLanguage.RU);
            templates.save(template);
            DocumentTemplateVersion version = new DocumentTemplateVersion(); version.setTemplate(template); version.setVersionNumber(1);
            version.setContent(seed.content()); version.setRequiredFields(""); version.setChangeDescription("Начальная нейтральная версия"); versions.save(version);
            template.setCurrentVersion(version); templates.save(template);
        }
    }

    private record Seed(String name, MedicalDocumentType type, String content) {}
}
