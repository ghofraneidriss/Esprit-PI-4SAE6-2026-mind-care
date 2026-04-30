#!/bin/bash

# Docker Build and Push Script for MindCare Services
# Usage: ./build-and-push.sh <service-name> [registry-url] [version]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
SERVICE_NAME=${1:-lost-item-service}
REGISTRY=${2:-ghofraneidriss}
VERSION=${3:-latest}
IMAGE_NAME="${REGISTRY}/${SERVICE_NAME}:${VERSION}"
BUILD_CONTEXT="./server/${SERVICE_NAME}"

echo -e "${YELLOW}================================================${NC}"
echo -e "${YELLOW}Docker Build & Push - MindCare Services${NC}"
echo -e "${YELLOW}================================================${NC}"
echo -e "Service: ${GREEN}${SERVICE_NAME}${NC}"
echo -e "Image: ${GREEN}${IMAGE_NAME}${NC}"
echo -e "Build Context: ${GREEN}${BUILD_CONTEXT}${NC}"
echo -e "${YELLOW}================================================${NC}\n"

# Verify service directory exists
if [ ! -d "$BUILD_CONTEXT" ]; then
    echo -e "${RED}Error: Service directory not found at ${BUILD_CONTEXT}${NC}"
    exit 1
fi

# Verify Dockerfile exists
if [ ! -f "$BUILD_CONTEXT/Dockerfile" ]; then
    echo -e "${RED}Error: Dockerfile not found at ${BUILD_CONTEXT}/Dockerfile${NC}"
    exit 1
fi

# Step 1: Build Maven project
echo -e "${YELLOW}[1/4] Building Maven project...${NC}"
cd "$BUILD_CONTEXT"
mvn clean package -DskipTests -q
echo -e "${GREEN}[1/4] Maven build completed!${NC}\n"
cd - > /dev/null

# Step 2: Build Docker image
echo -e "${YELLOW}[2/4] Building Docker image: ${IMAGE_NAME}${NC}"
docker build \
    --tag "$IMAGE_NAME" \
    --tag "${REGISTRY}/${SERVICE_NAME}:latest" \
    --build-arg SERVICE_NAME="$SERVICE_NAME" \
    "$BUILD_CONTEXT"
echo -e "${GREEN}[2/4] Docker image built successfully!${NC}\n"

# Step 3: Verify image
echo -e "${YELLOW}[3/4] Verifying Docker image...${NC}"
docker inspect "$IMAGE_NAME" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    SIZE=$(docker images "$IMAGE_NAME" --format "{{.Size}}")
    echo -e "${GREEN}[3/4] Image verified! Size: ${SIZE}${NC}\n"
else
    echo -e "${RED}[3/4] Image verification failed!${NC}"
    exit 1
fi

# Step 4: Push to registry (optional)
if [ "$REGISTRY" != "local" ]; then
    echo -e "${YELLOW}[4/4] Pushing image to registry...${NC}"
    echo -e "${YELLOW}Note: Make sure you're logged in with: docker login${NC}"

    if docker push "$IMAGE_NAME"; then
        echo -e "${GREEN}[4/4] Image pushed successfully!${NC}\n"
        echo -e "${GREEN}================================================${NC}"
        echo -e "${GREEN}Build & Push completed successfully!${NC}"
        echo -e "${GREEN}================================================${NC}"
    else
        echo -e "${RED}[4/4] Failed to push image. Check your Docker credentials.${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}[4/4] Skipping push (local registry)${NC}"
    echo -e "${GREEN}================================================${NC}"
    echo -e "${GREEN}Build completed successfully!${NC}"
    echo -e "${GREEN}Image: ${IMAGE_NAME}${NC}"
    echo -e "${GREEN}================================================${NC}"
fi

# Summary
echo -e "\nUsage: docker run -p 8082:8080 ${IMAGE_NAME}"
echo -e "Or: docker-compose up lost-item-service"
