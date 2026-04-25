pipeline {
    agent any

    tools {
        git 'Default'
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        GITHUB_REPO = 'https://github.com/ghofraneidriss/Esprit-PI-4SAE6-2026-mind-care.git'
        GITHUB_BRANCH = 'volunteer'
        SONARQUBE_HOST_URL = 'http://sonarqube:9000'
        SONARQUBE_LOGIN = credentials('sonarqube-token')
    }

    stages {
        
        stage('Checkout Code') {
            steps {
                echo '📥 Cloning repository...'
                git branch: "${GITHUB_BRANCH}",
                    url: "${GITHUB_REPO}"
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo '🔍 Running SonarQube code analysis...'
                sh '''
                    # Analyze medical_report_service
                    cd medical_report_service
                    sonar-scanner \
                        -Dsonar.projectKey=medical-report-service \
                        -Dsonar.projectName="Medical Report Service" \
                        -Dsonar.sources=src \
                        -Dsonar.host.url=${SONARQUBE_HOST_URL} \
                        -Dsonar.login=${SONARQUBE_LOGIN}
                    cd ..

                    # Analyze volunteer service
                    cd volunteer
                    sonar-scanner \
                        -Dsonar.projectKey=volunteer-service \
                        -Dsonar.projectName="Volunteer Service" \
                        -Dsonar.sources=src \
                        -Dsonar.host.url=${SONARQUBE_HOST_URL} \
                        -Dsonar.login=${SONARQUBE_LOGIN}
                    cd ..
                '''
            }
        }

        stage('Build Docker Images') {
            steps {
                echo '🐳 Building Docker images...'
                sh '''
                    cd medical_report_service
                    docker build -t ${DOCKER_REGISTRY}/ghofrane/medical-report-service:${BUILD_NUMBER} . --progress=plain
                    docker tag ${DOCKER_REGISTRY}/ghofrane/medical-report-service:${BUILD_NUMBER} ${DOCKER_REGISTRY}/ghofrane/medical-report-service:latest
                    cd ..
                    
                    cd volunteer
                    docker build -t ${DOCKER_REGISTRY}/ghofrane/volunteer-service:${BUILD_NUMBER} . --progress=plain
                    docker tag ${DOCKER_REGISTRY}/ghofrane/volunteer-service:${BUILD_NUMBER} ${DOCKER_REGISTRY}/ghofrane/volunteer-service:latest
                    cd ..
                '''
            }
        }

        stage('Push to Docker Hub') {
            when {
                branch 'volunteer'
            }
            steps {
                echo '📤 Pushing images to Docker Hub...'
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USR', passwordVariable: 'DOCKER_PSW')]) {
                    sh '''
                        echo $DOCKER_PSW | docker login -u $DOCKER_USR --password-stdin
                        docker push ${DOCKER_REGISTRY}/ghofrane/medical-report-service:${BUILD_NUMBER}
                        docker push ${DOCKER_REGISTRY}/ghofrane/medical-report-service:latest
                        docker push ${DOCKER_REGISTRY}/ghofrane/volunteer-service:${BUILD_NUMBER}
                        docker push ${DOCKER_REGISTRY}/ghofrane/volunteer-service:latest
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy Services') {
            when {
                branch 'volunteer'
            }
            steps {
                echo '🚀 Deploying services...'
                sh '''
                    docker rm -f medical-report-service volunteer-service 2>/dev/null || true
                    docker run -d -p 8081:8080 --name medical-report-service ${DOCKER_REGISTRY}/ghofrane/medical-report-service:latest
                    docker run -d -p 8082:8080 --name volunteer-service ${DOCKER_REGISTRY}/ghofrane/volunteer-service:latest
                '''
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
    }
}
