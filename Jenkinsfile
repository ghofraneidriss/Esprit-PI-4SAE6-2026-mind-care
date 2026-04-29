pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JDK17'
    }

    options {
        // Temps maximum d'exécution pour éviter les builds qui bloquent indéfiniment
        timeout(time: 1, unit: 'HOURS')
        // Garde les logs propres
        timestamps()
    }

    stages {
        stage('Initialisation & Cleanup') {
            steps {
                // Nettoie l'espace de travail pour éviter les conflits de fichiers corrompus
                deleteDir()
            }
        }

        stage('Checkout') {
            steps {
                script {
                    // On force des paramètres Git robustes pour les grosses archives
                    sh 'git config --global http.postBuffer 524288000'
                    sh 'git config --global http.version HTTP/1.1'
                }
                // Récupération du code avec un timeout étendu pour les connexions lentes
                checkout([$class: 'GitSCM',
                    branches: scm.branches,
                    extensions: scm.extensions + [
                        [$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: true, timeout: 30],
                        [$class: 'CheckoutOption', timeout: 30]
                    ],
                    userRemoteConfigs: scm.userRemoteConfigs
                ])
            }
        }

        stage('Build & Compilation') {
            steps {
                // Utilisation de -B (Batch mode) pour des logs Jenkins plus propres
                sh 'mvn -B clean install -DskipTests -f backoffice/pom.xml'
            }
        }

        stage('Tests & Couverture JaCoCo') {
            steps {
                // On s'assure que JaCoCo génère bien le XML pour SonarQube
                sh 'mvn -B test jacoco:report -f backoffice/pom.xml'
            }
        }

        stage('Analyse SonarQube') {
            steps {
                // 'SonarQube' doit correspondre au nom dans Administrer Jenkins > System
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B -f backoffice/pom.xml sonar:sonar \
                        -Dsonar.projectKey=mind-care-backend \
                        -Dsonar.projectName=MindCare-Backoffice \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
                }
            }
        }

        stage('Construction Docker') {
            steps {
                script {
                    // On lance les builds Docker
                    // Note: Assurez-vous que l'utilisateur 'jenkins' a les droits sudo docker ou appartient au groupe docker
                    dir('backoffice/traitement_et_consultation') {
                        sh 'docker build -t mindcare-traitement:latest .'
                    }
                    dir('backoffice/ordonnance_et_medicaments') {
                        sh 'docker build -t mindcare-ordonnance:latest .'
                    }
                }
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline réussi ! Le code est testé, analysé et packagé.'
        }
        failure {
            echo '❌ Le pipeline a échoué. Vérifiez les logs ci-dessus.'
        }
        always {
            echo '🏁 Fin du traitement du pipeline.'
        }
    }
}