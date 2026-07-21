package fr.imta.acapela_webservice.model;

import java.util.Date;

import io.vertx.core.json.JsonObject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;

@Entity
public class File {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String hashKey;

    @Column(nullable = false)
    private String sourceText;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private int size;

    private Date lastAccessedAt;

    @ManyToOne
    private Cache cache;

    //GET AND SET
    public Long getId(){
        return id;
    }
    
    public String getSourceText(){
        return sourceText;
    }

    public String getFilePath(){
        return filePath;
    }

    public int getSize(){
        return size;
    }

    public Date getLastAccessedAt() {
        return lastAccessedAt;
    }

    public Cache getCache() {
            return cache;
    }

    public String getHashKey() {
            return hashKey;
    }

    public void setSourceText(String sourceText){
        this.sourceText = sourceText;
    }

    public void setFilePath(String filePath){
        this.filePath = filePath;
    }

    public void setSize(int size){
        this.size = size;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }

    public void setLastAccessedAt(Date lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public JsonObject toJSON(){
        JsonObject result = new JsonObject();
        result.put("id_file", id);
        result.put("file_source_text", sourceText);
        result.put("size_file", size);
        result.put("file_path",filePath);
        return result;
    }
}
