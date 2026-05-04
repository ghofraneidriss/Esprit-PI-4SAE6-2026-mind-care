# MySQL Monitoring & Alerting Setup

## What Was Added

### 1. MySQL Exporter 🗄️
Automatically collects metrics from MySQL database:
- **Active Connections** - current connections vs max
- **Slow Queries** - queries taking too long
- **Query Performance** - execution times
- **Buffer Pool Usage** - memory efficiency
- **InnoDB Metrics** - transaction/lock data

Container: `alzcare-mysql-exporter` (port 9104)

### 2. AlertManager 🚨
Handles alert routing and management:
- Receives alerts from Prometheus
- Groups related alerts together
- Routes to notification channels (email, Slack, PagerDuty, etc)
- Deduplicates and prioritizes alerts

Container: `alzcare-alertmanager` (port 9093)

### 3. Prometheus Alert Rules 📋
8 automated alerts monitoring:

| Alert | Condition | Severity |
|-------|-----------|----------|
| **ServiceDown** | Service unreachable >2min | 🔴 CRITICAL |
| **HighMemoryUsage** | Heap >85% for >5min | 🟡 WARNING |
| **HighRequestLatency** | p95 latency >1s for >5min | 🟡 WARNING |
| **HighErrorRate** | >5% errors for >5min | 🟡 WARNING |
| **MySQLDown** | DB unreachable >2min | 🔴 CRITICAL |
| **HighDatabaseConnections** | >80% connection pool used | 🟡 WARNING |
| **SlowQueryRate** | Slow queries detected | 🟡 WARNING |

### 4. Enhanced Dashboard 📊
Added to your Grafana dashboard:

**Top Row (Status Gauges)**
- Lost Item Service Status (Green=UP, Red=DOWN)
- Followup Alert Service Status (Green=UP, Red=DOWN)
- MySQL Database Status (Green=UP, Red=DOWN)

**Middle Rows (Existing + New)**
- JVM Memory (both services)
- HTTP Requests/sec (both services)
- Request Latency (both services)
- MySQL Connections - Active vs Max
- MySQL Slow Queries - Over time
- Active Alerts Status - Pie chart of firing alerts

## How to Deploy

From WSL terminal:

```bash
cd /mnt/c/Users/jaafe/Downloads/Esprit-PI-4SAE6-2026-mind-care-amenaazizfarah

# Stop old services
docker-compose down

# Start with MySQL exporter and AlertManager
docker-compose up -d
```

Wait 30 seconds for services to start, then:

```bash
bash restart-monitoring.sh
```

## Accessing the System

| Service | URL | Purpose |
|---------|-----|---------|
| Grafana | http://localhost:3000 | View all metrics & alerts |
| Prometheus | http://localhost:9090 | Query metrics directly |
| AlertManager | http://localhost:9093 | View & manage alerts |
| MySQL Exporter | http://localhost:9104/metrics | Raw database metrics |

## Viewing Alerts in Grafana

1. Go to http://localhost:3000
2. Open "Mind Care Services Monitoring" dashboard
3. Scroll to bottom → "Active Alerts Status" pie chart
4. Check the status gauges at top (RED = alert triggered)

## Prometheus Alert Rules Location

File: `monitoring/prometheus-rules.yml`
- Edit to adjust thresholds (memory %, latency limits, etc)
- Changes take effect after `docker-compose restart prometheus`

## AlertManager Configuration

File: `monitoring/alertmanager.yml`
- Currently uses webhook receiver (localhost:5000)
- Can be configured for:
  - 📧 Email notifications
  - 💬 Slack webhooks
  - 📱 PagerDuty integration
  - 📞 OpsGenie/Opsgenie
  - Custom webhooks

## Metrics Now Available

### Service Metrics
- `up{job="lost-item-service"}` - Service UP (1) or DOWN (0)
- `up{job="followup-alert-service"}` - Service UP/DOWN
- `http_server_requests_seconds_*` - Request metrics
- `jvm_memory_used_bytes` - Memory usage
- `process_*` - Process metrics

### Database Metrics
- `mysql_up` - Database UP/DOWN
- `mysql_global_status_threads_connected` - Active connections
- `mysql_global_variables_max_connections` - Max connections allowed
- `mysql_global_status_slow_queries` - Slow query count
- `mysql_global_status_innodb_*` - InnoDB metrics

## Next Steps (Optional)

1. **Configure Alert Notifications**
   - Edit `monitoring/alertmanager.yml`
   - Add email, Slack, or PagerDuty receiver
   - Restart AlertManager: `docker-compose restart alertmanager`

2. **Fine-tune Alert Thresholds**
   - Edit `monitoring/prometheus-rules.yml`
   - Adjust `> 0.85` for memory, `> 1` for latency, etc
   - Restart Prometheus: `docker-compose restart prometheus`

3. **Create Custom Dashboards**
   - Use metrics explorer to find new metrics
   - Create additional panels for specific queries
   - Save as new dashboards

## Troubleshooting

**MySQL exporter not appearing in Prometheus targets**
- Wait 30 seconds for targets to refresh
- Check: http://localhost:9090/targets
- Restart: `docker-compose restart mysql-exporter`

**No alerts firing**
- Check Prometheus rules loaded: http://localhost:9090/rules
- Verify alert conditions are being met (check metric values)
- Check AlertManager: http://localhost:9093

**AlertManager not receiving alerts**
- Verify AlertManager is running: `docker ps | grep alertmanager`
- Check prometheus.yml has correct AlertManager URL
- Restart: `docker-compose restart prometheus alertmanager`
