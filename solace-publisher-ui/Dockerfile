FROM node:20-bookworm-slim AS build

WORKDIR /workspace
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_BROKER_API_BASE_URL=http://localhost:8081
ENV VITE_BROKER_API_BASE_URL=${VITE_BROKER_API_BASE_URL}
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80
