package fr.imta.acapela_webservice.model;
import java.util.ArrayList;
import java.util.List;

import io.vertx.core.json.JsonObject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private Role role = Role.CLIENT;

    @Column(nullable = false)
    private boolean cachedByDefault = true;

    @OneToMany(mappedBy = "user")
    private List<ApiKey> apiKeys = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Request> requests= new ArrayList<>();
    
    //FONCTIONS SET AND GET POUR CHAQUE ATTRIBUT
    public Long getId(){
        return this.id;
    }

    public void setFirstName(String firstName){
        this.firstName = firstName;
    }

    public String getFirstName(){
        return this.firstName;
    }

    public void setLastName(String lastName){
        this.lastName = lastName;
    }

    public String getLastName(){
        return this.lastName;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public String getEmail(){
        return this.email;
    }

    public void setPasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }

    public String getPasswordHash(){
        return this.passwordHash;
    }

    public void setRole(Role role){
        this.role = role;
    }

    public Role getRole(){
        return this.role;
    }

    public void setCachedByDefault(Boolean cached){
        this.cachedByDefault = cached;
    }

    public Boolean getCachedByDefault(){
        return this.cachedByDefault;
    }

    public void setApiKeys(List<ApiKey> keys){
        this.apiKeys = keys;
    }

    public List<ApiKey> getApiKeys(){
        return this.apiKeys;
    }

    public void setRequest(List<Request> requests){
        this.requests = requests;
    }

    public List<Request> getRequests(){
        return this.requests;
    }
    // AUTRES FONCTIONS 

    public JsonObject toJSON() {
        JsonObject result = new JsonObject();
        result.put("id", id);
        result.put("first_name", firstName);
        result.put("last_name", lastName);
        result.put("email", email);
        result.put("role", role);
        result.put("cached_by_default", cachedByDefault);
        result.put("api_keys", apiKeys);
        result.put("requests", requests);
        return result;
    }
}
