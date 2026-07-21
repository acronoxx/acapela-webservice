package fr.imta.acapela_webservice.util;

import fr.imta.acapela_webservice.model.Role;
import fr.imta.acapela_webservice.model.AppException.UnauthorizedException;
import fr.imta.acapela_webservice.model.AppException.ForbiddenException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;


public class AuthUtils {

    // Assertions methods

    public static void assertSelf(RoutingContext ctx, Long ownerId){
        Long userId = getUserIdFromJWT(ctx);
        if(!userId.equals(ownerId)) throw new ForbiddenException();
    }

    public static void assertSelfOrAdmin(RoutingContext ctx, Long ownerId){
        if (isAdmin(ctx)) return;
        assertSelf(ctx, ownerId);
    }

    public static void assertAdmin(RoutingContext ctx){
        if (!isAdmin(ctx)) throw new ForbiddenException();
    }

    // Utils

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

    public static boolean isAdmin(RoutingContext ctx){
        return getRoleFromJWT(ctx) == Role.ADMIN;
    }

}