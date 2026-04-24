package tn.esprit.microservice.volunteer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class VolunteerNotFoundException extends RuntimeException {

    public VolunteerNotFoundException(Long volunteerId) {
        super("Volunteer not found with id: " + volunteerId);
    }
}
