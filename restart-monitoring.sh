#!/bin/bash

set -e

echo "🔄 Restarting monitoring services..."
echo ""

cd "$(dirname "$0")"

echo "📊 Restarting Prometheus..."
docker-compose restart prometheus
sleep 3

echo "📈 Restarting Grafana..."
docker-compose restart grafana
sleep 5

echo ""
echo "✅ Monitoring services restarted successfully!"
echo ""
echo "Access Grafana at: http://localhost:3000"
echo "  - Username: admin"
echo "  - Password: admin (or your custom password)"
echo ""
echo "The 'Mind Care Services Monitoring' dashboard should now be available."
echo ""
