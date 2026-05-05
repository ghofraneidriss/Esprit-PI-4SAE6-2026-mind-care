#!/bin/bash

set -e

NEXUS_URL="http://localhost:8081"
NEXUS_USER="admin"
NEXUS_PASS="admin123"
JENKINS_USER="jenkins"
JENKINS_PASS="nexus-jenkins-pwd"

echo "⏳ Waiting for Nexus to be ready..."
until curl -s -f -u ${NEXUS_USER}:${NEXUS_PASS} ${NEXUS_URL}/service/rest/v1/status > /dev/null 2>&1; do
    echo "Waiting for Nexus..."
    sleep 5
done

echo "✅ Nexus is ready!"
echo ""

echo "📝 Creating Nexus repositories..."

# Create hosted repository for releases
curl -X POST \
  -u ${NEXUS_USER}:${NEXUS_PASS} \
  -H "Content-Type: application/json" \
  ${NEXUS_URL}/service/rest/v1/repositories/maven/hosted \
  -d '{
    "name": "mind-care-releases",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": true
    },
    "maven": {
      "versionPolicy": "RELEASE",
      "layoutPolicy": "STRICT"
    }
  }' 2>/dev/null || echo "Repository might already exist"

echo "✅ Hosted repository created"

# Create proxy repository for Maven Central
curl -X POST \
  -u ${NEXUS_USER}:${NEXUS_PASS} \
  -H "Content-Type: application/json" \
  ${NEXUS_URL}/service/rest/v1/repositories/maven/proxy \
  -d '{
    "name": "maven-central-proxy",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": true
    },
    "proxy": {
      "remoteUrl": "https://repo1.maven.org/maven2/",
      "contentMaxAge": 1440,
      "metadataMaxAge": 1440
    },
    "maven": {
      "versionPolicy": "RELEASE",
      "layoutPolicy": "STRICT"
    }
  }' 2>/dev/null || echo "Proxy repository might already exist"

echo "✅ Proxy repository created"

# Create group repository
curl -X POST \
  -u ${NEXUS_USER}:${NEXUS_PASS} \
  -H "Content-Type: application/json" \
  ${NEXUS_URL}/service/rest/v1/repositories/maven/group \
  -d '{
    "name": "mind-care-all",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": true
    },
    "group": {
      "memberNames": [
        "mind-care-releases",
        "maven-central-proxy"
      ]
    }
  }' 2>/dev/null || echo "Group repository might already exist"

echo "✅ Group repository created"
echo ""

echo "👤 Creating Jenkins user..."

# Create Jenkins user
curl -X POST \
  -u ${NEXUS_USER}:${NEXUS_PASS} \
  -H "Content-Type: application/json" \
  ${NEXUS_URL}/service/rest/v1/security/users \
  -d "{
    \"userId\": \"${JENKINS_USER}\",
    \"firstName\": \"Jenkins\",
    \"lastName\": \"CI\",
    \"email\": \"jenkins@mindcare.local\",
    \"password\": \"${JENKINS_PASS}\",
    \"roles\": [\"nx-admin\"]
  }" 2>/dev/null || echo "User might already exist"

echo "✅ Jenkins user created"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✨ Nexus Setup Complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📍 Access Nexus: http://localhost:8081"
echo "👤 Admin Login: ${NEXUS_USER} / ${NEXUS_PASS}"
echo "🔑 Jenkins Login: ${JENKINS_USER} / ${JENKINS_PASS}"
echo ""
echo "📦 Repositories Created:"
echo "  - mind-care-releases (Hosted)"
echo "  - maven-central-proxy (Proxy)"
echo "  - mind-care-all (Group)"
echo ""
echo "Next steps:"
echo "1. Access http://localhost:8081"
echo "2. Verify repositories in 'Repositories' menu"
echo "3. Add deployment step to Jenkins pipelines"
echo "4. Update service pom.xml files with distributionManagement"
