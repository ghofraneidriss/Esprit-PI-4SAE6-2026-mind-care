pipeline {
    agent any

    environment {
        ARTIFACTORY_URL = 'http://artifactory:8082/artifactory'
        ARTIF_USER      = 'admin'
        ARTIF_PASS      = credentials('artifactory-creds')
    }

    tools {
        maven 'Maven'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'Amena-Farah-Finale',
                    url: 'https://github.com/ghofraneidriss/Esprit-PI-4SAE6-2026-mind-care.git'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('Test') {
            steps {
                dir('forums_service') {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'forums_service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }

        stage('Deploy to Artifactory') {
            steps {
                sh """
                    mvn deploy \
                      -DskipTests \
                      -DaltDeploymentRepository=artifactory::default::${ARTIFACTORY_URL}/libs-snapshot-local \
                      -Dusername=${ARTIF_USER} \
                      -Dpassword=${ARTIF_PASS}
                """
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline réussi — artifact déployé sur Artifactory !'
        }
        failure {
            echo '❌ Pipeline échoué — vérifier les logs.'
        }
    }
}
