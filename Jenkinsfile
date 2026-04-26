pipeline {
    agent any

    tools {
        maven 'Maven'   // configure in Jenkins (Global Tool Config)
        jdk 'Java17'    // configure in Jenkins
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_USERNAME = 'ghofrane'   // your Docker Hub username
        SONARQUBE_HOST_URL = 'http://localhost:9000'
    }

    stages {

        // ========================
        // 1. CHECKOUT
        // ========================
        stage('Checkout Code') {
            steps {
                echo '📥 Cloning repository...'
                git branch: 'volunteer',
                    url: 'https://github.com/ghofraneidriss/Esprit-PI-4SAE6-2026-mind-care.git'
            }
        }

        // ========================
        // 2. BUILD (JAR)
        // ========================
        stage('Build JARs') {
            steps {
                echo '⚙️ Building microservices...'
                sh '''
                    cd medical-report-service
                    mvn clean package -DskipTests
                    cd ..

                    cd volunteering-service
                    mvn clean package -DskipTests
                    cd ..
                '''
            }
        }

        // ========================
        // 3. TEST
        // ========================
        stage('Run Tests') {
            steps {
                echo '🧪 Running tests...'
                sh '''
                    cd medical-report-service
                    mvn test
                    cd ..

                    cd volunteering-service
                    mvn test
                    cd ..
                '''
            }
        }

        // ========================
        // 4. SONARQUBE
        // ========================
        stage('SonarQube Analysis') {
            steps {
                echo '🔍 Running SonarQube analysis...'
                withCredentials([string(credentialsId: 'SONAR_AUTH_TOKEN', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        cd medical-report-service
                        mvn sonar:sonar \
                          -Dsonar.projectKey=medical-report-service \
                          -Dsonar.host.url=${SONARQUBE_HOST_URL} \
                          -Dsonar.login=${SONAR_TOKEN}
                        cd ..

                        cd volunteering-service
                        mvn sonar:sonar \
                          -Dsonar.projectKey=volunteer-service \
                          -Dsonar.host.url=${SONARQUBE_HOST_URL} \
                          -Dsonar.login=${SONAR_TOKEN}
                        cd ..
                    '''
                }
            }
        }

        // ========================
        // 5. DOCKER BUILD
        // ========================
        stage('Build Docker Images') {
            steps {
                echo '🐳 Building Docker images...'
                sh '''
                    docker build -t ${DOCKER_USERNAME}/medical-report-service:${BUILD_NUMBER} ./medical-report-service
                    docker tag ${DOCKER_USERNAME}/medical-report-service:${BUILD_NUMBER} ${DOCKER_USERNAME}/medical-report-service:latest

                    docker build -t ${DOCKER_USERNAME}/volunteering-service:${BUILD_NUMBER} ./volunteering-service
                    docker tag ${DOCKER_USERNAME}/volunteering-service:${BUILD_NUMBER} ${DOCKER_USERNAME}/volunteering-service:latest
                '''
            }
        }

        // ========================
        // 6. PUSH DOCKER
        // ========================
        stage('Push to Docker Hub') {
            when {
                branch 'volunteer'
            }
            steps {
                echo '📤 Pushing images...'
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USR', passwordVariable: 'DOCKER_PSW')]) {
                    sh '''
                        echo $DOCKER_PSW | docker login -u $DOCKER_USR --password-stdin

                        docker push ${DOCKER_USERNAME}/medical-report-service:${BUILD_NUMBER}
                        docker push ${DOCKER_USERNAME}/medical-report-service:latest

                        docker push ${DOCKER_USERNAME}/volunteering-service:${BUILD_NUMBER}
                        docker push ${DOCKER_USERNAME}/volunteering-service:latest

                        docker logout
                    '''
                }
            }
        }

        // ========================
        // 7. DEPLOY (SIMULATION)
        // ========================
        stage('Deploy Services') {
            when {
                branch 'volunteer'
            }
            steps {
                echo '🚀 Deploying services...'
                sh '''
                    docker rm -f medical-report-service volunteering-service 2>/dev/null || true

                    docker run -d -p 8081:8080 --name medical-report-service \
                        ${DOCKER_USERNAME}/medical-report-service:latest

                    docker run -d -p 8082:8080 --name volunteering-service \
                        ${DOCKER_USERNAME}/volunteering-service:latest
                '''
            }
        }
    }

    // ========================
    // POST ACTIONS
    // ========================
    post {
        success {
            echo '✅ Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
    }
}