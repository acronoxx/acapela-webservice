package fr.imta.acapela_webservice.util;

import fr.imta.acapela_webservice.model.Role;
import fr.imta.acapela_webservice.model.AppException.UnauthorizedException;
import fr.imta.acapela_webservice.model.AppException.ForbiddenException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;


/**
 * Authorization helpers used by the handlers.
 *
 * <p>They read the JWT principal that {@code JWTAuthHandler} attaches to the request
 * ({@code {"id": <userId>, "role": "ADMIN" | "CLIENT"}}, see {@code UserHandler#login})
 * and throw an {@code AppException} when the caller is not allowed to go further,
 * which the handlers turn into a 401 or 403 response.
 */
public class AuthUtils {

    // Assertions methods

    /**
     * Ensures the authenticated user is the owner of the resource.
     *
     * @param ownerId id of the user who owns the resource
     * @throws UnauthorizedException if the request has no valid JWT
     * @throws ForbiddenException if the caller is not the owner
     */
    public static void assertSelf(RoutingContext ctx, Long ownerId){
        Long userId = getUserIdFromJWT(ctx);
        if(!userId.equals(ownerId)) throw new ForbiddenException();
    }

    /**
     * Ensures the authenticated user is either an admin or the owner of the resource.
     *
     * @param ownerId id of the user who owns the resource
     * @throws ForbiddenException if the caller is neither an admin nor the owner
     */
    public static void assertSelfOrAdmin(RoutingContext ctx, Long ownerId){
        if (isAdmin(ctx)) return;
        assertSelf(ctx, ownerId);
    }

    /**
     * Ensures the authenticated user has the {@link Role#ADMIN} role.
     *
     * @throws ForbiddenException if the caller is not an admin
     */
    public static void assertAdmin(RoutingContext ctx){
        if (!isAdmin(ctx)) throw new ForbiddenException();
    }

    // Utils

    /**
     * Returns the user id stored in the {@code id} claim of the JWT.
     *
     * @throws UnauthorizedException if there is no authenticated user or no {@code id} claim
     */
    public static Long getUserIdFromJWT(RoutingContext ctx) {
        if (ctx.user() == null) {
            throw new UnauthorizedException();
        }
        JsonObject payload = ctx.user().principal();
        Long id = payload.getLong("id");
        if (id == null) {
            throw new UnauthorizedException();
        }
        return id;
    }

    /**
     * Returns the role stored in the {@code role} claim of the JWT.
     *
     * @throws UnauthorizedException if the claim is missing or is not a valid {@link Role}
     */
    public static Role getRoleFromJWT(RoutingContext ctx){
        if (ctx.user() == null) {
            throw new UnauthorizedException();
        }
        JsonObject payload = ctx.user().principal();
        String roleString = payload.getString("role");
        if (roleString == null) {
            throw new UnauthorizedException();
        }
        try {
            return Role.valueOf(roleString);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException();
        }
    }

    /** Returns {@code true} if the authenticated user is an admin. */
    public static boolean isAdmin(RoutingContext ctx){
        return getRoleFromJWT(ctx) == Role.ADMIN;
    }

}