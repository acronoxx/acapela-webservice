package fr.imta.acapela_webservice.model;

import java.util.Date;
import java.util.UUID;

import io.vertx.core.json.JsonObject;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;

@Entity
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private int credits;

    private String name;

    @Column(nullable = false)
    private boolean active = false;

    @Column(nullable = false)
    private boolean unlimited = false;

    private Date expirationDate;

    @ManyToOne
    private User user;

    private Status status;

    @OneToOne(mappedBy = "apiKey")
    private Cache cache;

    //GET and SET fonction

    public Long getId(){
        return id;
    }

    public String getToken(){
        return token;
    }

    public int getCredit(){
        return credits;
    }

    public String getName(){
        return name;
    }

    public boolean getActive(){
        return active;
    }

    public boolean getUnlimited(){
        return unlimited;
    }

    public User getUser(){
        return user;
    }

    public Status getStatus(){
        return status;
    }

    public Date getExpDate(){
        return expirationDate;
    }

    public Cache getCache() {
        return cache;
    }

    public void setName(String name){
        this.name =name;
    }

    public void setActive(boolean active){
        this.active = active;
    }

    public void setUnlimited(boolean unlimited){
        this.unlimited = unlimited;
    }

    public void setDate(Date experDate){
        this.expirationDate = experDate;
    }

    public void setUser(User user){
        this.user=user;
    }

    public void setCredits(int credits){
        this.credits=credits;
    }

    public void setStatus(Status status){
        this.status=status;
    }

    @PrePersist
    public void generateToken() {
        // Génère le token uniquement s'il n'existe pas déjà
        if (this.token == null) {
            this.token = UUID.randomUUID().toString();
        }
    }

    public JsonObject toJSON(){
        JsonObject result = new JsonObject();
        result.put("id", id);
        result.put("name", name);
        result.put("token", token);
        result.put("credit", credits);
        result.put("active", active);
        result.put("status", status);
        result.put("expiration_Date", expirationDate);
        result.put("user_id", user.getId());
        result.put("user_first_name", user.getFirstName());
        result.put("user_last_name", user.getLastName());
        return result;
    }

    
}
