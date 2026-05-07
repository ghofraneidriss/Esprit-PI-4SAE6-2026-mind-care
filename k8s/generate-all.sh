#!/bin/bash

declare -A SERVICES=(
  ["eureka"]="8761"
  ["api-gateway"]="8080"
  ["forums-service"]="8082"
  ["incident-service"]="8083"
  ["medical-report-service"]="8083"
  ["ordonnance-et-medicaments"]="8083"
  ["users-service"]="8082"
  ["localization-service"]="8085"
  ["recommendation-service"]="8085"
  ["volunteer"]="8085"
  ["souvenir-service"]="8086"
  ["movement-service"]="8086"
  ["traitement-et-consultation"]="8089"
  ["activities-service"]="8084"
)

for SERVICE in "${!SERVICES[@]}"; do
  PORT=${SERVICES[$SERVICE]}

  cat > k8s/${SERVICE}.yaml << YAML
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${SERVICE}
  namespace: mindcare
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ${SERVICE}
  template:
    metadata:
      labels:
        app: ${SERVICE}
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "${PORT}"
    spec:
      containers:
        - name: ${SERVICE}
          image: mindcare/${SERVICE}:latest
          imagePullPolicy: Never
          ports:
            - containerPort: ${PORT}
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "docker"
            - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
              value: "http://eureka:8761/eureka"
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "300m"
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: ${PORT}
            initialDelaySeconds: 40
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: ${PORT}
            initialDelaySeconds: 60
            periodSeconds: 15
---
apiVersion: v1
kind: Service
metadata:
  name: ${SERVICE}
  namespace: mindcare
spec:
  selector:
    app: ${SERVICE}
  ports:
    - protocol: TCP
      port: ${PORT}
      targetPort: ${PORT}
  type: ClusterIP
YAML

  echo "✅ ${SERVICE}.yaml généré"
done

# api-gateway en NodePort pour accès externe
sed -i 's/type: ClusterIP/type: NodePort/' k8s/api-gateway.yaml
sed -i 's/targetPort: 8080/targetPort: 8080\n      nodePort: 30080/' k8s/api-gateway.yaml

# eureka en NodePort pour accès externe
sed -i 's/type: ClusterIP/type: NodePort/' k8s/eureka.yaml
sed -i 's/targetPort: 8761/targetPort: 8761\n      nodePort: 30761/' k8s/eureka.yaml

echo ""
echo "✅ Tous les YAMLs générés dans k8s/"
echo "👉 Pour déployer : kubectl apply -f k8s/ -n mindcare"
