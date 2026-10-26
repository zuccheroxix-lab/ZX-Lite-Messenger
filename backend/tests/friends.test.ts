import test from 'node:test';
import assert from 'node:assert/strict';
import { buildServer } from '../src/server.js';

test('Friends: Requests, Accept, Decline, Blocking, and Chat Gating', async (t) => {
  const app = buildServer();

  // Create User A
  const uARes = await app.inject({
    method: 'POST',
    url: '/auth/register',
    payload: {
      username: 'user_a',
      displayName: 'User Alpha',
      password: 'password123',
      phoneNumber: '+6281234567890'
    }
  });
  const uA = JSON.parse(uARes.body);

  // Create User B
  const uBRes = await app.inject({
    method: 'POST',
    url: '/auth/register',
    payload: {
      username: 'user_b',
      displayName: 'User Beta',
      password: 'password123',
      phoneNumber: '+6289876543210'
    }
  });
  const uB = JSON.parse(uBRes.body);

  // 1. Direct chat before friendship must be rejected
  await t.test('Direct chat is rejected before becoming friends', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/conversations/direct',
      headers: { Authorization: `Bearer ${uA.accessToken}` },
      payload: { peerUsername: 'user_b' }
    });
    assert.equal(res.statusCode, 403);
  });

  // 2. Self request is rejected
  await t.test('Cannot send friend request to self', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/friends/request',
      headers: { Authorization: `Bearer ${uA.accessToken}` },
      payload: { targetUsername: 'user_a' }
    });
    assert.equal(res.statusCode, 400);
  });

  let requestId = '';

  // 3. Send friend request succeeds
  await t.test('Send friend request from A to B', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/friends/request',
      headers: { Authorization: `Bearer ${uA.accessToken}` },
      payload: { targetUsername: 'user_b' }
    });
    assert.equal(res.statusCode, 201);
    const body = JSON.parse(res.body);
    assert.ok(body.id);
    assert.equal(body.status, 'PENDING');
    requestId = body.id;
  });

  // 4. Duplicate friend request rejected
  await t.test('Duplicate friend request is rejected with 409', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/friends/request',
      headers: { Authorization: `Bearer ${uA.accessToken}` },
      payload: { targetUsername: 'user_b' }
    });
    assert.equal(res.statusCode, 409);
  });

  // 5. Incoming requests for B
  await t.test('User B sees incoming request from User A', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/friends/requests/incoming',
      headers: { Authorization: `Bearer ${uB.accessToken}` }
    });
    assert.equal(res.statusCode, 200);
    const body = JSON.parse(res.body);
    assert.equal(body.length, 1);
    assert.equal(body[0].sender.username, 'user_a');
  });

  // 6. User B accepts friend request
  await t.test('User B accepts friend request', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/friends/accept',
      headers: { Authorization: `Bearer ${uB.accessToken}` },
      payload: { requestId }
    });
    assert.equal(res.statusCode, 200);
    const body = JSON.parse(res.body);
    assert.equal(body.status, 'ACCEPTED');
  });

  // 7. Verify both now show each other in friends list
  await t.test('Both users see each other in /friends', async () => {
    const resA = await app.inject({
      method: 'GET',
      url: '/friends',
      headers: { Authorization: `Bearer ${uA.accessToken}` }
    });
    const friendsA = JSON.parse(resA.body);
    assert.equal(friendsA.length, 1);
    assert.equal(friendsA[0].username, 'user_b');

    const resB = await app.inject({
      method: 'GET',
      url: '/friends',
      headers: { Authorization: `Bearer ${uB.accessToken}` }
    });
    const friendsB = JSON.parse(resB.body);
    assert.equal(friendsB.length, 1);
    assert.equal(friendsB[0].username, 'user_a');
  });

  // 8. Direct chat now succeeds after friendship
  await t.test('Direct chat succeeds after becoming friends', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/conversations/direct',
      headers: { Authorization: `Bearer ${uA.accessToken}` },
      payload: { peerUsername: 'user_b' }
    });
    assert.equal(res.statusCode, 201);
    const body = JSON.parse(res.body);
    assert.ok(body.id);
  });

  // 9. Contact discovery test
  await t.test('Contact discovery matches normalized phone number', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/contacts/discover',
      headers: { Authorization: `Bearer ${uA.accessToken}` },
      payload: {
        phoneNumbers: ['+62 898-7654-3210', '+1 555 000 1111']
      }
    });
    assert.equal(res.statusCode, 200);
    const body = JSON.parse(res.body);
    assert.equal(body.matches.length, 1);
    assert.equal(body.matches[0].username, 'user_b');
    assert.equal(body.matches[0].isFriend, true);
  });

  // 10. Blocking user test
  await t.test('Blocking user halts communication', async () => {
    const blockRes = await app.inject({
      method: 'POST',
      url: '/users/block',
      headers: { Authorization: `Bearer ${uB.accessToken}` },
      payload: { targetUserId: uA.user.id }
    });
    assert.equal(blockRes.statusCode, 200);

    // Verify chat creation is now blocked
    const chatRes = await app.inject({
      method: 'POST',
      url: '/conversations/direct',
      headers: { Authorization: `Bearer ${uA.accessToken}` },
      payload: { peerUsername: 'user_b' }
    });
    assert.equal(chatRes.statusCode, 403);
  });

  await app.close();
});
