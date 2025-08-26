const API_BASE = "/api";

// Prosty helper do fetch
async function getJSON(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`${res.status} ${res.statusText} for ${url}`);
  return res.json();
}

let plChart;

async function loadSummary() {

  const summary = await getJSON(`${API_BASE}/portfolio/summary`).catch(() => null);
  if (!summary) {
    setText("balanceDisplay", "—");
    setText("balanceValue", "—");
    setText("profitValue", "—");
    setText("profitPct", "—");
    setText("lossValue", "—");
    setText("lossPct", "—");
    renderPLChart(0, 0);
    return;
  }

  const currency = summary.currency || "";
  setText("balanceDisplay", fmtMoney(summary.balance, currency));
  setText("balanceValue", fmtMoney(summary.balance, currency));
  setText("profitValue", fmtMoney(summary.profit, currency));
  setText("profitPct", pct(summary.profitPct));
  setText("lossValue", fmtMoney(Math.abs(summary.loss), currency));
  setText("lossPct", pct(summary.lossPct));

  renderPLChart(summary.profit || 0, Math.abs(summary.loss || 0));
}

async function loadPortfolio() {

  const assets = await getJSON(`${API_BASE}/portfolio/assets`).catch(() => []);
  const tbody = document.getElementById("portfolioBody");
  tbody.innerHTML = "";

  if (!assets.length) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td colspan="6" style="opacity:.7;">No assets in portfolio.</td>`;
    tbody.appendChild(tr);
    return;
  }

  assets.forEach(a => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${safe(a.ticker)}</td>
      <td>${safe(a.name)}</td>
      <td>${num(a.close)}</td>
      <td>${num(a.percentChange)}%</td>
      <td>${num(a.volume)}</td>
      <td>${safe(a?.marketType?.name)}</td>
    `;
    tbody.appendChild(tr);
  });
}

async function loadTransactions() {

  // [ { time, action: "ADD"|"REMOVE", ticker, name, quantity, price } ]
  const tx = await getJSON(`${API_BASE}/portfolio/transactions`).catch(() => []);
  const tbody = document.getElementById("transactionsBody");
  tbody.innerHTML = "";

  if (!tx.length) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td colspan="6" style="opacity:.7;">No transactions yet.</td>`;
    tbody.appendChild(tr);
    return;
  }

  tx.forEach(t => {
    const tr = document.createElement("tr");
    const actionColor = t.action === "ADD" ? "#2e7d32" : "#c62828";
    tr.innerHTML = `
      <td>${safe(formatTime(t.time))}</td>
      <td style="color:${actionColor}; font-weight:600;">${safe(t.action)}</td>
      <td>${safe(t.ticker)}</td>
      <td>${safe(t.name)}</td>
      <td>${num(t.quantity)}</td>
      <td>${num(t.price)}</td>
    `;
    tbody.appendChild(tr);
  });
}


function renderPLChart(profit, lossAbs) {
  const ctx = document.getElementById("plChart").getContext("2d");

  if (plChart) {
    plChart.data.datasets[0].data = [profit, lossAbs];
    plChart.update();
    return;
  }

  plChart = new Chart(ctx, {
    type: "bar",
    data: {
      labels: ["Profit", "Loss"],
      datasets: [{
        label: "Amount",
        data: [profit, lossAbs],
        backgroundColor: ["#2e7d32", "#c62828"]
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { display: false }
      },
      scales: {
        y: { beginAtZero: true }
      }
    }
  });
}


function setupTabs() {
  const portfolioBtn = document.getElementById("tabPortfolioBtn");
  const txBtn = document.getElementById("tabTxBtn");
  const portfolioPanel = document.getElementById("tab-portfolio");
  const txPanel = document.getElementById("tab-transactions");

  portfolioBtn.addEventListener("click", () => {
    portfolioPanel.classList.add("active");
    txPanel.classList.remove("active");
  });
  txBtn.addEventListener("click", () => {
    txPanel.classList.add("active");
    portfolioPanel.classList.remove("active");
  });
}


function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}
function fmtMoney(v, currency) {
  if (v == null) return "—";
  try {
    return new Intl.NumberFormat(undefined, { style: "currency", currency: currency || "USD" }).format(v);
  } catch {
    return `${v} ${currency || ""}`.trim();
  }
}
function pct(v) {
  if (v == null) return "—";
  return `${num(v)}%`;
}
function num(v) {
  if (v == null || v === "") return "—";
  const n = Number(v);
  return Number.isFinite(n) ? n.toLocaleString() : String(v);
}
function safe(s) {
  if (s == null) return "—";
  return String(s);
}
function formatTime(t) {

  try {
    return new Date(t).toLocaleString();
  } catch { return t; }
}

/* ---------- Start ---------- */
(async function init() {
  setupTabs();
  await Promise.all([loadSummary(), loadPortfolio(), loadTransactions()]).catch(console.error);

})();
