package fr.imta.acapela_webservice.handler;

import fr.imta.acapela_webservice.model.AppException;
import fr.imta.acapela_webservice.model.CacheStorageRequest;
import fr.imta.acapela_webservice.model.File;
import fr.imta.acapela_webservice.model.Cache;
import fr.imta.acapela_webservice.service.CacheService;
import fr.imta.acapela_webservice.util.AuthUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.List;

public class CacheHandler {
    private final CacheService cacheService;

    public CacheHandler(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public void getFiles(RoutingContext ctx) {
        try {
            Long cacheId = getCacheIdFromPath(ctx);
            Cache cache = cacheService.findCacheById(cacheId);
            if (cache == null) {
                throw new AppException.CacheNotFoundException(cacheId);
            }
            AuthUtils.assertSelfOrAdmin(ctx, cache.getApiKey().getUser().getId());

            List<File> files = cacheService.getFiles(cacheId);
            JsonArray jsonFiles = new JsonArray();
            files.forEach(f -> jsonFiles.add(f.toJSON()));
            ctx.json(jsonFiles);
        } catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    public void getFileById(RoutingContext ctx) {
        try {
            Long cacheId = getCacheIdFromPath(ctx);
            Cache cache = cacheService.findCacheById(cacheId);
            if (cache == null) {
                throw new AppException.CacheNotFoundException(cacheId);
            }
            AuthUtils.assertSelfOrAdmin(ctx, cache.getApiKey().getUser().getId());

            Long fileId = getFileIdFromPath(ctx);
            File f = cacheService.getFileById(cacheId, fileId);
            if (f == null) {
                ctx.response().setStatusCode(404).end("File not found");
            } else {
                ctx.json(f.toJSON());
            }
        } catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    public void setCacheMaxSize(RoutingContext ctx) {
        try {
            AuthUtils.assertAdmin(ctx);

            Long cacheId = getCacheIdFromPath(ctx);
            JsonObject input = ctx.body().asJsonObject();
            if (input == null || !input.containsKey("maxSize")) {
                ctx.response().setStatusCode(400).end("Missing maxSize field");
                return;
            }
            int maxSize = input.getInteger("maxSize");
            cacheService.setCacheMaxSize(cacheId, maxSize);
            ctx.response().setStatusCode(200).end();
        } catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    public void clearCache(RoutingContext ctx) {
        try {
            Long cacheId = getCacheIdFromPath(ctx);
            Cache cache = cacheService.findCacheById(cacheId);
            if (cache == null) {
                throw new AppException.CacheNotFoundException(cacheId);
            }
            AuthUtils.assertSelfOrAdmin(ctx, cache.getApiKey().getUser().getId());

            cacheService.clearCache(cacheId);
            ctx.response().setStatusCode(200).end();
        } catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    public void deleteFile(RoutingContext ctx) {
        try {
            Long cacheId = getCacheIdFromPath(ctx);
            Cache cache = cacheService.findCacheById(cacheId);
            if (cache == null) {
                throw new AppException.CacheNotFoundException(cacheId);
            }
            AuthUtils.assertSelfOrAdmin(ctx, cache.getApiKey().getUser().getId());

            Long fileId = getFileIdFromPath(ctx);
            cacheService.deleteFile(cacheId, fileId);
            ctx.response().setStatusCode(200).end();
        } catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    public void createCacheRequest(RoutingContext ctx) {
        try {
            JsonObject input = ctx.body().asJsonObject();
            if (input == null) {
                ctx.response().setStatusCode(400).end("Invalid JSON body");
                return;
            }
            Long userId = getUserIdFromJSON(input);
            AuthUtils.assertSelf(ctx, userId);

            String description = getDescriptionFromJSON(input);
            int size = getSizeFromJSON(input);
            Long apiKeyId = getApiKeyIdFromJSON(input);
            
            CacheStorageRequest cRequest = cacheService.requestCacheStorage(size, apiKeyId, userId, description);
            ctx.json(cRequest.toJSON());            
        } catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        }
    }

    private Long getCacheIdFromPath(RoutingContext ctx) {
        return Long.parseLong(ctx.pathParam("cacheId"));
    }

    private Long getFileIdFromPath(RoutingContext ctx) {
        return Long.parseLong(ctx.pathParam("fileId"));
    }

    private String getDescriptionFromJSON(JsonObject input) {
        return input.getString("description");
    }

    private int getSizeFromJSON(JsonObject input) {
        return input.getInteger("size") != null ? input.getInteger("size") : 0;
    }

    private Long getApiKeyIdFromJSON(JsonObject input) {
        return input.getLong("apiKeyId");
    }

    private Long getUserIdFromJSON(JsonObject input) {
        return input.getLong("userId");
    }
}