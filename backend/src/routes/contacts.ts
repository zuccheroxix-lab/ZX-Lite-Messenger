import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { z } from 'zod';
import { authenticate } from '../middleware/auth.js';
import { db, memoryStore } from '../db/db.js';
import { checkFriendship } from './friends.js';

const discoverContactsSchema = z.object({
  phoneNumbers: z.array(z.string().min(5).max(32)).max(100)
});

export async function contactRoutes(fastify: FastifyInstance) {
  // Contact Discovery with privacy safeguards and rate limiting
  fastify.post('/contacts/discover', {
    preHandler: [authenticate],
    config: {
      rateLimit: {
        max: 20, // Max 20 queries per minute to prevent enumeration attacks
        timeWindow: 60000
      }
    }
  }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = discoverContactsSchema.safeParse(request.body);
    if (!parse.success) {
      return reply.status(400).send({ error: 'Max 100 phone numbers allowed per batch' });
    }

    const myUserId = request.user!.userId;
    const rawNumbers = parse.data.phoneNumbers;

    // Normalize phone numbers (strip whitespace, hyphens, parentheses)
    const normalizedList = Array.from(new Set(
      rawNumbers.map(n => n.replace(/[^0-9+]/g, '')).filter(n => n.length >= 7)
    ));

    if (normalizedList.length === 0) {
      return reply.send({ matches: [] });
    }

    const matches: any[] = [];

    if (db.isPostgres()) {
      const res = await db.getPool()!.query(
        `SELECT u.id, u.username, u.display_name AS "displayName", u.avatar_url AS "avatarUrl", u.bio, u.phone_number AS "phoneNumber"
         FROM users u
         LEFT JOIN user_privacy_settings ups ON ups.user_id = u.id
         WHERE u.phone_number = ANY($1) 
           AND u.id != $2 
           AND (ups.allow_phone_discovery IS NULL OR ups.allow_phone_discovery = true)`,
        [normalizedList, myUserId]
      );

      for (const row of res.rows) {
        const isFriend = await checkFriendship(myUserId, row.id);
        matches.push({
          id: row.id,
          username: row.username,
          displayName: row.displayName,
          avatarUrl: row.avatarUrl,
          bio: row.bio,
          phoneNumber: row.phoneNumber,
          isFriend
        });
      }
    } else {
      for (const u of memoryStore.users.values()) {
        if (u.id === myUserId) continue;
        if (u.phone_number && normalizedList.includes(u.phone_number)) {
          const privacy = memoryStore.privacySettings.get(u.id);
          if (privacy && !privacy.allow_phone_discovery) continue;

          const isFriend = await checkFriendship(myUserId, u.id);
          matches.push({
            id: u.id,
            username: u.username,
            displayName: u.display_name,
            avatarUrl: u.avatar_url,
            bio: u.bio,
            phoneNumber: u.phone_number,
            isFriend
          });
        }
      }
    }

    return reply.send({ matches });
  });
}
