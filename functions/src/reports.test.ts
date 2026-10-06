import test from "node:test";
import assert from "node:assert/strict";
import {adminSummary, isFinalizedRevenue} from "./reports.js";

const deliveredCod = {
  id: "order-1", customerId: "customer-1", orderStatus: "DELIVERED", paymentStatus: "COLLECTED",
  totalMinor: 11200, items: [
    {productId: "mutton", name: "Mutton Curry Cut", unit: "KILOGRAM", quantity: 2,
      unitPriceMinor: 5000, lineTotalMinor: 10000},
  ],
};

test("finalized revenue requires delivered and paid or collected", () => {
  assert.equal(isFinalizedRevenue(deliveredCod), true);
  assert.equal(isFinalizedRevenue({...deliveredCod, paymentStatus: "PENDING"}), false);
  assert.equal(isFinalizedRevenue({...deliveredCod, orderStatus: "CANCELLED"}), false);
  assert.equal(isFinalizedRevenue({...deliveredCod, paymentStatus: "PAID"}), true);
});

test("product report uses immutable line snapshot totals", () => {
  const result = adminSummary([deliveredCod], [deliveredCod], "mutton");
  assert.equal(result.revenueMinor, 10000);
  assert.equal(result.orders, 1);
  assert.equal(result.quantitySold, 2);
  assert.equal(result.topProducts[0].unit, "KILOGRAM");
});

test("all-product revenue uses finalized order total without double counting", () => {
  const pending = {...deliveredCod, id: "order-2", orderStatus: "PENDING", paymentStatus: "PENDING"};
  const result = adminSummary([deliveredCod, pending], [deliveredCod, pending], null);
  assert.equal(result.revenueMinor, 11200);
  assert.equal(result.orders, 2);
  assert.equal(result.completedOrders, 1);
});
