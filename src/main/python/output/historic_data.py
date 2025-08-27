import yfinance as yf
import pandas as pd
import csv

categories = {
    "stocks": "src/main/resources/data/stocks.csv",
    "crypto": "src/main/resources/data/crypto.csv",
    "etf": "src/main/resources/data/etf.csv",
    "commodities": "src/main/resources/data/commodities.csv"
}

all_rows = []
for category, in_file in categories.items():
    df_symbols = pd.read_csv(in_file, header=None)
    symbols = df_symbols.iloc[:, 0].astype(str).tolist()

    for symbol in symbols:
        ticker = yf.Ticker(symbol)
        data = ticker.history(period="5d")
        if data.empty:
            continue

        closes = data["Close"].tail(5).tolist()
        while len(closes) < 5:
            closes.insert(0, None)

        all_rows.append({
            "Symbol": symbol,
            "Category": category,
            "Close_1": closes[-1],
            "Close_2": closes[-2],
            "Close_3": closes[-3],
            "Close_4": closes[-4],
            "Close_5": closes[-5],
        })

        print(f"Fetched {symbol} ({category}) -> last 5 closes: {closes}")

if all_rows:
    out_file = "src/main/resources/data/all_assets_last3closes.csv"
    df_out = pd.DataFrame(all_rows)
    df_out.to_csv(out_file, index=False, quoting=csv.QUOTE_ALL)
    print(f"Wrote {len(df_out)} rows to {out_file}")