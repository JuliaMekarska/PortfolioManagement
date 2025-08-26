import yfinance as yf
import pandas as pd
import csv

# Define input and output files for each category
categories = {
    "stocks": "C:/Users/Administrator/Desktop/PortfolioManagement/PortfolioManagement/src/main/resources/data/stocks.csv",
    "crypto": "C:/Users/Administrator/Desktop/PortfolioManagement/PortfolioManagement/src/main/resources/data/crypto.csv",
    "etf": "C:/Users/Administrator/Desktop/PortfolioManagement/PortfolioManagement/src/main/resources/data/etf.csv",
    "commodities": "C:/Users/Administrator/Desktop/PortfolioManagement/PortfolioManagement/src/main/resources/data/commodities.csv"
}

for category, in_file in categories.items():
    # Read symbols from CSV (assuming one symbol per line, no header)
    df_symbols = pd.read_csv(in_file, header=None)
    symbols = df_symbols.iloc[:, 0].astype(str).tolist()

    # Fetch and collect data
    all_data = []
    for symbol in symbols:
        ticker = yf.Ticker(symbol)
        data = ticker.history(period="1d")  # get most recent trading day
        if data.empty:
            continue  # skip if no data
        data = data.reset_index()  # make Date a column
        data['Symbol'] = symbol    # add symbol column

        # Fetch company name and previous close
        company_name = ticker.info.get("longName") or ticker.info.get("shortName") or ""
        previous_close = ticker.info.get("previousClose")

        data['CompanyName'] = company_name
        data['PreviousClose'] = previous_close

        print(f"Fetched {symbol} -> {company_name}, Previous Close: {previous_close}")  # debug output

        all_data.append(data)

    if not all_data:
        continue

    # Concatenate all fetched data
    df = pd.concat(all_data, ignore_index=True)

    # Reorder columns for consistency: Date, Open, High, Low, Close, Volume, PreviousClose, CompanyName, Symbol
    df = df[['Date', 'Open', 'High', 'Low', 'Close', 'Volume', 'PreviousClose', 'CompanyName', 'Symbol']]

    out_file = f"C:/Users/Administrator/Desktop/PortfolioManagement/PortfolioManagement/src/main/resources/data/{category}_data.csv"
    df.to_csv(out_file, index=False, quoting=csv.QUOTE_ALL)
    print(f"Wrote {len(df)} rows to {out_file}")