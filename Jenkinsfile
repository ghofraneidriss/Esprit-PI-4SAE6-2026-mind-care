pipeline {
    agent any

    environment {
        BRANCH_NAME_TARGET = 'khaoula-integration-globale'
        DOCKER_IMAGE_ORDONNANCE = 'mindcare-ordonnance:latest'
        DOCKER_IMAGE_TRAITEMENT = 'mindcare-traitement:latest'
    }

    parameters {
        booleanParam(
            name: 'RUN_DOCKER_CD',
            defaultValue: false,
            description: 'Cocher seulement quand Jenkins a acces a Docker pour construire et deployer les conteneurs.'
        )
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Runtime Jenkins') {
            steps {
                sh 'java -version'
            }
        }

        stage('CI microservices backend') {
            parallel {
                stage('CI ordonnance et medicaments') {
                    steps {
                        dir('backoffice/ordonnance_et_medicaments') {
                            sh 'chmod +x mvnw || true'
                            sh './mvnw -B clean verify jacoco:report'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'backoffice/ordonnance_et_medicaments/target/surefire-reports/*.xml'
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'backoffice/ordonnance_et_medicaments/target/site/jacoco/**'
                        }
                    }
                }

                stage('CI traitement et consultation') {
                    steps {
                        dir('backoffice/traitement_et_consultation') {
                            sh 'chmod +x mvnw || true'
                            sh './mvnw -B clean verify jacoco:report'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'backoffice/traitement_et_consultation/target/surefire-reports/*.xml'
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'backoffice/traitement_et_consultation/target/site/jacoco/**'
                        }
                    }
                }
            }
        }

        stage('SonarQube backend') {
            parallel {
                stage('Sonar ordonnance et medicaments') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            dir('backoffice/ordonnance_et_medicaments') {
                                sh './mvnw -B sonar:sonar -Dsonar.projectKey=mindcare-ordonnance -Dsonar.projectName="MindCare Ordonnance Medicaments" -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
                            }
                        }
                    }
                }

                stage('Sonar traitement et consultation') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            dir('backoffice/traitement_et_consultation') {
                                sh './mvnw -B sonar:sonar -Dsonar.projectKey=mindcare-traitement-consultation -Dsonar.projectName="MindCare Traitement Consultation" -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
                            }
                        }
                    }
                }
            }
        }

        stage('Build Docker images') {
            when {
                expression { params.RUN_DOCKER_CD }
            }
            steps {
                sh 'docker build -f backoffice/ordonnance_et_medicaments/Dockerfile -t $DOCKER_IMAGE_ORDONNANCE backoffice'
                sh 'docker build -f backoffice/traitement_et_consultation/Dockerfile -t $DOCKER_IMAGE_TRAITEMENT backoffice'
            }
        }

        stage('CD backend global') {
            when {
                expression { params.RUN_DOCKER_CD }
            }
            steps {
                sh '''
                    if docker compose version >/dev/null 2>&1; then
                      docker compose -f docker-compose.yml up -d --build mysql ordonnance-service traitement-service prometheus grafana
                    else
                      docker-compose -f docker-compose.yml up -d --build mysql ordonnance-service traitement-service prometheus grafana
                    fi
                '''
            }
        }
    }

    post {
        success {
            echo 'Pipeline Khaoula OK: tests, couverture, SonarQube, images Docker et deploiement global.'
        }
        failure {
            echo 'Pipeline Khaoula en echec. Consulter les logs Jenkins.'
        }
    }
}
