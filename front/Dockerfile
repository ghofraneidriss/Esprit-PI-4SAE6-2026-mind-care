# Stage 1: Build
FROM node:18-alpine AS builder

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

RUN npm run build -- --configuration production

# Stage 2: Serve with Nginx
FROM nginx:alpine

COPY --from=builder /app/dist/front /usr/share/nginx/html

COPY front/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
