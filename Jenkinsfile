pipeline {
    agent any

    tools {
        git 'Default'
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        GITHUB_REPO = 'https://github.com/ghofraneidriss/Esprit-PI-4SAE6-2026-mind-care.git'
        GITHUB_BRANCH = 'volunteer'
    }

    stages {
        
        stage('Checkout Code') {
            steps {
                echo '📥 Cloning repository...'
                git branch: "${GITHUB_BRANCH}",
                    url: "${GITHUB_REPO}"
            }
        }

        stage('Build Medical Report Service') {
            steps {
                echo '🏗️ Building medical_report_service...'
                dir('medical_report_service') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Volunteer Service') {
            steps {
                echo '🏗️ Building volunteer service...'
                dir('volunteer') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                echo '🐳 Building Docker images...'
                sh '''
                    cd medical_report_service
                    docker build -t ${DOCKER_REGISTRY}/ghofrane/medical-report-service:${BUILD_NUMBER} .
                    docker tag ${DOCKER_REGISTRY}/ghofrane/medical-report-service:${BUILD_NUMBER} ${DOCKER_REGISTRY}/ghofrane/medical-report-service:latest
                    cd ..
                    
                    cd volunteer
                    docker build -t ${DOCKER_REGISTRY}/ghofrane/volunteer-service:${BUILD_NUMBER} .
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
                withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USR', passwordVariable: 'DOCKER_PSW')]) {
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
