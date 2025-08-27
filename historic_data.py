# fetch_last_closes.py
import yfinance as yf
import pandas as pd
import csv

# Input CSVs (like in your working script)
categories = {
    "stocks": "src/main/resources/data/stocks.csv",
    "crypto": "src/main/resources/data/crypto.csv",
    "etf": "src/main/resources/data/etf.csv",
    "commodities": "src/main/resources/data/commodities.csv"
}

all_rows = []  # collect all categories together

for category, in_file in categories.items():
    # Read symbols (1 per line, no header)
    df_symbols = pd.read_csv(in_file, header=None)
    symbols = df_symbols.iloc[:, 0].astype(str).tolist()

    for symbol in symbols:
        ticker = yf.Ticker(symbol)
        # Get last 5 trading days of data
        data = ticker.history(period="5d")
        if data.empty:
            continue

        closes = data["Close"].tail(5).tolist()  # last 3 closes
        # Pad with None if fewer than 3
        while len(closes) < 5:
            closes.insert(0, None)

        all_rows.append({
            "Symbol": symbol,
            "Category": category,
            "Close_1": closes[-1],  # most recent
            "Close_2": closes[-2],
            "Close_3": closes[-3],
            "Close_4": closes[-4],
            "Close_5": closes[-5],
        })

        print(f"Fetched {symbol} ({category}) -> last 5 closes: {closes}")

# Save to single CSV
if all_rows:
    out_file = "src/main/resources/data/all_assets_last3closes.csv"
    df_out = pd.DataFrame(all_rows)
    df_out.to_csv(out_file, index=False, quoting=csv.QUOTE_ALL)
    print(f"Wrote {len(df_out)} rows to {out_file}")