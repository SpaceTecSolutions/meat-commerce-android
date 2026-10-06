import assert from "node:assert/strict";
import test from "node:test";
import {summarizeDashboard, DashboardOrder} from "./admin-dashboard.js";

const range = {
  start: new Date("2026-09-01T00:00:00.000Z"),
  end: new Date("2026-09-08T00:00:00.000Z"),
  label: "This Week",
  period: "THIS_WEEK",
};

function order(overrides: Partial<DashboardOrder>): DashboardOrder {
  return {id: "order", orderStatus: "DELIVERED", paymentStatus: "COLLECTED",
    deliveredAtEpochMillis: Date.parse("2026-09-01T10:00:00.000Z"), totalMinor: 1000,
    currencyCode: "INR", items: [], ...overrides};
}

test("dashboard revenue only includes finalized payments", () => {
  const collected = order({id: "collected", totalMinor: 1200});
  const pendingPayment = order({id: "pending", paymentStatus: "PENDING", totalMinor: 900});
  const cancelled = order({id: "cancelled", orderStatus: "CANCELLED", totalMinor: 800});
  const summary = summarizeDashboard([], [collected, pendingPayment, cancelled], [collected], range);
  assert.equal(summary.todayRevenueMinor, 1200);
});

test("top product ranks immutable order item revenue", () => {
  const first = order({id: "one", items: [{productId: "mutton", name: "Mutton Curry Cut",
    unit: "kg", quantity: 2, lineTotalMinor: 1500}]});
  const second = order({id: "two", items: [{productId: "chicken", name: "Chicken",
    unit: "kg", quantity: 1, lineTotalMinor: 500}]});
  const summary = summarizeDashboard([], [first, second], [first, second], range);
  assert.equal(summary.topSellingProduct?.productId, "mutton");
  assert.equal(summary.topSellingProduct?.quantitySold, 2);
});
