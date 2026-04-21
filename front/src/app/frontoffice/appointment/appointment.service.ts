import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Enumération des types de rendez-vous possibles.
 * ONLINE  : consultation à distance (téléconsultation)
 * IN_PERSON : consultation physique en cabinet
 */
export enum AppointmentType {
    ONLINE = 'ONLINE',
    IN_PERSON = 'IN_PERSON'
}

/**
 * Enumération des catégories de rendez-vous.
 * DAILY_FOLLOW_UP  : suivi quotidien d'un patient existant
 * NEW_CONSULTATION : première consultation avec un nouveau patient
 */
export enum AppointmentCategory {
    DAILY_FOLLOW_UP = 'DAILY_FOLLOW_UP',
    NEW_CONSULTATION = 'NEW_CONSULTATION'
}

/**
 * Enumération des statuts possibles d'un rendez-vous.
 * PENDING     : en attente de confirmation par le médecin
 * CONFIRMED   : confirmé (synchronisé avec Google Calendar)
 * RESCHEDULED : reprogrammé à une autre date/heure
 * CANCELLED   : annulé par le patient ou le médecin
 */
export enum AppointmentStatus {
    PENDING = 'PENDING',
    CONFIRMED = 'CONFIRMED',
    RESCHEDULED = 'RESCHEDULED',
    CANCELLED = 'CANCELLED'
}

/**
 * Interface représentant un rendez-vous médical.
 * Correspond à l'entité Appointment côté back-end Spring Boot.
 */
export interface Appointment {
    id?: number;                    // Identifiant unique (absent lors de la création)
    patientId: number;              // Identifiant du patient concerné
    doctorId: number;               // Identifiant du médecin consulté
    appointmentDate: string;        // Date et heure au format ISO (YYYY-MM-DDTHH:mm)
    isUrgent: boolean;              // Indique si le rendez-vous est urgent
    type: AppointmentType;          // Type de consultation (en ligne ou présentiel)
    category: AppointmentCategory;  // Catégorie du rendez-vous
    status: AppointmentStatus;      // Statut actuel du rendez-vous
    googleEventId?: string;         // Identifiant de l'événement Google Calendar (optionnel)
    priorityScore?: number;         // Score d'urgence calculé basé sur les maladies et le MMSE
    title?: string;                 // Titre personnalisé (utile pour l'affichage d'évènements externes Google)
    meetLink?: string;              // Google Meet link for online appointments
}

/**
 * Service Angular responsable de toutes les communications HTTP
 * avec l'API REST de gestion des rendez-vous (back-end Spring Boot).
 * URL de base : http://localhost:8081/api/appointments
 */
@Injectable({
    providedIn: 'root'
})
export class AppointmentService {

    /** URL de base de l'API back-end pour les rendez-vous */
    private apiUrl = 'http://localhost:8081/api/appointments';

    constructor(private http: HttpClient) { }

    /**
     * Crée un nouveau rendez-vous via l'API.
     * Le statut initial sera automatiquement défini à PENDING côté back-end.
     *
     * @param appointment données du rendez-vous à créer
     * @return Observable contenant le rendez-vous créé avec son identifiant
     */
    createAppointment(appointment: Appointment): Observable<Appointment> {
        return this.http.post<Appointment>(this.apiUrl, appointment);
    }

    /**
     * Récupère la liste complète de tous les rendez-vous enregistrés.
     *
     * @return Observable contenant la liste des rendez-vous
     */
    getAppointments(): Observable<Appointment[]> {
        return this.http.get<Appointment[]>(this.apiUrl);
    }

    /**
     * COMMENTAIRE POUR LE REPERAGE (Demande utilisateur) :
     * Fonction pour interroger le backend Spring Boot afin d'obtenir
     * la liste des rendez-vous filtrée côté serveur !
     */
    getFilteredAppointments(doctorId?: number, patientId?: number, status?: string, isUrgent?: boolean, date?: string, minScore?: number, maxScore?: number, sortByScore: boolean = false): Observable<Appointment[]> {
        let params = new URLSearchParams();
        if (doctorId) params.set('doctorId', doctorId.toString());
        if (patientId) params.set('patientId', patientId.toString());
        if (status && status !== 'all') params.set('status', status);
        if (isUrgent !== undefined && isUrgent !== null) params.set('isUrgent', isUrgent.toString());
        if (date) params.set('date', date);
        if (minScore !== undefined && minScore !== null) params.set('minScore', minScore.toString());
        if (maxScore !== undefined && maxScore !== null) params.set('maxScore', maxScore.toString());
        params.set('sortByScore', sortByScore.toString());

        return this.http.get<Appointment[]>(`${this.apiUrl}/filter?${params.toString()}`);
    }

    getFilterDates(doctorId?: number): Observable<string[]> {
        let params = new URLSearchParams();
        if (doctorId) params.set('doctorId', doctorId.toString());
        return this.http.get<string[]>(`${this.apiUrl}/filter-options/dates?${params.toString()}`);
    }

    getFilterPatients(doctorId?: number): Observable<number[]> {
        let params = new URLSearchParams();
        if (doctorId) params.set('doctorId', doctorId.toString());
        return this.http.get<number[]>(`${this.apiUrl}/filter-options/patients?${params.toString()}`);
    }

    /**
     * Récupère un rendez-vous spécifique par son identifiant.
     *
     * @param id identifiant du rendez-vous
     * @return Observable contenant le rendez-vous correspondant
     */
    getAppointmentById(id: number): Observable<Appointment> {
        return this.http.get<Appointment>(`${this.apiUrl}/${id}`);
    }

    /**
     * Récupère tous les rendez-vous d'un médecin donné.
     * Utilisé pour afficher l'agenda du médecin et calculer les créneaux disponibles.
     *
     * @param doctorId identifiant du médecin
     * @return Observable contenant la liste des rendez-vous du médecin
     */
    getAppointmentsByDoctor(doctorId: number): Observable<Appointment[]> {
        return this.http.get<Appointment[]>(`${this.apiUrl}/doctor/${doctorId}`);
    }

    /**
     * Met à jour un rendez-vous existant.
     *
     * @param id          identifiant du rendez-vous à modifier
     * @param appointment nouvelles données du rendez-vous
     * @return Observable contenant le rendez-vous mis à jour
     */
    updateAppointment(id: number, appointment: Appointment): Observable<Appointment> {
        return this.http.put<Appointment>(`${this.apiUrl}/${id}`, appointment);
    }

    /**
     * Supprime un rendez-vous par son identifiant.
     *
     * @param id identifiant du rendez-vous à supprimer
     * @return Observable void (aucune donnée retournée)
     */
    deleteAppointment(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    /**
     * Envoie un e-mail d'alerte à une adresse destinataire.
     * Utilisé pour notifier le patient ou le médecin d'un événement important.
     *
     * @param email   adresse e-mail du destinataire
     * @param subject sujet de l'e-mail
     * @param message contenu du message
     * @return Observable contenant la réponse textuelle de l'API
     */
    sendAlertEmail(email: string, subject: string, message: string): Observable<string> {
        return this.http.post(`${this.apiUrl}/alert`, { email, subject, message }, { responseType: 'text' });
    }

    /**
     * Confirme un rendez-vous en attente.
     * Déclenche côté back-end la création de l'événement dans Google Calendar
     * et le passage du statut à CONFIRMED.
     *
     * @param id identifiant du rendez-vous à confirmer
     * @return Observable contenant le rendez-vous avec son statut et googleEventId mis à jour
     */
    confirmAppointment(id: number): Observable<Appointment> {
        return this.http.post<Appointment>(`${this.apiUrl}/${id}/confirm`, {});
    }

    /**
     * Cancels an appointment by setting its status to CANCELLED.
     * Can be called by both patient and doctor.
     *
     * @param id appointment ID to cancel
     * @return Observable containing the updated appointment
     */
    cancelAppointment(id: number): Observable<Appointment> {
        return this.http.post<Appointment>(`${this.apiUrl}/${id}/cancel`, {});
    }

    /**
     * Retrieves all appointments for a given patient.
     * Used to display the patient's booking history.
     *
     * @param patientId patient user ID
     * @return Observable containing the list of appointments
     */
    getAppointmentsByPatient(patientId: number): Observable<Appointment[]> {
        return this.http.get<Appointment[]>(`${this.apiUrl}/patient/${patientId}`);
    }

    /**
     * Demande au back-end de calculer et retourner le meilleur créneau disponible
     * sur une plage de dates, en tenant compte des agendas du médecin et du patient.
     *
     * L'algorithme côté serveur applique les règles suivantes :
     *   - Plage de travail : 09h00 – 16h00
     *   - Pause déjeuner exclue : 13h00 – 14h00
     *   - Durée de consultation : 35 min (plage acceptable : 30-40 min)
     *   - Pause obligatoire entre deux patients : 10 min
     *   - Vérification croisée : agenda médecin + rendez-vous existants du patient
     *
     * @param doctorId  identifiant du médecin ciblé
     * @param patientId identifiant du patient (pour croiser avec son agenda)
     * @param startDate date de début de la période de recherche (format YYYY-MM-DD)
     * @param endDate   date de fin de la période de recherche (format YYYY-MM-DD)
     * @return Observable contenant la date/heure du meilleur créneau (format ISO LocalDateTime)
     */
    suggestBestSlot(doctorId: number, patientId: number, startDate: string, endDate: string): Observable<string> {
        return this.http.get<string>(`${this.apiUrl}/doctor/${doctorId}/suggest-slot`, {
            params: { patientId: patientId.toString(), startDate, endDate }
        });
    }
}
