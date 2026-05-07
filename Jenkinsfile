pipeline {
    agent any

    environment {
        KUBE_NS = "mindcare"
    }

    stages {

        stage('Checkout') {
            steps {
                git url: 'https://github.com/ghofraneidriss/Esprit-PI-4SAE6-2026-mind-care.git',
                    branch: 'main'
            }
        }

        stage('Deploy tous les services K8s') {
            steps {
                echo 'Deploying all services to K3s...'
                sh '''
                    kubectl apply -f k8s/ -n mindcare
                    kubectl rollout restart deployment -n mindcare
                '''
            }
        }

        stage('Verifier les pods') {
            steps {
                sh '''
                    sleep 30
                    kubectl get pods -n mindcare
                    kubectl get services -n mindcare
                '''
            }
        }

        stage('Health Check Eureka') {
            steps {
                sh '''
                    for i in $(seq 1 12); do
                        curl -s http://localhost:8761/actuator/health | grep -q UP && break
                        echo "Waiting Eureka... ($i/12)"
                        sleep 10
                    done
                '''
            }
        }

        stage('Prometheus + Grafana') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    sh '''
                        docker compose -f docker-compose-monitoring.yml up -d || true
                        sleep 10
                        curl -s http://localhost:9090/api/v1/targets | grep -q health && echo "Prometheus OK" || echo "Prometheus not ready"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'CD Pipeline SUCCESS tous les services deployes'
        }
        failure {
            echo 'CD Pipeline FAILED'
        }
    }
}
