pipeline {
    agent any

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
                dir('server') {
                    sh 'mvn clean compile -DskipTests'
                }
            }
        }

        stage('Test Forums Service') {
            steps {
                dir('server/forums_service') {
                    sh 'mvn test -Dtest=tn.esprit.forums_service.service.CategoryServiceTest -Dsurefire.failIfNoSpecifiedTests=false'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'server/forums_service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test Incident Service') {
            steps {
                dir('server/incident_service') {
                    sh 'mvn test -Dtest=tn.esprit.incident_service.service.IncidentServiceTest -Dsurefire.failIfNoSpecifiedTests=false'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'server/incident_service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                dir('server') {
                    sh 'mvn package -DskipTests'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t mindcare-alzheimer-forums:1.0 server/forums_service/'
                sh 'docker build -t mindcare-alzheimer-incident:1.0 server/incident_service/'
                sh 'docker tag mindcare-alzheimer-forums:1.0 mindcare-alzheimer-forums:latest'
                sh 'docker tag mindcare-alzheimer-incident:1.0 mindcare-alzheimer-incident:latest'
            }
        }

        stage('Deploy to Artifactory') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'artifactory-creds',
                    usernameVariable: 'ARTIF_USER',
                    passwordVariable: 'ARTIF_PASS'
                )]) {
                    dir('server') {
                        sh """
                            echo '<settings><servers><server><id>artifactory</id><username>\${ARTIF_USER}</username><password>\${ARTIF_PASS}</password></server></servers></settings>' > /tmp/settings.xml
                            mvn deploy -DskipTests \
                              --settings /tmp/settings.xml \
                              -DaltDeploymentRepository="artifactory::http://artifactory:8081/artifactory/libs-snapshot-local"
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            // Archive les fichiers JAR pour les voir dans l'interface Jenkins
            archiveArtifacts artifacts: 'server/**/*.jar', fingerprint: true
            echo '✅ Pipeline réussi — artifact déployé sur Artifactory !'
        }
        failure {
            echo '❌ Pipeline échoué — vérifier les logs.'
        }
    }
}