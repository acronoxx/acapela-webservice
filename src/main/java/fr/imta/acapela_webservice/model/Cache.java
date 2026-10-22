package fr.imta.acapela_webservice.model;

import java.util.ArrayList;
import java.util.List;
import io.vertx.core.json.JsonObject; // Import pour Vert.x

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToOne;

/**
 * Audio cache of an {@link ApiKey}: the {@link File}s already synthesized for that key,
 * so that a text sent again is not synthesized twice.
 * {@code size} and {@code maxSize} track how much storage the cache uses and is allowed.
 */
@Entity
public class Cache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String cachePath;

    @Column(nullable = false)
    private int size;

    @Column(nullable = false)
    private int maxSize;

    @OneToOne
    private ApiKey apiKey;

    @OneToMany(mappedBy = "cache")
    private List<File> files = new ArrayList<>();

    // Getters & Setters

    public Long getId(){
        return id;
    }

    public String getCachePath(){
        return cachePath;
    }

    public int getSize(){
        return size;
    }

    public int getMaxSize(){
        return maxSize;
    }

    public ApiKey getApiKey(){
        return apiKey;
    }

    public List<File> getFiles(){
        return files;
    }

    public void setCachePath(String cachePath){
        this.cachePath = cachePath;
    }

    public void setSize(int size){
        this.size = size;
    }

    public void setMaxSize(int maxSize){
        this.maxSize = maxSize;
    }

    public void setApiKey(ApiKey apiKey){
        this.apiKey = apiKey;
    }

    public void setFiles(List<File> files){
        this.files = files;
    }

    public void createCache(ApiKey apiKey, String path,int max_size){
        setApiKey(apiKey);
        setCachePath(path);
        setSize(0);
        setMaxSize(max_size);
        setFiles(new ArrayList<>());
    }

    public JsonObject toJSON(){
        JsonObject result = new JsonObject();
        result.put("id", id);
        result.put("apiKey_id", apiKey.getId());
        result.put("path", cachePath);
        result.put("size", size);
        result.put("max_size", maxSize);
        for (File f : files){
            result.put("id_file", f.getId());
            result.put("file_source_text", f.getSourceText());
            result.put("size_file", f.getSize());
            result.put("file_path", f.getFilePath());
        }
        return result;
    }
}
