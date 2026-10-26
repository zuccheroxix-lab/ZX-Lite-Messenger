import { WebSocket } from 'ws';
import jwt from 'jsonwebtoken';
import { config } from '../config.js';
import { AuthTokenPayload } from '../routes/auth.js';

interface ConnectedClient {
  userId: string;
  username: string;
  ws: WebSocket;
}

class WebSocketManager {
  private clients = new Map<string, ConnectedClient>();

  handleConnection(ws: WebSocket, tokenOrQuery: string) {
    try {
      let token = tokenOrQuery;
      if (token.startsWith('token=')) {
        token = token.substring(6);
      }

      const decoded = jwt.verify(token, config.jwtSecret) as AuthTokenPayload;
      const client: ConnectedClient = {
        userId: decoded.userId,
        username: decoded.username,
        ws
      };

      this.clients.set(decoded.userId, client);
      console.log(`[WS] User connected: @${decoded.username} (${decoded.userId})`);

      ws.send(JSON.stringify({
        type: 'AUTH_OK',
        payload: { userId: decoded.userId, username: decoded.username }
      }));

      ws.on('message', (data: Buffer | string) => {
        try {
          const text = data.toString();
          const message = JSON.parse(text);
          this.handleClientMessage(client, message);
        } catch (err: any) {
          console.error('[WS] Error processing message:', err.message);
        }
      });

      ws.on('close', () => {
        this.clients.delete(decoded.userId);
        console.log(`[WS] User disconnected: @${decoded.username}`);
      });

      ws.on('error', (err) => {
        console.warn(`[WS] Client error (@${decoded.username}):`, err.message);
      });
    } catch (err: any) {
      console.warn('[WS] Authentication failed on WS connect:', err.message);
      ws.close(4001, 'Unauthorized');
    }
  }

  private handleClientMessage(client: ConnectedClient, msg: any) {
    if (msg.type === 'PING') {
      client.ws.send(JSON.stringify({ type: 'PONG', timestamp: Date.now() }));
    }
  }

  sendToUser(userId: string, type: string, payload: any): boolean {
    const client = this.clients.get(userId);
    if (client && client.ws.readyState === WebSocket.OPEN) {
      client.ws.send(JSON.stringify({ type, payload }));
      return true;
    }
    return false;
  }

  isUserOnline(userId: string): boolean {
    const client = this.clients.get(userId);
    return client !== undefined && client.ws.readyState === WebSocket.OPEN;
  }
}

export const wsManager = new WebSocketManager();
