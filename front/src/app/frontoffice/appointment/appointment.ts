import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { AppointmentService, Appointment, AppointmentType, AppointmentCategory, AppointmentStatus } from './appointment.service';
import { AuthService, AuthUser } from '../auth/auth.service';
import { Router } from '@angular/router';

declare var google: any;

/**
 * Composant Angular gérant la page de prise de rendez-vous côté patient.
 * Permet de :
 *  - Afficher un sélecteur de date et des créneaux horaires disponibles
 *  - Vérifier automatiquement la disponibilité du médecin et du patient
 *  - Suggérer intelligemment le meilleur créneau via l'API back-end
 *  - Créer, modifier et supprimer des rendez-vous
 */
@Component({
  selector: 'app-appointment',
  standalone: false,
  templateUrl: './appointment.html',
  styleUrls: ['./appointment.css'],
})
export class AppointmentFrontPage implements OnInit {

  /** Objet rendez-vous en cours de saisie ou de modification */
  appointment: Appointment = {
    patientId: 0,
    doctorId: 1,
    appointmentDate: '',
    isUrgent: false,
    type: AppointmentType.IN_PERSON,
    category: AppointmentCategory.NEW_CONSULTATION,
    status: AppointmentStatus.PENDING
  };

  isLoading = false;      // Indicateur de chargement lors de la sauvegarde
  successMessage = '';    // Message de succès affiché après une action réussie
  errorMessage = '';      // Error message displayed on failure
  minDateTime = '';       // Date/heure minimale autorisée pour la saisie (aujourd'hui)
  userAppointments: Appointment[] = []; // Liste des rendez-vous existants du patient connecté
  filteredUserAppointments: Appointment[] = []; // Liste filtrée des rendez-vous du patient
  isEditMode = false;                   // Indique si le formulaire est en mode édition
  selectedAppointmentId?: number;       // Identifiant du rendez-vous sélectionné pour édition

  /** Date sélectionnée par le patient dans le calendrier (format YYYY-MM-DD) */
  selectedDateStr: string = '';

  /** Liste des rendez-vous du médecin sélectionné, utilisée pour le calcul de disponibilité */
  doctorAppointments: Appointment[] = [];

  // Filter properties for patient's appointments
  statusFilter: string = 'all';

  /**
   * Créneaux horaires disponibles calculés pour la date sélectionnée.
   * Chaque créneau contient : l'heure, un indicateur de désactivation et la raison si indisponible.
   */
  availableSlots: { time: string, disabled: boolean, reason: string }[] = [];

  /**
   * Créneaux fixes calculés selon les règles métier :
   * - Travail : 09h00 à 16h00
   * - Pause déjeuner : 13h00 à 14h00
   * - Durée consultation : 35 min + 10 min de pause = 45 min entre chaque créneau
   */
  fixedSlots = ['09:00', '09:45', '10:30', '11:15', '14:00', '14:45', '15:10'];

  /** Durée d'une consultation en millisecondes (35 min = plage 30-40 min) */
  readonly CONSULT_DURATION_MS = 35 * 60000;

  /** Date minimale autorisée pour le sélecteur de date (aujourd'hui, format YYYY-MM-DD) */
  minDateOnly = '';

  /** Liste des médecins chargés depuis la base de données */
  doctors: any[] = [];

  // ─── PATIENT CALENDAR ──────────────────────────────────────────────
  patCalMode: 'DAY' | 'WEEK' | 'MONTH' | 'AGENDA' = 'MONTH';
  patCalDate: Date = new Date();
  patCalMap: Map<string, Appointment[]> = new Map();

  /**
   * Cache de jours fériés pour l'année courante.
   * Clé : "YYYY-MM-DD", Valeur : { name: string }
   * Détection automatique : si le navigateur est en arabe/turc/persan → fêtes musulmanes
   * sinon → fêtes chrétiennes universelles + fêtes laiques.
   */
  private patHolidayMap: Map<string, { name: string }> = new Map();
  private patHolidayYear = -1;

  /**
   * Calcule les jours fériés fixes pour une année donnée.
   * Inclut les fêtes chrétiennes/laiques ET les fêtes musulmanes approx (fixes pour simplicité).
   * Note : les dates islamiques varient ~11 jours/an ; ce sont des approximations.
   */
  private getHolidayMap(year: number): Map<string, { name: string }> {
    if (this.patHolidayYear === year) return this.patHolidayMap;
    this.patHolidayYear = year;
    const map = new Map<string, { name: string }>();
    const add = (m: number, d: number, name: string) =>
      map.set(`${year}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`, { name });

    // Détection du contexte local (arabe, turc, perse → contexte musulman)
    const lang = navigator.language || 'en';
    const isMuslimContext = /^(ar|fa|ur|tr|ms|id|az|kk|ky|tg|uz|ug)/.test(lang);

    if (isMuslimContext) {
      // Fêtes musulmanes (dates approximatives fixées) — variables selon l'année hijri
      // Pour 2026 (approx)
      if (year === 2026) {
        add(1, 1, '\uD83C\uDF19 New Year'); add(3, 20, '\uD83C\uDF19 Spring Equinox');
        add(3, 29, '\uD83C\uDF19 Eid al-Fitr'); add(3, 30, '\uD83C\uDF19 Eid al-Fitr');
        add(3, 31, '\uD83C\uDF19 Eid al-Fitr (Holiday)');
        add(6, 6, '\uD83C\uDF19 Eid al-Adha'); add(6, 7, '\uD83C\uDF19 Eid al-Adha');
        add(6, 8, '\uD83C\uDF19 Eid al-Adha (Holiday)');
        add(6, 27, '\uD83C\uDF19 Islamic New Year (Muharram)');
        add(9, 5, '\uD83C\uDF19 Mawlid al-Nabi');
      } else {
        // Générique : seul le 1er janvier
        add(1, 1, '\uD83C\uDF19 New Year\'s Day');
      }
    } else {
      // Fêtes chrétiennes + laïques universelles
      add(1, 1, '\u2728 New Year\'s Day');
      add(2, 14, '\u2764\uFE0F Valentine\'s Day');
      add(5, 1, '\uD83D\uDCA1 Labour Day');
      add(12, 25, '\uD83C\uDF84 Christmas Day');
      add(12, 26, '\uD83C\uDF84 Boxing Day');
      add(12, 31, '\uD83C\uDF86 New Year\'s Eve');

      // Pâques (algorithme de Computus)
      const a = year % 19, b = Math.floor(year / 100), c = year % 100;
      const d2 = Math.floor(b / 4), e = b % 4;
      const f = Math.floor((b + 8) / 25), g = Math.floor((b - f + 1) / 3);
      const h = (19 * a + b - d2 - g + 15) % 30;
      const i = Math.floor(c / 4), k = c % 4;
      const l = (32 + 2 * e + 2 * i - h - k) % 7;
      const m2 = Math.floor((a + 11 * h + 22 * l) / 451);
      const easterMonth = Math.floor((h + l - 7 * m2 + 114) / 31);
      const easterDay = ((h + l - 7 * m2 + 114) % 31) + 1;
      add(easterMonth, easterDay, '\u271D\uFE0F Easter Sunday');
      // Vendredi saint (2 jours avant)
      const gf = new Date(year, easterMonth - 1, easterDay - 2);
      add(gf.getMonth() + 1, gf.getDate(), '\u271D\uFE0F Good Friday');
    }
    this.patHolidayMap = map;
    return map;
  }

  // ─── Google Calendar Integration ─────────────────────────────────────────
  private CLIENT_ID = '176577385927-lsna4hmpuno7ihbi9chuk5poff915dfi.apps.googleusercontent.com';
  private API_KEY = 'AIzaSyBuXyEJXGUNnFUhN3byOi74bTzfKvNpzgQ';
  private DISCOVERY_DOCS = ["https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest"];
  // Mettre à jour avec .events pour pouvoir créer un événement Google quand un Patient réserve
  private SCOPES = "https://www.googleapis.com/auth/calendar.events";

  gapiInited = false;
  isAuthorized = false;
  googleEventsLoaded: Appointment[] = [];

  appointmentTypes = Object.values(AppointmentType);
  appointmentCategories = Object.values(AppointmentCategory);

  constructor(
    private appointmentService: AppointmentService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) { }

  /**
   * Initialisation du composant :
   * - Récupère le patient connecté et charge ses rendez-vous
   * - Charge la liste des médecins depuis la base
   * - Initialise les dates minimales des champs de saisie
   */
  ngOnInit(): void {
    const user = this.authService.getLoggedUser();
    if (user) {
      // Utilisation de l'userId comme identifiant patient
      this.appointment.patientId = user.userId;
      this.loadUserAppointments(user.userId);
    } else {
      // Redirection vers la page de connexion si l'utilisateur n'est pas authentifié
      this.router.navigate(['/auth/login']);
    }

    // Chargement de la liste des médecins depuis la table utilisateurs
    this.authService.getAllUsers().subscribe({
      next: (users) => {
        this.doctors = users.filter(u => this.authService.normalizeRole(u.role) === 'DOCTOR');
        // Ne PAS pré-sélectionner : le patient doit choisir son médecin explicitement
        this.appointment.doctorId = 0 as any;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[Appointment] Error loading doctors:', err);
      }
    });

    // Définition de la date/heure minimale (maintenant) pour les champs de saisie
    const now = new Date();
    this.minDateTime = now.toISOString().slice(0, 16); // Format : YYYY-MM-DDTHH:mm
    this.minDateOnly = now.toISOString().slice(0, 10); // Format : YYYY-MM-DD

    // We use GIS now, so API is instantly available
    this.gapiInited = true;

    // Check if we already have an access token saved
    const savedToken = sessionStorage.getItem('google_access_patient');
    if (savedToken) {
      this.isAuthorized = true;
      this.fetchGoogleEventsWithToken(savedToken);
    } else {
      this.isAuthorized = false;
    }
  }

  /**
   * Appelée lorsque le médecin sélectionné change dans le formulaire.
   * Charge les rendez-vous du nouveau médecin et recalcule les créneaux disponibles.
   */
  onDoctorChange(): void {
    if (this.appointment.doctorId) {
      this.appointmentService.getAppointmentsByDoctor(this.appointment.doctorId).subscribe({
        next: (appts) => {
          this.doctorAppointments = appts;
          this.generateSlots(); // Mise à jour des créneaux après changement de médecin
        },
        error: (err) => {
          console.error('[Appointment] Error loading doctor appointments:', err);
        }
      });
    }
  }

  /**
   * Appelée lorsque la date sélectionnée dans le calendrier change.
   * Recalcule les créneaux disponibles pour la nouvelle date.
   */
  onDateChange(): void {
    this.generateSlots();
  }

  /**
   * Génère la liste des créneaux horaires pour la date sélectionnée.
   * Pour chaque créneau fixe, détermine s'il est disponible ou non en croisant :
   *   1. Si le créneau est déjà passé (pour aujourd'hui uniquement)
   *   2. L'agenda du médecin : chevauchement + respect de la pause de 10 min entre patients
   *   3. L'agenda du patient : chevauchement direct avec ses autres rendez-vous
   *
   * Le résultat est stocké dans `availableSlots` avec une raison lisible si désactivé.
   */
  generateSlots(): void {
    // Pas de créneau à afficher si aucune date n'est sélectionnée
    if (!this.selectedDateStr) {
      this.availableSlots = [];
      return;
    }

    // Filtrage des rendez-vous du médecin pour la date sélectionnée uniquement (ignorer celui en édition)
    const doctorApptsThisDay = this.doctorAppointments.filter(appt =>
      appt.appointmentDate && appt.appointmentDate.startsWith(this.selectedDateStr) && appt.id !== this.selectedAppointmentId
    );

    // Filtrage des rendez-vous existants du patient pour la date sélectionnée (ignorer celui en édition)
    const userApptsThisDay = this.userAppointments.filter(appt =>
      appt.appointmentDate && appt.appointmentDate.startsWith(this.selectedDateStr) && appt.id !== this.selectedAppointmentId
    );

    const now = new Date();
    const todayStr = now.toISOString().slice(0, 10);
    const currentTimeMs = now.getTime();

    // Construction de la liste des créneaux avec leur statut
    this.availableSlots = this.fixedSlots.map(time => {
      const slotTimeStr = `${this.selectedDateStr}T${time}`;
      const slotStart = new Date(slotTimeStr).getTime();
      const slotEnd = slotStart + this.CONSULT_DURATION_MS; // Fin du créneau (durée : 35 min)

      let disabled = false;
      let reason = '';

      // Rétablissement du contrôle des créneaux passés
      if (this.selectedDateStr === todayStr && slotStart < currentTimeMs) {
        disabled = true;
        reason = 'Past';
      }

      if (!disabled) {
        // Vérification de la disponibilité du médecin
        // Un créneau est bloqué si un rendez-vous existant chevauche la plage (+ 10 min de pause)
        const isDoctorBusy = doctorApptsThisDay.some(appt => {
          const apptStart = new Date(appt.appointmentDate!).getTime();
          const apptEnd = apptStart + this.CONSULT_DURATION_MS;
          return Math.max(slotStart, apptStart) < Math.min(slotEnd + 10 * 60000, apptEnd + 10 * 60000);
        });

        if (isDoctorBusy) {
          disabled = true;
          reason = 'Dr. Busy';
        } else {
          // Vérification de la disponibilité du patient
          // Simule l'intégration avec son Google Calendar via ses rendez-vous enregistrés
          const isPatientBusy = userApptsThisDay.some(appt => {
            const apptStart = new Date(appt.appointmentDate!).getTime();
            const apptEnd = apptStart + this.CONSULT_DURATION_MS;
            // Pour le patient, on vérifie uniquement le chevauchement direct (sans pause)
            return Math.max(slotStart, apptStart) < Math.min(slotEnd, apptEnd);
          });

          if (isPatientBusy) {
            disabled = true;
            reason = "You're Busy";
          }
        }
      }

      return { time, disabled, reason };
    });
  }

  /**
   * Sélectionne un créneau horaire en mettant à jour la date de rendez-vous.
   * Format résultant : YYYY-MM-DDTHH:mm (compatible avec l'API back-end).
   *
   * @param time heure du créneau sélectionné (ex: "09:00")
   */
  selectSlot(time: string): void {
    this.appointment.appointmentDate = `${this.selectedDateStr}T${time}`;
  }

  /**
   * Charge depuis l'API la liste des rendez-vous du patient connecté.
   * Filtre côté backend via getFilteredAppointments.
   *
   * @param patientId identifiant du patient connecté
   */
  loadUserAppointments(patientId: number): void {
    this.appointmentService.getFilteredAppointments(undefined, patientId, this.statusFilter).subscribe({
      next: (list) => {
        this.userAppointments = list;
        this.filteredUserAppointments = list;

        // Tri par date (le plus récent en premier) - Le backend pourrait le faire mais pour s'assurer du tri d'affichage :
        this.filteredUserAppointments.sort((a, b) => {
          const timeA = new Date(a.appointmentDate).getTime();
          const timeB = new Date(b.appointmentDate).getTime();
          return timeB - timeA;
        });

        this.buildPatientCalendarMap();   // Mise à jour du calendrier patient
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[Appointment] Error loading appointments:', err);
        this.cdr.detectChanges();
      }
    });
  }

  /** Change le filtre de statut et recalcule via le backend */
  setStatusFilter(status: string): void {
    this.statusFilter = status;
    if (this.appointment && this.appointment.patientId) {
      this.loadUserAppointments(this.appointment.patientId);
    }
  }

  /** Indicateur de chargement pendant la recherche du meilleur créneau */
  isSuggesting = false;

  /**
   * Interroge le back-end pour obtenir automatiquement le meilleur créneau disponible
   * sur les 7 prochains jours, en tenant compte des agendas du médecin et du patient.
   *
   * En cas de succès :
   *   - Le calendrier est positionné sur la date suggérée
   *   - Le créneau est pré-sélectionné dans l'affichage
   *
   * In case of failure (404 = no free slot) or network error:
   *   - An error message is displayed to the user
   */
  suggestSlot(): void {
    // Vérification préalable : un médecin doit être sélectionné
    if (!this.appointment.doctorId) {
      this.errorMessage = 'Please select a doctor first.';
      return;
    }

    this.isSuggesting = true;
    this.errorMessage = '';
    this.successMessage = '';

    // Définition de la plage de recherche : aujourd'hui + 7 jours
    const today = new Date();
    const nextWeek = new Date();
    nextWeek.setDate(today.getDate() + 7);

    const start = today.toISOString().split('T')[0];
    const end = nextWeek.toISOString().split('T')[0];

    // Appel à l'API de suggestion intelligente avec les identifiants du médecin et du patient
    this.appointmentService.suggestBestSlot(
      this.appointment.doctorId,
      this.appointment.patientId,
      start,
      end
    ).subscribe({
      next: (suggestedDateStr: string) => {
        if (suggestedDateStr) {
          // Mise à jour du formulaire avec la date/heure suggérée
          this.appointment.appointmentDate = suggestedDateStr.slice(0, 16);
          this.selectedDateStr = suggestedDateStr.slice(0, 10);
          this.generateSlots(); // Recalcul des créneaux pour positionner visuellement la sélection
          this.successMessage = 'Best available slot found!';
        } else {
          this.errorMessage = 'No slots available in the next 7 days.';
        }
        this.isSuggesting = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        // 404 = le back-end n'a trouvé aucun créneau libre
        if (err.status === 404) {
          this.errorMessage = 'No slots available in the next 7 days.';
        } else {
          this.errorMessage = 'Unable to find a slot. Please try again.';
        }
        this.isSuggesting = false;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Sauvegarde le rendez-vous (création ou mise à jour selon le mode actif).
   * Valide que la date choisie est bien dans le futur avant d'envoyer la requête.
   * Un mécanisme de sécurité coupe le chargement au bout de 5 secondes si l'API ne répond pas.
   */
  saveAppointment(): void {
    this.isLoading = true;
    this.successMessage = '';
    this.errorMessage = '';

    // Sécurité anti-blocage : forcer l'arrêt du spinner après 5 secondes
    setTimeout(() => {
      if (this.isLoading) {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    }, 5000);

    // Rétablissement de la validation des dates futures
    const selectedDate = new Date(this.appointment.appointmentDate);
    const now2 = new Date();
    if (selectedDate <= now2) {
      this.errorMessage = 'Please select a future date and time for your appointment.';
      this.isLoading = false;
      return;
    }

    if (this.isEditMode && this.selectedAppointmentId) {
      // Validate category switch
      if (this.appointment.category === AppointmentCategory.NEW_CONSULTATION && this.userAppointments.length > 1) {
        this.errorMessage = "You already have appointments. You must choose 'Daily Follow Up' instead of New Consultation.";
        this.isLoading = false;
        return;
      }

      // Mode édition : mise à jour du rendez-vous existant
      this.appointmentService.updateAppointment(this.selectedAppointmentId, this.appointment).subscribe({
        next: () => {
          this.successMessage = 'Appointment updated successfully!';
          this.isLoading = false;
          this.cdr.detectChanges();
          this.cancelEdit();
          this.loadUserAppointments(this.appointment.patientId);
          window.scrollTo(0, 0);
        },
        error: () => {
          this.errorMessage = 'Failed to update the appointment.';
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      // Validate category limit on new bookings
      if (this.appointment.category === AppointmentCategory.NEW_CONSULTATION && this.userAppointments.length > 0) {
        this.errorMessage = "You already have past appointments. You must choose 'Daily Follow Up' instead of New Consultation.";
        this.isLoading = false;
        return;
      }

      // Mode création : enregistrement d'un nouveau rendez-vous
      this.appointmentService.createAppointment(this.appointment).subscribe({
        next: () => {
          this.successMessage = 'Your appointment has been booked successfully!';
          this.isLoading = false;
          this.cdr.detectChanges();
          this.loadUserAppointments(this.appointment.patientId);
          this.resetForm();
          window.scrollTo(0, 0);
        },
        error: (err) => {
          console.error('[Appointment] Error creating appointment:', err);
          if (err.error && err.error.message) {
            this.errorMessage = err.error.message;
          } else {
            this.errorMessage = 'Booking failed. Please try again.';
          }
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  /**
   * Supprime (annule) un rendez-vous après confirmation de l'utilisateur.
   *
   * @param id identifiant du rendez-vous à supprimer
   */
  deleteAppointment(id: number | undefined): void {
    if (!id) return;
    if (confirm('Are you sure you want to permanently delete this appointment?')) {
      this.isLoading = true;
      this.appointmentService.deleteAppointment(id).subscribe({
        next: () => {
          this.successMessage = 'Appointment deleted successfully.';
          this.isLoading = false;
          this.cdr.detectChanges();
          this.loadUserAppointments(this.appointment.patientId);
          window.scrollTo(0, 0);
        },
        error: () => {
          this.errorMessage = 'Failed to delete the appointment.';
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  /**
   * Soft-cancels an appointment by setting its status to CANCELLED.
   * The appointment record is kept in the database for tracking purposes.
   * The freed time slot becomes available for other bookings.
   *
   * @param id appointment ID to cancel
   */
  cancelAppointment(id: number | undefined): void {
    if (!id) return;
    if (confirm('Cancel this appointment? The slot will be freed and your doctor will be notified.')) {
      this.appointmentService.cancelAppointment(id).subscribe({
        next: (updated) => {
          // Mise à jour locale sans rechargement complet
          const idx = this.userAppointments.findIndex(a => a.id === id);
          if (idx !== -1) this.userAppointments[idx] = updated;
          this.buildPatientCalendarMap();
          this.successMessage = 'Appointment cancelled successfully.';
          this.cdr.detectChanges();
          window.scrollTo(0, 0);
        },
        error: () => {
          this.errorMessage = 'Failed to cancel the appointment. Please try again.';
          this.cdr.detectChanges();
        }
      });
    }
  }

  /**
   * Passe le formulaire en mode édition en chargeant les données du rendez-vous sélectionné.
   * Le calendrier est repositionné sur la date existante et les créneaux sont recalculés.
   *
   * @param appt le rendez-vous à modifier
   */
  editAppointment(appt: Appointment): void {
    this.isEditMode = true;
    this.selectedAppointmentId = appt.id;
    this.appointment = { ...appt }; // Copie pour éviter la mutation directe de la liste

    if (this.appointment.appointmentDate) {
      // Formatage de la date pour compatibilité avec le calendrier
      this.appointment.appointmentDate = this.appointment.appointmentDate.slice(0, 16);
      this.selectedDateStr = this.appointment.appointmentDate.slice(0, 10);
    }

    // Charge les rendez-vous du médecin et régénère les créneaux pour afficher la sélection
    this.onDoctorChange();

    window.scrollTo(0, 0);
  }

  /**
   * Annule le mode édition et réinitialise le formulaire à son état vide.
   */
  cancelEdit(): void {
    this.isEditMode = false;
    this.selectedAppointmentId = undefined;
    this.resetForm();
  }

  /**
   * Retourne le nom complet du médecin à partir de son identifiant.
   * Utilisé dans le tableau de liste des rendez-vous du patient.
   *
   * @param doctorId identifiant du médecin
   * @return chaîne formatée "Dr. Prénom Nom" ou "Doctor ID: X" si non trouvé
   */
  getDoctorName(doctorId: number): string {
    if (doctorId === -1) return '';
    const doc = this.doctors.find(d => d.userId === doctorId);
    return doc ? `Dr. ${doc.firstName} ${doc.lastName}` : `Doctor ID: ${doctorId}`;
  }

  /**
   * Réinitialise complètement le formulaire de rendez-vous à son état initial.
   * Conserve l'identifiant du patient connecté et recharge les créneaux du médecin par défaut.
   */
  resetForm(): void {
    const userId = this.appointment.patientId;
    this.appointment = {
      patientId: this.appointment.patientId,
      doctorId: 0 as any,
      appointmentDate: '',
      isUrgent: false,
      type: AppointmentType.IN_PERSON,
      category: AppointmentCategory.DAILY_FOLLOW_UP,
      status: AppointmentStatus.PENDING
    };
    this.selectedDateStr = '';
    this.availableSlots = [];
    this.selectedAppointmentId = undefined;
    this.isEditMode = false;

    // Recharge la liste avec le nouveau rendez-vous (qui sera inclus dans le filtre actuel)
    this.loadUserAppointments(this.appointment.patientId);
  }

  // ─── GOOGLE CALENDAR ACTIONS ───────────────────────────────────────────

  handleAuthClick(): void {
    const tokenClient = google.accounts.oauth2.initTokenClient({
      client_id: this.CLIENT_ID,
      scope: this.SCOPES,
      callback: (tokenResponse: any) => {
        if (tokenResponse && tokenResponse.access_token) {
          sessionStorage.setItem('google_access_patient', tokenResponse.access_token);
          this.isAuthorized = true;
          this.fetchGoogleEventsWithToken(tokenResponse.access_token);
        }
      }
    });
    tokenClient.requestAccessToken({ prompt: 'consent' });
  }

  handleSignoutClick(): void {
    sessionStorage.removeItem('google_access_patient');
    this.isAuthorized = false;
    this.googleEventsLoaded = [];
    this.buildPatientCalendarMap();
    this.cdr.detectChanges();
  }

  private async fetchGoogleEventsWithToken(accessToken: string): Promise<void> {
    const timeMin = new Date();
    timeMin.setFullYear(timeMin.getFullYear() - 1); // Get all from past 1 year
    const currentUserId = this.appointment.patientId;

    const url = `https://www.googleapis.com/calendar/v3/calendars/primary/events?timeMin=${encodeURIComponent(timeMin.toISOString())}&showDeleted=false&singleEvents=true&maxResults=2500&orderBy=startTime`;

    try {
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Accept': 'application/json'
        }
      });

      if (response.status === 401 || response.status === 403) {
        this.handleSignoutClick();
        throw new Error('Unauthorized Google Access. Session expired');
      }

      if (!response.ok) throw new Error('Bad network response');

      const data = await response.json();
      const dbEvents = data.items || [];

      this.googleEventsLoaded = dbEvents
        .filter((e: any) => !(e.summary && e.summary.includes('Mind-Care')))
        .map((e: any): Appointment => {
          return {
            id: -1000 - Math.floor(Math.random() * 9000), // Fake ID
            patientId: currentUserId,
            doctorId: -1,
            appointmentDate: e.start?.dateTime || e.start?.date,
            isUrgent: false,
            type: AppointmentType.IN_PERSON as any,
            category: AppointmentCategory.DAILY_FOLLOW_UP as any,
            status: AppointmentStatus.CONFIRMED as any,
            title: 'Google: ' + (e.summary || '(Sans Titre)')
          };
        });

      console.log('Google events injected into patient calendar view', this.googleEventsLoaded);
      this.errorMessage = '';
      this.buildPatientCalendarMap(); // Rebuild with google events included
      this.cdr.detectChanges();

      // 🔄 TWO-WAY SYNC: Push missing local appointments to Google Calendar
      this.syncLocalToGoogle(accessToken, dbEvents);

    } catch (err: any) {
      console.error('Error fetching Google events', err);
      this.errorMessage = 'Google Calendar Error: ' + (err.message || 'sync failed');
      this.cdr.detectChanges();
    }
  }

  private async syncLocalToGoogle(accessToken: string, dbEvents: any[]): Promise<void> {
    for (const apt of this.userAppointments) {
      if (apt.status !== AppointmentStatus.CONFIRMED || !apt.appointmentDate) continue;

      const aptDate = new Date(apt.appointmentDate);
      const title = 'Mind-Care: ' + this.getDoctorName(apt.doctorId);

      const alreadyInGoogle = dbEvents.some((gEvent: any) => {
        if (!gEvent.start || (!gEvent.start.dateTime && !gEvent.start.date)) return false;
        const gDate = new Date(gEvent.start.dateTime || gEvent.start.date);
        const diffMinutes = Math.abs(gDate.getTime() - aptDate.getTime()) / 60000;
        return diffMinutes < 120 && gEvent.summary && gEvent.summary.includes('Mind-Care');
      });

      if (!alreadyInGoogle) {
        const endApt = new Date(aptDate.getTime() + 30 * 60000); // 30 min duration
        const newEvent = {
          summary: title,
          start: { dateTime: aptDate.toISOString() },
          end: { dateTime: endApt.toISOString() },
          description: `Type: ${apt.type}.\nConsultation ajoutée automatiquement par Mind-Care.`
        };

        try {
          await fetch(`https://www.googleapis.com/calendar/v3/calendars/primary/events`, {
            method: 'POST',
            headers: {
              'Authorization': `Bearer ${accessToken}`,
              'Content-Type': 'application/json'
            },
            body: JSON.stringify(newEvent)
          });
          console.log("Pushed local to Google:", title);
        } catch (e) {
          console.error("Failed to push to GC:", e);
        }
      }
    }
  }

  // ─── PATIENT CALENDAR METHODS ──────────────────────────────────────────────

  /**
   * Construit la map date → rendez-vous pour le calendrier du patient.
   * Appelée après chaque chargement ou modification de rendez-vous.
   */
  buildPatientCalendarMap(): void {
    const map = new Map<string, Appointment[]>();

    // Merge standard appointments + Google loaded appointments (if any)
    const allAppointments = [...this.userAppointments, ...this.googleEventsLoaded];

    for (const apt of allAppointments) {
      if (!apt.appointmentDate) continue;
      const key = apt.appointmentDate.slice(0, 10);
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(apt);
    }
    this.patCalMap = map;
  }

  /**
   * Convertit une Date JS en clé locale "YYYY-MM-DD" sans conversion UTC.
   * Évite le décalage de +1/-1 jour causé par toISOString() en fuseau UTC+1.
   */
  patLocalKey(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  setPatCalMode(mode: 'DAY' | 'WEEK' | 'MONTH' | 'AGENDA'): void {
    this.patCalMode = mode;
    this.cdr.detectChanges();
  }

  navigatePatCal(dir: number): void {
    const d = new Date(this.patCalDate);
    if (this.patCalMode === 'MONTH') d.setMonth(d.getMonth() + dir);
    else if (this.patCalMode === 'WEEK') d.setDate(d.getDate() + dir * 7);
    else if (this.patCalMode === 'DAY') d.setDate(d.getDate() + dir);
    this.patCalDate = d;
    this.cdr.detectChanges();
  }

  get patCalTitle(): string {
    if (this.patCalMode === 'AGENDA') return 'Upcoming Appointments';
    if (this.patCalMode === 'DAY')
      return this.patCalDate.toLocaleDateString('en-US',
        { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
    return this.patCalDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  }

  /** Grille 6×7 pour la vue mensuelle (commence le lundi) */
  /** Grille 6×7 pour la vue mensuelle — inclut les jours fériés */
  get patCalMonthDays(): { date: Date; otherMonth: boolean; isToday: boolean; apts: Appointment[]; holiday: { name: string } | null }[] {
    const year = this.patCalDate.getFullYear();
    const month = this.patCalDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const offset = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1;
    const start = new Date(firstDay);
    start.setDate(start.getDate() - offset);
    const todayStr = new Date().toDateString();
    const holidays = this.getHolidayMap(year);

    const cells: { date: Date; otherMonth: boolean; isToday: boolean; apts: Appointment[]; holiday: { name: string } | null }[] = [];
    for (let i = 0; i < 42; i++) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      // Récupère le jours férié (si le jour appartient à une autre année, utilise la bonne map)
      const hmap = this.getHolidayMap(d.getFullYear());
      const hKey = this.patLocalKey(d);
      const holiday = hmap.get(hKey) ?? null;
      cells.push({
        date: d,
        otherMonth: d.getMonth() !== month,
        isToday: d.toDateString() === todayStr,
        apts: this.patCalMap.get(hKey) || [],
        holiday
      });
    }
    return cells;
  }

  /** 7 jours (lun-dim) de la semaine courante — inclut les jours fériés */
  get patCalWeekDays(): { date: Date; isToday: boolean; apts: Appointment[]; holiday: { name: string } | null }[] {
    const dow = this.patCalDate.getDay();
    const mon = new Date(this.patCalDate);
    mon.setDate(this.patCalDate.getDate() - (dow === 0 ? 6 : dow - 1));
    const todayStr = new Date().toDateString();
    const result: { date: Date; isToday: boolean; apts: Appointment[]; holiday: { name: string } | null }[] = [];
    for (let i = 0; i < 7; i++) {
      const d = new Date(mon);
      d.setDate(mon.getDate() + i);
      const hKey = this.patLocalKey(d);
      const hmap = this.getHolidayMap(d.getFullYear());
      result.push({
        date: d,
        isToday: d.toDateString() === todayStr,
        apts: this.patCalMap.get(hKey) || [],
        holiday: hmap.get(hKey) ?? null
      });
    }
    return result;
  }

  /** Rendez-vous du jour sélectionné (vue DAY) */
  get patCalDayApts(): Appointment[] {
    return this.patCalMap.get(this.patLocalKey(this.patCalDate)) || [];
  }

  /** Liste des rendez-vous futurs groupés par date, avec fêtes (vue AGENDA) */
  get patCalAgendaGroups(): { label: string; apts: Appointment[]; holiday: string | null }[] {
    const today = this.patLocalKey(new Date());
    const keys = Array.from(this.patCalMap.keys()).filter(k => k >= today).sort();
    return keys.map(k => {
      const d = new Date(k + 'T12:00:00');
      const hmap = this.getHolidayMap(d.getFullYear());
      const hEntry = hmap.get(k);
      return {
        label: d.toLocaleDateString('en-US',
          { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' }),
        apts: this.patCalMap.get(k)!,
        holiday: hEntry ? hEntry.name : null
      };
    });
  }

  /** Classe CSS selon le statut du rendez-vous */
  getPatAptClass(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'apt-confirmed';
      case 'CANCELLED': return 'apt-cancelled';
      case 'RESCHEDULED': return 'apt-rescheduled';
      default: return 'apt-pending';
    }
  }
}
