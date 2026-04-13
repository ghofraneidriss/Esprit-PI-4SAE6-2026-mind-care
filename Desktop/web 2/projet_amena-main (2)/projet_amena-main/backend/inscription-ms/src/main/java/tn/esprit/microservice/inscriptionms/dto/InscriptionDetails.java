package tn.esprit.microservice.inscriptionms.dto;

import tn.esprit.microservice.inscriptionms.entity.Inscription;

public record InscriptionDetails(Inscription inscription, CoursSummary cours) {
}
