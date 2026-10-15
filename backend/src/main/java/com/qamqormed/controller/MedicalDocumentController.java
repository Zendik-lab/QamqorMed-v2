package com.qamqormed.controller;

import com.qamqormed.dto.DocumentContentRequest;
import com.qamqormed.dto.DocumentDraftRequest;
import com.qamqormed.dto.MedicalDocumentResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.MedicalDocumentService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class MedicalDocumentController {
    private final MedicalDocumentService service;
    public MedicalDocumentController(MedicalDocumentService service) { this.service = service; }

    @PostMapping("/api/doctor/appointments/{appointmentId}/document-drafts")
    public MedicalDocumentResponse createDraft(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long appointmentId,
                                                @Valid @RequestBody DocumentDraftRequest request) {
        return service.createDraft(user.getId(), appointmentId, request);
    }

    @GetMapping("/api/doctor/documents/{documentId}")
    public MedicalDocumentResponse doctorDocument(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long documentId) {
        return service.doctorDocument(user.getId(), documentId);
    }

    @PutMapping("/api/doctor/documents/{documentId}")
    public MedicalDocumentResponse save(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long documentId,
                                         @Valid @RequestBody DocumentContentRequest request) {
        return service.saveDraft(user.getId(), documentId, request);
    }

    @PostMapping("/api/doctor/documents/{documentId}/approve")
    public MedicalDocumentResponse approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long documentId) {
        return service.approve(user.getId(), documentId);
    }

    @PostMapping("/api/doctor/documents/{documentId}/cancel")
    public MedicalDocumentResponse cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long documentId) {
        return service.cancel(user.getId(), documentId);
    }

    @GetMapping("/api/doctor/documents/{documentId}/pdf")
    public ResponseEntity<byte[]> doctorPdf(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long documentId) {
        return pdfResponse(service.doctorPdf(user.getId(), documentId), documentId);
    }

    @GetMapping("/api/patient/documents/{documentId}/pdf")
    public ResponseEntity<byte[]> patientPdf(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long documentId,
                                             @RequestParam(required = false) Long subjectId) {
        return pdfResponse(service.patientPdf(user.getId(), subjectId, documentId), documentId);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] value, Long id) {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename("qamqormed-document-" + id + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(value);
    }
}
