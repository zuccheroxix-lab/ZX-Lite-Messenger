import test from 'node:test';
import assert from 'node:assert/strict';
import { buildServer } from '../src/server.js';

test('Auth: Registration, Duplicate Rejection, and Login', async (t) => {
  const app = buildServer();

  await t.test('Register new account succeeds', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/auth/register',
      payload: {
        username: 'alice_zx',
        displayName: 'Alice ZX',
        password: 'password123'
      }
    });

    assert.equal(res.statusCode, 201);
    const body = JSON.parse(res.body);
    assert.ok(body.accessToken);
    assert.ok(body.refreshToken);
    assert.equal(body.user.username, 'alice_zx');
    assert.equal(body.user.displayName, 'Alice ZX');
  });

  await t.test('Duplicate username registration is rejected with 409', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/auth/register',
      payload: {
        username: 'alice_zx',
        displayName: 'Alice Duplicate',
        password: 'password123'
      }
    });

    assert.equal(res.statusCode, 409);
  });

  await t.test('Login with valid credentials succeeds', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/auth/login',
      payload: {
        username: 'alice_zx',
        password: 'password123'
      }
    });

    assert.equal(res.statusCode, 200);
    const body = JSON.parse(res.body);
    assert.ok(body.accessToken);
    assert.equal(body.user.username, 'alice_zx');
  });

  await t.test('Login with incorrect password is rejected with 401', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/auth/login',
      payload: {
        username: 'alice_zx',
        password: 'wrong_password'
      }
    });

    assert.equal(res.statusCode, 401);
  });

  await app.close();
});
