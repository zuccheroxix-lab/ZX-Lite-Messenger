import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { v4 as uuidv4 } from 'uuid';
import { z } from 'zod';
import { authenticate } from '../middleware/auth.js';
import { db, memoryStore, UserStatusRow } from '../db/db.js';
import { checkFriendship } from './friends.js';
import { wsManager } from '../websocket/server.js';

const createStatusSchema = z.object({
  text: z.string().min(1).max(500)
});

export async function updateRoutes(fastify: FastifyInstance) {
  // Get active updates (from user and their friends)
  fastify.get('/updates', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const myUserId = request.user!.userId;
    const now = new Date();

    if (db.isPostgres()) {
      const res = await db.getPool()!.query(
        `SELECT s.id, s.text, s.created_at AS "createdAt", s.expires_at AS "expiresAt",
                u.id AS "userId", u.username, u.display_name AS "displayName", u.avatar_url AS "avatarUrl"
         FROM user_statuses s
         INNER JOIN users u ON u.id = s.user_id
         WHERE s.expires_at > $1 
           AND (
             s.user_id = $2 
             OR s.user_id IN (
               SELECT CASE WHEN f.user1_id = $2 THEN f.user2_id ELSE f.user1_id END 
               FROM friendships f 
               WHERE f.user1_id = $2 OR f.user2_id = $2
             )
           )
         ORDER BY s.created_at DESC`,
        [now, myUserId]
      );

      const list = res.rows.map(r => ({
        id: r.id,
        text: r.text,
        createdAt: new Date(r.createdAt).getTime(),
        expiresAt: new Date(r.expiresAt).getTime(),
        user: {
          id: r.userId,
          username: r.username,
          displayName: r.displayName,
          avatarUrl: r.avatarUrl
        }
      }));

      return reply.send(list);
    } else {
      const list = [];
      for (const s of memoryStore.userStatuses.values()) {
        if (s.expires_at > now) {
          const isMe = s.user_id === myUserId;
          const isFriend = isMe || await checkFriendship(myUserId, s.user_id);
          if (isFriend) {
            const u = memoryStore.users.get(s.user_id);
            if (u) {
              list.push({
                id: s.id,
                text: s.text,
                createdAt: new Date(s.created_at).getTime(),
                expiresAt: new Date(s.expires_at).getTime(),
                user: {
                  id: u.id,
                  username: u.username,
                  displayName: u.display_name,
                  avatarUrl: u.avatar_url
                }
              });
            }
          }
        }
      }
      list.sort((a, b) => b.createdAt - a.createdAt);
      return reply.send(list);
    }
  });

  // Post new update
  fastify.post('/updates', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = createStatusSchema.safeParse(request.body);
    if (!parse.success) {
      return reply.status(400).send({ error: 'Valid status text required' });
    }

    const myUserId = request.user!.userId;
    const { text } = parse.data;
    const now = new Date();
    const expiresAt = new Date(now.getTime() + 24 * 3600 * 1000);
    const id = uuidv4();

    if (db.isPostgres()) {
      await db.getPool()!.query(
        'INSERT INTO user_statuses (id, user_id, text, created_at, expires_at) VALUES ($1, $2, $3, $4, $5)',
        [id, myUserId, text, now, expiresAt]
      );
    } else {
      memoryStore.userStatuses.set(id, {
        id,
        user_id: myUserId,
        text,
        created_at: now,
        expires_at: expiresAt
      });
    }

    const myUser = memoryStore.users.get(myUserId);
    const result = {
      id,
      text,
      createdAt: now.getTime(),
      expiresAt: expiresAt.getTime(),
      user: {
        id: myUserId,
        username: request.user!.username,
        displayName: myUser?.display_name || request.user!.username,
        avatarUrl: myUser?.avatar_url || null
      }
    };

    return reply.status(201).send(result);
  });
}
