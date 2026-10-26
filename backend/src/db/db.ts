import pg from 'pg';
import { config } from '../config.js';
import fs from 'fs';
import path from 'path';

export interface UserRow {
  id: string;
  username: string;
  display_name: string;
  password_hash: string;
  phone_number?: string | null;
  phone_verified?: boolean;
  bio?: string;
  avatar_url: string | null;
  created_at: Date;
  updated_at: Date;
}

export interface ConversationRow {
  id: string;
  title: string | null;
  is_direct: boolean;
  created_at: Date;
  updated_at: Date;
}

export interface MessageRow {
  id: string;
  conversation_id: string;
  sender_id: string;
  recipient_id: string | null;
  content: string;
  idempotency_key: string;
  created_at: Date;
}

export interface FriendshipRequestRow {
  id: string;
  sender_id: string;
  receiver_id: string;
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED';
  created_at: Date;
  updated_at: Date;
}

export interface FriendshipRow {
  id: string;
  user1_id: string;
  user2_id: string;
  created_at: Date;
}

export interface BlockedUserRow {
  id: string;
  blocker_id: string;
  blocked_id: string;
  created_at: Date;
}

export interface UserPrivacySettingsRow {
  user_id: string;
  auto_accept_matched_contacts: boolean;
  allow_phone_discovery: boolean;
  read_receipts_enabled: boolean;
  updated_at: Date;
}

export interface UserStatusRow {
  id: string;
  user_id: string;
  text: string;
  created_at: Date;
  expires_at: Date;
}

export interface UserFcmTokenRow {
  id: string;
  user_id: string;
  token: string;
  device_id: string;
  updated_at: Date;
}

// In-Memory store for tests or when PostgreSQL is not running
export class MemoryStore {
  users = new Map<string, UserRow>();
  refreshSessions = new Map<string, { id: string; userId: string; tokenHash: string; expiresAt: Date }>();
  conversations = new Map<string, ConversationRow>();
  conversationMembers = new Map<string, { id: string; conversationId: string; userId: string }>();
  messages = new Map<string, MessageRow>();
  messageReceipts = new Map<string, { id: string; messageId: string; userId: string; status: string }>();
  friendshipRequests = new Map<string, FriendshipRequestRow>();
  friendships = new Map<string, FriendshipRow>();
  blockedUsers = new Map<string, BlockedUserRow>();
  privacySettings = new Map<string, UserPrivacySettingsRow>();
  userStatuses = new Map<string, UserStatusRow>();
  userFcmTokens = new Map<string, UserFcmTokenRow>();
}

export const memoryStore = new MemoryStore();
let pgPool: pg.Pool | null = null;
let usePostgres = false;

export async function initDatabase(): Promise<boolean> {
  if (!config.databaseUrl) {
    console.log('[DB] No DATABASE_URL provided. Using in-memory store.');
    return false;
  }

  try {
    const pool = new pg.Pool({
      connectionString: config.databaseUrl,
      connectionTimeoutMillis: 3000
    });
    // Test connection
    const client = await pool.connect();
    client.release();
    pgPool = pool;
    usePostgres = true;
    console.log('[DB] Connected to PostgreSQL. Running migrations...');
    await runMigrations();
    return true;
  } catch (err: any) {
    console.warn(`[DB] PostgreSQL connection unavailable (${err.message}). Using in-memory store for development/testing.`);
    usePostgres = false;
    return false;
  }
}

async function runMigrations() {
  if (!pgPool) return;
  try {
    const migrationFiles = [
      'src/db/migrations/001_initial_schema.sql',
      'src/db/migrations/002_friends_contacts_privacy.sql'
    ];
    for (const relPath of migrationFiles) {
      const fullPath = path.resolve(relPath);
      if (fs.existsSync(fullPath)) {
        const sql = fs.readFileSync(fullPath, 'utf8');
        await pgPool.query(sql);
        console.log(`[DB] Migration ${relPath} applied successfully.`);
      }
    }
  } catch (err: any) {
    console.error('[DB] Migration error:', err.message);
  }
}

export const db = {
  isPostgres(): boolean {
    return usePostgres && pgPool !== null;
  },
  getPool(): pg.Pool | null {
    return pgPool;
  }
};
