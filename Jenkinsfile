cat > Jenkinsfile << 'EOF'
pipeline {
    agent any

    environment {
        KUBE_NS = "mindcare"
        REGISTRY = "mindcare"
    }

    stages {

        stage('Checkout') {
            steps {
                git url: 'https://github.com/ghofraneidriss/Esprit-PI-4SAE6-2026-mind-care.git',
                    branch: 'main'
            }
        }

        stage('Build tous les services') {
            steps {
                script {
                    def services = [
                        'Activities_service',
                        'api_gateway',
                        'eureka',
                        'forums_service',
                        'incident_service',
                        'localization_service',
                        'medical_report_service',
                        'movement_service',
                        'ordonnance_et_medicaments',
                        'recommendation_service',
                        'souvenir_service',
                        'traitement_et_consultation',
                        'users_service',
                        'volunteer'
                    ]
                    services.each { svc ->
                        if (fileExists("${svc}/Dockerfile")) {
                            echo "🔨 Building ${svc}..."
                            sh "docker build -t mindcare/${svc.toLowerCase()}:latest ./${svc}/"
                            echo "✅ ${svc} built"
                        }
                    }
                }
            }
        }

        stage('Deploy tous les services K8s') {
            steps {
                echo 'Deploying all services to K8s...'
                sh '''
                    kubectl apply -f k8s/ -n mindcare
                    kubectl rollout restart deployment -n mindcare
                '''
            }
        }

        stage('Vérifier les pods') {
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
                        curl -s http://localhost:8761/actuator/health | grep -q UP && echo "✅ Eureka UP" && break
                        echo "Waiting Eureka... ($i/12)"
                        sleep 10
                    done
                '''
            }
        }

        stage('Prometheus + Grafana') {
            steps {
                sh '''
                    sudo systemctl restart prometheus || true
                    sleep 5
                    curl -s http://localhost:9090/-/healthy && echo "✅ Prometheus OK" || echo "⚠️ Prometheus check failed"
                    curl -s http://localhost:3000/api/health && echo "✅ Grafana OK" || echo "⚠️ Grafana check failed"
                '''
            }
        }
    }

    post {
        success {
            echo '✅ CD Pipeline SUCCESS — tous les services déployés'
        }
        failure {
            echo '❌ CD Pipeline FAILED'
        }
    }
}
EOF
