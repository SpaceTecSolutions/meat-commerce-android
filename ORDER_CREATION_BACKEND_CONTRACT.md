# Order creation backend contract

`customerPlaceOrder` is the only trusted order-creation boundary. In one server-side transaction it revalidates the checkout quote, stock, prices, delivery fees, payment availability and feature configuration, allocates an order number, decrements stock, and persists the order.

## Initial state

Every newly created order has `orderStatus: "PENDING"`, including online-payment orders. Only an authorized Admin workflow may later confirm it. COD starts with `paymentStatus: "PENDING"`; online payment follows its verification state without changing the order's confirmation state.

The success UI therefore says **Order Placed Successfully** and **Waiting for shop confirmation**. It must never infer confirmation from payment success.

## Immutable item snapshots

The order document stores an `items` array. Each entry is copied from the authoritative catalog and validated cart at creation time:

```text
productId, name, imageUrl, unit, quantity,
unitPriceMinor, regularPriceMinor, lineTotalMinor, attributes
```

These values are historical facts. Product edits, deactivation, deletion, price changes, and image changes must not rewrite them. Refunds and reports use order snapshots, not current product documents.

## Required response

The callable returns `order` containing:

```text
orderId, orderNumber, orderStatus, paymentStatus,
totalMinor, currencyCode, estimatedDelivery, items
```

`estimatedDelivery` is a customer-ready string calculated from the chosen delivery option/slot. Retries with the same customer-scoped idempotency key return the same order and snapshots.
