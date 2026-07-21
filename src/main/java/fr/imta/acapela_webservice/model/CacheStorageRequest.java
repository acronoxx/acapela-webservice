package fr.imta.acapela_webservice.model;

import io.vertx.core.json.JsonObject;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
@DiscriminatorValue("CACHESTORAGE")
public class CacheStorageRequest extends Request {
    
    @Column(nullable = false)
    private int requestedSize;
    
    @OneToOne
    private ApiKey apiKey;

    //SET AND GET
    public void setRequestedSize(int requestsize){
        this.requestedSize = requestsize;
    }

    public int getRequestedSize(){
        return requestedSize;
    }

    public void setApiKey(ApiKey apiKey){
        this.apiKey = apiKey;
    }

    public ApiKey getApiKey(){
        return apiKey;
    }

    public JsonObject toJSON(){
        JsonObject result = super.toJSON();
        result.put("apiKey_Id", apiKey.getId());
        result.put("apiKey_name", apiKey.getName());
        result.put("requested_size", requestedSize);
        return result;
    }
    
}
