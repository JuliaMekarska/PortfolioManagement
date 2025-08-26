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
        await renderChart(asset);

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
async function renderChart(asset) {
    try {
        // Fetch the CSV file with last 5 closes
        const res = await fetch("/all_assets_last3closes.csv");
        if (!res.ok) throw new Error("Cannot load CSV with closes");
        const text = await res.text();

        let history = [];
        let foundRow = false;

        // Parse CSV rows
        text.split("\n").slice(1).forEach((row, index) => {
            if (!row.trim()) return; // skip empty lines
            const cols = row.split(",").map(c => c.replace(/"/g, "").trim());

            // Debug log for each row
            console.log(`Row ${index}:`, cols);

            const symbolFromCsv = cols[0];
            const categoryFromCsv = cols[1];
            console.log(symbolFromCsv)
            console.log(asset.ticker)
            if (symbolFromCsv === asset.ticker) {
                foundRow = true;
                // CSV format: Symbol,Category,Close_1,Close_2,Close_3,Close_4,Close_5
                history = cols.slice(2, 7).map(v => parseFloat(v));
                console.log("Matched row history:", history);
            }
        });

        if (!foundRow || history.length === 0) {
            console.warn("No chart data found for", asset.ticker, "in category", asset.category);
            return;
        }

        const ctx = document.getElementById("assetChart").getContext("2d");

        new Chart(ctx, {
            type: 'line',
            data: {
                labels: ["Day 1", "Day 2", "Day 3", "Day 4", "Day 5"], // oldest → newest
                datasets: [{
                    label: `${asset.ticker} Closing Prices`,
                    data: history,
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

    } catch (err) {
        console.error("Chart error:", err);
    }
}
document.getElementById("addAsset").addEventListener("click", () => {
    alert(`Added ${ticker} to your portfolio!`);
});

document.getElementById("removeAsset").addEventListener("click", () => {
    alert(`Removed ${ticker} from your portfolio!`);
});

loadAssetDetails();