import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { v4 as uuidv4 } from 'uuid';
import { z } from 'zod';
import { config } from '../config.js';
import { db, memoryStore, UserRow } from '../db/db.js';

export interface AuthTokenPayload {
  userId: string;
  username: string;
}

const registerSchema = z.object({
  username: z.string().min(3).max(32).regex(/^[a-zA-Z0-9_]+$/),
  displayName: z.string().min(1).max(64),
  password: z.string().min(6).max(128),
  phoneNumber: z.string().max(32).optional()
});

const loginSchema = z.object({
  username: z.string().min(3),
  password: z.string().min(6)
});

const refreshSchema = z.object({
  refreshToken: z.string().min(10)
});

export async function authRoutes(fastify: FastifyInstance) {
  // Register
  fastify.post('/auth/register', async (request: FastifyRequest, reply: FastifyReply) => {
    const parseResult = registerSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.status(400).send({
        error: 'Validation failed: ' + parseResult.error.errors.map(e => e.message).join(', ')
      });
    }

    const { username, displayName, password, phoneNumber } = parseResult.data;
    const cleanUsername = username.toLowerCase();
    const cleanPhone = phoneNumber ? phoneNumber.replace(/[^0-9+]/g, '') : null;

    // Check duplicate
    if (db.isPostgres()) {
      const existing = await db.getPool()!.query('SELECT id FROM users WHERE username = $1', [cleanUsername]);
      if (existing.rows.length > 0) {
        return reply.status(409).send({ error: 'Username is already taken' });
      }
    } else {
      for (const u of memoryStore.users.values()) {
        if (u.username.toLowerCase() === cleanUsername) {
          return reply.status(409).send({ error: 'Username is already taken' });
        }
      }
    }

    const passwordHash = await bcrypt.hash(password, config.bcryptRounds);
    const userId = uuidv4();
    const now = new Date();

    const user: UserRow = {
      id: userId,
      username: cleanUsername,
      display_name: displayName,
      password_hash: passwordHash,
      phone_number: cleanPhone,
      phone_verified: false,
      bio: 'Available on ZX Lite',
      avatar_url: null,
      created_at: now,
      updated_at: now
    };

    if (db.isPostgres()) {
      await db.getPool()!.query(
        'INSERT INTO users (id, username, display_name, password_hash, phone_number, phone_verified, bio, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)',
        [user.id, user.username, user.display_name, user.password_hash, user.phone_number, user.phone_verified, user.bio, user.created_at, user.updated_at]
      );
    } else {
      memoryStore.users.set(userId, user);
    }

    const accessToken = generateAccessToken(user.id, user.username);
    const refreshToken = uuidv4();
    await saveRefreshToken(user.id, refreshToken);

    return reply.status(201).send({
      accessToken,
      refreshToken,
      user: {
        id: user.id,
        username: user.username,
        displayName: user.display_name,
        avatarUrl: user.avatar_url,
        bio: user.bio,
        phoneNumber: user.phone_number,
        createdAt: user.created_at.getTime()
      }
    });
  });

  // Login
  fastify.post('/auth/login', async (request: FastifyRequest, reply: FastifyReply) => {
    const parseResult = loginSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.status(400).send({ error: 'Invalid login payload' });
    }

    const { username, password } = parseResult.data;
    const cleanUsername = username.toLowerCase();

    let user: UserRow | null = null;
    if (db.isPostgres()) {
      const res = await db.getPool()!.query('SELECT * FROM users WHERE username = $1', [cleanUsername]);
      if (res.rows.length > 0) user = res.rows[0];
    } else {
      for (const u of memoryStore.users.values()) {
        if (u.username.toLowerCase() === cleanUsername) {
          user = u;
          break;
        }
      }
    }

    if (!user) {
      return reply.status(401).send({ error: 'Invalid username or password' });
    }

    const match = await bcrypt.compare(password, user.password_hash);
    if (!match) {
      return reply.status(401).send({ error: 'Invalid username or password' });
    }

    const accessToken = generateAccessToken(user.id, user.username);
    const refreshToken = uuidv4();
    await saveRefreshToken(user.id, refreshToken);

    return reply.send({
      accessToken,
      refreshToken,
      user: {
        id: user.id,
        username: user.username,
        displayName: user.display_name,
        avatarUrl: user.avatar_url,
        createdAt: new Date(user.created_at).getTime()
      }
    });
  });

  // Refresh
  fastify.post('/auth/refresh', async (request: FastifyRequest, reply: FastifyReply) => {
    const parseResult = refreshSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.status(400).send({ error: 'Refresh token required' });
    }

    const { refreshToken } = parseResult.data;
    const session = await findRefreshToken(refreshToken);
    if (!session || new Date(session.expiresAt) < new Date()) {
      return reply.status(401).send({ error: 'Invalid or expired refresh token' });
    }

    let user: UserRow | null = null;
    if (db.isPostgres()) {
      const res = await db.getPool()!.query('SELECT * FROM users WHERE id = $1', [session.userId]);
      if (res.rows.length > 0) user = res.rows[0];
    } else {
      user = memoryStore.users.get(session.userId) || null;
    }

    if (!user) {
      return reply.status(401).send({ error: 'User not found' });
    }

    const newAccessToken = generateAccessToken(user.id, user.username);
    const newRefreshToken = uuidv4();
    await deleteRefreshToken(refreshToken);
    await saveRefreshToken(user.id, newRefreshToken);

    return reply.send({
      accessToken: newAccessToken,
      refreshToken: newRefreshToken
    });
  });

  // Logout
  fastify.post('/auth/logout', async (request: FastifyRequest, reply: FastifyReply) => {
    return reply.send({ success: true });
  });
}

function generateAccessToken(userId: string, username: string): string {
  return jwt.sign(
    { userId, username } as AuthTokenPayload,
    config.jwtSecret,
    { expiresIn: '7d' }
  );
}

async function saveRefreshToken(userId: string, token: string): Promise<void> {
  const expiresAt = new Date(Date.now() + config.refreshTokenExpiresDays * 24 * 3600 * 1000);
  const tokenHash = token; // Can be hashed or unique UUID lookup

  if (db.isPostgres()) {
    await db.getPool()!.query(
      'INSERT INTO refresh_sessions (id, user_id, token_hash, expires_at) VALUES ($1, $2, $3, $4)',
      [uuidv4(), userId, tokenHash, expiresAt]
    );
  } else {
    memoryStore.refreshSessions.set(tokenHash, {
      id: uuidv4(),
      userId,
      tokenHash,
      expiresAt
    });
  }
}

async function findRefreshToken(token: string) {
  if (db.isPostgres()) {
    const res = await db.getPool()!.query(
      'SELECT id, user_id as "userId", token_hash as "tokenHash", expires_at as "expiresAt" FROM refresh_sessions WHERE token_hash = $1',
      [token]
    );
    return res.rows[0] || null;
  } else {
    return memoryStore.refreshSessions.get(token) || null;
  }
}

async function deleteRefreshToken(token: string): Promise<void> {
  if (db.isPostgres()) {
    await db.getPool()!.query('DELETE FROM refresh_sessions WHERE token_hash = $1', [token]);
  } else {
    memoryStore.refreshSessions.delete(token);
  }
}
