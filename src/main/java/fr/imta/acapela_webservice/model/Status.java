package fr.imta.acapela_webservice.model;

/** Processing state, used both for {@link Request}s and for {@link ApiKey}s. */
public enum Status {
    REFUSED,
    CANCELLED,
    PENDING,
    PROCESSING,
    DONE,
    ERROR
}
