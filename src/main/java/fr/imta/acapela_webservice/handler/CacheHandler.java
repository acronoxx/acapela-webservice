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

/**
 * HTTP handlers for API key caches, their audio files, and cache storage requests.
 *
 * <p>Each API key owns one {@link Cache}. Only the owner of that key, or an admin, can read
 * or clear it; changing its maximum size is reserved for admins.
 */
public class CacheHandler {
    private final CacheService cacheService;

    public CacheHandler(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /** {@code GET /api/caches/:cacheId/files} (SELF or ADMIN): lists the audio files stored in a cache. */
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

    /** {@code GET /api/caches/:cacheId/files/:fileId} (SELF or ADMIN): returns one file of a cache, or 404. */
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

    /**
     * {@code PATCH /api/caches/:cacheId/max-size} (ADMIN): changes the maximum size of a cache.
     * Body: {@code {"maxSize": int}}. Responds 200, or 400 if {@code maxSize} is missing.
     */
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

    /** {@code DELETE /api/caches/:cacheId} (SELF or ADMIN): empties a cache. Responds 200. */
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

    /** {@code DELETE /api/caches/:cacheId/files/:fileId} (SELF or ADMIN): removes one file from a cache. Responds 200. */
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

    /**
     * {@code POST /api/requests/cache} (SELF): asks an admin for more cache storage on an API key.
     * Body: {@code {"userId": long, "apiKeyId": long, "size": int, "description": string}};
     * {@code userId} must be the caller's id. Returns the created request.
     */
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