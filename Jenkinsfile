pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JDK17'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Global') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Tests & Couverture (JaCoCo)') {
            parallel {
                stage('Test Traitement') {
                    steps {
                        dir('traitement_et_consultation') {
                            sh 'mvn test jacoco:report'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Test Ordonnance') {
                    steps {
                        dir('ordonnance_et_medicaments') {
                            sh 'mvn test jacoco:report'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                        }
                    }
                }
            }
        }

        stage('Analyse SonarQube') {
            parallel {
                stage('Sonar Traitement') {
                    steps {
                        dir('traitement_et_consultation') {
                            sh 'mvn sonar:sonar'
                        }
                    }
                }
                stage('Sonar Ordonnance') {
                    steps {
                        dir('ordonnance_et_medicaments') {
                            sh 'mvn sonar:sonar'
                        }
                    }
                }
            }
        }

        stage('Construction des Images Docker') {
            parallel {
                stage('Docker Traitement') {
                    steps {
                        dir('traitement_et_consultation') {
                            sh 'docker build -t mindcare-traitement:latest .'
                        }
                    }
                }
                stage('Docker Ordonnance') {
                    steps {
                        dir('ordonnance_et_medicaments') {
                            sh 'docker build -t mindcare-ordonnance:latest .'
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
            echo '✅ Pipeline réussi !'
        }
        failure {
            echo '❌ Pipeline échoué — Vérifiez les logs.'
        }
    }
}
