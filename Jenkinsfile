pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'jdk17'
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        IMAGE_NAME_BACK = 'ghofrane/medical-report-service'
        IMAGE_NAME_VOL = 'ghofrane/volunteer-service'
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_PROJECT_KEY = 'Mindcare_project'
        SONAR_PROJECT_NAME = 'Mindcare_project'
        SERVICE_NAME = 'volunteer'
        SERVICE_DIR = 'volunteer'
        SENTRY_ORG = 'ghofrane-i6'
        SENTRY_PROJECT = 'volunteer-service'
        SENTRY_ENVIRONMENT = 'test'
    }

    options {
        skipDefaultCheckout()
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Cloning repository...'
                git url: 'https://github.com/ghofraneidriss/Esprit-PI-4SAE6-2026-mind-care.git',
                    branch: 'volunteer'
            }
        }

        stage('Build Backend') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo 'Building services...'
                    sh '''
                        cd medical_report_service
                        mvn clean package -DskipTests
                        cd ..

                        cd volunteer
                        mvn clean package -DskipTests
                        cd ..
                    '''
                }
            }
        }

        stage('Run Tests') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo 'Running tests...'
                    sh '''
                        cd medical_report_service
                        mvn test
                        cd ..

                        cd volunteer
                        mvn test
                        cd ..
                    '''
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    withCredentials([string(credentialsId: 'sonar-token-mindcare', variable: 'SONAR_TOKEN')]) {
                        echo "[SONAR] $SERVICE_NAME"
                        sh '''
                            set -e
                            cd "$SERVICE_DIR"
                            mvn -B clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
                            -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
                            -Dsonar.projectName="$SONAR_PROJECT_NAME" \
                            -Dsonar.host.url="$SONAR_HOST_URL" \
                            -Dsonar.token="$SONAR_TOKEN" \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo 'Building Docker images...'
                    script {
                        if (sh(script: 'command -v docker >/dev/null 2>&1', returnStatus: true) != 0) {
                            error('Docker is not installed on this Jenkins agent.')
                        }

                        sh '''
                            docker build --pull=false -t $IMAGE_NAME_BACK:latest ./medical_report_service
                            docker build --pull=false -t $IMAGE_NAME_VOL:latest ./volunteer
                        '''

                        echo 'Docker images built successfully.'
                    }
                }
            }
        }

        stage('Push Images') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo 'Pushing images...'
                    script {
                        try {
                            withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                                sh '''
                                    echo $PASS | docker login -u $USER --password-stdin
                                    docker push $IMAGE_NAME_BACK:latest
                                    docker push $IMAGE_NAME_VOL:latest
                                    docker logout
                                '''
                            }
                        } catch (err) {
                            echo "Skipping Docker push: ${err.getMessage()}"
                        }
                    }
                }
            }
        }
        stage('Sentry Release') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    withCredentials([string(credentialsId: 'sentry-auth-token', variable: 'SENTRY_AUTH_TOKEN')]) {
                        script {
                            env.SENTRY_RELEASE = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        }

                        sh '''
                            docker run --rm \
                              -e SENTRY_AUTH_TOKEN="$SENTRY_AUTH_TOKEN" \
                              -e SENTRY_ORG="$SENTRY_ORG" \
                              -e SENTRY_PROJECT="$SENTRY_PROJECT" \
                              -v "$PWD:/work" -w /work \
                              getsentry/sentry-cli:2 releases new "$SENTRY_RELEASE"

                            docker run --rm \
                              -e SENTRY_AUTH_TOKEN="$SENTRY_AUTH_TOKEN" \
                              -e SENTRY_ORG="$SENTRY_ORG" \
                              -e SENTRY_PROJECT="$SENTRY_PROJECT" \
                              -v "$PWD:/work" -w /work \
                              getsentry/sentry-cli:2 releases set-commits "$SENTRY_RELEASE" --auto

                            docker run --rm \
                              -e SENTRY_AUTH_TOKEN="$SENTRY_AUTH_TOKEN" \
                              -e SENTRY_ORG="$SENTRY_ORG" \
                              -e SENTRY_PROJECT="$SENTRY_PROJECT" \
                              -v "$PWD:/work" -w /work \
                              getsentry/sentry-cli:2 releases finalize "$SENTRY_RELEASE"
                        '''
                    }
                }
            }
        }


        stage('Deploy (Simulation)') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo 'Deploying...'
                    script {
                        withCredentials([string(credentialsId: 'sentry-dsn', variable: 'SENTRY_DSN')]) {
                            sh '''
                                docker rm -f medical-report-service volunteer-service || true

                                docker run -d -p 8081:8080 --name medical-report-service $IMAGE_NAME_BACK:latest
                                docker run -d -p 8082:8085 --name volunteer-service \
                                  -e SPRING_PROFILES_ACTIVE=test \
                                  -e SENTRY_DSN="$SENTRY_DSN" \
                                  -e SENTRY_ENVIRONMENT="$SENTRY_ENVIRONMENT" \
                                  -e SENTRY_RELEASE="$SENTRY_RELEASE" \
                                  $IMAGE_NAME_VOL:latest
                            '''
                        }

                        echo 'Deployment simulation started.'
                    }
                }
            }
        }
        stage('Start Monitoring Stack') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo 'Starting Prometheus and Grafana...'
                    sh '''
                        cd volunteer
                        docker compose -f docker-compose-monitoring.yml up -d
                        sleep 10
                    '''
                    echo 'Monitoring stack started.'
                }
            }
        }

        stage('Health Check') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo 'Checking service health...'
                    sh '''
                        cd volunteer

                        # Wait for volunteer service to become healthy, and show logs if it exits.
                        # The volunteer app takes around 90-100s to boot in CI, so allow up to 3 minutes.
                        for i in $(seq 1 36); do
                          status="$(docker inspect -f '{{.State.Status}}' volunteer-service 2>/dev/null || true)"
                          if [ "$status" = "exited" ] || [ "$status" = "dead" ]; then
                            echo "Volunteer service container stopped unexpectedly."
                            docker logs volunteer-service || true
                            exit 1
                          fi

                          if curl -s http://localhost:8082/actuator/health | grep -q 'UP'; then
                            break
                          fi

                          echo "Waiting for volunteer service on 8082... ($i/36)"
                          sleep 5
                        done
                        curl -s http://localhost:8082/actuator/health | grep -q 'UP' || {
                          echo "Volunteer service health check failed."
                          docker ps -a || true
                          docker logs volunteer-service || true
                          exit 1
                        }

                        # Wait for Prometheus target to be healthy
                        for i in $(seq 1 12); do
                          curl -s http://localhost:9090/api/v1/targets | grep -q '"health":"up"' && break
                          echo "Waiting for Prometheus to scrape target... ($i/12)"
                          sleep 5
                        done
                        curl -s http://localhost:9090/api/v1/targets | grep -q '"health":"up"' || exit 1
                    '''
                }
            }
        }

        stage('Verify Metrics') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo 'Verifying Prometheus metrics...'
                    sh '''
                        curl -s http://localhost:9090/api/v1/query?query=up | grep -q "value" && echo "Metrics collected successfully"
                    '''
                }
            }
        }
        stage('Sentry Deploy Notify') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    withCredentials([string(credentialsId: 'sentry-auth-token', variable: 'SENTRY_AUTH_TOKEN')]) {
                        sh '''
                            docker run --rm \
                              -e SENTRY_AUTH_TOKEN="$SENTRY_AUTH_TOKEN" \
                              -e SENTRY_ORG="$SENTRY_ORG" \
                              -e SENTRY_PROJECT="$SENTRY_PROJECT" \
                              -v "$PWD:/work" -w /work \
                              getsentry/sentry-cli:2 releases deploys "$SENTRY_RELEASE" new -e "$SENTRY_ENVIRONMENT"
                        '''
                    }
                }
            }
        }

    }

    post {
        success {
            echo 'CI/CD PIPELINE SUCCESS'
        }
        failure {
            echo 'PIPELINE FAILED'
        }
    }
}
