const portfolioInfo = document.getElementById('portfolioInfo');
const portfolioAssetsDiv = document.getElementById('portfolioAssets');
const portfolioTransactionsDiv = document.getElementById('portfolioTransactions');

const userId = 1;

function fetchPortfolio() {
    fetch(`/api/portfolio/${userId}`)
        .then(res => res.json())
        .then(data => {
            portfolioInfo.innerHTML = `
                <p><strong>User:</strong> ${data.user}</p>
                <p><strong>Balance:</strong> ${data.balance}</p>
                <p><strong>Profit/Loss:</strong> ${data.profitLoss}</p>
            `;

            portfolioAssetsDiv.innerHTML = "";
            data.assets.forEach(asset => {
                const card = document.createElement('div');
                card.className = 'asset-card';
                card.id = `asset-${asset.id}`;
                card.innerHTML = `
                    <h3>${asset.asset.name} (${asset.asset.ticker})</h3>
                    <p>Quantity: <span id="assetQty-${asset.id}">${asset.quantity}</span></p>
                    <p>Purchase Price: <span id="assetPrice-${asset.id}">${asset.purchasePrice}</span></p>
                    <label>Quantity to sell:</label>
                    <input type="number" step="0.01" id="sellAmount-${asset.id}" value="1">
                    <label>Price to sell:</label>
                    <input type="number" step="0.01" id="sellPrice-${asset.id}" value="${asset.asset.closePrice ?? 0}">
                    <button id="sellBtn-${asset.id}">Sell</button>
                    <p id="sellMsg-${asset.id}" style="color:green;"></p>
                `;
                portfolioAssetsDiv.appendChild(card);

                document.getElementById(`sellBtn-${asset.id}`).addEventListener("click", () => {
                    const amount = parseFloat(document.getElementById(`sellAmount-${asset.id}`).value);
                    const price = parseFloat(document.getElementById(`sellPrice-${asset.id}`).value);

                    if (amount > asset.quantity) {
                        document.getElementById(`sellMsg-${asset.id}`).textContent =
                            `❌ You can not sell more than you have (${asset.quantity})`;
                        document.getElementById(`sellMsg-${asset.id}`).style.color = "red";
                        return;
                    }

                    fetch(`/api/transactions/sell?userId=${userId}&assetId=${asset.id}&amount=${amount}&price=${price}`, {
                        method: "POST"
                    })
                    .then(res => res.json())
                    .then(tx => {
                        document.getElementById(`sellMsg-${asset.id}`).textContent =
                            `✅ Sold: ${tx.amount} assets of ${tx.asset.name} at ${tx.price} each`;
                        fetchPortfolio();
                    })
                    .catch(err => {
                        document.getElementById(`sellMsg-${asset.id}`).textContent = `❌ ${err}`;
                        document.getElementById(`sellMsg-${asset.id}`).style.color = "red";
                    });
                });
            });

            portfolioTransactionsDiv.innerHTML = "";
            data.transactions.forEach(tx => {
                const txDiv = document.createElement('div');
                txDiv.className = 'transaction';
                txDiv.id = `tx-${tx.id}`;
                txDiv.innerHTML = `
                    ${tx.type} => ${tx.amount} assets of ${tx.asset.name} at ${tx.price} each
                    <button class="editTx" data-id="${tx.id}">✏️ Edit</button>
                    <button class="deleteTx" data-id="${tx.id}">🗑️ Delete</button>
                    <div id="editForm-${tx.id}" style="display:none; margin-top:5px;">
                        <label>Quantity:</label>
                        <input type="number" step="0.01" id="editAmount-${tx.id}" value="${tx.amount}">
                        <label>Price:</label>
                        <input type="number" step="0.01" id="editPrice-${tx.id}" value="${tx.price}">
                        <label>Type:</label>
                        <select id="editType-${tx.id}">
                            <option value="BUY" ${tx.type === 'BUY' ? 'selected' : ''}>BUY</option>
                            <option value="SELL" ${tx.type === 'SELL' ? 'selected' : ''}>SELL</option>
                        </select>
                        <button class="confirmEdit" data-id="${tx.id}">✅ Update</button>
                        <p id="editMsg-${tx.id}" style="color:green;"></p>
                    </div>
                `;
                portfolioTransactionsDiv.appendChild(txDiv);
            });

            document.querySelectorAll(".deleteTx").forEach(btn => {
                btn.addEventListener("click", (e) => {
                    const txId = e.target.dataset.id;
                    const txType = data.transactions.find(t => t.id == txId)?.type;
                    const txAmount = data.transactions.find(t => t.id == txId)?.amount;
                    const txAssetId = data.transactions.find(t => t.id == txId)?.asset.id;

                    fetch(`/api/transactions/${txId}?userId=${userId}`, { method: "DELETE" })
                        .then(res => {
                            if (!res.ok) throw new Error("Error while deleting a transaction");

                            if (txType === 'SELL') {
                                const assetCard = document.getElementById(`asset-${txAssetId}`);
                                if (assetCard) {
                                    const qtySpan = assetCard.querySelector(`#assetQty-${txAssetId}`);
                                    qtySpan.textContent = parseFloat(qtySpan.textContent) + parseFloat(txAmount);
                                }
                            }

                            fetchPortfolio();
                        })
                        .catch(err => alert(`❌ ${err}`));
                });
            });

            document.querySelectorAll(".editTx").forEach(btn => {
                btn.addEventListener("click", (e) => {
                    const txId = e.target.dataset.id;
                    const form = document.getElementById(`editForm-${txId}`);
                    form.style.display = form.style.display === "none" ? "block" : "none";
                });
            });

            document.querySelectorAll(".confirmEdit").forEach(btn => {
                btn.addEventListener("click", (e) => {
                    const txId = e.target.dataset.id;
                    const amount = document.getElementById(`editAmount-${txId}`).value;
                    const price = document.getElementById(`editPrice-${txId}`).value;
                    const type = document.getElementById(`editType-${txId}`).value;

                    fetch(`/api/transactions/${txId}/edit?userId=${userId}&amount=${amount}&price=${price}&type=${type}`, { method: "PUT" })
                        .then(res => res.json())
                        .then(updatedTx => {
                            document.getElementById(`editForm-${txId}`).style.display = "none";
                            fetchPortfolio();
                        })
                        .catch(err => {
                            document.getElementById(`editMsg-${txId}`).textContent = `❌ ${err}`;
                            document.getElementById(`editMsg-${txId}`).style.color = "red";
                        });
                });
            });

        });
}

fetchPortfolio();
