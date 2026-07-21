package fr.imta.acapela_webservice.model;

import java.util.Date;

import io.vertx.core.json.JsonObject;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) 
@DiscriminatorColumn(name = "type")
public class Request {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    private String description;

    private RequestType type;

    @ManyToOne
    private User user;

    @ManyToOne
    private ApiKey apiKey;

    private Date requestedAt;

    private Date processedAt;

    //GET AND SET
    public Long getId(){
        return id;
    }

    public void setStatus(Status status){
        this.status = status;
    }

    public Status getStatus(){
        return status;
    }

    public void setDescription(String desc){
        this.description =desc;
    }

    public String getDescription(){
        return description;
    }

    public void setType(RequestType type){
        this.type = type;
    }

    public RequestType getType (){
        return type;
    }

    public void setUser(User user){
        this.user = user;
    }

    public User getUser(){
        return user;
    }

    @PrePersist
    public void generateRequestedAt() {
        this.requestedAt= new Date();
    }

    public Date getRequestedAt(){
        return requestedAt;
    }

    public Date getProcessedAt(){
        return processedAt;
    }

    public void setProcessedAt(Date processedAt){
        this.processedAt = processedAt;
    }

    // JSON

    public JsonObject toJSON(){
        JsonObject result = new JsonObject();
        result.put("id", id);
        result.put("status", status);
        result.put("description", description);
        result.put("requested_at", requestedAt);
        result.put("processed_at", processedAt);
        result.put("user_id", user.getId());
        return result;
    }
}
