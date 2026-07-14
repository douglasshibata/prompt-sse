# --- Build Stage ---
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package.json and package-lock.json
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci

# Copy configuration and source files
COPY angular.json tsconfig.app.json tsconfig.json tsconfig.spec.json ./
COPY public ./public
COPY src ./src

# Build the Angular application for production
RUN npm run build

# --- Run Stage ---
FROM nginx:1.25-alpine

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Copy build artifacts to Nginx default public directory
# Note: Angular 17+ uses dist/<project-name>/browser by default
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Run Nginx
CMD ["nginx", "-g", "daemon off;"]
