const API_BASE = "/api";

let assets = [];
let marketTypes = [];

async function loadData() {
  const assetsRes = await fetch(`${API_BASE}/assets`);
  assets = await assetsRes.json();

  const marketsRes = await fetch(`${API_BASE}/market-types`);
  marketTypes = await marketsRes.json();

  populateMarketFilter();
  renderAssets();
}

function populateMarketFilter() {
  const select = document.getElementById('marketFilter');
  marketTypes.forEach(m => {
    const option = document.createElement('option');
    option.value = m.name;
    option.textContent = m.name;
    select.appendChild(option);
  });
}

function renderAssets() {
  const search = document.getElementById('search').value.toLowerCase();
  const filter = document.getElementById('marketFilter').value;

  const container = document.getElementById('assetList');
  container.innerHTML = '';

  assets
    .filter(a => (filter === 'ALL' || a.marketType.name === filter))
    .filter(a => a.name.toLowerCase().includes(search) || a.ticker.toLowerCase().includes(search))
    .forEach(a => {
      const div = document.createElement('div');
      div.className = 'asset-row';
      div.innerHTML = `
        <span>${a.name} (${a.ticker}) - Close: ${a.close}</span>
        <div>
          <button onclick="viewDetails('${a.ticker}')">Details</button>
        </div>
      `;
      container.appendChild(div);
    });
}

function viewDetails(ticker) {
  localStorage.setItem('selectedTicker', ticker);
  window.location.href = 'asset-details.html';
}

document.getElementById('search').addEventListener('input', renderAssets);
document.getElementById('marketFilter').addEventListener('change', renderAssets);

// start
loadData();

