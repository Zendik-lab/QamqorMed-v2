package com.qamqormed.controller;

import com.qamqormed.dto.DoctorReferralResponse;
import com.qamqormed.entity.ReferralStatus;
import com.qamqormed.entity.ReferralType;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.ClinicalWorkflowService;
import com.qamqormed.service.ReferralPdfService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctor/referrals")
public class DoctorReferralController {
    private final ClinicalWorkflowService workflow;
    private final ReferralPdfService pdf;

    public DoctorReferralController(ClinicalWorkflowService workflow, ReferralPdfService pdf) {
        this.workflow = workflow;
        this.pdf = pdf;
    }

    @GetMapping
    public List<DoctorReferralResponse> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @RequestParam(required = false) String query,
                                             @RequestParam(required = false) ReferralStatus status,
                                             @RequestParam(required = false) ReferralType type,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return workflow.doctorReferrals(principal.getId(), query, status, type, from, to);
    }

    @GetMapping("/{referralId}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long referralId) {
        byte[] value = workflow.doctorReferralPdf(principal.getId(), referralId, pdf);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename("qamqormed-referral-" + referralId + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(value);
    }
}
