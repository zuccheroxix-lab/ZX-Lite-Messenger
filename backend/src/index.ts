import { buildServer } from './server.js';
import { config } from './config.js';
import { initDatabase } from './db/db.js';

async function start() {
  console.log('--- Starting ZX Lite Messenger Backend ---');
  console.log(`Brand: ZUCCHERO XANN`);
  console.log(`Environment: ${config.nodeEnv}`);

  await initDatabase();

  const app = buildServer();

  try {
    const address = await app.listen({ port: config.port, host: config.host });
    console.log(`ZX Lite Server listening on ${address}`);
    console.log(`WebSocket endpoint ready at ws://${config.host}:${config.port}/ws`);
  } catch (err: any) {
    app.log.error(err);
    process.exit(1);
  }
}

start();
