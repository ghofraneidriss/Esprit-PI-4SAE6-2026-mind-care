# Monitoring and Jenkins Setup

This stack uses the official Docker Hub images:

- `prom/prometheus:latest`
- `grafana/grafana:latest`

## Start the monitoring stack

From the repository root:

```powershell
docker compose -f volunteer/docker-compose-monitoring.yml up -d
```

Grafana will be available at `http://localhost:3000`.
Prometheus will be available at `http://localhost:9090`.

Default Grafana credentials:

- Username: `admin`
- Password: `admin`

## Connect Jenkins to Prometheus

Install this Jenkins plugin:

- `Prometheus metrics`

After installation, Jenkins exposes metrics at:

- `http://localhost:8080/prometheus/`

This repository already configures Prometheus to scrape that endpoint using:

- job: `jenkins`
- target: `host.docker.internal:8080`
- path: `/prometheus/`

If your Jenkins runs on a different host port, update `volunteer/docker/prometheus/prometheus.yml`.

## Credentials

No credentials are required if the Jenkins Prometheus endpoint is accessible without authentication.

If Jenkins protects the endpoint, create:

1. A Jenkins user or API token with read access.
2. A Prometheus scrape config using `basic_auth`.

Example:

```yaml
  - job_name: 'jenkins'
    metrics_path: '/prometheus/'
    static_configs:
      - targets: ['host.docker.internal:8080']
    basic_auth:
      username: 'your-jenkins-user'
      password: 'your-api-token'
```

## Jenkins pipeline notes

Your pipeline in `volunteer/Jenkinsfile` already starts the monitoring stack with:

```groovy
docker-compose -f docker-compose-monitoring.yml up -d
```

If `docker-compose` is not installed on the Jenkins agent, replace it with:

```groovy
docker compose -f docker-compose-monitoring.yml up -d
```
