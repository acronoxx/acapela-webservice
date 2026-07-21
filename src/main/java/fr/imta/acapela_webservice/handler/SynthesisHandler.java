package fr.imta.acapela_webservice.handler;

import fr.imta.acapela_webservice.model.AppException;
import fr.imta.acapela_webservice.service.SynthesisService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class SynthesisHandler {
    
    private SynthesisService synthesisService;

    public SynthesisHandler(SynthesisService synthesisService) {
        this.synthesisService = synthesisService;
    }

    /**
     * Méthode appelée lors d'un POST /api/synthesis
     */
    public void createSynthesis(RoutingContext ctx) {
        try {
            // Lecture du corps de la requête HTTP
            JsonObject body = ctx.body().asJsonObject();
            if (body == null) throw new AppException.JsonBodyNotValidException();

            // Extraction des paramètres
            String text = body.getString("text");
            String voice = body.getString("voice", "Margaux"); // Margaux par défaut
            String language = body.getString("language", "fr"); // fr par défaut
            String token = body.getString("token");
            String format = body.getString("format", "wav"); // wav par défaut
            Boolean useCache = body.getBoolean("use_cache", true);

            // Vérification des champs obligatoires
            if (text == null || token == null) {
                throw new AppException.AllArgRequiredException("Le 'text' et le 'token' sont obligatoires.");
            }

            // Appel du service métier asynchrone
            synthesisService.generateAudio(text, voice, language, token, format, useCache)
                .onSuccess(audioPath -> {
                    // Si la synthèse a réussi, on renvoie un code 200 avec le chemin
                    JsonObject response = new JsonObject()
                        .put("status", "success")
                        .put("audio_path", audioPath)
                        .put("message", "Le fichier audio a bien été généré ou récupéré.");
                        
                    ctx.response()
                       .setStatusCode(200)
                       .putHeader("content-type", "application/json")
                       .end(response.encode());
                })
                .onFailure(error -> {
                    // Gestion des erreurs (Crédits insuffisants, Clé invalide, Crash JSAPI...)
                    if (error instanceof AppException) {
                        AppException appEx = (AppException) error;
                        ctx.response()
                           .setStatusCode(appEx.getStatusCode())
                           .end(appEx.getMessage());
                    } else {
                        ctx.response()
                           .setStatusCode(500)
                           .end("Erreur interne du serveur lors de la synthèse : " + error.getMessage());
                    }
                });

        } catch (AppException e) {
            ctx.response().setStatusCode(e.getStatusCode()).end(e.getMessage());
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end("Le format JSON de votre requête est invalide.");
        }
    }
}