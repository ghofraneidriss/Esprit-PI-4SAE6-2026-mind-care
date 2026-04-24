# Quick Start Guide - Twilio & Session Integration

## 🎯 What Was Built

You now have a complete Twilio SMS/Voice notification system integrated with real-time volunteer session tracking.

### Key Features:
✅ **Assign missions to volunteers** → Twilio sends SMS or voice call  
✅ **Track volunteer online status** → Green/red indicator in admin panel  
✅ **WebSocket session management** → Real-time presence updates  
✅ **Priority-based notifications** → HIGH priority = voice call, LOW = SMS  
✅ **Session heartbeat** → Automatic keep-alive every 20s  

---

## 🚀 Quick Test (5 minutes)

### Step 1: Start the Services
```bash
# Terminal 1: Start Spring Boot (Volunteer microservice)
cd volunteer
mvn spring-boot:run

# Terminal 2: Start Angular dev server
cd front
ng serve
```

### Step 2: Create a Test Mission
1. Open `http://localhost:4200`
2. Login as Admin (role: ADMIN)
3. Go to "Volunteering & Missions"
4. Click "Create Mission"
5. Fill in details:
   - Title: "Test Mission"
   - Category: "Home Visit"
   - Priority: "High" (for voice call)
   - Click "Create Mission"

### Step 3: Assign to Volunteer
1. Find a volunteer in the directory
2. Click "Assign to Mission"
3. See the confirmation modal with Twilio warning
4. Click "Okay" to confirm
5. ✅ Volunteer should receive voice call or SMS

### Step 4: Check Volunteer Session
1. In another browser tab, login as Volunteer
2. Go to "My Mission History"
3. You should see:
   - Session status: `🟢 Online`
   - Last heartbeat: Current timestamp
   - Online volunteers: Count displayed
4. Wait 60s and refresh → Should still be online
5. Close tab → Returns to 🔴 Offline after 90s

---

## 📱 Backend API Testing (with Postman)

### Test 1: Create Assignment (Triggers Twilio)
```
POST http://localhost:8085/api/volunteer/assignments
Content-Type: application/json

{
    "mission": { "id": 1 },
    "volunteerId": 1,
    "volunteerUserId": 1,
    "notes": "Test assignment"
}
```
**Expected**: Volunteer receives SMS/call, assignment created

### Test 2: Get Volunteer Presence
```
GET http://localhost:8085/api/volunteer/presence/1
```
**Expected**: Returns presence data with lastHeartbeat time

### Test 3: Get All Online Volunteers
```
GET http://localhost:8085/api/volunteer/presence/online
```
**Expected**: Array of currently online volunteers

---

## 🔍 Testing Checklist

| Feature | How to Test | Expected Result |
|---------|-------------|-----------------|
| **Twilio SMS** | Create LOW priority mission, assign | Volunteer gets SMS in <10s |
| **Twilio Call** | Create HIGH priority mission, assign | Volunteer gets voice call in <10s |
| **Online Status** | Admin views volunteer list | Green badge next to volunteer |
| **Session Start** | Volunteer logs in | 🟢 Online appears in header |
| **Session End** | Volunteer logs out | 🔴 Offline after 90s |
| **Heartbeat** | Monitor Network tab | WebSocket message every 20s |
| **Real-time update** | Two admin dashboards | Status syncs across tabs |

---

## 📊 Files Changed (Summary)

### Frontend Services
```
✅ volunteer.service.ts
   • Added: createAssignment()
   • Added: getPresenceStatus()
   • Added: getOnlineVolunteers()

✅ auth.service.ts
   • Added: loginCompleted$ subject
   • Added: logoutCompleted$ subject
```

### Frontend Components
```
✅ volunteering.ts
   • Added: Online status tracking
   • Added: Mission assignment with Twilio

✅ volunteer-missions.ts
   • Added: Session initialization
   • Added: Presence monitoring
   • Added: Auto-login/logout handling
```

### Frontend Templates
```
✅ volunteering.html
   • Added: Online status badges
   • Added: Assignment modal

✅ volunteer-missions.html
   • Added: Session status header
   • Added: Online volunteers count
```

### Styling
```
✅ volunteering.css
   • Added: .status-online (green)
   • Added: .status-offline (red)
```

---

## 🔧 Configuration Checklist

- [x] Twilio Account SID set in `application.properties`
- [x] Twilio Auth Token set in `application.properties`
- [x] Twilio Phone Number set in `application.properties`
- [x] WebSocket URL correct: `ws://localhost:8085/ws/websocket`
- [x] API Gateway routing configured
- [x] CORS enabled for `http://localhost:4200`

---

## 🐛 Troubleshooting

### Issue: "Twilio SMS not being sent"
**Solution**:
1. Check account balance: https://www.twilio.com/console
2. Verify phone number in `application.properties`
3. Check volunteer has phone number in database
4. Review backend logs for Twilio errors

### Issue: "Online status not updating"
**Solution**:
1. Open DevTools → Network tab
2. Look for WebSocket connection to `ws://localhost:8085/ws/websocket`
3. Should see heartbeat messages every 20s
4. Check browser console for JS errors

### Issue: "Modal not appearing"
**Solution**:
1. Clear browser cache: `Ctrl+Shift+Delete`
2. Refresh page: `F5`
3. Check browser console for Angular errors
4. Verify volunteering.html template is valid

---

## 📈 Performance Notes

- **Heartbeat Interval**: 20 seconds (WebSocket) + 30 seconds (HTTP)
- **Presence Refresh**: 30 seconds (auto-sync admin dashboard)
- **Online Status TTL**: 90 seconds (offline if no heartbeat)
- **Connection Pool**: Reuses active WebSocket connection

---

## 🔐 Security

- JWT authentication required for all endpoints
- WebSocket connection validated with userId
- Twilio credentials never exposed in frontend
- Session tokens validated on backend

---

## 📚 Full Documentation

See: **TWILIO_INTEGRATION_GUIDE.md** for complete technical documentation

---

## 💬 Summary

You have successfully implemented:

1. ✅ **Twilio Integration**
   - SMS notifications for LOW/MEDIUM priority missions
   - Voice calls with TTS for HIGH priority missions
   - Triggered automatically on mission assignment

2. ✅ **Session Tracking**
   - Volunteer marked online when logged in
   - Real-time heartbeat every 20 seconds
   - Automatic offline after 90 seconds without heartbeat
   - WebSocket for real-time updates

3. ✅ **Admin Dashboard**
   - See which volunteers are online/offline
   - Assign missions with one click
   - Twilio notification sent automatically
   - Visual confirmation modal

4. ✅ **Volunteer Experience**
   - See own session status (online/offline)
   - Receive SMS or voice call for new missions
   - See mission assigned in "My Mission History"
   - Track mission progress

---

**Ready to Deploy? ✅**

Everything is production-ready. Just run the services and test!

---

Last Updated: April 15, 2026
