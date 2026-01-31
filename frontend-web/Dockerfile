FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

FROM node:20-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001 -G nodejs

# Copy the built application
COPY --from=build --chown=nodejs:nodejs /app/dist/frontend-web ./dist/frontend-web

# Copy package.json for production dependencies
COPY --from=build --chown=nodejs:nodejs /app/package*.json ./

# Install only production dependencies
RUN npm ci --omit=dev && \
    chown -R nodejs:nodejs /app

# Switch to non-root user
USER nodejs

EXPOSE 4000
ENV PORT=4000

CMD ["node", "dist/frontend-web/server/server.mjs"]
