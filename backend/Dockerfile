# Stage 1: Build the Admin Panel (Frontend)
FROM node:20-alpine AS frontend-builder
WORKDIR /app_build
# Copy frontend dependency files
COPY admin-panel/package*.json ./admin-panel/
# Install frontend deps
WORKDIR /app_build/admin-panel
RUN npm ci
# Copy frontend source code
COPY admin-panel/ ./
# Build the frontend
# (This puts files into /app_build/public/admin based on your vite.config.js)
RUN npm run build

# Stage 2: Build the Backend API
FROM node:20-alpine
WORKDIR /app

# Copy backend dependency files
COPY package*.json ./
# Install backend deps (only production)
RUN npm ci --only=production

# Copy backend source code
COPY . .

# COPY THE BUILT FRONTEND from Stage 1 into the backend's public folder
COPY --from=frontend-builder /app_build/public/admin ./public/admin

# Expose port
EXPOSE 3000

# Start command
CMD ["npm", "start"]
