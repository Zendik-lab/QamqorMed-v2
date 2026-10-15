package com.qamqormed.ai;

/** Raised only when the configured Ollama/model rejects the tools payload. */
public class AiToolCallingUnsupportedException extends RuntimeException {
    public AiToolCallingUnsupportedException(Throwable cause) { super(cause); }
}
