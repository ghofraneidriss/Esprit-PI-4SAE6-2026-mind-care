# Twilio API Integration Guide - MindCare Volunteer Management

## Overview
This guide explains the complete integration of Twilio API with the MindCare volunteer microservice for real-time mission assignments and session tracking.

---

## Architecture Overview

### Backend (Spring Boot)
**Location**: `volunteer/` microservice

**Key Components**:
- **TwilioService** (`TwilioService.java`) - Handles SMS/voice calls via Twilio
- **AssignmentService** (`AssignmentService.java`) - Manages mission assignments and triggers Twilio notifications
- **VolunteerPresenceRepository** - Tracks online/offline status of volunteers
- **WebSocketConfig** - Real-time presence updates via WebSocket

### Frontend (Angular)
**Location**: `front/src/app/`

**Key Components**:
- **VolunteerService** - HTTP + WebSocket client for volunteer data
- **AuthService** - Login/session management with events
- **VolunteeringComponent** - Admin panel for assigning missions to volunteers
- **VolunteerMissionsComponent** - Volunteer view of assigned missions

---

## Feature Implementation

### 1. **Twilio Notifications on Mission Assignment**

#### How It Works:
1. Admin clicks "Assign to Mission" for a volunteer
2. Frontend calls `VolunteerService.createAssignment(missionId, volunteerId)`
3. Backend receives assignment request at `POST /api/volunteer/assignments`
4. `AssignmentService.manualAssign()` creates the assignment
5. `notifyVolunteer()` method checks mission priority:
   - **HIGH Priority** → Sends voice call via `twilioService.makeCall()`
   - **MEDIUM/LOW Priority** → Sends SMS via `twilioService.sendSms()`
6. Volunteer receives notification with mission details

#### Backend Code Flow:
```
AssignmentController.createAssignment()
    ↓
AssignmentService.manualAssign()
    ├─ Validate mission and volunteer capacity
    ├─ Create Assignment entity
    ├─ Mark mission as ASSIGNED
    └─ notifyVolunteer()
        └─ Check priority and call TwilioService
            ├─ makeCall() for HIGH priority
            └─ sendSms() for MEDIUM/LOW priority
```

#### Frontend Usage:
```typescript
// In VolunteeringComponent
assignMissionToVolunteer(volunteer: VolunteerDirectoryEntry, mission?: Mission): void {
    const targetMission = mission || this.selectedMissionForAssignment;
    
    this.volunteerService.createAssignment(
        targetMission.id, 
        volunteer.id
    ).subscribe({
        next: (assignment) => {
            // Update mission status
            // Show success message
        },
        error: (err) => {
            // Handle error
        }
    });
}
```

---

### 2. **Session Tracking - Volunteer Online Status**

#### How It Works:
1. Volunteer logs in (AuthService emits `loginCompleted$` event)
2. VolunteerMissionsComponent subscribes to login event
3. `initializeSession()` marks volunteer as online via `markOnline()`
4. WebSocket connection established for real-time updates
5. Heartbeat sent every 20 seconds to maintain presence
6. HTTP heartbeat every 30 seconds to `/api/volunteers/heartbeat/{userId}`
7. Volunteer logs out → `markOffline()` called, WebSocket disconnected

#### Frontend Code Flow:
```
AuthService.login()
    ↓
Emits: loginCompleted$ event
    ↓
VolunteerMissionsComponent.ngOnInit()
    ├─ Subscribes to loginCompleted$
    ├─ Calls initializeSession()
    │   ├─ markOnline(userId, displayName)
    │   ├─ connectWebSocket()
    │   └─ loadVolunteerPresence()
    ├─ WebSocket heartbeat every 20 seconds
    └─ HTTP heartbeat every 30 seconds
```

#### Backend Session Flow:
```
POST /api/volunteers/{userId}/login
    ↓
VolunteerController.markOnline()
    ├─ Create/update VolunteerPresence entity
    ├─ Set status = "ONLINE"
    └─ Record login timestamp

WebSocket heartbeat
    ↓
Update lastHeartbeat timestamp

POST /api/volunteers/heartbeat/{userId}
    ↓
Update presence lastHeartbeat time
```

---

### 3. **Real-Time Presence Updates**

#### Volunteer Sees Other Volunteers' Status:
```typescript
// In VolunteeringComponent
loadVolunteerPresence(): void {
    this.volunteerService.getPresenceStatus().subscribe({
        next: (presenceList) => {
            this.volunteerPresenceMap.clear();
            presenceList.forEach(presence => {
                this.volunteerPresenceMap.set(presence.userId, presence);
            });
        }
    });
}

getVolunteerOnlineStatus(volunteerId: number): string {
    const presence = this.volunteerPresenceMap.get(volunteerId);
    if (!presence) return 'Offline';
    
    const lastHeartbeat = new Date(presence.lastHeartbeat);
    const now = new Date();
    const diffSeconds = (now.getTime() - lastHeartbeat.getTime()) / 1000;
    
    // Online if heartbeat within 90 seconds
    return diffSeconds < 90 ? 'Online' : 'Offline';
}
```

#### Display in Template:
```html
<span [ngClass]="getVolunteerOnlineStatusClass(v.id)" title="Online Status">
    {{ getVolunteerOnlineStatus(v.id) }}
</span>
```

---

## API Endpoints

### Assignment Endpoints

#### Create Assignment (Triggers Twilio)
```
POST /api/volunteer/assignments
Content-Type: application/json

{
    "mission": { "id": 123 },
    "volunteerId": 456,
    "volunteerUserId": 456,
    "notes": "Optional notes"
}

Response:
{
    "id": 789,
    "volunteerId": 456,
    "mission": { ... },
    "status": "ASSIGNED",
    "assignedAt": "2026-04-15T10:30:00Z"
}
```

#### Get Volunteer Assignments
```
GET /api/volunteer/assignments/volunteer/{volunteerId}

Response: Assignment[]
```

#### Update Assignment Status
```
PUT /api/volunteer/assignments/{id}
Content-Type: application/json

{
    "status": "IN_PROGRESS"
}
```

### Presence Endpoints

#### Mark Volunteer Online
```
POST /api/volunteers/{userId}/login
Content-Type: application/json

{
    "displayName": "John Doe",
    "userAgent": "Mozilla/5.0..."
}
```

#### Mark Volunteer Offline
```
POST /api/volunteers/{userId}/logout
```

#### Get Volunteer Presence
```
GET /api/volunteers/presence/{userId}

Response:
{
    "userId": 123,
    "displayName": "John Doe",
    "status": "ONLINE",
    "lastHeartbeat": "2026-04-15T10:45:30Z",
    "connectedAt": "2026-04-15T10:00:00Z"
}
```

#### Get All Online Volunteers
```
GET /api/volunteer/presence/online

Response: VolunteerPresenceDTO[]
```

#### Get Presence Status Summary
```
GET /api/volunteer/presence/summary

Response:
{
    "online": 5,
    "offline": 12,
    "idle": 2
}
```

---

## Frontend Components

### VolunteerService (Updated)
**File**: `front/src/app/backoffice/volunteering/volunteer.service.ts`

**New Methods**:
```typescript
// Create manual assignment - triggers Twilio notification
createAssignment(missionId: number, volunteerId: number, notes?: string): Observable<any>

// Get presence status for all volunteers
getPresenceStatus(): Observable<VolunteerPresenceDTO[]>

// Get presence status for a specific volunteer
getVolunteerPresence(userId: number): Observable<VolunteerPresenceDTO>

// Get all online volunteers
getOnlineVolunteers(): Observable<VolunteerPresenceDTO[]>

// Mark volunteer as online (session start)
markOnline(userId: number, displayName?: string): Observable<void>

// Mark volunteer as offline (session end)
markOffline(userId: number): Observable<void>

// Connect WebSocket for real-time updates
connectWebSocket(): void

// Disconnect WebSocket
disconnectWebSocket(): void
```

### AuthService (Updated)
**File**: `front/src/app/frontoffice/auth/auth.service.ts`

**New Subjects**:
```typescript
// Emitted when login is successful
loginCompleted$: Subject<AuthUser>

// Emitted when logout is successful
logoutCompleted$: Subject<void>
```

### VolunteeringComponent (Updated)
**File**: `front/src/app/backoffice/volunteering/volunteering.ts`

**New Properties**:
```typescript
volunteerPresenceMap: Map<number, any> = new Map()
selectedMissionForAssignment: Mission | null = null
isAssignModalOpen = false
assignmentLoading = false
assignmentError = ''
```

**New Methods**:
```typescript
// Load all volunteer presence/status
loadVolunteerPresence(): void

// Get volunteer online status
getVolunteerOnlineStatus(volunteerId: number): string
getVolunteerOnlineStatusClass(volunteerId: number): string

// Open/close assignment modal
openAssignModal(mission: Mission): void
closeAssignModal(): void

// Assign mission to volunteer (triggers Twilio)
assignMissionToVolunteer(volunteer: VolunteerDirectoryEntry, mission?: Mission): void
```

### VolunteerMissionsComponent (Updated)
**File**: `front/src/app/frontoffice/volunteer-missions/volunteer-missions.ts`

**New Properties**:
```typescript
volunteerSessionPresence: any = null
onlineVolunteers: any[] = []
isSessionActive = false
```

**New Methods**:
```typescript
// Initialize volunteer session on login
initializeSession(): void

// Load current volunteer's presence info
loadVolunteerPresence(userId: number): void

// Load list of all online volunteers
loadOnlineVolunteers(): void

// Get session status string
getSessionStatus(): string

// Get last heartbeat time
getLastHeartbeat(): string
```

---

## Frontend UI Updates

### Volunteering Component Template
- Added online status indicator next to each volunteer name (🟢 Online / 🔴 Offline)
- Added "Assign to Mission" button with Twilio confirmation dialog
- Added assignment modal showing mission details and notification warning

### Volunteer Missions Component Template
- Added session status header showing:
  - 🟢 Online / 🔴 Offline indicator
  - Last heartbeat timestamp
  - Count of online volunteers

### CSS Additions
```css
.status-online {
    display: inline-block;
    padding: 2px 8px;
    background: #dcfce7;
    color: #166534;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
}

.status-offline {
    display: inline-block;
    padding: 2px 8px;
    background: #fee2e2;
    color: #7f1d1d;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
}
```

---

## Configuration

### Twilio Settings (Backend)
**File**: `volunteer/src/main/resources/application.properties`

```properties
twilio.account.sid=${TWILIO_ACCOUNT_SID}
twilio.auth.token=${TWILIO_AUTH_TOKEN}
twilio.phone.number=${TWILIO_PHONE_NUMBER}
```

### WebSocket Configuration (Backend)
**File**: `volunteer/src/main/java/tn/esprit/microservice/volunteer/Config/WebSocketConfig.java`

- Broker endpoint: `/ws/websocket`
- Allowed origins: `http://localhost:4200`
- Application destination prefix: `/app`

### API Gateway Routing (Frontend)
**Base URLs**:
- Volunteer microservice: `http://localhost:8085/api/volunteer/`
- Volunteers service: `http://localhost:8085/api/volunteers/`

---

## Usage Flow

### Admin Assigning Mission to Volunteer

1. **Admin logs in** → Views volunteering dashboard
2. **Views volunteer directory** → Sees each volunteer with online status
3. **Creates a mission** (if not already created)
4. **Clicks "Assign to Mission"** → Opens assignment confirmation modal
5. **Confirms assignment** → Backend creates assignment
6. **Volunteer receives notification**:
   - **HIGH priority** → Twilio voice call with mission title
   - **MEDIUM/LOW priority** → Twilio SMS with mission details
7. **Admin sees** mission moved to "Assigned" column with volunteer name

### Volunteer Using the System

1. **Volunteer logs in** → Session marked as ONLINE
2. **WebSocket connects** → Real-time presence tracking starts
3. **Views "My Mission History"** → Sees:
   - Current session status (🟢 Online)
   - Last heartbeat time
   - Number of other online volunteers
4. **Receives notification** → Mission assigned (SMS or voice call)
5. **Navigates to missions** → Clicks "Start" to begin mission
6. **Updates mission status** → "In Progress" → "Completed"
7. **Logs out** → Session marked as OFFLINE, WebSocket disconnected

---

## Testing

### Test Twilio Notification
```
POST /api/volunteer/assignments/test-notification/{volunteerId}
```

This endpoint sends a test SMS to the volunteer's registered phone number.

### Test WebSocket Connection
Open browser console and check:
```javascript
// In browser console
// Should see WebSocket connected message
console.log('Check Network tab > WS for ws://localhost:8085/ws/websocket')
```

### Monitor Presence Updates
1. Open volunteer dashboard in two browser tabs
2. See same volunteer appear/disappear as "Online" in real-time
3. Close one tab → Status updates to "Offline" after 90 seconds

---

## Troubleshooting

### Twilio Notifications Not Sending

**Check**:
1. Verify Twilio credentials in `application.properties`
2. Ensure volunteer has phone number registered in `VolunteerPresence`
3. Check Twilio account balance
4. Review backend logs for Twilio API errors

### WebSocket Connection Issues

**Check**:
1. Verify WebSocket URL is correct: `ws://localhost:8085/ws/websocket`
2. Ensure `@EnableWebSocketMessageBroker` is configured
3. Check browser Network tab for connection status
4. Verify CORS allowed origins include `http://localhost:4200`

### Online Status Not Updating

**Check**:
1. Verify heartbeat being sent every 20 seconds (check Network tab)
2. Ensure `loadVolunteerPresence()` is called periodically
3. Check if volunteer presence records exist in database
4. Verify last heartbeat time is being updated

---

## Security Considerations

1. **Authentication**: JWT tokens validated for all endpoints
2. **Authorization**: Only VOLUNTEER role can see own assignments
3. **Twilio Credentials**: Keep in environment variables, not in code
4. **WebSocket**: STOMP headers include userId for authentication
5. **Session Tracking**: Automatic cleanup of stale presence records

---

## Performance Optimization

1. **Presence Cache**: Volunteer presence map cached and reloaded every 30 seconds
2. **WebSocket Heartbeat**: Lightweight JSON heartbeat (not full user data)
3. **Selective Loading**: Only online volunteers displayed in priority
4. **Lazy Loading**: Assignment history paginated (not shown in this doc)

---

## Future Enhancements

1. **Two-Way Communication**: In-app messaging between admin and volunteer
2. **Geolocation Tracking**: Show volunteer location on map
3. **Availability Slots**: Volunteers can set preferred hours
4. **Skill-Based Matching**: Auto-assign based on volunteer skills
5. **Push Notifications**: Browser push notifications in addition to Twilio
6. **Analytics Dashboard**: Track response times and completion rates

---

## Contact & Support

For issues or questions about this implementation:
1. Check backend logs: `volunteer/target/logs/`
2. Check frontend console: Browser DevTools → Console
3. Verify API endpoints: Use Postman to test REST endpoints
4. Check WebSocket: Use WebSocket client tool

---

**Last Updated**: April 15, 2026
**Version**: 1.0
**Status**: Production Ready
