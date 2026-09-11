require('dotenv').config();

const path = require('path');
const http = require('http');
const express = require('express');
const cors = require('cors');
const { WebSocketServer } = require('ws');
const { MongoClient } = require('mongodb');

const PORT = process.env.PORT || 3000;
const MONGO_URI = process.env.MONGO_URI;
const DB_NAME = 'pulsenet';
const COLLECTION_NAME = 'messages';

if (!MONGO_URI || MONGO_URI.includes('TO_BE_FILLED')) {
  console.error('MONGO_URI is not set in backend/.env — fill it in after provisioning the Atlas cluster.');
  process.exit(1);
}

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

const server = http.createServer(app);
const wss = new WebSocketServer({ server });

function broadcast(payload) {
  const data = JSON.stringify(payload);
  wss.clients.forEach((client) => {
    if (client.readyState === client.OPEN) {
      client.send(data);
    }
  });
}

async function main() {
  const client = new MongoClient(MONGO_URI);
  await client.connect();
  const db = client.db(DB_NAME);
  const messages = db.collection(COLLECTION_NAME);

  await messages.createIndex({ location: '2dsphere' });
  await messages.createIndex({ priority: 1, createdAt: -1 });
  await messages.createIndex({ messageId: 1 }, { unique: true });

  // Bulk insert — used by the Android Bridge if it targets this backend instead
  // of (or in addition to) the Atlas Data API directly.
  app.post('/api/messages/bulk', async (req, res) => {
    const documents = req.body.documents;
    if (!Array.isArray(documents) || documents.length === 0) {
      res.status(400).json({ error: 'documents must be a non-empty array' });
      return;
    }
    try {
      const ops = documents.map((doc) => ({
        updateOne: {
          filter: { messageId: doc.messageId },
          update: { $setOnInsert: doc },
          upsert: true
        }
      }));
      const result = await messages.bulkWrite(ops, { ordered: false });
      documents.forEach((doc) => broadcast({ type: 'NEW_MESSAGE', message: doc }));
      res.json({ inserted: result.upsertedCount });
    } catch (err) {
      console.error('bulk insert failed', err);
      res.status(500).json({ error: 'insert failed' });
    }
  });

  app.get('/api/messages', async (req, res) => {
    const priority = req.query.priority !== undefined ? Number(req.query.priority) : undefined;
    const limit = Math.min(Number(req.query.limit) || 200, 1000);
    const filter = priority !== undefined && !Number.isNaN(priority) ? { priority } : {};
    const docs = await messages.find(filter).sort({ createdAt: -1 }).limit(limit).toArray();
    res.json(docs);
  });

  app.get('/api/messages/sos', async (_req, res) => {
    const docs = await messages.find({ priority: 0 }).sort({ createdAt: -1 }).limit(100).toArray();
    res.json(docs);
  });

  app.get('/api/stats', async (_req, res) => {
    const [totalMessages, activeSOS, uniqueSenders] = await Promise.all([
      messages.countDocuments(),
      messages.countDocuments({ priority: 0 }),
      messages.distinct('senderPublicKey')
    ]);
    res.json({ totalMessages, activeSOS, uniqueNodes: uniqueSenders.length });
  });

  app.get('/api/heatmap', async (_req, res) => {
    const points = await messages
      .aggregate([
        { $match: { location: { $exists: true } } },
        { $project: { _id: 0, location: 1, priority: 1 } }
      ])
      .toArray();
    res.json(points);
  });

  // Change Streams need a replica set — Atlas clusters (including the free M0
  // tier) always are one, so this works in production; a bare standalone
  // mongod locally would not support it, hence the guard.
  try {
    const changeStream = messages.watch([{ $match: { operationType: 'insert' } }]);
    changeStream.on('change', (change) => {
      broadcast({ type: 'NEW_MESSAGE', message: change.fullDocument });
    });
  } catch (err) {
    console.warn('Change Streams unavailable — falling back to bulk-endpoint broadcasts only.', err.message);
  }

  server.listen(PORT, () => {
    console.log(`PulseNet Rescue Dashboard running on http://localhost:${PORT}`);
  });
}

main().catch((err) => {
  console.error('Failed to start server', err);
  process.exit(1);
});
