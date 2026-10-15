package com.qamqormed.ai;

import com.qamqormed.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AiBusyException extends ApiException {

    public AiBusyException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "QamqorMed AI сейчас обрабатывает другие запросы. Попробуйте ещё раз через несколько секунд.");
    }
}
