pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JDK17'
    }

    stages {
        stage('Checkout') {
            steps {
                // Récupération du code depuis GitHub
                checkout scm
            }
        }

        stage('Build & Compilation') {
            steps {
                // Compilation globale du dossier backoffice
                sh 'mvn clean install -DskipTests -f backoffice/pom.xml'
            }
        }

        stage('Tests & Couverture JaCoCo') {
            steps {
                // Exécution des tests et génération des rapports de couverture
                // On le fait sur le POM parent pour traiter tous les modules
                sh 'mvn test jacoco:report -f backoffice/pom.xml'
            }
        }

        stage('Analyse SonarQube') {
            steps {
                // Analyse de la qualité du code
                // 'SonarQube' doit être le nom configuré dans l'administration de Jenkins
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -f backoffice/pom.xml sonar:sonar \
                        -Dsonar.projectKey=mind-care-backend \
                        -Dsonar.projectName=MindCare-Backoffice \
                        -Dsonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml'
                }
            }
        }

        stage('Construction Docker') {
            parallel {
                stage('Docker Traitement') {
                    steps {
                        dir('backoffice/traitement_et_consultation') {
                            sh 'docker build -t mindcare-traitement:latest .'
                        }
                    }
                }
                stage('Docker Ordonnance') {
                    steps {
                        dir('backoffice/ordonnance_et_medicaments') {
                            sh 'docker build -t mindcare-ordonnance:latest .'
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline réussi ! Le code est testé, analysé et packagé dans Docker.'
        }
        failure {
            echo 'Le pipeline a échoué. Vérifiez les logs Sonar ou les tests unitaires.'
        }
        always {
            echo 'Fin du traitement du pipeline.'
        }
    }
}