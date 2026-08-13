FROM node:20.11.0-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY index.html tsconfig.json tsconfig.node.json vite.config.ts ./
COPY src ./src
RUN npm run build

FROM nginx:1.27.3-alpine
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
