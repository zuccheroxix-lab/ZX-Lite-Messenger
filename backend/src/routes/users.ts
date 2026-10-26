import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { v4 as uuidv4 } from 'uuid';
import { z } from 'zod';
import { authenticate } from '../middleware/auth.js';
import { db, memoryStore, UserRow } from '../db/db.js';

const profileUpdateSchema = z.object({
  displayName: z.string().min(1).max(64).optional(),
  bio: z.string().max(255).optional(),
  phoneNumber: z.string().max(32).optional()
});

const privacySchema = z.object({
  autoAcceptMatchedContacts: z.boolean().optional(),
  allowPhoneDiscovery: z.boolean().optional(),
  readReceiptsEnabled: z.boolean().optional()
});

const blockSchema = z.object({
  targetUserId: z.string().uuid()
});

const fcmTokenSchema = z.object({
  token: z.string().min(10),
  deviceId: z.string().min(1)
});

export async function userRoutes(fastify: FastifyInstance) {
  // Search users by username or display name
  fastify.get('/users/search', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const { q } = request.query as { q?: string };
    const query = (q || '').trim().toLowerCase();

    if (!query) {
      return reply.send([]);
    }

    if (db.isPostgres()) {
      const res = await db.getPool()!.query(
        `SELECT id, username, display_name as "displayName", avatar_url as "avatarUrl", bio, created_at as "createdAt"
         FROM users 
         WHERE LOWER(username) LIKE $1 OR LOWER(display_name) LIKE $1
         LIMIT 20`,
        [`%${query}%`]
      );
      return reply.send(res.rows);
    } else {
      const results = [];
      for (const u of memoryStore.users.values()) {
        if (u.username.toLowerCase().includes(query) || u.display_name.toLowerCase().includes(query)) {
          results.push({
            id: u.id,
            username: u.username,
            displayName: u.display_name,
            avatarUrl: u.avatar_url,
            bio: u.bio || 'Available on ZX Lite',
            createdAt: new Date(u.created_at).getTime()
          });
        }
      }
      return reply.send(results.slice(0, 20));
    }
  });

  // Get My Profile
  fastify.get('/users/profile', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const myUserId = request.user!.userId;
    let user: UserRow | null = null;

    if (db.isPostgres()) {
      const res = await db.getPool()!.query('SELECT * FROM users WHERE id = $1', [myUserId]);
      if (res.rows.length > 0) user = res.rows[0];
    } else {
      user = memoryStore.users.get(myUserId) || null;
    }

    if (!user) return reply.status(404).send({ error: 'User not found' });

    return reply.send({
      id: user.id,
      username: user.username,
      displayName: user.display_name,
      avatarUrl: user.avatar_url,
      bio: user.bio || 'Available on ZX Lite',
      phoneNumber: user.phone_number || null,
      phoneVerified: user.phone_verified || false,
      createdAt: new Date(user.created_at).getTime()
    });
  });

  // Update My Profile
  fastify.put('/users/profile', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = profileUpdateSchema.safeParse(request.body);
    if (!parse.success) return reply.status(400).send({ error: 'Invalid profile data' });

    const myUserId = request.user!.userId;
    const { displayName, bio, phoneNumber } = parse.data;

    let cleanPhone: string | null | undefined = phoneNumber ? phoneNumber.replace(/[^0-9+]/g, '') : undefined;

    if (db.isPostgres()) {
      await db.getPool()!.query(
        `UPDATE users 
         SET display_name = COALESCE($1, display_name),
             bio = COALESCE($2, bio),
             phone_number = COALESCE($3, phone_number),
             updated_at = NOW()
         WHERE id = $4`,
        [displayName || null, bio || null, cleanPhone || null, myUserId]
      );
    } else {
      const u = memoryStore.users.get(myUserId);
      if (u) {
        if (displayName) u.display_name = displayName;
        if (bio) u.bio = bio;
        if (cleanPhone) u.phone_number = cleanPhone;
        u.updated_at = new Date();
      }
    }

    return reply.send({ success: true, message: 'Profile updated' });
  });

  // Get Privacy Settings
  fastify.get('/users/privacy', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const myUserId = request.user!.userId;

    if (db.isPostgres()) {
      const res = await db.getPool()!.query(
        `SELECT auto_accept_matched_contacts AS "autoAcceptMatchedContacts",
                allow_phone_discovery AS "allowPhoneDiscovery",
                read_receipts_enabled AS "readReceiptsEnabled"
         FROM user_privacy_settings WHERE user_id = $1`,
        [myUserId]
      );
      if (res.rows.length > 0) return reply.send(res.rows[0]);
    } else {
      const s = memoryStore.privacySettings.get(myUserId);
      if (s) {
        return reply.send({
          autoAcceptMatchedContacts: s.auto_accept_matched_contacts,
          allowPhoneDiscovery: s.allow_phone_discovery,
          readReceiptsEnabled: s.read_receipts_enabled
        });
      }
    }

    return reply.send({
      autoAcceptMatchedContacts: false,
      allowPhoneDiscovery: true,
      readReceiptsEnabled: true
    });
  });

  // Update Privacy Settings
  fastify.put('/users/privacy', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = privacySchema.safeParse(request.body);
    if (!parse.success) return reply.status(400).send({ error: 'Invalid privacy payload' });

    const myUserId = request.user!.userId;
    const { autoAcceptMatchedContacts, allowPhoneDiscovery, readReceiptsEnabled } = parse.data;

    const autoAccept = autoAcceptMatchedContacts !== undefined ? autoAcceptMatchedContacts : false;
    const phoneDiscovery = allowPhoneDiscovery !== undefined ? allowPhoneDiscovery : true;
    const readReceipts = readReceiptsEnabled !== undefined ? readReceiptsEnabled : true;

    if (db.isPostgres()) {
      await db.getPool()!.query(
        `INSERT INTO user_privacy_settings (user_id, auto_accept_matched_contacts, allow_phone_discovery, read_receipts_enabled, updated_at)
         VALUES ($1, $2, $3, $4, NOW())
         ON CONFLICT (user_id) DO UPDATE SET
            auto_accept_matched_contacts = EXCLUDED.auto_accept_matched_contacts,
            allow_phone_discovery = EXCLUDED.allow_phone_discovery,
            read_receipts_enabled = EXCLUDED.read_receipts_enabled,
            updated_at = NOW()`,
        [myUserId, autoAccept, phoneDiscovery, readReceipts]
      );
    } else {
      memoryStore.privacySettings.set(myUserId, {
        user_id: myUserId,
        auto_accept_matched_contacts: autoAccept,
        allow_phone_discovery: phoneDiscovery,
        read_receipts_enabled: readReceipts,
        updated_at: new Date()
      });
    }

    return reply.send({
      success: true,
      autoAcceptMatchedContacts: autoAccept,
      allowPhoneDiscovery: phoneDiscovery,
      readReceiptsEnabled: readReceipts
    });
  });

  // Block User
  fastify.post('/users/block', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = blockSchema.safeParse(request.body);
    if (!parse.success) return reply.status(400).send({ error: 'Valid targetUserId required' });

    const myUserId = request.user!.userId;
    const { targetUserId } = parse.data;

    if (myUserId === targetUserId) {
      return reply.status(400).send({ error: 'Cannot block yourself' });
    }

    const id = uuidv4();
    const now = new Date();

    if (db.isPostgres()) {
      await db.getPool()!.query(
        'INSERT INTO blocked_users (id, blocker_id, blocked_id, created_at) VALUES ($1, $2, $3, $4) ON CONFLICT DO NOTHING',
        [id, myUserId, targetUserId, now]
      );
    } else {
      memoryStore.blockedUsers.set(`${myUserId}_${targetUserId}`, {
        id,
        blocker_id: myUserId,
        blocked_id: targetUserId,
        created_at: now
      });
    }

    return reply.send({ success: true, message: 'User blocked' });
  });

  // Unblock User
  fastify.post('/users/unblock', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = blockSchema.safeParse(request.body);
    if (!parse.success) return reply.status(400).send({ error: 'Valid targetUserId required' });

    const myUserId = request.user!.userId;
    const { targetUserId } = parse.data;

    if (db.isPostgres()) {
      await db.getPool()!.query('DELETE FROM blocked_users WHERE blocker_id = $1 AND blocked_id = $2', [myUserId, targetUserId]);
    } else {
      memoryStore.blockedUsers.delete(`${myUserId}_${targetUserId}`);
    }

    return reply.send({ success: true, message: 'User unblocked' });
  });

  // List Blocked Users
  fastify.get('/users/blocked', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const myUserId = request.user!.userId;

    if (db.isPostgres()) {
      const res = await db.getPool()!.query(
        `SELECT u.id, u.username, u.display_name AS "displayName", u.avatar_url AS "avatarUrl", b.created_at AS "blockedAt"
         FROM blocked_users b
         INNER JOIN users u ON u.id = b.blocked_id
         WHERE b.blocker_id = $1
         ORDER BY b.created_at DESC`,
        [myUserId]
      );
      return reply.send(res.rows);
    } else {
      const list = [];
      for (const b of memoryStore.blockedUsers.values()) {
        if (b.blocker_id === myUserId) {
          const u = memoryStore.users.get(b.blocked_id);
          if (u) {
            list.push({
              id: u.id,
              username: u.username,
              displayName: u.display_name,
              avatarUrl: u.avatar_url,
              blockedAt: new Date(b.created_at).getTime()
            });
          }
        }
      }
      return reply.send(list);
    }
  });

  // Register / update FCM push token
  fastify.post('/users/fcm-token', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = fcmTokenSchema.safeParse(request.body);
    if (!parse.success) return reply.status(400).send({ error: 'Valid token and deviceId required' });

    const myUserId = request.user!.userId;
    const { token, deviceId } = parse.data;
    const id = uuidv4();
    const now = new Date();

    if (db.isPostgres()) {
      await db.getPool()!.query(
        `INSERT INTO user_fcm_tokens (id, user_id, token, device_id, updated_at)
         VALUES ($1, $2, $3, $4, $5)
         ON CONFLICT (user_id, device_id) DO UPDATE SET token = EXCLUDED.token, updated_at = NOW()`,
        [id, myUserId, token, deviceId, now]
      );
    } else {
      memoryStore.userFcmTokens.set(`${myUserId}_${deviceId}`, {
        id,
        user_id: myUserId,
        token,
        device_id: deviceId,
        updated_at: now
      });
    }

    return reply.send({ success: true, message: 'FCM token registered' });
  });

  // Delete FCM push token on logout
  fastify.delete('/users/fcm-token', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const myUserId = request.user!.userId;
    const { deviceId } = (request.query || {}) as { deviceId?: string };

    if (db.isPostgres()) {
      if (deviceId) {
        await db.getPool()!.query('DELETE FROM user_fcm_tokens WHERE user_id = $1 AND device_id = $2', [myUserId, deviceId]);
      } else {
        await db.getPool()!.query('DELETE FROM user_fcm_tokens WHERE user_id = $1', [myUserId]);
      }
    } else {
      for (const [key, t] of memoryStore.userFcmTokens.entries()) {
        if (t.user_id === myUserId && (!deviceId || t.device_id === deviceId)) {
          memoryStore.userFcmTokens.delete(key);
        }
      }
    }

    return reply.send({ success: true, message: 'FCM token cleared' });
  });
}
