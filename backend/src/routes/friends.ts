import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { v4 as uuidv4 } from 'uuid';
import { z } from 'zod';
import { authenticate } from '../middleware/auth.js';
import { db, memoryStore, FriendshipRequestRow, FriendshipRow, UserRow } from '../db/db.js';
import { wsManager } from '../websocket/server.js';

const sendFriendRequestSchema = z.object({
  targetUsername: z.string().optional(),
  targetPhoneNumber: z.string().optional()
}).refine(data => data.targetUsername || data.targetPhoneNumber, {
  message: "Either targetUsername or targetPhoneNumber must be provided"
});

const requestIdSchema = z.object({
  requestId: z.string().uuid()
});

export async function friendRoutes(fastify: FastifyInstance) {
  // 1. Send Friend Request (with mutual verified auto-accept support)
  fastify.post('/friends/request', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = sendFriendRequestSchema.safeParse(request.body);
    if (!parse.success) {
      return reply.status(400).send({ error: parse.error.errors[0].message });
    }

    const myUserId = request.user!.userId;
    const { targetUsername, targetPhoneNumber } = parse.data;

    // Find target user
    let targetUser: UserRow | null = null;
    if (db.isPostgres()) {
      if (targetUsername) {
        const res = await db.getPool()!.query('SELECT * FROM users WHERE username = $1', [targetUsername.trim().toLowerCase()]);
        if (res.rows.length > 0) targetUser = res.rows[0];
      } else if (targetPhoneNumber) {
        const res = await db.getPool()!.query('SELECT * FROM users WHERE phone_number = $1', [targetPhoneNumber.trim()]);
        if (res.rows.length > 0) targetUser = res.rows[0];
      }
    } else {
      for (const u of memoryStore.users.values()) {
        if (targetUsername && u.username.toLowerCase() === targetUsername.trim().toLowerCase()) {
          targetUser = u;
          break;
        }
        if (targetPhoneNumber && u.phone_number === targetPhoneNumber.trim()) {
          targetUser = u;
          break;
        }
      }
    }

    if (!targetUser) {
      return reply.status(404).send({ error: 'Target user not found' });
    }

    if (targetUser.id === myUserId) {
      return reply.status(400).send({ error: 'Cannot send friend request to yourself' });
    }

    // Check if blocked in either direction
    const isBlocked = await checkBlocked(myUserId, targetUser.id);
    if (isBlocked) {
      return reply.status(403).send({ error: 'Cannot send friend request: communication blocked' });
    }

    // Check if already friends
    const alreadyFriends = await checkFriendship(myUserId, targetUser.id);
    if (alreadyFriends) {
      return reply.status(409).send({ error: 'You are already friends with this user' });
    }

    // Check if a PENDING request already exists between these two users
    const existingPending = await findPendingRequest(myUserId, targetUser.id);
    if (existingPending) {
      if (existingPending.sender_id === myUserId) {
        return reply.status(409).send({ error: 'Friend request already sent and pending' });
      } else {
        // Reverse request exists! Auto-accept into friendship!
        await acceptRequestById(existingPending.id, myUserId, targetUser.id);
        return reply.send({
          success: true,
          status: 'ACCEPTED',
          message: 'Both users requested each other. Friendship established!'
        });
      }
    }

    // Check Auto-Accept rule:
    // Requires both users to have auto_accept_matched_contacts = true AND phone_verified = true
    const myPrivacy = await getPrivacySettings(myUserId);
    const targetPrivacy = await getPrivacySettings(targetUser.id);
    const myUser = await getUserById(myUserId);

    const canAutoAccept = myPrivacy?.auto_accept_matched_contacts &&
                          targetPrivacy?.auto_accept_matched_contacts &&
                          myUser?.phone_verified &&
                          targetUser.phone_verified;

    const reqId = uuidv4();
    const now = new Date();

    if (canAutoAccept) {
      // Auto-accept immediately
      await createFriendshipPair(myUserId, targetUser.id);
      saveFriendshipRequestRow({
        id: reqId,
        sender_id: myUserId,
        receiver_id: targetUser.id,
        status: 'ACCEPTED',
        created_at: now,
        updated_at: now
      });

      wsManager.sendToUser(targetUser.id, 'FRIEND_REQUEST_ACCEPTED', {
        friendshipId: reqId,
        user: { id: myUser!.id, username: myUser!.username, displayName: myUser!.display_name }
      });

      return reply.status(201).send({
        id: reqId,
        status: 'ACCEPTED',
        autoAccepted: true,
        message: 'Mutual auto-accept verified: friend added immediately!'
      });
    }

    // Normal pending friend request
    await saveFriendshipRequestRow({
      id: reqId,
      sender_id: myUserId,
      receiver_id: targetUser.id,
      status: 'PENDING',
      created_at: now,
      updated_at: now
    });

    // Realtime notification over WebSocket to target user
    wsManager.sendToUser(targetUser.id, 'FRIEND_REQUEST_RECEIVED', {
      requestId: reqId,
      sender: {
        id: myUser!.id,
        username: myUser!.username,
        displayName: myUser!.display_name,
        avatarUrl: myUser!.avatar_url,
        bio: myUser!.bio
      },
      createdAt: now.getTime()
    });

    return reply.status(201).send({
      id: reqId,
      status: 'PENDING',
      message: 'Friend request sent successfully'
    });
  });

  // 2. Accept Friend Request
  fastify.post('/friends/accept', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = requestIdSchema.safeParse(request.body);
    if (!parse.success) return reply.status(400).send({ error: 'Valid requestId required' });

    const myUserId = request.user!.userId;
    const { requestId } = parse.data;

    const reqRow = await getFriendshipRequest(requestId);
    if (!reqRow) return reply.status(404).send({ error: 'Friend request not found' });

    if (reqRow.receiver_id !== myUserId) {
      return reply.status(403).send({ error: 'Not authorized to accept this request' });
    }

    if (reqRow.status !== 'PENDING') {
      return reply.status(400).send({ error: `Request cannot be accepted (current status: ${reqRow.status})` });
    }

    await acceptRequestById(requestId, myUserId, reqRow.sender_id);

    const myUser = await getUserById(myUserId);
    wsManager.sendToUser(reqRow.sender_id, 'FRIEND_REQUEST_ACCEPTED', {
      requestId,
      user: {
        id: myUser!.id,
        username: myUser!.username,
        displayName: myUser!.display_name,
        avatarUrl: myUser!.avatar_url
      }
    });

    return reply.send({ success: true, status: 'ACCEPTED' });
  });

  // 3. Decline Friend Request
  fastify.post('/friends/decline', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = requestIdSchema.safeParse(request.body);
    if (!parse.success) return reply.status(400).send({ error: 'Valid requestId required' });

    const myUserId = request.user!.userId;
    const { requestId } = parse.data;

    const reqRow = await getFriendshipRequest(requestId);
    if (!reqRow) return reply.status(404).send({ error: 'Friend request not found' });

    if (reqRow.receiver_id !== myUserId) {
      return reply.status(403).send({ error: 'Not authorized to decline this request' });
    }

    await updateFriendshipRequestStatus(requestId, 'DECLINED');
    return reply.send({ success: true, status: 'DECLINED' });
  });

  // 4. Cancel Sent Request
  fastify.post('/friends/cancel', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = requestIdSchema.safeParse(request.body);
    if (!parse.success) return reply.status(400).send({ error: 'Valid requestId required' });

    const myUserId = request.user!.userId;
    const { requestId } = parse.data;

    const reqRow = await getFriendshipRequest(requestId);
    if (!reqRow) return reply.status(404).send({ error: 'Friend request not found' });

    if (reqRow.sender_id !== myUserId) {
      return reply.status(403).send({ error: 'Not authorized to cancel this request' });
    }

    await updateFriendshipRequestStatus(requestId, 'CANCELLED');
    return reply.send({ success: true, status: 'CANCELLED' });
  });

  // 5. Get Friends List
  fastify.get('/friends', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const myUserId = request.user!.userId;

    if (db.isPostgres()) {
      const res = await db.getPool()!.query(
        `SELECT u.id, u.username, u.display_name AS "displayName", u.avatar_url AS "avatarUrl", u.bio, f.created_at AS "friendsSince"
         FROM friendships f
         INNER JOIN users u ON u.id = CASE WHEN f.user1_id = $1 THEN f.user2_id ELSE f.user1_id END
         WHERE f.user1_id = $1 OR f.user2_id = $1
         ORDER BY u.display_name ASC`,
        [myUserId]
      );
      const friends = res.rows.map(r => ({
        ...r,
        friendsSince: new Date(r.friendsSince).getTime(),
        isOnline: wsManager.isUserOnline(r.id)
      }));
      return reply.send(friends);
    } else {
      const friends = [];
      for (const f of memoryStore.friendships.values()) {
        if (f.user1_id === myUserId || f.user2_id === myUserId) {
          const peerId = f.user1_id === myUserId ? f.user2_id : f.user1_id;
          const u = memoryStore.users.get(peerId);
          if (u) {
            friends.push({
              id: u.id,
              username: u.username,
              displayName: u.display_name,
              avatarUrl: u.avatar_url,
              bio: u.bio || 'Available on ZX Lite',
              friendsSince: new Date(f.created_at).getTime(),
              isOnline: wsManager.isUserOnline(u.id)
            });
          }
        }
      }
      friends.sort((a, b) => a.displayName.localeCompare(b.displayName));
      return reply.send(friends);
    }
  });

  // 6. Get Incoming Pending Requests
  fastify.get('/friends/requests/incoming', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const myUserId = request.user!.userId;

    if (db.isPostgres()) {
      const res = await db.getPool()!.query(
        `SELECT fr.id, fr.created_at AS "createdAt",
                u.id AS "senderId", u.username, u.display_name AS "displayName", u.avatar_url AS "avatarUrl", u.bio
         FROM friendship_requests fr
         INNER JOIN users u ON u.id = fr.sender_id
         WHERE fr.receiver_id = $1 AND fr.status = 'PENDING'
         ORDER BY fr.created_at DESC`,
        [myUserId]
      );
      return reply.send(res.rows.map(r => ({
        id: r.id,
        createdAt: new Date(r.createdAt).getTime(),
        sender: {
          id: r.senderId,
          username: r.username,
          displayName: r.displayName,
          avatarUrl: r.avatarUrl,
          bio: r.bio
        }
      })));
    } else {
      const list = [];
      for (const fr of memoryStore.friendshipRequests.values()) {
        if (fr.receiver_id === myUserId && fr.status === 'PENDING') {
          const u = memoryStore.users.get(fr.sender_id);
          if (u) {
            list.push({
              id: fr.id,
              createdAt: new Date(fr.created_at).getTime(),
              sender: {
                id: u.id,
                username: u.username,
                displayName: u.display_name,
                avatarUrl: u.avatar_url,
                bio: u.bio
              }
            });
          }
        }
      }
      list.sort((a, b) => b.createdAt - a.createdAt);
      return reply.send(list);
    }
  });

  // 7. Get Outgoing Pending Requests
  fastify.get('/friends/requests/outgoing', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const myUserId = request.user!.userId;

    if (db.isPostgres()) {
      const res = await db.getPool()!.query(
        `SELECT fr.id, fr.created_at AS "createdAt",
                u.id AS "receiverId", u.username, u.display_name AS "displayName", u.avatar_url AS "avatarUrl", u.bio
         FROM friendship_requests fr
         INNER JOIN users u ON u.id = fr.receiver_id
         WHERE fr.sender_id = $1 AND fr.status = 'PENDING'
         ORDER BY fr.created_at DESC`,
        [myUserId]
      );
      return reply.send(res.rows.map(r => ({
        id: r.id,
        createdAt: new Date(r.createdAt).getTime(),
        receiver: {
          id: r.receiverId,
          username: r.username,
          displayName: r.displayName,
          avatarUrl: r.avatarUrl,
          bio: r.bio
        }
      })));
    } else {
      const list = [];
      for (const fr of memoryStore.friendshipRequests.values()) {
        if (fr.sender_id === myUserId && fr.status === 'PENDING') {
          const u = memoryStore.users.get(fr.receiver_id);
          if (u) {
            list.push({
              id: fr.id,
              createdAt: new Date(fr.created_at).getTime(),
              receiver: {
                id: u.id,
                username: u.username,
                displayName: u.display_name,
                avatarUrl: u.avatar_url,
                bio: u.bio
              }
            });
          }
        }
      }
      list.sort((a, b) => b.createdAt - a.createdAt);
      return reply.send(list);
    }
  });

  // 8. Delete / Unfriend
  fastify.delete('/friends/:id', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const { id: peerUserId } = request.params as { id: string };
    const myUserId = request.user!.userId;

    const u1 = myUserId < peerUserId ? myUserId : peerUserId;
    const u2 = myUserId < peerUserId ? peerUserId : myUserId;

    if (db.isPostgres()) {
      await db.getPool()!.query('DELETE FROM friendships WHERE user1_id = $1 AND user2_id = $2', [u1, u2]);
    } else {
      for (const [key, f] of memoryStore.friendships.entries()) {
        if (f.user1_id === u1 && f.user2_id === u2) {
          memoryStore.friendships.delete(key);
        }
      }
    }

    return reply.send({ success: true, message: 'Friend removed' });
  });
}

// Helpers
export async function checkBlocked(u1: string, u2: string): Promise<boolean> {
  if (db.isPostgres()) {
    const res = await db.getPool()!.query(
      'SELECT id FROM blocked_users WHERE (blocker_id = $1 AND blocked_id = $2) OR (blocker_id = $2 AND blocked_id = $1) LIMIT 1',
      [u1, u2]
    );
    return res.rows.length > 0;
  } else {
    for (const b of memoryStore.blockedUsers.values()) {
      if ((b.blocker_id === u1 && b.blocked_id === u2) || (b.blocker_id === u2 && b.blocked_id === u1)) {
        return true;
      }
    }
    return false;
  }
}

export async function checkFriendship(u1: string, u2: string): Promise<boolean> {
  const minId = u1 < u2 ? u1 : u2;
  const maxId = u1 < u2 ? u2 : u1;
  if (db.isPostgres()) {
    const res = await db.getPool()!.query(
      'SELECT id FROM friendships WHERE user1_id = $1 AND user2_id = $2 LIMIT 1',
      [minId, maxId]
    );
    return res.rows.length > 0;
  } else {
    for (const f of memoryStore.friendships.values()) {
      if (f.user1_id === minId && f.user2_id === maxId) return true;
    }
    return false;
  }
}

async function findPendingRequest(u1: string, u2: string) {
  if (db.isPostgres()) {
    const res = await db.getPool()!.query(
      `SELECT * FROM friendship_requests 
       WHERE ((sender_id = $1 AND receiver_id = $2) OR (sender_id = $2 AND receiver_id = $1)) 
         AND status = 'PENDING' 
       LIMIT 1`,
      [u1, u2]
    );
    return res.rows[0] || null;
  } else {
    for (const r of memoryStore.friendshipRequests.values()) {
      if (((r.sender_id === u1 && r.receiver_id === u2) || (r.sender_id === u2 && r.receiver_id === u1)) && r.status === 'PENDING') {
        return r;
      }
    }
    return null;
  }
}

async function getFriendshipRequest(id: string) {
  if (db.isPostgres()) {
    const res = await db.getPool()!.query('SELECT * FROM friendship_requests WHERE id = $1', [id]);
    return res.rows[0] || null;
  } else {
    return memoryStore.friendshipRequests.get(id) || null;
  }
}

async function updateFriendshipRequestStatus(id: string, status: string) {
  if (db.isPostgres()) {
    await db.getPool()!.query('UPDATE friendship_requests SET status = $1, updated_at = NOW() WHERE id = $2', [status, id]);
  } else {
    const r = memoryStore.friendshipRequests.get(id);
    if (r) {
      r.status = status as any;
      r.updated_at = new Date();
    }
  }
}

async function createFriendshipPair(u1: string, u2: string) {
  const minId = u1 < u2 ? u1 : u2;
  const maxId = u1 < u2 ? u2 : u1;
  const id = uuidv4();
  const now = new Date();
  if (db.isPostgres()) {
    await db.getPool()!.query(
      'INSERT INTO friendships (id, user1_id, user2_id, created_at) VALUES ($1, $2, $3, $4) ON CONFLICT DO NOTHING',
      [id, minId, maxId, now]
    );
  } else {
    memoryStore.friendships.set(`${minId}_${maxId}`, {
      id,
      user1_id: minId,
      user2_id: maxId,
      created_at: now
    });
  }
}

async function acceptRequestById(requestId: string, receiverId: string, senderId: string) {
  await updateFriendshipRequestStatus(requestId, 'ACCEPTED');
  await createFriendshipPair(receiverId, senderId);
}

async function saveFriendshipRequestRow(row: FriendshipRequestRow) {
  if (db.isPostgres()) {
    await db.getPool()!.query(
      'INSERT INTO friendship_requests (id, sender_id, receiver_id, status, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6)',
      [row.id, row.sender_id, row.receiver_id, row.status, row.created_at, row.updated_at]
    );
  } else {
    memoryStore.friendshipRequests.set(row.id, row);
  }
}

async function getPrivacySettings(userId: string) {
  if (db.isPostgres()) {
    const res = await db.getPool()!.query('SELECT * FROM user_privacy_settings WHERE user_id = $1', [userId]);
    return res.rows[0] || null;
  } else {
    return memoryStore.privacySettings.get(userId) || null;
  }
}

async function getUserById(userId: string) {
  if (db.isPostgres()) {
    const res = await db.getPool()!.query('SELECT * FROM users WHERE id = $1', [userId]);
    return res.rows[0] || null;
  } else {
    return memoryStore.users.get(userId) || null;
  }
}
