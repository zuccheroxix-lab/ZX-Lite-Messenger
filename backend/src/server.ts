import Fastify, { FastifyInstance } from 'fastify';
import cors from '@fastify/cors';
import rateLimit from '@fastify/rate-limit';
import fastifyWebsocket from '@fastify/websocket';
import { config } from './config.js';
import { authRoutes } from './routes/auth.js';
import { userRoutes } from './routes/users.js';
import { conversationRoutes } from './routes/conversations.js';
import { friendRoutes } from './routes/friends.js';
import { contactRoutes } from './routes/contacts.js';
import { updateRoutes } from './routes/updates.js';
import { wsManager } from './websocket/server.js';

export function buildServer(): FastifyInstance {
  const fastify = Fastify({
    logger: {
      level: config.nodeEnv === 'development' ? 'info' : 'warn',
      redact: ['headers.authorization', 'req.headers.authorization', 'password', 'body.password']
    }
  });

  // CORS for web clients / development
  fastify.register(cors, {
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS']
  });

  // Rate Limiting
  fastify.register(rateLimit, {
    max: config.rateLimitMax,
    timeWindow: config.rateLimitWindowMs
  });

  // WebSocket support
  fastify.register(fastifyWebsocket);

  fastify.register(async (instance) => {
    instance.get('/ws', { websocket: true }, (connection, req) => {
      const url = req.url || '';
      const queryIndex = url.indexOf('?');
      let token = '';
      if (queryIndex !== -1) {
        const queryParams = new URLSearchParams(url.substring(queryIndex));
        token = queryParams.get('token') || '';
      }
      wsManager.handleConnection(connection.socket, token);
    });
  });

  // Health check
  fastify.get('/health', async () => {
    return {
      status: 'ok',
      service: 'ZX Lite Messenger Backend',
      timestamp: Date.now()
    };
  });

  // App routes
  fastify.register(authRoutes);
  fastify.register(userRoutes);
  fastify.register(conversationRoutes);
  fastify.register(friendRoutes);
  fastify.register(contactRoutes);
  fastify.register(updateRoutes);

  return fastify;
}
