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

public class ApiKeyCreditHandler {
    private ApiKeyCreditService apiKeysCreditService;

    public ApiKeyCreditHandler(ApiKeyCreditService apiKeysCreditService) {
        this.apiKeysCreditService = apiKeysCreditService;
    }

    // ===== API KEYS =====

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