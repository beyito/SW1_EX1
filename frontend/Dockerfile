# syntax=docker/dockerfile:1.7
FROM node:20-alpine AS build
WORKDIR /workspace

COPY frontend/package*.json frontend/
COPY frontend/joint-plus.tgz frontend/
RUN cd frontend && npm ci

COPY frontend/ frontend/
RUN cd frontend && npm run build

FROM nginx:1.27-alpine
COPY infra/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/dist/flow-web/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
