package com.qamqormed.controller;

import com.qamqormed.dto.NotificationResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    public NotificationController(NotificationService notifications) { this.notifications = notifications; }
    @GetMapping public List<NotificationResponse> list(@AuthenticationPrincipal AuthenticatedUser user) { return notifications.list(user.getId()); }
    @GetMapping("/unread-count") public Map<String, Long> unread(@AuthenticationPrincipal AuthenticatedUser user) { return Map.of("count", notifications.unreadCount(user.getId())); }
    @PatchMapping("/{id}/read") public void read(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) { notifications.markRead(user.getId(), id); }
    @PatchMapping("/read-all") public void readAll(@AuthenticationPrincipal AuthenticatedUser user) { notifications.markAllRead(user.getId()); }
}
