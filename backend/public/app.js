const PRIORITY_COLORS = {
  0: '#ff2d2d', // SOS
  1: '#ffa726', // Medical
  2: '#ffd54f', // Resource
  3: '#9e9e9e' // General
};

const map = L.map('map', { zoomControl: true }).setView([26.9124, 75.7873], 13);
L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
  attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
  maxZoom: 19
}).addTo(map);

const markerLayer = L.layerGroup().addTo(map);
const heatLayer = L.heatLayer([], { radius: 30, blur: 20 }).addTo(map);
const seenMessageIds = new Set();

function iconFor(priority) {
  const color = PRIORITY_COLORS[priority] ?? PRIORITY_COLORS[3];
  return L.divIcon({
    className: '',
    html: `<div class="marker-pin" style="background:${color}"></div>`,
    iconSize: [16, 16]
  });
}

function popupHtml(msg) {
  const coords = msg.location && msg.location.coordinates ? msg.location.coordinates : [0, 0];
  const hasTranslation = msg.translatedContent && msg.translatedContent !== msg.content;
  return `
    <strong>${msg.senderAlias || 'Unknown'}</strong><br/>
    ${msg.content || ''}<br/>
    ${hasTranslation ? `<em>${msg.translatedContent}</em><br/>` : ''}
    <span>Severity: ${msg.severityTag || '—'}</span><br/>
    <span>Hop: ${msg.hopCount ?? 0}</span><br/>
    <span>${coords[1].toFixed(5)}, ${coords[0].toFixed(5)}</span><br/>
    <small>${msg.createdAt || ''}</small>
  `;
}

function addMessageToMap(msg) {
  if (!msg.messageId || seenMessageIds.has(msg.messageId)) return;
  seenMessageIds.add(msg.messageId);

  const coords = msg.location && msg.location.coordinates;
  if (!coords || coords.length !== 2) return;
  const [lng, lat] = coords;

  const marker = L.marker([lat, lng], { icon: iconFor(msg.priority) }).bindPopup(popupHtml(msg));
  markerLayer.addLayer(marker);
  heatLayer.addLatLng([lat, lng, msg.priority === 0 ? 1 : 0.4]);

  if (msg.priority === 0) prependSosFeed(msg);
  prependTicker(msg);
}

function prependSosFeed(msg) {
  const feed = document.getElementById('sos-feed');
  const li = document.createElement('li');
  li.textContent = `🔴 ${msg.senderAlias || 'Unknown'}: "${msg.content || ''}"`;
  feed.prepend(li);
  while (feed.children.length > 20) feed.removeChild(feed.lastChild);
}

function prependTicker(msg) {
  const ticker = document.getElementById('ticker');
  const span = document.createElement('span');
  span.textContent = `${msg.senderAlias || 'Unknown'}: ${msg.content || ''}`;
  ticker.appendChild(span);
  while (ticker.children.length > 40) ticker.removeChild(ticker.firstChild);
}

async function refreshStats() {
  try {
    const res = await fetch('/api/stats');
    const stats = await res.json();
    document.getElementById('stat-total').textContent = stats.totalMessages ?? 0;
    document.getElementById('stat-sos').textContent = stats.activeSOS ?? 0;
    document.getElementById('stat-nodes').textContent = stats.uniqueNodes ?? 0;
    document.getElementById('message-count-label').textContent = `${stats.totalMessages ?? 0} msgs`;
  } catch (err) {
    console.error('Failed to refresh stats', err);
  }
}

async function loadInitialMessages() {
  try {
    const res = await fetch('/api/messages?limit=500');
    const messages = await res.json();
    messages.forEach(addMessageToMap);
  } catch (err) {
    console.error('Failed to load messages', err);
  }
}

function connectWebSocket() {
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
  const ws = new WebSocket(`${protocol}://${location.host}`);
  const dot = document.getElementById('connection-dot');
  const label = document.getElementById('connection-label');

  ws.onopen = () => {
    dot.classList.add('live');
    label.textContent = 'Live';
  };
  ws.onclose = () => {
    dot.classList.remove('live');
    label.textContent = 'Reconnecting…';
    setTimeout(connectWebSocket, 3000);
  };
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      if (data.type === 'NEW_MESSAGE' && data.message) {
        addMessageToMap(data.message);
        refreshStats();
      }
    } catch (err) {
      console.error('Bad WS payload', err);
    }
  };
}

loadInitialMessages().then(refreshStats);
connectWebSocket();
setInterval(refreshStats, 15000);
