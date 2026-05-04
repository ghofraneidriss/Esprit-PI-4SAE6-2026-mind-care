# Monitoring Setup - Prometheus & Grafana

## Changes Made

### 1. Fixed Prometheus Configuration
**File**: `monitoring/prometheus.yml`
- **Removed**: Nexus scrape configuration (was causing HTTP 404 errors)
- **Kept**: 
  - lost-item-service metrics (port 8080, /actuator/prometheus)
  - followup-alert-service metrics (port 8080, /actuator/prometheus)
  - Prometheus self-monitoring (port 9090)

### 2. Created Grafana Dashboard
**Files created**:
- `monitoring/grafana/dashboards/mind-care-services.json` - Dashboard definition
- `monitoring/grafana/provisioning/dashboards/dashboards.yml` - Dashboard provisioning config
- `monitoring/grafana/provisioning/dashboards/definitions/mind-care-services.json` - Provisioned dashboard

**Dashboard includes**:
- JVM Memory Usage (separate panels for each service)
- HTTP Request Rates (requests/sec)
- HTTP Request Latency (p95 percentiles)
- All metrics auto-refresh every 10 seconds

### 3. Data Source
**Already configured**: `monitoring/grafana/provisioning/datasources/prometheus.yml`
- Connects to Prometheus at `http://prometheus:9090`
- Auto-provisioned on Grafana startup

## How to Verify

### Step 1: Restart Services
```bash
bash restart-monitoring.sh
```

Or manually:
```bash
docker compose restart prometheus grafana
```

### Step 2: Access Grafana
1. Open browser: `http://localhost:3000`
2. Login with: `admin` / `admin`
3. Change password if prompted

### Step 3: Verify Dashboard
1. In left sidebar, click on **Dashboards**
2. Look for **"Mind Care Services Monitoring"**
3. Click to open the dashboard

### Step 4: Verify Prometheus Targets
1. In a new tab, open `http://localhost:9090/targets`
2. You should see:
   - **lost-item-service**: UP ✅
   - **followup-alert-service**: UP ✅
   - **prometheus**: UP ✅
   - (Nexus removed - was returning 404)

## Expected Metrics

The dashboard will show:
- **JVM Memory**: Used heap memory over time for each service
- **HTTP Requests**: Rate of HTTP requests per second
- **Request Latency**: 95th percentile response times

## Troubleshooting

### Dashboard Not Appearing
- Restart Grafana again: `docker compose restart grafana`
- Wait 10-15 seconds for provisioning to complete
- Refresh the Grafana page in your browser

### No Data in Dashboard
- Wait 1-2 minutes for Prometheus to collect initial metrics
- Check Prometheus targets page (http://localhost:9090/targets)
- Verify services are UP and have recent scrape times

### Services Not Healthy
If lost-item-service or followup-alert-service show DOWN:
- Check they're running: `docker compose ps`
- Check logs: `docker compose logs lost-item-service`
- Verify they're exposing metrics on port 8080

## Configuration Files

All provisioning configurations are automatically loaded by Docker Compose:
```yaml
volumes:
  - ./monitoring/grafana/provisioning:/etc/grafana/provisioning:ro
  - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
```

## Next Steps

After verification:
1. Create additional dashboards as needed
2. Set up alerts in Grafana for critical metrics
3. Configure Prometheus retention policies (currently 30 days)
4. Set up backup of Prometheus data if needed
