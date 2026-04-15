package tn.esprit.ordonnance_et_medicaments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.ordonnance_et_medicaments.entities.Prescription;
import tn.esprit.ordonnance_et_medicaments.entities.PrescriptionLine;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Service d'envoi d'email lors de la signature d'une ordonnance.
 *
 * Inspiré du pattern de AppointmentService (traitement_et_consultation) :
 *  1. Appel REST vers traitement_et_consultation pour récupérer le profil du patient (et son email)
 *  2. Génération d'un email HTML dans le même style visuel que les emails de rendez-vous
 *  3. Envoi via EmailService (JavaMailSender / Gmail SMTP)
 *
 * L'email liste tous les médicaments prescrits avec posologie et dates de traitement.
 */
@Service
@RequiredArgsConstructor
public class PrescriptionMailService {

    /** Service d'envoi SMTP — même bean que dans traitement_et_consultation */
    private final EmailService emailService;

    /** RestTemplate pour appeler traitement_et_consultation et récupérer le profil du patient */
    private final RestTemplate restTemplate;

    /** URL de base de traitement_et_consultation, configurée dans application.properties */
    @Value("${traitement.service.url:http://localhost:8081}")
    private String traitementServiceUrl;

    /**
     * Envoie un email de notification au patient après la signature de sa prescription.
     *
     * Récupère le profil patient via traitement_et_consultation (même source de données que les RDV).
     * L'email est envoyé uniquement si le patient possède une adresse email enregistrée.
     * En cas d'échec, l'erreur est loguée sans bloquer la réponse API du médecin.
     *
     * @param prescription La prescription venant d'être signée (statut SIGNED)
     */
    public void sendSignedPrescriptionEmail(Prescription prescription) {
        if (prescription.getPatientId() == null) return;

        // Récupération du profil patient depuis traitement_et_consultation
        // Same pattern as AppointmentService.sendEmailNotification()
        String patientEmail = fetchPatientEmail(prescription.getPatientId());
        if (patientEmail == null || patientEmail.isBlank()) {
            System.out.println("[PrescriptionMail] No email found for patient #" + prescription.getPatientId());
            return;
        }

        String subject = "Your Prescription Has Been Signed - Mind Care";
        String htmlBody = generateHtmlEmail(prescription);

        try {
            emailService.sendHtmlEmail(patientEmail, subject, htmlBody);
        } catch (Exception e) {
            // Ne pas propager l'exception — la signature a réussi, l'email est secondaire
            System.err.println("[PrescriptionMail] Error sending email: " + e.getMessage());
        }
    }

    /**
     * Appelle GET /api/patient-profiles/by-user/{patientId} sur traitement_et_consultation
     * pour récupérer l'adresse email du patient depuis son profil.
     * Même source de données que AppointmentService.
     *
     * @param patientId ID du patient
     * @return Email du patient, ou null si non trouvé
     */
    @SuppressWarnings("unchecked")
    private String fetchPatientEmail(Long patientId) {
        try {
            // Appel vers GET /api/profiles/user/{patientId} sur traitement_et_consultation
            // Même source de données que AppointmentService (le profil contient un champ "email")
            String url = traitementServiceUrl + "/api/profiles/user/" + patientId;
            Map<String, Object> profile = restTemplate.getForObject(url, Map.class);
            if (profile != null && profile.get("email") != null) {
                return profile.get("email").toString();
            }
        } catch (Exception e) {
            System.err.println("[PrescriptionMail] Could not fetch patient profile: " + e.getMessage());
        }
        return null;
    }

    /**
     * Génère l'email HTML de notification dans le même style visuel que les emails de rendez-vous.
     * Dark theme + gradient vert #2D9A9B — Mind Care branding.
     * Liste tous les médicaments prescrits avec posologie et dates.
     *
     * @param prescription La prescription signée
     * @return Chaîne HTML complète
     */
    private String generateHtmlEmail(Prescription prescription) {
        // Couleurs identiques aux emails de rendez-vous
        String primaryGreen = "#2D9A9B";
        String darkBg = "#121212";
        String cardBg = "#222222";

        // Formatage de la date de création de la prescription
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.ENGLISH);
        String issuedDate = prescription.getCreatedAt() != null
                ? prescription.getCreatedAt().format(dateFmt)
                : "N/A";

        String prescriptionRef = "#PR-" + prescription.getId();

        // Construction des lignes de médicaments en HTML — une ligne par médicament prescrit
        StringBuilder medicationRows = new StringBuilder();
        if (prescription.getPrescriptionLines() != null && !prescription.getPrescriptionLines().isEmpty()) {
            DateTimeFormatter lineDateFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
            for (PrescriptionLine line : prescription.getPrescriptionLines()) {
                String medicineName = line.getMedicine() != null ? line.getMedicine().getCommercialName() : "—";
                String medicineInn  = line.getMedicine() != null ? line.getMedicine().getInn() : "";
                String dosage       = line.getDosage() != null ? line.getDosage() : "—";
                String startDate    = line.getStartDate() != null ? line.getStartDate().format(lineDateFmt) : "—";
                String endDate      = line.getEndDate()   != null ? line.getEndDate().format(lineDateFmt)   : "—";

                medicationRows.append(
                    "<tr>" +
                    "  <td style=\"padding:12px 16px; border-bottom:1px solid #2a2a2a;\">" +
                    "    <div style=\"color:#E2E8F0; font-weight:700; font-size:14px;\">" + medicineName + "</div>" +
                    "    <div style=\"color:#718096; font-size:12px; margin-top:2px;\">" + medicineInn + "</div>" +
                    "  </td>" +
                    "  <td style=\"padding:12px 16px; border-bottom:1px solid #2a2a2a; color:#4FD1C5; font-weight:700; font-size:14px;\">" + dosage + "</td>" +
                    "  <td style=\"padding:12px 16px; border-bottom:1px solid #2a2a2a; color:#A0AEC0; font-size:13px;\">" + startDate + " → " + endDate + "</td>" +
                    "</tr>"
                );
            }
        } else {
            medicationRows.append(
                "<tr><td colspan=\"3\" style=\"padding:16px; color:#718096; text-align:center;\">No medications listed.</td></tr>"
            );
        }

        // Template HTML — même structure visuelle que generateHtmlEmail() dans AppointmentService
        return "<div style=\"font-family:'Instrument Sans','Inter','Segoe UI',Arial,sans-serif; max-width:600px; margin:auto; background-color:" + darkBg + "; color:#FFFFFF; border-radius:16px; overflow:hidden; box-shadow:0 20px 50px rgba(0,0,0,0.5);\">" +

            // Header — identique aux emails de rendez-vous
            "<div style=\"background:linear-gradient(135deg,#2D9A9B 0%,#1B5E5F 100%); padding:50px 30px; text-align:center;\">" +
            "  <h1 style=\"margin:0; font-size:32px; font-weight:800; letter-spacing:-0.5px; color:#FFFFFF;\">Mind Care</h1>" +
            "  <p style=\"margin:8px 0 0; opacity:0.8; font-size:14px; font-weight:500; letter-spacing:0.5px; text-transform:uppercase;\">Your Mental Health Companion</p>" +
            "</div>" +

            // Content
            "<div style=\"padding:40px 30px; background-color:#1A1A1A;\">" +
            "  <h2 style=\"color:#FFFFFF; margin-top:0; font-size:24px; font-weight:700; text-align:center;\">Your Prescription Is Ready 💊</h2>" +
            "  <p style=\"color:#A0AEC0; line-height:1.6; font-size:16px; text-align:center; margin-bottom:30px;\">" +
            "    Your doctor has signed prescription <strong style=\"color:#4FD1C5;\">" + prescriptionRef + "</strong> on <strong style=\"color:#E2E8F0;\">" + issuedDate + "</strong>.<br>" +
            "    Please purchase the medications listed below and take them strictly as prescribed." +
            "  </p>" +

            // Alert box
            "  <div style=\"background-color:#1A302E; border:1px solid #2D9A9B; padding:20px; border-radius:12px; margin-bottom:30px;\">" +
            "    <div style=\"color:#4FD1C5; font-size:13px; font-weight:700; text-transform:uppercase; margin-bottom:8px; letter-spacing:1px;\">⚠ IMPORTANT REMINDER</div>" +
            "    <p style=\"color:#A0AEC0; font-size:14px; margin:0; line-height:1.6;\">Do not stop or modify your medication without consulting your doctor. Follow the prescribed dosage and treatment period carefully.</p>" +
            "  </div>" +

            // Medications table — dark card style comme AppointmentService
            "  <div style=\"color:#718096; font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:1px; margin-bottom:12px;\">PRESCRIBED MEDICATIONS</div>" +
            "  <div style=\"background-color:" + cardBg + "; border-radius:12px; overflow:hidden; margin-bottom:30px;\">" +
            "    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:14px;\">" +
            "      <thead>" +
            "        <tr style=\"border-bottom:1px solid #2a2a2a;\">" +
            "          <th style=\"padding:12px 16px; text-align:left; color:#718096; font-size:11px; text-transform:uppercase; letter-spacing:1px;\">Medication</th>" +
            "          <th style=\"padding:12px 16px; text-align:left; color:#718096; font-size:11px; text-transform:uppercase; letter-spacing:1px;\">Dosage</th>" +
            "          <th style=\"padding:12px 16px; text-align:left; color:#718096; font-size:11px; text-transform:uppercase; letter-spacing:1px;\">Period</th>" +
            "        </tr>" +
            "      </thead>" +
            "      <tbody>" + medicationRows + "</tbody>" +
            "    </table>" +
            "  </div>" +

            // Dashboard button — identique aux emails de rendez-vous
            "  <div style=\"text-align:center; margin-top:40px;\">" +
            "    <a href=\"#\" style=\"background-color:" + primaryGreen + "; color:#FFFFFF; padding:16px 45px; text-decoration:none; border-radius:12px; font-weight:700; display:inline-block;\">Go to Dashboard</a>" +
            "  </div>" +

            "  <p style=\"color:#718096; font-size:13px; text-align:center; margin-top:40px; line-height:1.5;\">If you have any questions about your treatment, please contact your physician through the application.</p>" +
            "</div>" +

            // Footer — identique aux emails de rendez-vous
            "<div style=\"padding:30px; background-color:" + darkBg + "; text-align:center; border-top:1px solid #1A1A1A;\">" +
            "  <p style=\"margin:0; color:#4A5568; font-size:12px;\">© 2026 Mind Care. All rights reserved.</p>" +
            "  <p style=\"margin:5px 0 0; color:#4A5568; font-size:12px;\">Helping you care for your mind, every day.</p>" +
            "</div>" +

            "</div>";
    }
}
