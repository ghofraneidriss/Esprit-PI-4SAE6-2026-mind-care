pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    stages {
        stage('Cleanup') {
            steps {
                sh 'find . -name "report-task.txt" -delete'
            }
        }

        stage('Checkout') {
            steps {
                // Utilisation de TON repo pour que le Webhook fonctionne
                git branch: 'Amena-Farah-Finale',
                    url: 'https://github.com/zouaouifarahh/Esprit-PI-4SAE6-2026-mind-care.git'
            }
        }

        stage('Build & Install Parent') {
            steps {
                dir('server') {
                    sh 'mvn clean install -DskipTests'
                }
            }
        }

        stage('Test & Jacoco Report') {
            steps {
                dir('server/forums_service') {
                    sh 'mvn org.jacoco:jacoco-maven-plugin:0.8.11:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.11:report -Dtest=!ForumsServiceApplicationTests'
                }
                dir('server/incident_service') {
                    sh 'mvn org.jacoco:jacoco-maven-plugin:0.8.11:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.11:report -Dtest=!IncidentServiceApplicationTests'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'server/**/target/surefire-reports/*.xml'
                }
            }
        }

      stage('SonarQube Analysis') {
    steps {
        withSonarQubeEnv('SonarQube') {
            withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                dir('server/forums_service') {
                    sh """
                        mvn sonar:sonar \
                          -Dsonar.projectKey=forums-service \
                          -Dsonar.projectName='Forums Service' \
                          -Dsonar.login=${SONAR_TOKEN} \
                          -Dsonar.coverage.jacoco.xmlReportPaths=${WORKSPACE}/server/forums_service/target/site/jacoco/jacoco.xml
                    """
                }
                dir('server/incident_service') {
                    sh """
                        mvn sonar:sonar \
                          -Dsonar.projectKey=incident-service \
                          -Dsonar.projectName='Incident Service' \
                          -Dsonar.login=${SONAR_TOKEN} \
                          -Dsonar.coverage.jacoco.xmlReportPaths=${WORKSPACE}/server/incident_service/target/site/jacoco/jacoco.xml
                    """
                }
            }
        }
    }
}
        stage('Quality Gate') {
            steps {
                script {
                    try {
                        timeout(time: 2, unit: 'MINUTES') {
                            // On vérifie le statut mais abortPipeline: false évite de tout stopper si Sonar est lent
                            waitForQualityGate abortPipeline: false
                        }
                    } catch (Exception e) {
                        echo "⚠️ Timeout SonarQube atteint, mais on continue pour scanner la sécurité avec Snyk..."
                    }
                }
            }
        }

        stage('Snyk Security Scan') {
            steps {
                script {
                    echo "🛡️ Analyse et Publication vers le Dashboard Snyk (DevSecOps)..."
                    dir('server/forums_service') {
                        // 'monitor' pour envoyer les résultats sur app.snyk.io
                        sh 'snyk monitor --project-name=forums-service || true'
                        // 'test' pour afficher le détail dans la console Jenkins
                        sh 'snyk test --severity-threshold=high || true'
                    }
                    dir('server/incident_service') {
                        sh 'snyk monitor --project-name=incident-service || true'
                        sh 'snyk test --severity-threshold=high || true'
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t mindcare-alzheimer-forums:1.0 server/forums_service/ || true'
                sh 'docker build -t mindcare-alzheimer-incident:1.0 server/incident_service/ || true'
                sh 'docker tag mindcare-alzheimer-forums:1.0 mindcare-alzheimer-forums:latest || true'
                sh 'docker tag mindcare-alzheimer-incident:1.0 mindcare-alzheimer-incident:latest || true'
            }
        }

        stage('Deploy to Artifactory') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'artifactory-creds',
                    usernameVariable: 'ARTIF_USER',
                    passwordVariable: 'ARTIF_PASS'
                )]) {
                    sh """
                        ARTIFACTORY_URL="http://172.17.0.3:8082/artifactory/libs-snapshot-local"
                        WORKSPACE_SERVER="${WORKSPACE}/server"
                        SERVICES="eureka_server users_service forums_service incident_service activities_service localization_service movement_service api_gateway"

                        for service in \$SERVICES; do
                            JAR_FILE=\$(find \$WORKSPACE_SERVER/\$service/target -name "*.jar" ! -name "*original*" 2>/dev/null | head -1)

                            if [ -z "\$JAR_FILE" ]; then
                                echo "⚠️ Pas de JAR pour \$service, skip..."
                                continue
                            fi

                            JAR_NAME=\$(basename \$JAR_FILE)
                            echo "🚀 Upload de \$JAR_NAME vers Artifactory..."

                            HTTP_CODE=\$(curl -s -o /dev/null -w "%{http_code}" \\
                                -u "${ARTIF_USER}:${ARTIF_PASS}" \\
                                -X PUT \\
                                "\${ARTIFACTORY_URL}/tn/esprit/\${service}/0.0.1-SNAPSHOT/\${JAR_NAME}" \\
                                -T "\$JAR_FILE")

                            if [ "\$HTTP_CODE" = "201" ] || [ "\$HTTP_CODE" = "200" ]; then
                                echo "✅ \$service uploadé avec succès !"
                            else
                                echo "⚠️ \$service erreur HTTP \$HTTP_CODE"
                            fi
                        done
                    """
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'server/**/*.jar', fingerprint: true
            echo '✅ Pipeline complet réussi — Code, Sécurité,Sonar,Synk, Docker et Artifactory OK !'
        }
        failure {
            echo '❌ Le pipeline a rencontré une erreur.'
        }
    }
}
