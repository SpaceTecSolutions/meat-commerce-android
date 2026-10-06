import assert from "node:assert/strict";
import test from "node:test";
import {applyCategoryFallback} from "./admin-orders.js";

test("order category fallback enriches every item in mixed orders", () => {
  const orders = [{items: [
    {productId: "chicken", name: "Chicken"},
    {productId: "mutton", name: "Mutton", categoryId: "mutton-category", categoryName: "Mutton"},
  ]}];
  const products = new Map([
    ["chicken", {categoryId: "chicken-category", categoryName: "Chicken"}],
    ["mutton", {categoryId: "changed", categoryName: "Changed"}],
  ]);
  const result = applyCategoryFallback(orders, products)[0].items;
  assert.equal(result[0].categoryId, "chicken-category");
  assert.equal(result[0].categoryName, "Chicken");
  assert.equal(result[1].categoryId, "mutton-category");
  assert.equal(result[1].categoryName, "Mutton");
});
