package fr.imta.acapela_webservice.service;

import fr.imta.acapela_webservice.model.*;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import jakarta.persistence.EntityManager;

import javax.speech.Central;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import java.util.Locale;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Service de synthèse vocale, basé sur l'API JSAPI d'Acapela ({@code javax.speech}).
 *
 * <p>Note : l'enregistrement du moteur Acapela et l'écriture du fichier audio sont encore
 * commentés, donc aucun fichier n'est réellement produit pour l'instant.
 */
public class SynthesisService {

    private EntityManager db;
    private Vertx vertx;

    public SynthesisService(EntityManager db, Vertx vertx) {
        this.db = db;
        this.vertx = vertx;
        
        try {
            // Central.registerEngineCentral("com.acapela.javax.speech.AcapelaCentral");
        } catch (Exception e) {
            System.err.println("Erreur d'initialisation du moteur Acapela : " + e.getMessage());
        }
    }

    /**
     * Génère (ou récupère depuis le cache) le fichier audio correspondant à un texte.
     *
     * <p>Étapes, dans une seule transaction exécutée hors de l'event loop :
     * <ol>
     *   <li>retrouve la clé API à partir de son token et vérifie qu'elle est active et a des crédits ;</li>
     *   <li>si le cache est utilisé et contient déjà ce texte, renvoie le fichier existant ;</li>
     *   <li>sinon lance la synthèse Acapela, enregistre le fichier dans le cache,
     *       débite un crédit (sauf clé illimitée) et historise la demande.</li>
     * </ol>
     *
     * @return un {@link Future} contenant le chemin du fichier audio
     */
    public Future<String> generateAudio(String text, String voice, String language, String token, String format, Boolean useCache) {
        
        return vertx.executeBlocking(() -> {
            
            db.getTransaction().begin(); 
            
            try {
                // VERIFICATION DE L'IDENTITE
                List<ApiKey> keys = db.createQuery("SELECT a FROM ApiKey a WHERE a.token = :token", ApiKey.class)
                                      .setParameter("token", token)
                                      .getResultList();
                
                if (keys.isEmpty()) throw new AppException.ApiKeyNotFoundException(null); 
                
                ApiKey apiKey = keys.get(0);
                User user = apiKey.getUser();
                
                if (!apiKey.getActive()) throw new AppException.ApiKeyStatusNotValidException();
                
                // Vérification des crédits
                if (!apiKey.getUnlimited() && apiKey.getCredit() <= 0) {
                    throw new AppException.CreditsNotValidException();
                }

                // VERIFICATION DU CACHE
                if (useCache && apiKey.getCache() != null) {
                    List<File> cachedFiles = db.createQuery(
                            "SELECT f FROM File f WHERE f.cache.id = :cacheId AND f.sourceText = :text", File.class)
                            .setParameter("cacheId", apiKey.getCache().getId())
                            .setParameter("text", text)
                            .getResultList();
                    
                    if (!cachedFiles.isEmpty()) {
                        db.getTransaction().commit(); 
                        return cachedFiles.get(0).getFilePath();
                    }
                }

                // SYNTHESE VOCALE
                String fileName = "audio_" + UUID.randomUUID().toString() + "." + format;
                String finalPath = "cache_audio/" + fileName;
                
                vertx.fileSystem().mkdirsBlocking("cache_audio");

                try {
                    SynthesizerModeDesc desc = new SynthesizerModeDesc(Locale.of(language));
                    Synthesizer synthesizer = Central.createSynthesizer(desc);
                    
                    synthesizer.allocate();
                    synthesizer.resume();

                    // Ligne à adapter selon acapela-jsapi-extensions.jar
                    // synthesizer.getAudioManager().setMediaLocator(new com.acapela.audio.WavDestination(finalPath));

                    synthesizer.speakPlainText(text, null);
                    synthesizer.waitEngineState(Synthesizer.QUEUE_EMPTY);
                    synthesizer.deallocate();

                } catch (Exception e) {
                    throw new AppException.InternalServerException();
                }

                // MISE A JOUR DE LA BASE DE DONNEES
                
                // Enregistrement dans le Cache
                if (useCache && apiKey.getCache() != null) {
                    File newFile = new File();
                    newFile.setSourceText(text);
                    newFile.setFilePath(finalPath);
                    newFile.setHashKey(UUID.randomUUID().toString());
                    newFile.setSize(1024); 
                    newFile.setCache(apiKey.getCache());
                    db.persist(newFile);
                }

                // On débite un crédit uniquement si la clé n'est pas illimitée
                if (!apiKey.getUnlimited()) {
                    apiKey.setCredits(apiKey.getCredit() - 1);
                    db.merge(apiKey);
                }

                // Historique de l'opération
                SynthesisRequest request = new SynthesisRequest();
                request.setSourceText(text);
                request.setLanguage(language);
                request.setVoice(voice);
                request.setCached(false); 
                request.setUser(user);
                request.setStatus(Status.DONE);
                request.setProcessedAt(new Date());
                db.persist(request);

                db.getTransaction().commit(); 
                
                return finalPath; 

            } catch (Exception e) {
                if (db.getTransaction().isActive()) db.getTransaction().rollback();
                throw e; 
            }
        });
    }
}