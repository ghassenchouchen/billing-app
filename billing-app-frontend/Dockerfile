FROM node:14-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install --legacy-peer-deps
COPY . .
RUN npm run build -- --configuration production

FROM nginx:alpine
COPY --from=builder /app/dist/factureProjet /usr/share/nginx/html
EXPOSE 80
