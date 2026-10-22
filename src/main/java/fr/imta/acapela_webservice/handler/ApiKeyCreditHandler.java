package fr.imta.acapela_webservice.handler;

import java.util.List;

import fr.imta.acapela_webservice.model.ApiKey;
import fr.imta.acapela_webservice.model.AppException;
import fr.imta.acapela_webservice.model.CreditsRequest;
import fr.imta.acapela_webservice.model.Status;
import fr.imta.acapela_webservice.service.ApiKeyCreditService;
import fr.imta.acapela_webservice.util.AuthUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * HTTP handlers for API keys and credit requests.
 *
 * <p>Each method reads the path parameters and JSON body, checks the caller's rights with
 * {@link AuthUtils}, delegates to {@link ApiKeyCreditService} and writes the response.
 * Any {@link AppException} is turned into a response with its status code and message.
 */
public class ApiKeyCreditHandler {
    private ApiKeyCreditService apiKeysCreditService;

    public ApiKeyCreditHandler(ApiKeyCreditService apiKeysCreditService) {
        this.apiKeysCreditService = apiKeysCreditService;
    }

    // ===== API KEYS =====

    /**
     * {@code POST /api/apikeys} (SELF): creates an API key, and its cache, for the caller.
     * Body: {@code {"name": string}}. Returns the created key.
     */
    public void createApiKey(RoutingContext ctx){
        try {
            Long userId = AuthUtils.getUserIdFromJWT(ctx);

            JsonObject input = ctx.body().asJsonObject();
            String name = getNameFromJSON(input);
            
            ApiKey apikey = apiKeysCreditService.createApiKey(name, userId);
            ctx.json(apikey.toJSON());
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code GET /api/apikeys} (ADMIN): lists every API key. */
    public void getApiKeys(RoutingContext ctx){
        try {
            AuthUtils.assertAdmin(ctx);

            List<ApiKey> apikeys = apiKeysCreditService.findAllApiKeys();
            JsonArray response = new JsonArray();
            for (ApiKey apikey : apikeys) {
                response.add(apikey.toJSON());
            }
            ctx.json(response);
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code GET /api/apikeys/users/:userId} (SELF or ADMIN): lists the API keys of a user. */
    public void getApiKeysByUserId(RoutingContext ctx){
        try {
            Long userId = getUserIdFromPath(ctx);
            AuthUtils.assertSelfOrAdmin(ctx, userId);

            List<ApiKey> apikeys = apiKeysCreditService.findApiKeysByUserId(userId);
            JsonArray response = new JsonArray();
            for (ApiKey apikey : apikeys) {
                response.add(apikey.toJSON());
            }
            ctx.json(response);
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code GET /api/apikeys/:apiKeyId} (SELF or ADMIN): returns one API key. */
    public void getApiKeyById(RoutingContext ctx){
        try {
            Long apiKeyId = getApiKeyIdFromPath(ctx);
            ApiKey apiKey = apiKeysCreditService.findApiKeyById(apiKeyId);

            Long ownerId = apiKey.getUser().getId();
            AuthUtils.assertSelfOrAdmin(ctx, ownerId);

            ctx.json(apiKey.toJSON());
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /**
     * {@code PATCH /api/apikeys/:apiKeyId/active} (SELF or ADMIN): enables or disables a key.
     * Body: {@code {"active": boolean}}. Responds 204.
     */
    public void setApiKeyActive(RoutingContext ctx){
        try {
            Long apiKeyId = getApiKeyIdFromPath(ctx);
            Long ownerId = apiKeysCreditService.findApiKeyById(apiKeyId).getUser().getId();
            AuthUtils.assertSelfOrAdmin(ctx, ownerId);

            JsonObject input = ctx.body().asJsonObject();
            Boolean active = getActiveFromJSON(input);
            apiKeysCreditService.setApiKeyActive(apiKeyId, active);

            ctx.response().setStatusCode(204).end();
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /**
     * {@code PATCH /api/apikeys/:apiKeyId/credits} (ADMIN): sets the credit balance of a key.
     * Body: {@code {"credits": int}} (must be &gt;= 0). Responds 204.
     */
    public void setApiKeyCredits(RoutingContext ctx){
        try {
            AuthUtils.assertAdmin(ctx);
            Long apiKeyId = getApiKeyIdFromPath(ctx);

            JsonObject input = ctx.body().asJsonObject();
            int credits = getCreditsFromJSON(input);
            apiKeysCreditService.setApiKeyCredits(apiKeyId, credits);

            ctx.response().setStatusCode(204).end();
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /**
     * {@code PATCH /api/apikeys/:apiKeyId/status} (ADMIN): changes the status of a key.
     * Body: {@code {"status": "PENDING" | "PROCESSING" | "DONE" | ...}} (see {@link Status}). Responds 204.
     */
    public void setApiKeyStatus(RoutingContext ctx){
        try {
            AuthUtils.assertAdmin(ctx);
            Long apiKeyId = getApiKeyIdFromPath(ctx);

            JsonObject input = ctx.body().asJsonObject();
            Status status = getStatusFromJSON(input);
            apiKeysCreditService.setApiKeyStatus(apiKeyId, status);

            ctx.response().setStatusCode(204).end();
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /**
     * {@code PATCH /api/apikeys/:apiKeyId/unlimited} (ADMIN): lets a key synthesize without spending credits.
     * Body: {@code {"unlimited": boolean}}. Responds 204.
     */
    public void setApiKeyUnlimited(RoutingContext ctx){
        try {
            AuthUtils.assertAdmin(ctx);
            Long apiKeyId = getApiKeyIdFromPath(ctx);

            JsonObject input = ctx.body().asJsonObject();
            Boolean unlimited = getUnlimitedFromJSON(input);
            apiKeysCreditService.setApiKeyUnlimited(apiKeyId, unlimited);

            ctx.response().setStatusCode(204).end();
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    /** {@code DELETE /api/apikeys/:apiKeyId} (SELF or ADMIN): deletes a key. Responds 204. */
    public void deleteApiKey(RoutingContext ctx){
        try {
            Long apiKeyId = getApiKeyIdFromPath(ctx);
            Long ownerId = apiKeysCreditService.findApiKeyById(apiKeyId).getUser().getId();
            AuthUtils.assertSelfOrAdmin(ctx, ownerId);

            apiKeysCreditService.deleteApiKey(apiKeyId);
            ctx.response().setStatusCode(204).end();
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    // ===== CREDITS REQUESTS =====

    /**
     * {@code POST /api/requests/credits} (SELF): asks an admin for more credits on one of the caller's keys.
     * Body: {@code {"api_key_id": long, "amount": int, "description": string}}.
     * Returns the created request, with status {@code PENDING}.
     */
    public void createCreditsRequest(RoutingContext ctx){
        try {
            Long userId = AuthUtils.getUserIdFromJWT(ctx);

            JsonObject input = ctx.body().asJsonObject();
            Long apiKeyId = input.getLong("api_key_id");
            String description = getDescriptionFromJSON(input);
            int amount = getAmountFromJSON(input);

            // verify the apikey belongs to the user
            ApiKey apiKey = apiKeysCreditService.findApiKeyById(apiKeyId);
            AuthUtils.assertSelf(ctx, apiKey.getUser().getId());

            CreditsRequest cRequest = apiKeysCreditService.requestCredit(amount, apiKeyId, userId, description);
            ctx.json(cRequest.toJSON());
        }
        catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    // ===== UTILS =====

    private Long getApiKeyIdFromPath(RoutingContext ctx) {
        return Long.parseLong(ctx.pathParam("apiKeyId"));
    }

    private Long getUserIdFromPath(RoutingContext ctx) {
        return Long.parseLong(ctx.pathParam("userId"));
    }

    private String getNameFromJSON(JsonObject input) { 
        return input.getString("name"); 
    }

    private String getDescriptionFromJSON(JsonObject input) { 
        return input.getString("description"); 
    
    }
    private int getAmountFromJSON(JsonObject input) { 
        return input.getInteger("amount"); 
    }

    private int getCreditsFromJSON(JsonObject input) { 
        return input.getInteger("credits"); 
    }

    private Boolean getActiveFromJSON(JsonObject input) { 
        return input.getBoolean("active"); 
    }
    private Boolean getUnlimitedFromJSON(JsonObject input) { 
        return input.getBoolean("unlimited");
    }

    private Status getStatusFromJSON(JsonObject input) {
        String statusString = input.getString("status");
        if (statusString == null) {
            throw new AppException.ApiKeyStatusNotValidException();
        }
        try {
            return Status.valueOf(statusString);
        } catch (IllegalArgumentException e) {
            throw new AppException.ApiKeyStatusNotValidException();
        }
    }
}