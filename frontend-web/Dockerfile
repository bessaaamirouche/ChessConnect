FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

FROM node:20-alpine
WORKDIR /app

# Copy the built application
COPY --from=build /app/dist/frontend-web ./dist/frontend-web

# Copy package.json for production dependencies
COPY --from=build /app/package*.json ./

# Install only production dependencies
RUN npm ci --omit=dev

EXPOSE 4000
ENV PORT=4000

CMD ["node", "dist/frontend-web/server/server.mjs"]
