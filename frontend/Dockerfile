FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

FROM nginx:1.29-alpine
# envsubst only replaces these variables, leaving nginx's own $variables untouched.
ENV NGINX_ENVSUBST_FILTER="^(CUSTOMER|PRODUCT|CART)_SERVICE_URL$"
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 8080
