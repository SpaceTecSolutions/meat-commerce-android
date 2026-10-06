import assert from "node:assert/strict";
import test from "node:test";
import {aggregateProductSales, homeLocationLabel, selectHomeBestSellers, selectHomeCategories} from "./customer-home.js";

test("home categories include active entries in configured order", () => {
  const result = selectHomeCategories([
    {id: "fish", data: {name: "Fish", active: true, sortOrder: 2}},
    {id: "hidden", data: {name: "Hidden", active: false, sortOrder: 0}},
    {id: "chicken", data: {name: "Chicken", active: true, sortOrder: 1}},
  ]);
  assert.deepEqual(result.map((item) => item.id), ["chicken", "fish"]);
});

test("best sellers fall back to bounded order item sales", () => {
  const now = Date.now();
  const sales = aggregateProductSales([
    {createdAtEpochMillis: now, orderStatus: "DELIVERED", items: [{productId: "chicken", quantity: 3}]},
    {createdAtEpochMillis: now, orderStatus: "CANCELLED", items: [{productId: "mutton", quantity: 20}]},
  ], now - 30 * 86400000).thirtyDay;
  const result = selectHomeBestSellers([
    {id: "mutton", data: {name: "Mutton"}}, {id: "chicken", data: {name: "Chicken"}},
  ], sales);
  assert.deepEqual(result.map((item) => item.id), ["chicken"]);
});

test("best sellers use real ranking or explicit featured state", () => {
  const result = selectHomeBestSellers([
    {id: "ordinary", data: {name: "Ordinary", salesCount: 0}},
    {id: "featured", data: {name: "Featured", isFeatured: true}},
    {id: "popular", data: {name: "Popular", salesCount: 12}},
  ]);
  assert.deepEqual(result.map((item) => item.id), ["popular", "featured"]);
});

test("location label is compact and never hardcoded", () => {
  assert.equal(homeLocationLabel({address: "23, 3rd Cross", city: "Bengaluru"}), "23, Bengaluru");
  assert.equal(homeLocationLabel({}), "Select location");
});
