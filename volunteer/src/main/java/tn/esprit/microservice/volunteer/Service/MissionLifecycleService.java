package tn.esprit.microservice.volunteer.Service;

import org.springframework.stereotype.Service;

@Service
public class MissionLifecycleService {

    public void validateTransition(String current, String next) {

        if (current.equals("OPEN") && !next.equals("ASSIGNED")) {
            throw new IllegalStateException("OPEN → only ASSIGNED allowed");
        }

        if (current.equals("ASSIGNED") &&
                !(next.equals("IN_PROGRESS") || next.equals("CANCELLED"))) {
            throw new IllegalStateException("ASSIGNED → IN_PROGRESS or CANCELLED only");
        }

        if (current.equals("IN_PROGRESS") && !next.equals("COMPLETED")) {
            throw new IllegalStateException("IN_PROGRESS → COMPLETED only");
        }
    }
}