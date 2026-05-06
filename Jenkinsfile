pipeline {
    agent any

    environment {
        BRANCH_NAME_TARGET = 'khaoula-integration-globale'
        DOCKERHUB_NAMESPACE = '121999121999'
        DOCKER_IMAGE_ORDONNANCE = '121999121999/mindcare-ordonnance'
        DOCKER_IMAGE_TRAITEMENT = '121999121999/mindcare-traitement'
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT_COMMIT = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
                }
            }
        }

        stage('Runtime Jenkins') {
            steps {
                sh 'java -version'
            }
        }

        stage('CI microservices backend') {
            parallel {
                stage('CI ordonnance et medicaments') {
                    steps {
                        dir('backoffice/ordonnance_et_medicaments') {
                            sh 'chmod +x mvnw || true'
                            sh './mvnw -B clean verify jacoco:report'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'backoffice/ordonnance_et_medicaments/target/surefire-reports/*.xml'
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'backoffice/ordonnance_et_medicaments/target/site/jacoco/**'
                        }
                    }
                }

                stage('CI traitement et consultation') {
                    steps {
                        dir('backoffice/traitement_et_consultation') {
                            sh 'chmod +x mvnw || true'
                            sh './mvnw -B clean verify jacoco:report'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'backoffice/traitement_et_consultation/target/surefire-reports/*.xml'
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'backoffice/traitement_et_consultation/target/site/jacoco/**'
                        }
                    }
                }
            }
        }

        stage('SonarQube backend') {
            parallel {
                stage('Sonar ordonnance et medicaments') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            dir('backoffice/ordonnance_et_medicaments') {
                                sh './mvnw -B sonar:sonar -Dsonar.projectKey=mindcare-ordonnance -Dsonar.projectName="MindCare Ordonnance Medicaments" -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
                            }
                        }
                    }
                }

                stage('Sonar traitement et consultation') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            dir('backoffice/traitement_et_consultation') {
                                sh './mvnw -B sonar:sonar -Dsonar.projectKey=mindcare-traitement-consultation -Dsonar.projectName="MindCare Traitement Consultation" -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
                            }
                        }
                    }
                }
            }
        }

        stage('Build Docker images') {
            steps {
                sh '''
                    docker build \
                      -f backoffice/ordonnance_et_medicaments/Dockerfile \
                      -t "$DOCKER_IMAGE_ORDONNANCE:${BUILD_NUMBER}" \
                      -t "$DOCKER_IMAGE_ORDONNANCE:${GIT_SHORT_COMMIT}" \
                      -t "$DOCKER_IMAGE_ORDONNANCE:latest" \
                      backoffice
                '''
                sh '''
                    docker build \
                      -f backoffice/traitement_et_consultation/Dockerfile \
                      -t "$DOCKER_IMAGE_TRAITEMENT:${BUILD_NUMBER}" \
                      -t "$DOCKER_IMAGE_TRAITEMENT:${GIT_SHORT_COMMIT}" \
                      -t "$DOCKER_IMAGE_TRAITEMENT:latest" \
                      backoffice
                '''
            }
        }

        stage('Push Docker Hub images') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    sh '''
                        echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USERNAME" --password-stdin
                        docker push "$DOCKER_IMAGE_ORDONNANCE:${BUILD_NUMBER}"
                        docker push "$DOCKER_IMAGE_ORDONNANCE:${GIT_SHORT_COMMIT}"
                        docker push "$DOCKER_IMAGE_ORDONNANCE:latest"
                        docker push "$DOCKER_IMAGE_TRAITEMENT:${BUILD_NUMBER}"
                        docker push "$DOCKER_IMAGE_TRAITEMENT:${GIT_SHORT_COMMIT}"
                        docker push "$DOCKER_IMAGE_TRAITEMENT:latest"
                    '''
                }
            }
        }

        stage('CD backend global') {
            steps {
                sh '''
                    docker pull "$DOCKER_IMAGE_ORDONNANCE:latest"
                    docker pull "$DOCKER_IMAGE_TRAITEMENT:latest"
                    if docker compose version >/dev/null 2>&1; then
                      docker compose -f docker-compose.yml -f devops/docker-compose.devops.yml up -d --no-build mysql traitement-service ordonnance-service prometheus grafana
                    else
                      docker-compose -f docker-compose.yml -f devops/docker-compose.devops.yml up -d --no-build mysql traitement-service ordonnance-service prometheus grafana
                    fi
                '''
            }
        }

        stage('Smoke tests backend') {
            steps {
                sh '''
                    curl -fsS http://traitement-service:8081/actuator/health
                    curl -fsS http://ordonnance-service:8083/actuator/health
                    curl -fsS http://prometheus:9090/-/ready
                    curl -fsS http://grafana:3000/api/health
                '''
            }
        }
    }

    post {
        success {
            echo 'Pipeline Khaoula OK: tests, couverture, SonarQube, images Docker et deploiement global.'
        }
        failure {
            echo 'Pipeline Khaoula en echec. Consulter les logs Jenkins.'
        }
    }
}
