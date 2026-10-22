package fr.imta.acapela_webservice.handler;

import java.util.List;

import fr.imta.acapela_webservice.model.ApiKey;
import fr.imta.acapela_webservice.model.AppException;
import fr.imta.acapela_webservice.model.CreditsRequest;
import fr.imta.acapela_webservice.model.Request;
import fr.imta.acapela_webservice.model.CacheStorageRequest;
import fr.imta.acapela_webservice.model.Role;
import fr.imta.acapela_webservice.model.Status;
import fr.imta.acapela_webservice.model.User;
import fr.imta.acapela_webservice.service.UserService;
import fr.imta.acapela_webservice.util.AuthUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.JWTOptions;

/**
 * HTTP handlers for user accounts, login, and the requests (credits / cache storage) made by users.
 *
 * <p>{@link #createUser} and {@link #login} are the only public routes of the API;
 * {@link #login} issues the JWT that every other route requires.
 */
public class UserHandler {

    private UserService userService;
    private JWTAuth jwtAuth;

    public UserHandler(UserService userService, JWTAuth jwtAuth) {
        this.userService = userService;
        this.jwtAuth = jwtAuth;
    }

    // ===== ACCOUNT =====
    
    /**
     * {@code POST /api/users} (public): registers a new account with the {@code CLIENT} role.
     * Body: {@code {"first_name", "last_name", "email", "password", "password_confirmation", "cached_by_default": boolean}}.
     * Returns the created user.
     */
    public void createUser(RoutingContext ctx){
        try{
            JsonObject input = ctx.body().asJsonObject();
            String firstName = getFirstNameFromJSON(input);
            String lastName = getLastNameFromJSON(input);
            String email = getEmailFromJSON(input);
            String password = getPasswordFromJSON(input);
            String passwordConfirmation = getPasswordConfirmationFromJSON(input);
            boolean cachedByDefault = getCachedByDefaultFromJSON(input);

            User user = userService.createUser(firstName, lastName, email, password, passwordConfirmation, cachedByDefault);
            ctx.json(user.toJSON());
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /**
     * {@code POST /api/login} (public): checks the credentials and returns {@code {"token": jwt}}.
     * Body: {@code {"email", "password"}}. The token holds the user id and role and expires after one hour.
     */
    public void login(RoutingContext ctx){
        try {
            JsonObject input = ctx.body().asJsonObject();
            String email = getEmailFromJSON(input);
            String password = getPasswordFromJSON(input);

            User user = userService.authenticate(email, password);

            JsonObject payload = new JsonObject()
                .put("id", user.getId())
                .put("role", user.getRole().name());

            String token = jwtAuth.generateToken(payload, new JWTOptions().setExpiresInSeconds(3600));

            ctx.json(new JsonObject().put("token", token));
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code GET /api/users} (ADMIN): lists every user. */
    public void getUsers(RoutingContext ctx){
        try{
            AuthUtils.assertAdmin(ctx);

            List<User> users = userService.findAllUsers();
            JsonArray response = new JsonArray();
            for(User user : users){
                response.add(user.toJSON());
            }

            ctx.json(response);
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code GET /api/users/:userId} (SELF or ADMIN): returns one user. */
    public void getUserById(RoutingContext ctx){
        try{
            Long userId = getUserIdFromPath(ctx);
            AuthUtils.assertSelfOrAdmin(ctx, userId);
            User user = userService.findUserById(userId);                                                                 
            ctx.json(user.toJSON());                                                    
        } 
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /**
     * {@code PUT /api/users/:userId} (SELF): replaces the profile of the caller.
     * Body: {@code {"first_name", "last_name", "email", "cached_by_default": boolean}}. Returns the updated user.
     */
    public void updateUser(RoutingContext ctx){
        try{
            Long userId = getUserIdFromPath(ctx);
            AuthUtils.assertSelf(ctx, userId);

            JsonObject input = ctx.body().asJsonObject();
            String first_name = getFirstNameFromJSON(input);
            String last_name = getLastNameFromJSON(input);
            String email = getEmailFromJSON(input);
            Boolean cached = getCachedByDefaultFromJSON(input);

            userService.updateUser(userId, first_name, last_name, email, cached);
            User user = userService.findUserById(userId);
            ctx.json(user.toJSON());
        } 
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /**
     * {@code PATCH /api/users/:userId/password} (SELF): changes the caller's password.
     * Body: {@code {"password", "password_confirmation"}}. Responds 204.
     */
    public void updateUserPassword(RoutingContext ctx){
        try{
            Long userId = getUserIdFromPath(ctx);
            AuthUtils.assertSelf(ctx, userId);

            JsonObject input = ctx.body().asJsonObject();
            String password = getPasswordFromJSON(input);
            String passwordConfirmation = getPasswordConfirmationFromJSON(input);
            userService.updateUserPassword(userId, password, passwordConfirmation);

            ctx.response().setStatusCode(204).end();
        } 
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /**
     * {@code PATCH /api/users/:userId/cached-by-default} (SELF): sets whether syntheses are cached by default.
     * Body: {@code {"cached_by_default": boolean}}. Responds 204.
     */
    public void updateUserCachedByDefault(RoutingContext ctx){
        try{
            Long userId = getUserIdFromPath(ctx);
            AuthUtils.assertSelf(ctx, userId);

            JsonObject input = ctx.body().asJsonObject();
            Boolean cached = getCachedByDefaultFromJSON(input);
            userService.updateUserCacheByDefault(userId, cached);

            ctx.response().setStatusCode(204).end();
        } 
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /**
     * {@code PATCH /api/users/:userId/role} (ADMIN): changes the role of a user.
     * Body: {@code {"role": "ADMIN" | "CLIENT"}}. Responds 204.
     */
    public void updateUserRole(RoutingContext ctx){
        try{
            AuthUtils.assertAdmin(ctx);

            Long userId = getUserIdFromPath(ctx);
            JsonObject input = ctx.body().asJsonObject();
            Role role = getRoleFromJSON(input);
            userService.updateUserRole(userId, role);

            ctx.response().setStatusCode(204).end();
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code DELETE /api/users/:userId} (SELF or ADMIN): deletes an account. Responds 204. */
    public void deleteUser(RoutingContext ctx){
        try{
            Long userId = getUserIdFromPath(ctx);
            AuthUtils.assertSelfOrAdmin(ctx, userId);
                          
            userService.deleteUser(userId);

            ctx.response().setStatusCode(204).end();
        } 
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }        
    }

    // ===== REQUESTS =====

    /** {@code GET /api/requests} (ADMIN): lists every request, of every type. */
    public void getRequests(RoutingContext ctx){
        try{
            AuthUtils.assertAdmin(ctx);

            List<Request> requests = userService.findAllRequests();

            ctx.json(requests);
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code GET /api/requests/users/:userId} (SELF or ADMIN): lists the credits and cache requests made on a user's keys. */
    public void getRequestsByUserId(RoutingContext ctx){
        try{
            Long userId = getUserIdFromPath(ctx);
            AuthUtils.assertSelfOrAdmin(ctx, userId);

            User user = userService.findUserById(userId);  
            JsonArray results = new JsonArray();
            for (ApiKey apiKey : user.getApiKeys()){
                for (CreditsRequest creditsRequest : userService.findCreditsRequests(apiKey)){
                    results.add(creditsRequest.toJSON());
                }
                for (CacheStorageRequest cacheRequest : userService.findCacheRequests(apiKey)){
                    results.add(cacheRequest.toJSON());
                }
            }

            ctx.json(results);
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code GET /api/requests/users/:userId/credits} (SELF or ADMIN): lists the credits requests made on a user's keys. */
    public void getCreditsRequestsByUserId(RoutingContext ctx){
        try{
            Long userId = getUserIdFromPath(ctx);
            AuthUtils.assertSelfOrAdmin(ctx, userId);

            User user = userService.findUserById(userId);  
            JsonArray results = new JsonArray();
            for (ApiKey apiKey : user.getApiKeys()){
                for (CreditsRequest creditsRequest : userService.findCreditsRequests(apiKey)){
                    results.add(creditsRequest.toJSON());
                }
            }

            ctx.json(results);
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code GET /api/requests/users/:userId/caches} (SELF or ADMIN): lists the cache storage requests made on a user's keys. */
    public void getCacheRequestsByUserId(RoutingContext ctx){
        try{
            Long userId = getUserIdFromPath(ctx);
            AuthUtils.assertSelfOrAdmin(ctx, userId);

            User user = userService.findUserById(userId);  
            JsonArray results = new JsonArray();
            for (ApiKey apiKey : user.getApiKeys()){
                for (CacheStorageRequest cacheRequest : userService.findCacheRequests(apiKey)){
                    results.add(cacheRequest.toJSON());
                }
            }

            ctx.json(results);
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code GET /api/requests/:requestId} (SELF or ADMIN): returns one request. */
    public void getRequestById(RoutingContext ctx){
        try{
            Long requestId = getRequestIdFromPath(ctx);
            Request request = userService.findRequestById(requestId);
            Long userId = request.getUser().getId();
            AuthUtils.assertSelfOrAdmin(ctx, userId);                                                             
            ctx.json(request.toJSON());                                                    
        } 
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /**
     * {@code PATCH /api/requests/:requestId} (ADMIN): processes a request by changing its status.
     * Body: {@code {"status": "DONE" | "REFUSED" | ...}} (see {@link Status}). Responds 204.
     */
    public void updateRequest(RoutingContext ctx){
        try{
            AuthUtils.assertAdmin(ctx);

            Long requestId = getRequestIdFromPath(ctx);
            JsonObject input = ctx.body().asJsonObject();
            Status status = getStatusFromJSON(input);
            
            userService.updateRequestStatus(requestId, status);
            ctx.response().setStatusCode(204).end();
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code DELETE /api/requests/:requestId} (SELF or ADMIN): deletes a request. Responds 204. */
    public void deleteRequest(RoutingContext ctx){
        try{
            Long requestId = getRequestIdFromPath(ctx);
            Long userId = userService.findRequestById(requestId).getUser().getId();
            AuthUtils.assertSelfOrAdmin(ctx, userId);
                                 
            userService.deleteRequest(requestId);

            ctx.response().setStatusCode(204).end();
        } 
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }     
    }

    // UTILS

    private Long getUserIdFromPath(RoutingContext ctx) {
        return Long.parseLong(ctx.pathParam("userId"));
    }

    private Long getRequestIdFromPath(RoutingContext ctx) {
        return Long.parseLong(ctx.pathParam("requestId"));
    }


    private String getFirstNameFromJSON(JsonObject input) {
        return input.getString("first_name");
    }

    private String getLastNameFromJSON(JsonObject input) {
        return input.getString("last_name");
    }

    private String getEmailFromJSON(JsonObject input) {
        return input.getString("email");
    }

    private String getPasswordFromJSON(JsonObject input) {
        return input.getString("password");
    }

    private String getPasswordConfirmationFromJSON(JsonObject input) {
        return input.getString("password_confirmation");
    }

    private Boolean getCachedByDefaultFromJSON(JsonObject input) {
        return input.getBoolean("cached_by_default");
    }

    private Role getRoleFromJSON(JsonObject input) {
        String roleString = input.getString("role");
        if (roleString == null) {
            throw new AppException.RoleNotValidException();
        }
        try {
            return Role.valueOf(roleString);
        } catch (IllegalArgumentException e) {
            throw new AppException.RoleNotValidException();
        }
    }

    private Status getStatusFromJSON(JsonObject input) {
        String statusString = input.getString("status");
        if (statusString == null) {
            throw new AppException.RequestStatusNotValidException();
        }
        try {
            return Status.valueOf(statusString);
        } catch (IllegalArgumentException e) {
            throw new AppException.RequestStatusNotValidException();
        }
    }

}