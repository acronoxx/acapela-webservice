package fr.imta.acapela_webservice.model;

import jakarta.persistence.*;

/**
 * Cette classe représente une demande de synthèse vocale stockée en base de données.
 */
@Entity
@DiscriminatorValue("SYNTHESIS") // JPA utilise ceci pour différencier les types de requêtes dans la table
public class SynthesisRequest extends Request {

    private String sourceText;
    private String language;
    private String voice;
    private boolean cached;

    // Constructeur vide nécessaire pour JPA
    public SynthesisRequest() {
        super();
    }

    // --- Getters et Setters ---

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    /**
     * Méthode utilitaire pour convertir l'objet en JSON pour l'API
     */
    public io.vertx.core.json.JsonObject toJSON() {
        return new io.vertx.core.json.JsonObject()
            .put("id", this.getId())
            .put("source_text", this.sourceText)
            .put("language", this.language)
            .put("voice", this.voice)
            .put("cached", this.cached)
            .put("status", this.getStatus().toString())
            .put("requested_at", this.getRequestedAt() != null ? this.getRequestedAt().toString() : null)
            .put("processed_at", this.getProcessedAt() != null ? this.getProcessedAt().toString() : null);
    }
}