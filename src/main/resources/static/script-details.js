const API_BASE = "/api";
const ticker = localStorage.getItem("selectedTicker");

async function loadAssetDetails() {
  if (!ticker) {
    document.getElementById("detailsContainer").innerHTML = "<p>No asset selected.</p>";
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/assets/${ticker}`);
    if (!res.ok) throw new Error("Failed to load asset details");
    const asset = await res.json();

    renderAttributes(asset);
    renderChart(asset);

  } catch (err) {
    console.error(err);
    document.getElementById("detailsContainer").innerHTML = "<p>Error loading asset details.</p>";
  }
}

function renderAttributes(asset) {
  const container = document.getElementById("attributes");
  container.innerHTML = "";

  const attributes = [
    { label: "Ticker", value: asset.ticker },
    { label: "Name", value: asset.name },
    { label: "Open Price", value: asset.open },
    { label: "Close Price", value: asset.close },
    { label: "Low", value: asset.low },
    { label: "% Change", value: asset.percentChange },
    { label: "Volume", value: asset.volume },
    { label: "Date/Time", value: asset.dateTime },
    { label: "Exchange", value: asset.exchange },
    { label: "Previous Close", value: asset.previousClose },
    { label: "High", value: asset.high }
  ];

  attributes.forEach(attr => {
    const p = document.createElement("p");
    p.textContent = `${attr.label}: ${attr.value}`;
    container.appendChild(p);
  });
}

function renderChart(asset) {
  const ctx = document.getElementById("assetChart").getContext("2d");

  new Chart(ctx, {
    type: 'line',
    data: {
      labels: ["Day 1", "Day 2", "Day 3"],
      datasets: [{
        label: "Close Price",
        data: [asset.close, asset.close + 2, asset.close - 1],
        borderColor: "blue",
        fill: false
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { display: true }
      }
    }
  });
}

document.getElementById("addAsset").addEventListener("click", () => {
  alert(`Added ${ticker} to your portfolio!`);
});

document.getElementById("removeAsset").addEventListener("click", () => {
  alert(`Removed ${ticker} from your portfolio!`);
});

loadAssetDetails();




