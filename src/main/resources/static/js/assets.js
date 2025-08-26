const assetsList = document.getElementById('assetsList');
const marketSelect = document.getElementById('marketType');
const assetDetailsDiv = document.getElementById('assetDetails');

if (marketSelect) {
    fetch('/api/market-types')
        .then(res => res.json())
        .then(types => {
            types.forEach(type => {
                const option = document.createElement('option');
                option.value = type.name;
                option.textContent = type.name;
                marketSelect.appendChild(option);
            });
        });
}

function fetchAssets(market = "") {
    let url = '/api/assets';
    if (market) url = `/api/assets/market/${market}`;

    fetch(url)
        .then(res => res.json())
        .then(assets => {
            if (assetsList) {
                assetsList.innerHTML = "";
                assets.forEach(asset => {
                    const card = document.createElement('div');
                    card.className = 'asset-card';
                    card.innerHTML = `
                        <h3>${asset.name} (${asset.ticker})</h3>
                        <p>Price: ${asset.closePrice ?? 'N/A'}</p>
                        <button class="detailsBtn" data-ticker="${asset.ticker}">Details</button>
                        <button class="addBtn" data-ticker="${asset.ticker}" data-close="${asset.closePrice ?? 0}">Add</button>
                        <div class="addForm" id="form-${asset.ticker}" style="display:none; margin-top:10px;">
                            <label>Quantity:</label>
                            <input type="number" step="0.01" id="amount-${asset.ticker}" value="1">
                            <br>
                            <label>Price:</label>
                            <input type="number" step="0.01" id="price-${asset.ticker}" value="${asset.closePrice ?? 0}">
                            <br>
                            <button class="confirmAdd" data-ticker="${asset.ticker}">✅ Add to Portfolio</button>
                            <p id="msg-${asset.ticker}" style="color:green;"></p>
                        </div>
                    `;
                    assetsList.appendChild(card);
                });

                document.querySelectorAll(".detailsBtn").forEach(btn => {
                    btn.addEventListener("click", (e) => {
                        const ticker = e.target.dataset.ticker;
                        window.location.href = `asset-details.html?ticker=${ticker}`;
                    });
                });

                document.querySelectorAll(".addBtn").forEach(btn => {
                    btn.addEventListener("click", (e) => {
                        const ticker = e.target.dataset.ticker;
                        const formDiv = document.getElementById(`form-${ticker}`);
                        formDiv.style.display = formDiv.style.display === "none" ? "block" : "none";
                    });
                });

                document.querySelectorAll(".confirmAdd").forEach(btn => {
                    btn.addEventListener("click", (e) => {
                        const ticker = e.target.dataset.ticker;
                        const amount = document.getElementById(`amount-${ticker}`).value;
                        const price = document.getElementById(`price-${ticker}`).value;
                        const userId = 1;

                        fetch(`/api/transactions/buy?userId=${userId}&ticker=${ticker}&amount=${amount}&price=${price}`, {
                            method: "POST"
                        })
                        .then(res => {
                            if (!res.ok) throw new Error("Error while adding a new transaction");
                            return res.json();
                        })
                        .then(data => {
                            document.getElementById(`msg-${ticker}`).textContent =
                                `✅ Added: ${data.amount} assets of ${data.asset.name} sold at ${data.price} each`;
                            document.getElementById(`form-${ticker}`).style.display = "none";
                        })
                        .catch(err => {
                            document.getElementById(`msg-${ticker}`).textContent = `❌ ${err}`;
                            document.getElementById(`msg-${ticker}`).style.color = "red";
                        });
                    });
                });
            }
        });
}

if (marketSelect) {
    marketSelect.addEventListener('change', (e) => {
        fetchAssets(e.target.value);
    });
    fetchAssets();
}

if (assetDetailsDiv) {
    const params = new URLSearchParams(window.location.search);
    const ticker = params.get('ticker');

    if (ticker) {
        fetch(`/api/assets/${ticker}`)
            .then(res => res.json())
            .then(asset => {
                assetDetailsDiv.innerHTML = `
                    <h2>${asset.name} (${asset.ticker})</h2>
                    <p><strong>Close price:</strong> ${asset.closePrice ?? 'N/A'}</p>
                    <p><strong>Last Updated:</strong> ${asset.lastUpdated ?? 'N/A'}</p>
                    <p><strong>Market Type:</strong> ${asset.marketType?.name ?? 'N/A'}</p>
                    <div class="addForm" style="margin-top:10px;">
                        <label>Quantity:</label>
                        <input type="number" step="0.01" id="amount-detail" value="1">
                        <br>
                        <label>Price:</label>
                        <input type="number" step="0.01" id="price-detail" value="${asset.closePrice ?? 0}">
                        <br>
                        <button id="buyButton">✅ Add to Portfolio</button>
                        <p id="buyMessage" style="color:green;"></p>
                    </div>
                `;

                const buyButton = document.getElementById("buyButton");
                buyButton.addEventListener("click", () => {
                    const amount = document.getElementById("amount-detail").value;
                    const price = document.getElementById("price-detail").value;
                    const userId = 1;

                    fetch(`/api/transactions/buy?userId=${userId}&ticker=${ticker}&amount=${amount}&price=${price}`, {
                        method: "POST"
                    })
                    .then(res => res.json())
                    .then(data => {
                        document.getElementById("buyMessage").textContent =
                            `✅ Added to your portfolio: ${data.amount} szt. ${data.asset.name} po ${data.price}`;
                    })
                    .catch(err => {
                        document.getElementById("buyMessage").textContent = `❌ ${err}`;
                        document.getElementById("buyMessage").style.color = "red";
                    });
                });
            })
            .catch(err => {
                assetDetailsDiv.innerHTML = `<p style="color:red;">Error loading asset: ${err}</p>`;
            });
    } else {
        assetDetailsDiv.innerHTML = `<p>No ticker provided.</p>`;
    }
}
