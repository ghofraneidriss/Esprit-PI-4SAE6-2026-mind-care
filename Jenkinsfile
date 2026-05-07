pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    stages {
        stage('Cleanup') {
            steps {
                sh 'find . -name "report-task.txt" -delete || true'
            }
        }

        stage('Checkout') {
            steps {
                retry(3) {
                    checkout([$class: 'GitSCM',
                        branches: [[name: '*/Amena-Work']],
                        extensions: [
                            [$class: 'CloneOption',
                                depth: 1,
                                shallow: true,
                                noTags: true,
                                timeout: 60,
                                honorRefspec: true
                            ],
                            [$class: 'CheckoutOption', timeout: 60]
                        ],
                        userRemoteConfigs: [[
                            url: 'https://github.com/ghofraneidriss/Esprit-PI-4SAE6-2026-mind-care.git'
                        ]]
                    ])
                }
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
                dir('server/activities_service') {
                    sh 'mvn org.jacoco:jacoco-maven-plugin:0.8.11:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.11:report -Dmaven.test.failure.ignore=true'
                }
                dir('server/movement_service') {
                    sh 'mvn org.jacoco:jacoco-maven-plugin:0.8.11:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.11:report -Dmaven.test.failure.ignore=true'
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
                withSonarQubeEnv('SonarQube') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        dir('server/activities_service') {
                            sh '''
                                mvn sonar:sonar \
                                -Dsonar.projectKey=activities-service \
                                -Dsonar.projectName='Activities Service' \
                                -Dsonar.login=$SONAR_TOKEN \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                            '''
                        }
                        dir('server/movement_service') {
                            sh '''
                                mvn sonar:sonar \
                                -Dsonar.projectKey=movement-service \
                                -Dsonar.projectName='Movement Service' \
                                -Dsonar.login=$SONAR_TOKEN \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                            '''
                        }
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    try {
                        timeout(time: 5, unit: 'MINUTES') {
                            waitForQualityGate abortPipeline: false
                        }
                    } catch (Exception e) {
                        echo "⚠️ Quality Gate timeout, continuing anyway."
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
                    sh '''
                        echo "$HARBOR_PASS" | docker login 172.21.37.7:8085 -u "$HARBOR_USER" --password-stdin

                        docker build -t 172.21.37.7:8085/mindcare/activities-service:1.0 server/activities_service/
                        docker push 172.21.37.7:8085/mindcare/activities-service:1.0

                        docker build -t 172.21.37.7:8085/mindcare/movement-service:1.0 server/movement_service/
                        docker push 172.21.37.7:8085/mindcare/movement-service:1.0
                    '''
                }
            }
        }

    }  // closes stages

    post {
        success {
            echo '✅ Pipeline complet : Build, Test, Sonar, Docker OK!'
        }
        failure {
            echo '❌ ÉCHEC : Le pipeline a planté. Vérifie les logs.'
        }
    }

}  // closes pipeline
