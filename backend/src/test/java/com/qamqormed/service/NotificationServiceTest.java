package com.qamqormed.service;

import com.qamqormed.entity.Notification;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.NotificationRepository;
import com.qamqormed.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationService service = new NotificationService(repository, mock(UserRepository.class));

    @Test
    void readUsesAuthenticatedUserOwnershipScope() {
        Notification notification = new Notification();
        when(repository.findByIdAndUserId(7L, 42L)).thenReturn(Optional.of(notification));
        service.markRead(42L, 7L);
        verify(repository).findByIdAndUserId(7L, 42L);
        verify(repository).save(notification);
    }

    @Test
    void readDoesNotAllowAnotherUsersNotification() {
        when(repository.findByIdAndUserId(7L, 42L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.markRead(42L, 7L));
        verify(repository, never()).save(any());
    }
}
