import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { v4 as uuidv4 } from 'uuid';
import { z } from 'zod';
import { authenticate } from '../middleware/auth.js';
import { db, memoryStore, ConversationRow, MessageRow } from '../db/db.js';
import { wsManager } from '../websocket/server.js';
import { checkBlocked, checkFriendship } from './friends.js';

const directConvSchema = z.object({
  peerUsername: z.string().min(3)
});

const sendMessageSchema = z.object({
  content: z.string().min(1).max(4096),
  idempotencyKey: z.string().min(1).max(128)
});

export async function conversationRoutes(fastify: FastifyInstance) {
  // Get all conversations for current user
  fastify.get('/conversations', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const userId = request.user!.userId;

    if (db.isPostgres()) {
      const res = await db.getPool()!.query(
        `SELECT 
            c.id, 
            c.title, 
            c.is_direct AS "isDirect", 
            c.updated_at AS "updatedAt",
            peer.id AS "peerId",
            peer.username AS "peerUsername",
            peer.display_name AS "peerDisplayName",
            peer.avatar_url AS "peerAvatarUrl",
            m.id AS "lastMessageId",
            m.content AS "lastMessageContent",
            m.created_at AS "lastMessageCreatedAt",
            m.sender_id AS "lastMessageSenderId",
            COALESCE(unread.count, 0) AS "unreadCount"
         FROM conversations c
         INNER JOIN conversation_members cm ON cm.conversation_id = c.id AND cm.user_id = $1
         LEFT JOIN conversation_members cm_peer ON cm_peer.conversation_id = c.id AND cm_peer.user_id != $1
         LEFT JOIN users peer ON peer.id = cm_peer.user_id
         LEFT JOIN LATERAL (
            SELECT id, content, created_at, sender_id 
            FROM messages 
            WHERE conversation_id = c.id 
            ORDER BY created_at DESC 
            LIMIT 1
         ) m ON TRUE
         LEFT JOIN LATERAL (
            SELECT COUNT(*)::int AS count 
            FROM messages msg
            LEFT JOIN message_receipts mr ON mr.message_id = msg.id AND mr.user_id = $1
            WHERE msg.conversation_id = c.id 
              AND msg.sender_id != $1 
              AND (mr.status IS NULL OR mr.status != 'READ')
         ) unread ON TRUE
         ORDER BY c.updated_at DESC`,
        [userId]
      );

      const items = res.rows.map(r => ({
        id: r.id,
        title: r.title || r.peerDisplayName || r.peerUsername || 'Chat',
        isDirect: r.isDirect,
        peerUser: r.peerId ? {
          id: r.peerId,
          username: r.peerUsername,
          displayName: r.peerDisplayName,
          avatarUrl: r.peerAvatarUrl
        } : null,
        lastMessage: r.lastMessageId ? {
          id: r.lastMessageId,
          conversationId: r.id,
          senderId: r.lastMessageSenderId,
          content: r.lastMessageContent,
          timestamp: new Date(r.lastMessageCreatedAt).getTime()
        } : null,
        unreadCount: r.unreadCount,
        updatedAt: new Date(r.updatedAt).getTime()
      }));

      return reply.send(items);
    } else {
      // Memory store lookup
      const userConvs: string[] = [];
      for (const m of memoryStore.conversationMembers.values()) {
        if (m.userId === userId) userConvs.push(m.conversationId);
      }

      const items = [];
      for (const convId of userConvs) {
        const c = memoryStore.conversations.get(convId);
        if (!c) continue;

        let peerUser = null;
        for (const m of memoryStore.conversationMembers.values()) {
          if (m.conversationId === convId && m.userId !== userId) {
            const u = memoryStore.users.get(m.userId);
            if (u) {
              peerUser = {
                id: u.id,
                username: u.username,
                displayName: u.display_name,
                avatarUrl: u.avatar_url
              };
            }
            break;
          }
        }

        // Find last message
        const convMsgs: MessageRow[] = [];
        for (const msg of memoryStore.messages.values()) {
          if (msg.conversation_id === convId) convMsgs.push(msg);
        }
        convMsgs.sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime());
        const lastMsg = convMsgs[0];

        // Unread count
        let unreadCount = 0;
        for (const msg of convMsgs) {
          if (msg.sender_id !== userId) {
            const receipt = memoryStore.messageReceipts.get(`${msg.id}_${userId}`);
            if (!receipt || receipt.status !== 'READ') unreadCount++;
          }
        }

        items.push({
          id: c.id,
          title: c.title || peerUser?.displayName || peerUser?.username || 'Chat',
          isDirect: c.is_direct,
          peerUser,
          lastMessage: lastMsg ? {
            id: lastMsg.id,
            conversationId: c.id,
            senderId: lastMsg.sender_id,
            content: lastMsg.content,
            timestamp: new Date(lastMsg.created_at).getTime()
          } : null,
          unreadCount,
          updatedAt: new Date(c.updated_at).getTime()
        });
      }

      items.sort((a, b) => b.updatedAt - a.updatedAt);
      return reply.send(items);
    }
  });

  // Create or get direct conversation with peer
  fastify.post('/conversations/direct', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const parse = directConvSchema.safeParse(request.body);
    if (!parse.success) {
      return reply.status(400).send({ error: 'Valid peerUsername required' });
    }

    const myUserId = request.user!.userId;
    const peerUsername = parse.data.peerUsername.toLowerCase();

    // Find peer user
    let peerUser: any = null;
    if (db.isPostgres()) {
      const res = await db.getPool()!.query('SELECT * FROM users WHERE username = $1', [peerUsername]);
      if (res.rows.length > 0) peerUser = res.rows[0];
    } else {
      for (const u of memoryStore.users.values()) {
        if (u.username.toLowerCase() === peerUsername) {
          peerUser = u;
          break;
        }
      }
    }

    if (!peerUser) {
      return reply.status(404).send({ error: `User @${peerUsername} not found` });
    }

    if (peerUser.id === myUserId) {
      return reply.status(400).send({ error: 'Cannot create direct conversation with yourself' });
    }

    // Check if blocked
    const isBlocked = await checkBlocked(myUserId, peerUser.id);
    if (isBlocked) {
      return reply.status(403).send({ error: 'Cannot message user: communication blocked' });
    }

    // Check friendship policy
    const isFriend = await checkFriendship(myUserId, peerUser.id);
    if (!isFriend) {
      return reply.status(403).send({ error: 'You must be friends with this user to start a direct chat. Send a friend request first.' });
    }

    // Check if direct conversation already exists between both users
    let existingConvId: string | null = null;
    if (db.isPostgres()) {
      const res = await db.getPool()!.query(
        `SELECT c.id 
         FROM conversations c
         INNER JOIN conversation_members cm1 ON cm1.conversation_id = c.id AND cm1.user_id = $1
         INNER JOIN conversation_members cm2 ON cm2.conversation_id = c.id AND cm2.user_id = $2
         WHERE c.is_direct = true 
         LIMIT 1`,
        [myUserId, peerUser.id]
      );
      if (res.rows.length > 0) existingConvId = res.rows[0].id;
    } else {
      for (const c of memoryStore.conversations.values()) {
        if (c.is_direct) {
          let hasUser1 = false;
          let hasUser2 = false;
          for (const m of memoryStore.conversationMembers.values()) {
            if (m.conversationId === c.id) {
              if (m.userId === myUserId) hasUser1 = true;
              if (m.userId === peerUser.id) hasUser2 = true;
            }
          }
          if (hasUser1 && hasUser2) {
            existingConvId = c.id;
            break;
          }
        }
      }
    }

    if (existingConvId) {
      return reply.send({
        id: existingConvId,
        title: peerUser.display_name || peerUser.username,
        isDirect: true,
        peerUser: {
          id: peerUser.id,
          username: peerUser.username,
          displayName: peerUser.display_name || peerUser.username,
          avatarUrl: peerUser.avatar_url
        },
        unreadCount: 0,
        updatedAt: Date.now()
      });
    }

    // Create new conversation
    const newConvId = uuidv4();
    const now = new Date();

    if (db.isPostgres()) {
      await db.getPool()!.query(
        'INSERT INTO conversations (id, title, is_direct, created_at, updated_at) VALUES ($1, $2, $3, $4, $5)',
        [newConvId, peerUser.display_name, true, now, now]
      );
      await db.getPool()!.query(
        'INSERT INTO conversation_members (id, conversation_id, user_id) VALUES ($1, $2, $3), ($4, $5, $6)',
        [uuidv4(), newConvId, myUserId, uuidv4(), newConvId, peerUser.id]
      );
    } else {
      memoryStore.conversations.set(newConvId, {
        id: newConvId,
        title: peerUser.display_name,
        is_direct: true,
        created_at: now,
        updated_at: now
      });
      memoryStore.conversationMembers.set(uuidv4(), { id: uuidv4(), conversationId: newConvId, userId: myUserId });
      memoryStore.conversationMembers.set(uuidv4(), { id: uuidv4(), conversationId: newConvId, userId: peerUser.id });
    }

    return reply.status(201).send({
      id: newConvId,
      title: peerUser.display_name || peerUser.username,
      isDirect: true,
      peerUser: {
        id: peerUser.id,
        username: peerUser.username,
        displayName: peerUser.display_name || peerUser.username,
        avatarUrl: peerUser.avatar_url
      },
      unreadCount: 0,
      updatedAt: now.getTime()
    });
  });

  // Get messages with pagination (cursor)
  fastify.get('/conversations/:id/messages', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const { id: conversationId } = request.params as { id: string };
    const { cursor, limit = 30 } = request.query as { cursor?: string; limit?: number };
    const myUserId = request.user!.userId;
    const pageLimit = Math.min(Number(limit) || 30, 100);

    // Verify membership
    const isMember = await checkMembership(conversationId, myUserId);
    if (!isMember) {
      return reply.status(403).send({ error: 'You are not a member of this conversation' });
    }

    if (db.isPostgres()) {
      let query = `
        SELECT m.id, m.conversation_id AS "conversationId", m.sender_id AS "senderId", 
               u.username AS "senderUsername", m.recipient_id AS "recipientId", 
               m.content, m.created_at AS "createdAt", m.idempotency_key AS "idempotencyKey",
               COALESCE(mr.status, 'SENT') AS "status"
        FROM messages m
        INNER JOIN users u ON u.id = m.sender_id
        LEFT JOIN message_receipts mr ON mr.message_id = m.id AND mr.user_id = $1
        WHERE m.conversation_id = $2
      `;
      const params: any[] = [myUserId, conversationId];

      if (cursor) {
        const cursorDate = isNaN(Number(cursor)) ? new Date(cursor) : new Date(Number(cursor));
        params.push(cursorDate);
        query += ` AND m.created_at < $${params.length}`;
      }

      query += ` ORDER BY m.created_at DESC LIMIT $${params.length + 1}`;
      params.push(pageLimit + 1);

      const res = await db.getPool()!.query(query, params);
      const hasMore = res.rows.length > pageLimit;
      const rows = hasMore ? res.rows.slice(0, pageLimit) : res.rows;

      const messages = rows.map(r => ({
        id: r.id,
        conversationId: r.conversationId,
        senderId: r.senderId,
        senderUsername: r.senderUsername,
        recipientId: r.recipientId,
        content: r.content,
        timestamp: new Date(r.createdAt).getTime(),
        status: r.status,
        idempotencyKey: r.idempotencyKey
      })).reverse(); // oldest first for chronological chat rendering

      const nextCursor = rows.length > 0 ? new Date(rows[rows.length - 1].createdAt).getTime().toString() : null;

      return reply.send({
        messages,
        nextCursor,
        hasMore
      });
    } else {
      const msgs: MessageRow[] = [];
      for (const m of memoryStore.messages.values()) {
        if (m.conversation_id === conversationId) msgs.push(m);
      }
      msgs.sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime());

      let filtered = msgs;
      if (cursor) {
        const cursorTime = Number(cursor);
        filtered = msgs.filter(m => new Date(m.created_at).getTime() < cursorTime);
      }

      const hasMore = filtered.length > pageLimit;
      const page = hasMore ? filtered.slice(0, pageLimit) : filtered;

      const messages = page.map(m => {
        const sender = memoryStore.users.get(m.sender_id);
        const receipt = memoryStore.messageReceipts.get(`${m.id}_${myUserId}`);
        return {
          id: m.id,
          conversationId: m.conversation_id,
          senderId: m.sender_id,
          senderUsername: sender?.username || 'unknown',
          recipientId: m.recipient_id,
          content: m.content,
          timestamp: new Date(m.created_at).getTime(),
          status: receipt?.status || 'SENT',
          idempotencyKey: m.idempotency_key
        };
      }).reverse();

      const nextCursor = page.length > 0 ? new Date(page[page.length - 1].created_at).getTime().toString() : null;

      return reply.send({ messages, nextCursor, hasMore });
    }
  });

  // Send message
  fastify.post('/conversations/:id/messages', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const { id: conversationId } = request.params as { id: string };
    const myUserId = request.user!.userId;
    const myUsername = request.user!.username;

    const parse = sendMessageSchema.safeParse(request.body);
    if (!parse.success) {
      return reply.status(400).send({ error: 'Valid content and idempotencyKey required' });
    }

    const { content, idempotencyKey } = parse.data;

    // Verify membership
    const isMember = await checkMembership(conversationId, myUserId);
    if (!isMember) {
      return reply.status(403).send({ error: 'You are not a member of this conversation' });
    }

    // Check Idempotency: prevent duplication
    const existingMsg = await findMessageByIdempotency(idempotencyKey);
    if (existingMsg) {
      return reply.send(existingMsg);
    }

    const messageId = uuidv4();
    const now = new Date();

    // Determine recipient
    const recipientId = await findRecipient(conversationId, myUserId);

    if (recipientId) {
      const isBlocked = await checkBlocked(myUserId, recipientId);
      if (isBlocked) {
        return reply.status(403).send({ error: 'Cannot send message: communication blocked' });
      }
      const isFriend = await checkFriendship(myUserId, recipientId);
      if (!isFriend) {
        return reply.status(403).send({ error: 'Cannot send message: users must be friends' });
      }
    }

    if (db.isPostgres()) {
      await db.getPool()!.query(
        'INSERT INTO messages (id, conversation_id, sender_id, recipient_id, content, idempotency_key, created_at) VALUES ($1, $2, $3, $4, $5, $6, $7)',
        [messageId, conversationId, myUserId, recipientId, content, idempotencyKey, now]
      );
      await db.getPool()!.query(
        'UPDATE conversations SET updated_at = $1 WHERE id = $2',
        [now, conversationId]
      );
    } else {
      memoryStore.messages.set(messageId, {
        id: messageId,
        conversation_id: conversationId,
        sender_id: myUserId,
        recipient_id: recipientId,
        content,
        idempotency_key: idempotencyKey,
        created_at: now
      });
      const conv = memoryStore.conversations.get(conversationId);
      if (conv) {
        conv.updated_at = now;
      }
    }

    const responseDto = {
      id: messageId,
      conversationId,
      senderId: myUserId,
      senderUsername: myUsername,
      recipientId,
      content,
      timestamp: now.getTime(),
      status: 'SENT',
      idempotencyKey
    };

    // Realtime broadcast to recipient if online
    if (recipientId) {
      wsManager.sendToUser(recipientId, 'MESSAGE_RECEIVED', responseDto);
    }

    return reply.status(201).send(responseDto);
  });

  // Mark conversation read
  fastify.post('/conversations/:id/read', { preHandler: [authenticate] }, async (request: FastifyRequest, reply: FastifyReply) => {
    const { id: conversationId } = request.params as { id: string };
    const myUserId = request.user!.userId;

    if (db.isPostgres()) {
      await db.getPool()!.query(
        `INSERT INTO message_receipts (id, message_id, user_id, status, updated_at)
         SELECT gen_random_uuid(), m.id, $1, 'READ', NOW()
         FROM messages m
         WHERE m.conversation_id = $2 AND m.sender_id != $1
         ON CONFLICT (message_id, user_id) DO UPDATE SET status = 'READ', updated_at = NOW()`,
        [myUserId, conversationId]
      );
    } else {
      for (const m of memoryStore.messages.values()) {
        if (m.conversation_id === conversationId && m.sender_id !== myUserId) {
          memoryStore.messageReceipts.set(`${m.id}_${myUserId}`, {
            id: uuidv4(),
            messageId: m.id,
            userId: myUserId,
            status: 'READ'
          });
        }
      }
    }

    // Notify peer
    const peerId = await findRecipient(conversationId, myUserId);
    if (peerId) {
      wsManager.sendToUser(peerId, 'MESSAGE_READ', { conversationId, readBy: myUserId });
    }

    return reply.send({ success: true });
  });
}

async function checkMembership(conversationId: string, userId: string): Promise<boolean> {
  if (db.isPostgres()) {
    const res = await db.getPool()!.query(
      'SELECT id FROM conversation_members WHERE conversation_id = $1 AND user_id = $2',
      [conversationId, userId]
    );
    return res.rows.length > 0;
  } else {
    for (const m of memoryStore.conversationMembers.values()) {
      if (m.conversationId === conversationId && m.userId === userId) return true;
    }
    return false;
  }
}

async function findMessageByIdempotency(key: string) {
  if (db.isPostgres()) {
    const res = await db.getPool()!.query(
      `SELECT m.id, m.conversation_id AS "conversationId", m.sender_id AS "senderId",
              u.username AS "senderUsername", m.recipient_id AS "recipientId",
              m.content, m.created_at AS "createdAt", m.idempotency_key AS "idempotencyKey"
       FROM messages m
       INNER JOIN users u ON u.id = m.sender_id
       WHERE m.idempotency_key = $1`,
      [key]
    );
    if (res.rows.length === 0) return null;
    const r = res.rows[0];
    return {
      id: r.id,
      conversationId: r.conversationId,
      senderId: r.senderId,
      senderUsername: r.senderUsername,
      recipientId: r.recipientId,
      content: r.content,
      timestamp: new Date(r.createdAt).getTime(),
      status: 'SENT',
      idempotencyKey: r.idempotencyKey
    };
  } else {
    for (const m of memoryStore.messages.values()) {
      if (m.idempotency_key === key) {
        const u = memoryStore.users.get(m.sender_id);
        return {
          id: m.id,
          conversationId: m.conversation_id,
          senderId: m.sender_id,
          senderUsername: u?.username || 'unknown',
          recipientId: m.recipient_id,
          content: m.content,
          timestamp: new Date(m.created_at).getTime(),
          status: 'SENT',
          idempotencyKey: m.idempotency_key
        };
      }
    }
    return null;
  }
}

async function findRecipient(conversationId: string, senderId: string): Promise<string | null> {
  if (db.isPostgres()) {
    const res = await db.getPool()!.query(
      'SELECT user_id FROM conversation_members WHERE conversation_id = $1 AND user_id != $2 LIMIT 1',
      [conversationId, senderId]
    );
    return res.rows.length > 0 ? res.rows[0].user_id : null;
  } else {
    for (const m of memoryStore.conversationMembers.values()) {
      if (m.conversationId === conversationId && m.userId !== senderId) {
        return m.userId;
      }
    }
    return null;
  }
}
