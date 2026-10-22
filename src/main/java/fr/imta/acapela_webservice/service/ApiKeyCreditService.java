package fr.imta.acapela_webservice.service;

import java.util.List;
import java.util.ArrayList;

import fr.imta.acapela_webservice.model.ApiKey;
import fr.imta.acapela_webservice.model.AppException;
import fr.imta.acapela_webservice.model.CreditsRequest;
import fr.imta.acapela_webservice.model.RequestType;
import fr.imta.acapela_webservice.model.Cache;
import fr.imta.acapela_webservice.model.Status;
import fr.imta.acapela_webservice.model.User;
import jakarta.persistence.EntityManager;

/**
 * Business logic for API keys and credits requests.
 *
 * <p>Methods validate their arguments, throw an {@link AppException} when a rule is broken,
 * and wrap every write in a JPA transaction that is rolled back on error.
 */
public class ApiKeyCreditService {
    private EntityManager db;

    public ApiKeyCreditService(EntityManager db) {
        this.db = db;
    }

    // ===== API KEYS =====

    /**
     * Creates an API key for a user, together with its cache (max size 50).
     * The key starts inactive, with 0 credits and the {@code PROCESSING} status,
     * so that an admin can review it.
     *
     * @throws AppException.AllArgRequiredException if {@code name} is empty
     * @throws AppException.UserNotFoundException if the user does not exist
     */
    public ApiKey createApiKey(String name, Long userId) {
        if (name == null || name.isEmpty()) {
            throw new AppException.AllArgRequiredException("name");
        }
        User user = db.find(User.class, userId);
        if (user == null) {
            throw new AppException.UserNotFoundException(userId);
        }

        ApiKey apiKey = new ApiKey();
        apiKey.generateToken();
        apiKey.setName(name);
        apiKey.setCredits(0);
        apiKey.setActive(false);
        apiKey.setUser(user);
        apiKey.setStatus(Status.PROCESSING);

        Cache cache = new Cache();
        cache.createCache(apiKey, "", 50);

        db.getTransaction().begin();
        try {
            db.persist(apiKey);
            db.persist(cache);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }

        return apiKey;
    }

    /** @throws AppException.ApiKeyNotFoundException if no key has this id */
    public ApiKey findApiKeyById(Long id) {
        ApiKey apiKey = db.find(ApiKey.class, id);
        if (apiKey == null) {
            throw new AppException.ApiKeyNotFoundException(id);
        }
        return apiKey;
    }

    public List<ApiKey> findAllApiKeys() {
        return new ArrayList<>(db.createQuery("SELECT a FROM ApiKey a", ApiKey.class).getResultList());
    }

    public List<ApiKey> findApiKeysByUserId(Long userId) {
        return db.createQuery("SELECT a FROM ApiKey a WHERE a.user.id = :userId", ApiKey.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    /** Generates a new token for the key (not exposed by any route yet). */
    public void renewToken(Long id) {
        ApiKey apiKey = findApiKeyById(id);
        apiKey.generateToken();
        persistTransaction(apiKey);
    }

    public void setApiKeyActive(Long id, Boolean active) {
        if (active == null) {
            throw new AppException.AllArgRequiredException("active");
        }
        ApiKey apiKey = findApiKeyById(id);
        apiKey.setActive(active);
        persistTransaction(apiKey);
    }

    public void setApiKeyCredits(Long id, int credits) {
        if (credits < 0) {
            throw new AppException.CreditsNotValidException();
        }
        ApiKey apiKey = findApiKeyById(id);
        apiKey.setCredits(credits);
        persistTransaction(apiKey);
    }

    public void setApiKeyStatus(Long id, Status status) {
        if (status == null) {
            throw new AppException.ApiKeyStatusNotValidException();
        }
        ApiKey apiKey = findApiKeyById(id);
        apiKey.setStatus(status);
        persistTransaction(apiKey);
    }

    public void setApiKeyUnlimited(Long id, Boolean unlimited) {
        if (unlimited == null) {
            throw new AppException.AllArgRequiredException("unlimited");
        }
        ApiKey apiKey = findApiKeyById(id);
        apiKey.setUnlimited(unlimited);  // Assume that method exists on ApiKey
        persistTransaction(apiKey);
    }

    public void deleteApiKey(Long id) {
        ApiKey apiKey = findApiKeyById(id);
        db.getTransaction().begin();
        try {
            db.remove(apiKey);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }
    }

    // ===== CREDITS REQUESTS =====

    /**
     * Records a request for more credits on an API key, with the {@code PENDING} status.
     * The credits are only added once an admin processes the request.
     *
     * @throws AppException.AllArgRequiredException if {@code description} is empty or {@code amount} is not positive
     */
    public CreditsRequest requestCredit(int amount, Long apiKeyId, Long userId, String description) {
        if (description == null || description.isEmpty()) {
            throw new AppException.AllArgRequiredException("description");
        }
        if (amount <= 0) {
            throw new AppException.AllArgRequiredException("amount");
        }

        User user = db.find(User.class, userId);
        if (user == null) {
            throw new AppException.UserNotFoundException(userId);
        }
        ApiKey apiKey = db.find(ApiKey.class, apiKeyId);
        if (apiKey == null) {
            throw new AppException.ApiKeyNotFoundException(apiKeyId);
        }

        CreditsRequest cRequest = new CreditsRequest();
        cRequest.generateRequestedAt();
        cRequest.setRequestedAmount(amount);
        cRequest.setDescription(description);
        cRequest.setApiKey(apiKey);
        cRequest.setUser(user);
        cRequest.setStatus(Status.PENDING);
        cRequest.setProcessedAt(null);
        cRequest.setType(RequestType.CREDITS);

        db.getTransaction().begin();
        try {
            db.persist(cRequest);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }

        return cRequest;
    }

    // ===== UTILS =====

    // Saves the changes made to an entity inside a transaction, rolled back on error
    private void persistTransaction(Object entity) {
        db.getTransaction().begin();
        try {
            db.merge(entity);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }
    }
}