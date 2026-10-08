# Implementation Tasks

## Task 1: Fix GET /practice/stocks returning HTTP 500

- [ ] 1.1 Inspect V4, V5, V6 migrations to confirm the `practice.stocks` table column names match the `Stock` JPA entity (`symbol`, `name`, `price`, `change_pct`, `emoji`, `trend_json`).
- [ ] 1.2 If `practice.stocks` is missing data, write a new Flyway migration `V13__seed_stocks.sql` that inserts the 3 seeded stocks (RELIANCE, TCS, INFY) into `practice.stocks` — matching the `StockSeeder` values. Do not remove `StockSeeder`; the migration handles the DB-first path.
- [ ] 1.3 Confirm the `Order` entity FK `symbol` references `practice.symbols`, and that the same 3 symbols exist in `practice.symbols` (already seeded in V8). If the stocks are not in `practice.symbols`, add them to V8 or a new migration.
- [ ] 1.4 Start the practice-service and verify `GET /practice/stocks` returns HTTP 200 with a JSON array of stock objects.

---

## Task 2: Add GET /practice/account endpoint

- [ ] 2.1 Add an `AccountView` record inside `PracticeController` with fields: `balance` (BigDecimal), `reservedBalance` (BigDecimal), and `positions` (List of a `PositionView` record containing `symbol`, `quantity`, `reservedQuantity`).
- [ ] 2.2 Add a `GET /practice/account` handler in `PracticeController` that calls `paperOrderService.getOrExposeAccount(userId)` — or directly queries `PaperAccountRepository` and `PaperPositionRepository` — and returns `AccountView`.
- [ ] 2.3 Make `getOrCreateAccount` accessible (or duplicate the logic in the controller) so the endpoint auto-creates a `PaperAccount` with ₹1,00,000 for new users.
- [ ] 2.4 Ensure the endpoint is protected (returns 401 for unauthenticated requests) via the existing `ResourceServerSecurityConfig` in `common-security`.

---

## Task 3: Add DELETE /practice/orders/{orderId} cancel endpoint

- [ ] 3.1 Add a `cancelOrder` method in `PaperOrderService` that: finds the order by ID, verifies it belongs to the caller, checks status is `OPEN`, sets status to `CANCELLED`, and releases `reservedBalance` (for LIMIT BUY) or `reservedQuantity` (for LIMIT SELL) within a single `@Transactional` boundary.
- [ ] 3.2 Add a `DELETE /practice/orders/{orderId}` handler in `PracticeController` that calls `paperOrderService.cancelOrder(userId, orderId)` and returns HTTP 204 on success.
- [ ] 3.3 Throw and map appropriate `ResponseStatusException`s: 404 if not found, 403 if wrong user, 400 if order is not OPEN.

---

## Task 4: Expose `side` and `orderId` in OrderReceipt

- [ ] 4.1 Add `side` (String) and `orderId` (UUID) fields to the `OrderReceipt` record in `PracticeController` — these are needed by the frontend TradeSuccessScreen and PaperPortfolioScreen.
- [ ] 4.2 Update all `OrderReceipt` construction sites in `PracticeController` (placeOrder and myOrders) to populate the new fields.

---

## Task 5: Backend property-based tests

- [ ] 5.1 Add `jqwik` (or a compatible PBT library) as a `test` dependency in `practice-service/pom.xml`.
- [ ] 5.2 Write a `PaperOrderServicePBT` test class with the following properties:
  - **Property 1** — AvailableBalance never goes negative: after any sequence of market buys, limit buy placements, and limit sells, assert `balance − reservedBalance ≥ 0`.
  - **Property 2** — Market buy always produces `COMPLETE` status.
  - **Property 3** — Limit buy placement sets status `OPEN` and increments `reservedBalance` by exactly `limitPrice × quantity`.
  - **Property 4** — Limit sell with quantity > available shares is rejected with `IllegalArgumentException`.
  - **Property 5** — Cancel limit buy reduces `reservedBalance` by exactly `limitPrice × quantity`.
  - **Property 6** — Cancel limit sell reduces `reservedQuantity` by exactly `quantity`.
- [ ] 5.3 Run tests with `mvn test -pl practice-service` and confirm all properties pass.

---

## Task 6: Frontend — PracticeScreen dashboard

> Frontend lives in the React Native / Expo project. All paths below are relative to the frontend workspace root.

- [ ] 6.1 Add (or update) `API_ENDPOINTS.PRACTICE.ACCOUNT` pointing to `GET /practice/account` alongside the existing `STOCKS` and `ORDERS` constants.
- [ ] 6.2 Create a `usePracticeAccount` hook (or equivalent) that fetches `/practice/account` and returns `{ balance, reservedBalance, positions, loading, error }`.
- [ ] 6.3 In `PracticeScreen`, fire `usePracticeAccount` and the existing stocks fetch in parallel on mount.
- [ ] 6.4 Compute and display:
  - "Cash" = `balance − reservedBalance`
  - "Invested" = sum of `(position.quantity × stock.price)` for each held position
  - "Return %" = `((invested + cash − 100000) / 100000) × 100`
- [ ] 6.5 Render the stock list rows with symbol, name, price, changePct (coloured green/red), emoji, and a Sparkline component built from the `trend` JSON array.
- [ ] 6.6 On stock row tap: if user holds ≥ 1 share, show a choice sheet (Buy / Sell); otherwise open BuyModal directly.
- [ ] 6.7 Show loading skeletons while either fetch is in flight; show an inline error + retry button if stocks fetch fails.

---

## Task 7: Frontend — BuyModal

- [ ] 7.1 Create `BuyModal` as a bottom-sheet (or modal) component accepting `{ symbol, stockPrice, availableBalance, onClose }` props.
- [ ] 7.2 Add a segmented control for "MARKET" / "LIMIT" order type.
- [ ] 7.3 Add a quantity stepper (min 1) with "+" and "−" buttons.
- [ ] 7.4 For LIMIT orders, add a numeric text input for `limitPrice`; default it to `stockPrice`.
- [ ] 7.5 Display estimated cost = `quantity × (orderType === 'LIMIT' ? limitPrice : stockPrice)`.
- [ ] 7.6 Disable the submit button and show "Insufficient funds" if estimated cost > `availableBalance`.
- [ ] 7.7 On submit, POST to `/practice/orders` with `{ symbol, side: 'BUY', shares: quantity, orderType, price: limitPrice | undefined }`. Disable button while in-flight.
- [ ] 7.8 On success, close modal and navigate to `TradeSuccessScreen` passing the `OrderReceipt`.
- [ ] 7.9 On error, display the error message inline without closing the modal.

---

## Task 8: Frontend — SellModal

- [ ] 8.1 Create `SellModal` as a bottom-sheet component accepting `{ symbol, stockPrice, heldQuantity, reservedQuantity, onClose }` props.
- [ ] 8.2 Display "You hold X shares (Y available)" where available = `heldQuantity − reservedQuantity`.
- [ ] 8.3 Add segmented control for "MARKET" / "LIMIT".
- [ ] 8.4 Add quantity stepper with min 1, max = `heldQuantity − reservedQuantity`.
- [ ] 8.5 For LIMIT orders, add a numeric input for `limitPrice`.
- [ ] 8.6 Display estimated proceeds = `quantity × (orderType === 'LIMIT' ? limitPrice : stockPrice)`.
- [ ] 8.7 Disable submit and show error if quantity > available shares.
- [ ] 8.8 On submit, POST `/practice/orders` with `{ symbol, side: 'SELL', shares: quantity, orderType, price: limitPrice | undefined }`. Disable while in-flight.
- [ ] 8.9 On success, close and navigate to `TradeSuccessScreen` with `OrderReceipt`.
- [ ] 8.10 On error, display inline error without closing.

---

## Task 9: Frontend — TradeSuccessScreen

- [ ] 9.1 Create `TradeSuccessScreen` that accepts an `OrderReceipt` via navigation params.
- [ ] 9.2 Show a confetti animation on mount (use `react-native-confetti-cannon` or equivalent already in the project; do not add new dependencies without checking existing ones first).
- [ ] 9.3 Display: symbol, orderType, side, shares, pricePerShare (formatted ₹), totalPaid (formatted ₹), xpEarned, and orderId.
- [ ] 9.4 When `status === 'OPEN'`, show a banner: "Your limit order is pending execution."
- [ ] 9.5 Provide a "Keep Trading" button that navigates back to `PracticeScreen`.
- [ ] 9.6 Provide a "View Portfolio" button that navigates to `PaperPortfolioScreen`.

---

## Task 10: Frontend — PaperPortfolioScreen

- [ ] 10.1 Create `PaperPortfolioScreen`. On mount, fetch `GET /practice/account`, `GET /practice/orders`, and `GET /practice/stocks` in parallel.
- [ ] 10.2 Show a holdings section: one card per position where `quantity > 0`, displaying symbol, quantity, current price (from stocks), and unrealised P&L (`(currentPrice − avgCost) × quantity`). Derive `avgCost` from the COMPLETE BUY orders for that symbol from the orders list.
- [ ] 10.3 Show a cash section: "Available ₹X" and "Reserved ₹Y" (from AccountView).
- [ ] 10.4 Show an open orders section: list all orders where `status === 'OPEN'`, displaying symbol, side, orderType, price, shares, and a "Cancel" button.
- [ ] 10.5 "Cancel" button calls `DELETE /practice/orders/{orderId}`. On success, remove from list and refresh the account. On error, show toast/inline error.
- [ ] 10.6 Show loading skeletons while data is loading.

---

## Task 11: Frontend — PaperPortfolioChart

- [ ] 11.1 Create `PaperPortfolioChart` component using Victory Native area chart.
- [ ] 11.2 Add interval toggle buttons: 1D, 1W, 1M.
- [ ] 11.3 Replay completed orders chronologically to build portfolio value data points: start at ₹1,00,000 and adjust on each COMPLETE order.
- [ ] 11.4 Filter data points to the selected interval (last 1 day / 7 days / 30 days).
- [ ] 11.5 When no completed orders exist, render a flat line at ₹1,00,000 with the message "Place your first trade to see performance."
- [ ] 11.6 Label the Y-axis in ₹ (Indian Rupee format). Embed this chart in `PaperPortfolioScreen`.

---

## Task 12: Integration smoke test

- [ ] 12.1 Verify end-to-end: `GET /practice/stocks` returns HTTP 200 with stock list.
- [ ] 12.2 Verify `GET /practice/account` returns HTTP 200 with balance ₹1,00,000 for a fresh user.
- [ ] 12.3 Verify `POST /practice/orders` MARKET BUY returns `status: "COMPLETE"`.
- [ ] 12.4 Verify `POST /practice/orders` MARKET SELL returns `status: "COMPLETE"`.
- [ ] 12.5 Verify `POST /practice/orders` LIMIT BUY returns `status: "OPEN"` and `GET /practice/account` shows reduced available balance.
- [ ] 12.6 Verify `POST /practice/orders` LIMIT SELL returns `status: "OPEN"` and position `reservedQuantity` increases.
- [ ] 12.7 Verify `DELETE /practice/orders/{orderId}` for a LIMIT BUY releases `reservedBalance` and returns 204.
- [ ] 12.8 Verify the frontend PracticeScreen loads stocks and account balance without errors.
- [ ] 12.9 Verify BuyModal and SellModal submit orders and navigate to TradeSuccessScreen.
- [ ] 12.10 Verify PaperPortfolioScreen shows holdings, open orders, and cancel works.
