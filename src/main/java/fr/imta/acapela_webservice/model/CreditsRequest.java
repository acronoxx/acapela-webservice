package fr.imta.acapela_webservice.model;

import io.vertx.core.json.JsonObject;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

/** Request from a user to get more credits ({@code requestedAmount}) on one of their API keys. */
@Entity
@DiscriminatorValue("CREDITS")
public class CreditsRequest extends Request {
    
    @Column(nullable = false)
    private String token;

    @ManyToOne
    private ApiKey apiKey;

    @Column(nullable = false)
    private int requestedAmount;

    //GET AND SET

    public void setToken(String token){
        this.token = token;
    }

    public String getToken(){
        return token;
    }
    
    public void setApiKey(ApiKey apiKey){
        this.apiKey = apiKey;
    }

    public ApiKey getApiKey(){
        return apiKey;
    }

    public void setRequestedAmount(int number){
        this.requestedAmount = number;
    }

    public int getRequestedAmount(){
        return requestedAmount;
    }

    //toJSON

    public JsonObject toJSON(){
        JsonObject result = super.toJSON();
        result.put("apiKey_Id", apiKey.getId());
        result.put("apiKey_name", apiKey.getName());
        result.put("requested_amount", requestedAmount);
        return result;
    }
}
