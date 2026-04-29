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
                sh 'mvn clean package -DskipTests -f backoffice/pom.xml'
            }
        }

        stage('Tests & Couverture') {
            parallel {
                stage('Test Traitement') {
                    steps {
                        dir('backoffice/traitement_et_consultation') {
                            sh 'mvn test jacoco:report'
                        }
                    }
                }
                stage('Test Ordonnance') {
                    steps {
                        dir('backoffice/ordonnance_et_medicaments') {
                            sh 'mvn test jacoco:report'
                        }
                    }
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
        always {
            echo 'Pipeline terminé.'
        }
    }
}
