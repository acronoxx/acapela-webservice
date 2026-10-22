package fr.imta.acapela_webservice.service;

import fr.imta.acapela_webservice.model.*;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for API key caches, their audio files, and cache storage requests.
 *
 * <p>A cache stores the audio files already synthesized for an API key, so that the same
 * text does not need to be sent to Acapela again.
 */
public class CacheService {
    private final EntityManager db;

    public CacheService(EntityManager db) {
        this.db = db;
    }

    /** Returns the cache with this id, or {@code null} if it does not exist. */
    public Cache findCacheById(Long cacheId) {
        return db.find(Cache.class, cacheId);
    }

    public List<File> getFiles(Long cacheId) {
        Cache cache = findCacheById(cacheId);
        if (cache == null) {
            throw new AppException.CacheNotFoundException(cacheId);
        }
        return cache.getFiles();
    }

    /** Returns the file of the cache with this id, or {@code null} if the cache has no such file. */
    public File getFileById(Long cacheId, Long fileId) {
        Cache cache = findCacheById(cacheId);
        if (cache == null) {
            throw new AppException.CacheNotFoundException(cacheId);
        }
        return cache.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElse(null);
    }

    /** Empties the cache: resets its size and detaches its files. */
    public void clearCache(Long cacheId) {
        Cache cache = findCacheById(cacheId);
        if (cache == null) {
            throw new AppException.CacheNotFoundException(cacheId);
        }
        try {
            db.getTransaction().begin();
            cache.setSize(0);
            cache.setFiles(new ArrayList<>());
            db.merge(cache);
            db.getTransaction().commit();
        } catch (Exception e) {
            if (db.getTransaction().isActive()) db.getTransaction().rollback();
            throw e;
        }
    }

    public void deleteFile(Long cacheId, Long fileId) {
        Cache cache = findCacheById(cacheId);
        if (cache == null) {
            throw new AppException.CacheNotFoundException(cacheId);
        }
        try {
            db.getTransaction().begin();
            cache.getFiles().removeIf(f -> f.getId().equals(fileId));
            db.merge(cache);
            db.getTransaction().commit();
        } catch (Exception e) {
            if (db.getTransaction().isActive()) db.getTransaction().rollback();
            throw e;
        }
    }

    public void setCacheMaxSize(Long cacheId, int maxSize) {
        Cache cache = findCacheById(cacheId);
        if (cache == null) {
            throw new AppException.CacheNotFoundException(cacheId);
        }
        try {
            db.getTransaction().begin();
            cache.setMaxSize(maxSize); 
            db.merge(cache);
            db.getTransaction().commit();
        } catch (Exception e) {
            if (db.getTransaction().isActive()) db.getTransaction().rollback();
            throw e;
        }
    }

    /**
     * Records a request for more cache storage on an API key, with the {@code PROCESSING} status.
     *
     * @throws AppException.AllArgRequiredException if {@code description} is empty or {@code size} is not positive
     */
    public CacheStorageRequest requestCacheStorage(int size, Long id_apiKey, Long id_user, String description) {
        User user = db.find(User.class, id_user);
        ApiKey apiKey = db.find(ApiKey.class, id_apiKey);
        
        if (user == null) {                                                                                        
            throw new AppException.UserNotFoundException(id_user);           
        }
        if (apiKey == null) {                                                                                        
            throw new AppException.ApiKeyNotFoundException(id_apiKey);           
        }
        if (description == null || description.isEmpty()) {
            throw new AppException.AllArgRequiredException("description");
        }
        if (size <= 0) {
            throw new AppException.AllArgRequiredException("size");
        }

        CacheStorageRequest cRequest = new CacheStorageRequest();
        cRequest.generateRequestedAt();
        cRequest.setRequestedSize(size);
        cRequest.setDescription(description);
        cRequest.setApiKey(apiKey);
        cRequest.setUser(user);
        cRequest.setStatus(Status.PROCESSING);
        cRequest.setProcessedAt(null);
        cRequest.setType(RequestType.CACHESTORAGE);

        try {
            db.getTransaction().begin();
            db.persist(cRequest);
            db.getTransaction().commit();
        } catch (Exception e) {
            if (db.getTransaction().isActive()) db.getTransaction().rollback();
            throw e;
        }

        return cRequest;
    }

    public Request findByIdRequest(Long id) {
        Request cRequest = db.find(Request.class, id);
        if (cRequest == null) {
            throw new AppException.RequestNotFoundException(id);
        }
        return cRequest;
    }

    public void removeRequest(Request request) {
        try {
            db.getTransaction().begin();
            db.remove(db.contains(request) ? request : db.merge(request));
            db.getTransaction().commit();
        } catch (Exception e) {
            if (db.getTransaction().isActive()) db.getTransaction().rollback();
            throw e;
        }
    }
}