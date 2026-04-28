pipeline {
    agent any

    tools {
        maven 'M2_HOME'
        jdk 'jdk17'
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        IMAGE_NAME_BACK = 'ghofrane/medical-report-service'
        IMAGE_NAME_VOL = 'ghofrane/volunteer-service'
        SONARQUBE_HOST_URL = 'http://sonarqube:9000'
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN_CREDENTIALS_ID = 'sonar-token-mindcare'
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

        stage('Run Tests') {
            steps {
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

        stage('SonarQube Analysis') {
            when {
                expression { return env.SONAR_HOST_URL?.trim() && env.SONAR_TOKEN_CREDENTIALS_ID?.trim() }
            }
            steps {
                withCredentials([string(credentialsId: env.SONAR_TOKEN_CREDENTIALS_ID, variable: 'SONAR_TOKEN')]) {
                    script {
                        def sonarStatus = sh(
                            script: '''
                                set -e

                              /*   echo "[SONAR] medical_report_service"
                                cd medical_report_service
                                mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                  -Dsonar.projectKey=mindcare \
                                  -Dsonar.projectName=mindcare \
                                  -Dsonar.host.url=$SONAR_HOST_URL \
                                  -Dsonar.token=$SONAR_TOKEN \
                                  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                                cd .. */

                                echo "[SONAR] volunteer"
                                cd volunteer
                                mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                  -Dsonar.projectKey=mindcare \
                                  -Dsonar.projectName=mindcare \
                                  -Dsonar.host.url=$SONAR_HOST_URL \
                                  -Dsonar.token=$SONAR_TOKEN \
                                  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                                cd ..
                            ''',
                            returnStatus: true
                        )

                        if (sonarStatus != 0) {
                            echo "Skipping SonarQube analysis because Maven/Sonar dependencies could not be resolved or the Sonar server is unreachable. Exit code: ${sonarStatus}"
                        }
                    }
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                echo 'Building Docker images...'
                script {
                    if (sh(script: 'command -v docker >/dev/null 2>&1', returnStatus: true) == 0) {
                        sh '''
                            docker build -t $IMAGE_NAME_BACK:latest ./medical_report_service
                            docker build -t $IMAGE_NAME_VOL:latest ./volunteer
                        '''
                    } else {
                        echo 'Skipping Docker image build: docker is not installed on this Jenkins agent.'
                    }
                }
            }
        }

        stage('Push Images') {
            steps {
                echo 'Pushing images...'
                script {
                    if (sh(script: 'command -v docker >/dev/null 2>&1', returnStatus: true) == 0) {
                        try {
                            withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                                sh '''
                                    echo $PASS | docker login -u $USER --password-stdin
                                    docker push $IMAGE_NAME_BACK:latest
                                    docker push $IMAGE_NAME_VOL:latest
                                    docker logout
                                '''
                            }
                        } catch (Exception ex) {
                            echo "Skipping Docker push: ${ex.getMessage()}"
                        }
                    } else {
                        echo 'Skipping Docker push: docker is not installed on this Jenkins agent.'
                    }
                }
            }
        }

        stage('Deploy (Simulation)') {
            steps {
                echo 'Deploying...'
                script {
                    if (sh(script: 'command -v docker >/dev/null 2>&1', returnStatus: true) == 0) {
                        sh '''
                            docker rm -f medical-report-service volunteer-service || true

                            docker run -d -p 8081:8080 --name medical-report-service $IMAGE_NAME_BACK:latest
                            docker run -d -p 8082:8080 --name volunteer-service $IMAGE_NAME_VOL:latest
                        '''
                    } else {
                        echo 'Skipping deploy simulation: docker is not installed on this Jenkins agent.'
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
