package fr.imta.acapela_webservice.server;

import fr.imta.acapela_webservice.handler.*;
import fr.imta.acapela_webservice.service.*;

import java.util.Map;
import java.util.Set;

import org.eclipse.persistence.logging.SessionLog;
import static org.eclipse.persistence.config.PersistenceUnitProperties.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

public class VertxServer extends AbstractVerticle {
    private Vertx vertx;
    private EntityManager db; // database object

    public VertxServer() {
        this.vertx = Vertx.vertx();

        // setup database connexion
        Map<String, String> properties = Map.of(
            LOGGING_LEVEL, SessionLog.WARNING_LABEL
        );

        var emf = Persistence.createEntityManagerFactory("acapela-webservice", properties);
        db = emf.createEntityManager();
    }

    public void start() {
        Router router = Router.router(vertx);

        // Config : add handlers for payload parsing and to allow swagger to send requests
        router.route().handler(BodyHandler.create());
        router.route().handler(CorsHandler.create()
            .addOrigin("*")
            .allowedMethods(Set.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE))
            .allowedHeaders(Set.of("Authorization", "Content-Type")));

        // JWT Config (For authentification)
        String jwtKey = "g0AuxGSUTMpcFJC7zZD52iZxMo2lWQ9G"; // To-do : Dont leave this in the code

        JWTAuth jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
        .addPubSecKey(new PubSecKeyOptions()
            .setAlgorithm("HS256")
            .setBuffer(jwtKey)));

        // create services
        SynthesisService ss = new SynthesisService(db,vertx);
        ApiKeyCreditService akcs = new ApiKeyCreditService(db);
        CacheService cs = new CacheService(db);
        UserService us = new UserService(db);

        // create handlers
        SynthesisHandler sh = new SynthesisHandler(ss);
        ApiKeyCreditHandler akch = new ApiKeyCreditHandler(akcs);
        CacheHandler ch = new CacheHandler(cs);
        UserHandler uh = new UserHandler(us,jwtAuth);

        // ===== ROUTES ===== //
        // routers
        Router apiRouter = Router.router(vertx);

        // public routes
        apiRouter.post("/users").handler(uh::createUser);
        apiRouter.post("/login").handler(uh::login);

        // routes rights requierements
        apiRouter.route("/*").handler(JWTAuthHandler.create(jwtAuth));

        // auth routes
        apiRouter.get("/users/:userId").handler(uh::getUserById); // SELF or ADMIN
        apiRouter.put("/users/:userId").handler(uh::updateUser); // SELF
        apiRouter.patch("/users/:userId/password").handler(uh::updateUserPassword); // SELF
        apiRouter.patch("/users/:userId/cached-by-default").handler(uh::updateUserCachedByDefault); // SELF
        apiRouter.delete("/users/:userId").handler(uh::deleteUser); // SELF or ADMIN
        
        apiRouter.post("/apikeys").handler(akch::createApiKey); // SELF 
        apiRouter.get("/apikeys/users/:userId").handler(akch::getApiKeysByUserId); // SELF or ADMIN
        apiRouter.get("/apikeys/:apiKeyId").handler(akch::getApiKeyById); // SELF or ADMIN
        apiRouter.patch("/apikeys/:apiKeyId/active").handler(akch::setApiKeyActive); // SELF or ADMIN
        apiRouter.delete("/apikeys/:apiKeyId").handler(akch::deleteApiKey); // change method name // SELF or ADMIN

        apiRouter.get("/caches/:cacheId/files").handler(ch::getFiles); // SELF or ADMIN
        apiRouter.get("/caches/:cacheId/files/:fileId").handler(ch::getFileById); // Change methods name // SELF or ADMIN
        apiRouter.delete("/caches/:cacheId").handler(ch::clearCache); // SELF or ADMIN
        apiRouter.delete("/caches/:cacheId/files/:fileId").handler(ch::deleteFile); // change method name // SELF or ADMIN

        apiRouter.post("/requests/cache").handler(ch::createCacheRequest); //change method name // SELF
        apiRouter.post("/requests/credits").handler(akch::createCreditsRequest); // SELF
        apiRouter.get("/requests/users/:userId").handler(uh::getRequestsByUserId); // SELF or ADMIN 
        apiRouter.get("/requests/users/:userId/caches").handler(uh::getCacheRequestsByUserId); // SELF or ADMIN 
        apiRouter.get("/requests/users/:userId/credits").handler(uh::getCreditsRequestsByUserId); // SELF or ADMIN 
        apiRouter.get("/requests/:requestId").handler(uh::getRequestById); // SELF or ADMIN 
        apiRouter.delete("/requests/:requestId").handler(uh::deleteRequest); // SELF or ADMIN

        // admin routes
        apiRouter.get("/users").handler(uh::getUsers); // ADMIN
        apiRouter.patch("/users/:userId/role").handler(uh::updateUserRole); // ADMIN

        apiRouter.get("/apikeys").handler(akch::getApiKeys); // ADMIN
        apiRouter.patch("/apikeys/:apiKeyId/credits").handler(akch::setApiKeyCredits); // ADMIN
        apiRouter.patch("/apikeys/:apiKeyId/status").handler(akch::setApiKeyStatus); // ADMIN
        apiRouter.patch("/apikeys/:apiKeyId/unlimited").handler(akch::setApiKeyUnlimited); // ADMIN

        apiRouter.patch("/caches/:cacheId/max-size").handler(ch::setCacheMaxSize); // ADMIN

        apiRouter.get("/requests").handler(uh::getRequests); // ADMIN
        apiRouter.patch("/requests/:requestId").handler(uh::updateRequest); // ADMIN

        // subrouters
        router.route("/api/*").subRouter(apiRouter);
            
        // start the server
        vertx.createHttpServer().requestHandler(router).listen(8080)
            .onSuccess(e -> 
                System.out.println("Server is listening on localhost:" + e.actualPort())
            ).onFailure(e -> {
                System.out.println("Cannot start server, got error: " + e.getLocalizedMessage());
                System.exit(1);
            });
    }

    public static void main(String[] args) {
        new VertxServer().start();
    }

}
