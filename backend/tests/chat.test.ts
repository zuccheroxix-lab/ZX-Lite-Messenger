import test from 'node:test';
import assert from 'node:assert/strict';
import { buildServer } from '../src/server.js';

test('Chat: Messaging, Idempotency, and Access Control', async (t) => {
  const app = buildServer();

  // Create User 1
  const u1Res = await app.inject({
    method: 'POST',
    url: '/auth/register',
    payload: { username: 'bob_sender', displayName: 'Bob', password: 'password123' }
  });
  const u1 = JSON.parse(u1Res.body);

  // Create User 2
  const u2Res = await app.inject({
    method: 'POST',
    url: '/auth/register',
    payload: { username: 'charlie_receiver', displayName: 'Charlie', password: 'password123' }
  });
  const u2 = JSON.parse(u2Res.body);

  // Establish friendship
  const reqRes = await app.inject({
    method: 'POST',
    url: '/friends/request',
    headers: { Authorization: `Bearer ${u1.accessToken}` },
    payload: { targetUsername: 'charlie_receiver' }
  });
  const reqBody = JSON.parse(reqRes.body);

  await app.inject({
    method: 'POST',
    url: '/friends/accept',
    headers: { Authorization: `Bearer ${u2.accessToken}` },
    payload: { requestId: reqBody.id }
  });

  let conversationId = '';

  await t.test('Create direct conversation between two users', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/conversations/direct',
      headers: { Authorization: `Bearer ${u1.accessToken}` },
      payload: { peerUsername: 'charlie_receiver' }
    });

    assert.equal(res.statusCode, 201);
    const body = JSON.parse(res.body);
    assert.ok(body.id);
    assert.equal(body.peerUser.username, 'charlie_receiver');
    conversationId = body.id;
  });

  await t.test('Bob sends message with unique idempotencyKey', async () => {
    const res = await app.inject({
      method: 'POST',
      url: `/conversations/${conversationId}/messages`,
      headers: { Authorization: `Bearer ${u1.accessToken}` },
      payload: {
        content: 'Hello Charlie! This is ZX Lite.',
        idempotencyKey: 'idemp_key_001'
      }
    });

    assert.equal(res.statusCode, 201);
    const body = JSON.parse(res.body);
    assert.equal(body.content, 'Hello Charlie! This is ZX Lite.');
    assert.equal(body.idempotencyKey, 'idemp_key_001');
  });

  await t.test('Resending message with same idempotencyKey does NOT create duplicate', async () => {
    const res = await app.inject({
      method: 'POST',
      url: `/conversations/${conversationId}/messages`,
      headers: { Authorization: `Bearer ${u1.accessToken}` },
      payload: {
        content: 'Hello Charlie! This is ZX Lite.',
        idempotencyKey: 'idemp_key_001'
      }
    });

    assert.equal(res.statusCode, 200); // 200 returned for existing idempotent message
    const body = JSON.parse(res.body);
    assert.equal(body.idempotencyKey, 'idemp_key_001');

    // Verify messages count in conversation is still 1
    const listRes = await app.inject({
      method: 'GET',
      url: `/conversations/${conversationId}/messages`,
      headers: { Authorization: `Bearer ${u1.accessToken}` }
    });
    const listBody = JSON.parse(listRes.body);
    assert.equal(listBody.messages.length, 1);
  });

  await t.test('Unauthorized third user cannot read conversation', async () => {
    // Create User 3
    const u3Res = await app.inject({
      method: 'POST',
      url: '/auth/register',
      payload: { username: 'eve_intruder', displayName: 'Eve', password: 'password123' }
    });
    const u3 = JSON.parse(u3Res.body);

    const res = await app.inject({
      method: 'GET',
      url: `/conversations/${conversationId}/messages`,
      headers: { Authorization: `Bearer ${u3.accessToken}` }
    });

    assert.equal(res.statusCode, 403);
  });

  await app.close();
});
