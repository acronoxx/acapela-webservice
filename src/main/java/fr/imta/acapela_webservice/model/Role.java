package fr.imta.acapela_webservice.model;

/**
 * Role of a {@link User}. It is written in the JWT and checked by {@code AuthUtils}:
 * ADMIN can manage every user, key, cache and request; CLIENT only their own.
 */
public enum Role {
    ADMIN,
    CLIENT
}
