pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    stages {
        stage('Cleanup') {
            steps {
                deleteDir()
            }
        }

        stage('Checkout') {
            steps {
                // Utilisation de la syntaxe avancée pour forcer le clone léger (Shallow Clone)
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/Amena-Work']],
                    extensions: [
                        [$class: 'CloneOption', depth: 1, shallow: true, noTags: true, timeout: 30],
                        [$class: 'CheckoutOption', timeout: 30]
                    ],
                    userRemoteConfigs: [[url: 'https://github.com/ghofraneidriss/Esprit-PI-4SAE6-2026-mind-care.git']]
                ])
            }
        }

        stage('Build & Install Parent') {
            steps {
                dir('server') {
                    sh 'mvn clean install -DskipTests'
                }
            }
        }

        stage('Test & Jacoco') {
            steps {
                // Execution parallèle ou séquentielle des tests
                dir('server/activities_service') {
                    sh 'mvn test org.jacoco:jacoco-maven-plugin:0.8.11:report -Dmaven.test.failure.ignore=true'
                }
                dir('server/movement_service') {
                    sh 'mvn test org.jacoco:jacoco-maven-plugin:0.8.11:report -Dmaven.test.failure.ignore=true'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'server/**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                // Vérifie que 'SonarQube' est le nom configuré dans Système -> SonarQube installations
                withSonarQubeEnv('SonarQube') {
                    dir('server/activities_service') {
                        sh "mvn sonar:sonar -Dsonar.projectKey=activities-service -Dsonar.projectName='Activities Service' -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml"
                    }
                    dir('server/movement_service') {
                        sh "mvn sonar:sonar -Dsonar.projectKey=movement-service -Dsonar.projectName='Movement Service' -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml"
                    }
                }
            }
        }

        stage('Docker Build & Push Harbor') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'harbor-creds',
                        usernameVariable: 'HARBOR_USER',
                        passwordVariable: 'HARBOR_PASS'
                )]) {
                    sh """
                        # Connexion au registre local Harbor
                        docker login localhost:8085 -u ${HARBOR_USER} -p ${HARBOR_PASS}

                        # Build et Push Service Activités
                        cd server/activities_service && docker build -t localhost:8085/mindcare/activities-service:1.0 .
                        docker push localhost:8085/mindcare/activities-service:1.0
                        cd ../..

                        # Build et Push Service Mouvement
                        cd server/movement_service && docker build -t localhost:8085/mindcare/movement-service:1.0 .
                        docker push localhost:8085/mindcare/movement-service:1.0
                    """
                }
            }
        }
    }

    post {
        success {
            echo '✅ TOUT EST VERT : Code récupéré, compilé, testé et poussé sur Harbor !'
        }
        failure {
            echo '❌ ÉCHEC : Vérifie les logs de Checkout ou les rapports de tests.'
        }
    }
}