package com.qamqormed.controller;

import com.qamqormed.dto.*;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {
    private final WaitlistService waitlist;
    public WaitlistController(WaitlistService waitlist) { this.waitlist = waitlist; }
    @PostMapping public ResponseEntity<WaitlistResponse> create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody WaitlistCreateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(waitlist.create(user.getId(), request)); }
    @GetMapping public List<WaitlistResponse> list(@AuthenticationPrincipal AuthenticatedUser user) { return waitlist.list(user.getId()); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) { waitlist.cancel(user.getId(), id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/book") public AppointmentResponse book(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody WaitlistBookRequest request) { return waitlist.book(user.getId(), id, request); }
}
