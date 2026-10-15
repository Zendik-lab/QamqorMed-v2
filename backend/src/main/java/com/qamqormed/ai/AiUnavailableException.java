package com.qamqormed.ai;

import com.qamqormed.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AiUnavailableException extends ApiException {

    public AiUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "QamqorMed AI сейчас недоступен. Проверьте, запущена ли локальная модель.");
    }

    public AiUnavailableException(Throwable cause) {
        this();
        initCause(cause);
    }
}
