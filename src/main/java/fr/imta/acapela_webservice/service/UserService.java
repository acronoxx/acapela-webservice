package fr.imta.acapela_webservice.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.favre.lib.crypto.bcrypt.BCrypt;

import fr.imta.acapela_webservice.model.ApiKey;
import fr.imta.acapela_webservice.model.AppException;
import fr.imta.acapela_webservice.model.CreditsRequest;
import fr.imta.acapela_webservice.model.CacheStorageRequest;
import fr.imta.acapela_webservice.model.Cache;
import fr.imta.acapela_webservice.model.Request;
import fr.imta.acapela_webservice.model.Role;
import fr.imta.acapela_webservice.model.Status;
import fr.imta.acapela_webservice.model.User;
import jakarta.persistence.EntityManager;

public class UserService {
    private EntityManager db;

    public UserService(EntityManager db) {
        this.db = db;
    }

    // ===== ACCOUNT =====

    public User createUser(String firstName, String lastName, String email, String password, String passwordConfirmation, boolean cachedByDefault) {
        createValidation(firstName, lastName, email, password, passwordConfirmation, cachedByDefault);

        User user = new User();
        setUserFields(user, firstName, lastName, email, password, cachedByDefault);

        db.getTransaction().begin();
        try {
            db.persist(user);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }

        return user;
    }

    public User authenticate(String email, String password) {
        if (email == null || email.isEmpty()) {
            throw new AppException.UnauthorizedException();
        }
        if (password == null || password.isEmpty()) {
            throw new AppException.UnauthorizedException();
        }

        User user = findUserByEmail(email);
        if (user == null) {
            throw new AppException.UnauthorizedException();
        }

        BCrypt.Result result = BCrypt.verifyer().verify(
            password.toCharArray(),
            user.getPasswordHash()
        );
        if (!result.verified) {
            throw new AppException.UnauthorizedException();
        }

        return user;
    }

    public void updateUser(Long id, String firstName, String lastName, String email, Boolean cached) {
        User user = findUserById(id);
        if (user == null) {
            throw new AppException.UserNotFoundException(id);
        }

        if (!isEmailValid(email)) {
            throw new AppException.EmailNotValidException();
        }
        // Vérifie disponibilité de l'email seulement si l'utilisateur veut le changer
        if (!email.equals(user.getEmail()) && !isEmailAvailable(email)) {
            throw new AppException.EmailAlreadyExistException(email);
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setCachedByDefault(cached);

        db.getTransaction().begin();
        try {
            db.merge(user);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }
    }

    public void updateUserPassword(Long id, String password, String passwordConfirmation) {
        User user = findUserById(id);
        if (user == null) {
            throw new AppException.UserNotFoundException(id);
        }
        if (password == null || password.isEmpty()) {
            throw new AppException.AllArgRequiredException("password");
        }
        if (passwordConfirmation == null || passwordConfirmation.isEmpty()) {
            throw new AppException.AllArgRequiredException("password_confirmation");
        }
        if (!isPasswordValid(password)) {
            throw new AppException.PasswordNotValidException();
        }
        if (!password.equals(passwordConfirmation)) {
            throw new AppException.PasswordsNotSameException();
        }

        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        user.setPasswordHash(hash);

        db.getTransaction().begin();
        try {
            db.merge(user);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }
    }

    public void updateUserCacheByDefault(Long id, Boolean cached) {
        User user = findUserById(id);
        if (user == null) {
            throw new AppException.UserNotFoundException(id);
        }
        if (cached == null) {
            throw new AppException.AllArgRequiredException("cached_by_default");
        }

        user.setCachedByDefault(cached);

        db.getTransaction().begin();
        try {
            db.merge(user);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }
    }

    public void updateUserRole(Long id, Role role) {
        User user = findUserById(id);
        if (user == null) {
            throw new AppException.UserNotFoundException(id);
        }
        if (role == null) {
            throw new AppException.RoleNotValidException();
        }

        user.setRole(role);

        db.getTransaction().begin();
        try {
            db.merge(user);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }
    }

    public void deleteUser(Long id) {
        User user = findUserById(id);
        if (user == null) {
            throw new AppException.UserNotFoundException(id);
        }

        db.getTransaction().begin();
        try {
            db.remove(user);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }
    }

    // ===== FIND USERS =====

    public List<User> findAllUsers() {
        return new ArrayList<>(db.createQuery("SELECT u FROM User u", User.class).getResultList());
    }

    public User findUserById(Long id) {
        return db.find(User.class, id);
    }

    public User findUserByEmail(String email) {
        List<User> results = db.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
            .setParameter("email", email)
            .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    // ===== REQUESTS =====

    public Request findRequestById(Long id) {
        Request request = db.find(Request.class, id);
        if (request == null) {
            throw new AppException.RequestNotFoundException(id);
        }
        return request;
    }

    public List<Request> findAllRequests() {
        return new ArrayList<>(db.createQuery("SELECT r FROM Request r", Request.class).getResultList());
    }

    public List<CreditsRequest> findCreditsRequests(ApiKey apiKey) {
        return db.createQuery("SELECT cr FROM CreditsRequest cr WHERE cr.apiKey = :apikey", CreditsRequest.class)
            .setParameter("apikey", apiKey)
            .getResultList();
    }

    public List<CacheStorageRequest> findCacheRequests(ApiKey apiKey) {
        return db.createQuery("SELECT csr FROM CacheStorageRequest csr WHERE csr.apiKey = :apikey", CacheStorageRequest.class)
            .setParameter("apikey", apiKey)
            .getResultList();
    }

    public Cache findCache(ApiKey apiKey) {
        return db.createQuery("SELECT c FROM Cache c WHERE c.apiKey = :apikey", Cache.class)
            .setParameter("apikey", apiKey)
            .getSingleResult();
    }

    public void updateRequestStatus(Long requestId, Status status) {
        Request request = findRequestById(requestId);
        if (status == null) {
            throw new AppException.RequestStatusNotValidException();
        }

        request.setStatus(status);
        request.setProcessedAt(new Date());

        // TODO : side effects métier quand le status passe à APPROVED
        // - si CreditsRequest : ajouter les crédits demandés à l'ApiKey
        // - si CacheStorageRequest : augmenter le maxSize du Cache associé
        // À implémenter selon ta logique métier

        db.getTransaction().begin();
        try {
            db.merge(request);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }
    }

    public void deleteRequest(Long requestId) {
        Request request = findRequestById(requestId);

        db.getTransaction().begin();
        try {
            db.remove(request);
            db.getTransaction().commit();
        } catch (RuntimeException e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            throw e;
        }
    }

    // ===== UTILS =====

    private void setUserFields(User user, String firstName, String lastName, String email, String password, boolean cachedByDefault) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        user.setPasswordHash(hash);
        user.setCachedByDefault(cachedByDefault);
    }

    private void createValidation(String firstName, String lastName, String email, String password, String passwordConfirmation, boolean cachedByDefault) {
        if (firstName == null || firstName.isEmpty()) {
            throw new AppException.AllArgRequiredException("first_name");
        }
        if (lastName == null || lastName.isEmpty()) {
            throw new AppException.AllArgRequiredException("last_name");
        }
        if (email == null || email.isEmpty()) {
            throw new AppException.AllArgRequiredException("email");
        }
        if (!isEmailValid(email)) {
            throw new AppException.EmailNotValidException();
        }
        if (!isEmailAvailable(email)) {
            throw new AppException.EmailAlreadyExistException(email);
        }
        if (password == null || password.isEmpty()) {
            throw new AppException.AllArgRequiredException("password");
        }
        if (passwordConfirmation == null || passwordConfirmation.isEmpty()) {
            throw new AppException.AllArgRequiredException("password_confirmation");
        }
        if (!isPasswordValid(password)) {
            throw new AppException.PasswordNotValidException();
        }
        if (!password.equals(passwordConfirmation)) {
            throw new AppException.PasswordsNotSameException();
        }
    }

    private boolean isEmailValid(String email) {
        Pattern emailRule = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher emailMatcher = emailRule.matcher(email);
        return emailMatcher.matches();
    }

    private boolean isPasswordValid(String password) {
        if (password.length() < 12) return false;
        if (justUpperCase(password).isEmpty()) return false;
        if (justLowerCase(password).isEmpty()) return false;
        if (justDigits(password).isEmpty()) return false;
        if (!justSpace(password).isEmpty()) return false;
        if (justSpecialChars(password).isEmpty()) return false;
        char first = password.charAt(0);
        if (first == '.' || first == '-') return false;
        return true;
    }

    private boolean isEmailAvailable(String email) {
        return findUserByEmail(email) == null;
    }

    private String justUpperCase(String word) {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (Character.isUpperCase(c)) sb.append(c);
        }
        return sb.toString();
    }

    private String justLowerCase(String word) {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (Character.isLowerCase(c)) sb.append(c);
        }
        return sb.toString();
    }

    private String justDigits(String word) {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    private String justSpace(String word) {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (Character.isWhitespace(c)) sb.append(c);
        }
        return sb.toString();
    }

    private static final String SPECIAL_CHARS = "!()-.?[]_`~;:@#$%^&*+=";

    private String justSpecialChars(String word) {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0) sb.append(c);
        }
        return sb.toString();
    }
}