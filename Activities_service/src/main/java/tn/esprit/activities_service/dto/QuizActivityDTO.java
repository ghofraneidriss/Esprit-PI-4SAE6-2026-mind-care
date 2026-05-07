package tn.esprit.activities_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizActivityDTO {
    
    private Long id;
    private String title;
    private String theme;
    private String description;
    
    @JsonProperty("difficulty")
    private String difficulty;
    
    @JsonProperty("level")
    public String getLevel() {
        return difficulty; // Map difficulty to level for frontend compatibility
    }
    
    @JsonProperty("level")
    public void setLevel(String level) {
        this.difficulty = level; // Map level back to difficulty
    }
    
    @JsonProperty("created_at")
    private Date createdAt;
    
    @JsonProperty("updated_at")
    private Date updatedAt;
}
