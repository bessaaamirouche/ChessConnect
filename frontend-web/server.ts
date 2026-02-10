import { APP_BASE_HREF } from '@angular/common';
import { CommonEngine } from '@angular/ssr';
import express from 'express';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';
import http from 'node:http';
import bootstrap from './src/main.server';

// The Express app is exported so that it can be used by serverless Functions.
export function app(): express.Express {
  const server = express();
  const serverDistFolder = dirname(fileURLToPath(import.meta.url));
  const browserDistFolder = resolve(serverDistFolder, '../browser');
  const indexHtml = join(serverDistFolder, 'index.server.html');

  const commonEngine = new CommonEngine();

  server.set('view engine', 'html');
  server.set('views', browserDistFolder);

  // Security headers (SEO + security signal)
  server.use((req, res, next) => {
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'SAMEORIGIN');
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
    next();
  });

  // Proxy API requests to backend using native http
  const backendHost = process.env['API_URL']?.replace(/^https?:\/\//, '').replace(/\/api$/, '') || 'backend:8282';
  console.log(`Proxying /api requests to http://${backendHost}`);

  server.use('/api', (req, res) => {
    const isSse = req.originalUrl.includes('/notifications/stream');

    const options = {
      hostname: backendHost.split(':')[0],
      port: parseInt(backendHost.split(':')[1]) || 8282,
      path: req.originalUrl,
      method: req.method,
      headers: { ...req.headers, host: backendHost }
    };

    const proxyReq = http.request(options, (proxyRes) => {
      if (isSse) {
        // SSE: disable response timeout and buffering
        res.writeHead(proxyRes.statusCode || 500, {
          ...proxyRes.headers,
          'Cache-Control': 'no-cache',
          'X-Accel-Buffering': 'no'
        });
        res.flushHeaders();
      } else {
        res.writeHead(proxyRes.statusCode || 500, proxyRes.headers);
      }
      proxyRes.pipe(res);
    });

    if (isSse) {
      // SSE: disable socket timeouts to keep connection alive
      proxyReq.setTimeout(0);
      req.setTimeout(0);
      res.setTimeout(0);
      req.socket.setKeepAlive(true);

      // Clean up on client disconnect
      req.on('close', () => {
        proxyReq.destroy();
      });
    }

    proxyReq.on('error', (e) => {
      if (isSse && req.destroyed) return; // Expected on client disconnect
      console.error('Proxy error:', e);
      if (!res.headersSent) {
        res.status(502).send('Bad Gateway');
      }
    });

    req.pipe(proxyReq);
  });

  // Serve static files from /browser
  server.get('*.*', express.static(browserDistFolder, {
    maxAge: '1y'
  }));

  // All regular routes use the Angular engine
  server.get('*', (req, res, next) => {
    const { protocol, originalUrl, baseUrl, headers } = req;

    commonEngine
      .render({
        bootstrap,
        documentFilePath: indexHtml,
        url: `${protocol}://${headers.host}${originalUrl}`,
        publicPath: browserDistFolder,
        providers: [{ provide: APP_BASE_HREF, useValue: baseUrl }],
      })
      .then((html) => res.send(html))
      .catch((err) => next(err));
  });

  return server;
}

function run(): void {
  const port = process.env['PORT'] || 4000;

  // Start up the Node server
  const server = app();
  server.listen(port, () => {
    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

run();
