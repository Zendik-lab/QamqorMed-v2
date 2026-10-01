package com.qamqormed.dto;

import com.qamqormed.entity.Dependent;
import com.qamqormed.entity.User;

public record PatientSubjectResponse(String type, Long id, String fullName, java.time.LocalDate dateOfBirth,
                                     Integer age, String gender, String relationship) {
    public static PatientSubjectResponse self(User user) {
        MedicalSubjectDto subject = MedicalSubjectDto.self(user);
        return new PatientSubjectResponse(subject.type(), subject.id(), subject.fullName(), subject.dateOfBirth(),
                subject.age(), subject.gender(), subject.relationship());
    }

    public static PatientSubjectResponse dependent(Dependent dependent) {
        MedicalSubjectDto subject = MedicalSubjectDto.dependent(dependent);
        return new PatientSubjectResponse(subject.type(), subject.id(), subject.fullName(), subject.dateOfBirth(),
                subject.age(), subject.gender(), subject.relationship());
    }
}
