FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --prefer-offline
COPY . .
RUN npm run build -- --configuration production

FROM nginx:alpine
COPY --from=builder /app/dist/billing-app-frontend /usr/share/nginx/html
EXPOSE 80
