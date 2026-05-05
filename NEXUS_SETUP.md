# Nexus Repository Manager Setup

## Access & Initial Setup

**Nexus URL**: http://localhost:8081
**Initial Login**: admin / admin123

### Step 1: Change Admin Password

1. Go to http://localhost:8081
2. Click "Sign in" → admin / admin123
3. Settings → Security → Users
4. Edit "admin" user → change password
5. Save

### Step 2: Configure Repositories

Nexus will auto-create default repositories, but we'll add custom ones for mind-care:

**Hosted Repository** (for our builds):
- Name: `mind-care-releases`
- Format: Maven
- Type: Hosted
- Version Policy: Release
- Deployment Policy: Allow redeploy (for dev)

**Proxy Repository** (caches Maven Central):
- Name: `maven-central-proxy`
- Format: Maven
- Type: Proxy
- Remote Storage: https://repo1.maven.org/maven2/

**Group Repository** (combines all):
- Name: `mind-care-all`
- Format: Maven
- Type: Group
- Members: mind-care-releases, maven-central-proxy

### Step 3: Create Jenkins User

In Nexus Admin:
1. Settings → Security → Users → Create User
2. Username: `jenkins`
3. Password: `nexus-jenkins-pwd`
4. Roles: `nx-admin` (or custom role with deployment rights)
5. Save

### Step 4: Maven Settings Configuration

Create ~/.m2/settings.xml on Jenkins machine:

```xml
<settings>
  <servers>
    <server>
      <id>mind-care-releases</id>
      <username>jenkins</username>
      <password>nexus-jenkins-pwd</password>
    </server>
  </servers>
  
  <profiles>
    <profile>
      <id>nexus</id>
      <repositories>
        <repository>
          <id>mind-care-all</id>
          <name>Mind Care Repositories</name>
          <url>http://localhost:8081/repository/mind-care-all/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>mind-care-all</id>
          <name>Mind Care Repositories</name>
          <url>http://localhost:8081/repository/mind-care-all/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  
  <activeProfiles>
    <activeProfile>nexus</activeProfile>
  </activeProfiles>
</settings>
```

### Step 5: Update Jenkins Pipelines

In both Jenkinsfiles, add Maven deployment step:

```groovy
stage('Deploy to Nexus') {
  steps {
    dir('server/lost-item-service') {
      sh 'mvn deploy -DskipTests -Dmaven.install.skip=true'
    }
  }
}
```

Also update pom.xml with distribution management:

```xml
<distributionManagement>
  <repository>
    <id>mind-care-releases</id>
    <name>Mind Care Releases</name>
    <url>http://localhost:8081/repository/mind-care-releases/</url>
  </repository>
  <snapshotRepository>
    <id>mind-care-releases</id>
    <name>Mind Care Snapshots</name>
    <url>http://localhost:8081/repository/mind-care-releases/</url>
  </snapshotRepository>
</distributionManagement>
```

### Step 6: Monitor in Grafana

Nexus exposes metrics at:
- http://localhost:8081/service/rest/v1/status (health)
- http://localhost:8081/service/rest/v1/repositories (list)

Create a Grafana panel to track:
- Artifacts uploaded
- Repository sizes
- Build history

### Artifact Flow

```
Code Commit
    ↓
Jenkins Build
    ↓
Maven Package
    ↓
Deploy to Nexus ← NEW!
    ↓
Docker Build (pulls from Nexus if needed)
    ↓
Deploy to Production
```

### Repository Structure

After setup, artifacts will be stored as:

```
http://localhost:8081/repository/mind-care-releases/
├── com/mindcare/lost-item-service/
│   ├── 1.0.0/
│   │   ├── lost-item-service-1.0.0.jar
│   │   └── lost-item-service-1.0.0.pom
│   └── 1.1.0/
│       └── lost-item-service-1.1.0.jar
└── com/mindcare/followup-alert-service/
    └── 1.0.0/
        └── followup-alert-service-1.0.0.jar
```

### Benefits

✅ **Version Control** - Every build artifact is versioned
✅ **Reusability** - Quickly deploy old versions if needed
✅ **Dependency Management** - Centralized artifact management
✅ **Disaster Recovery** - Backed-up artifacts for quick recovery
✅ **CI/CD Integration** - Seamless Jenkins → Nexus → Deploy pipeline
✅ **Team Sharing** - Other developers can pull artifacts locally
