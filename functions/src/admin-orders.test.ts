import assert from "node:assert/strict";
import test from "node:test";
import {applyCategoryFallback, finalBillForReady, orderResponse} from "./admin-orders.js";

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

const packedOrder = {paymentMethod: "COD", discountMinor: 0, deliveryFeeMinor: 200,
  taxMinor: 0, totalMinor: 2700, items: [
    {productId: "meat", unit: "KILOGRAM", quantity: 2, unitPriceMinor: 1000, lineTotalMinor: 2000},
    {productId: "bag", unit: "PIECE", quantity: 1, unitPriceMinor: 500, lineTotalMinor: 500},
  ]};

test("ready step finalizes changed packed weight and amount", () => {
  const result = finalBillForReady(packedOrder, [{productId: "meat", finalQuantity: 1.5}]);
  assert.equal(result.items[0].finalQuantity, 1.5);
  assert.equal(result.items[0].lineTotalMinor, 1500);
  assert.equal(result.items[1].lineTotalMinor, 500);
  assert.equal(result.subtotalMinor, 2000);
  assert.equal(result.totalMinor, 2200);
  assert.equal(result.finalBillAdjusted, true);
});

test("ready step keeps estimate when packed weight is unchanged", () => {
  const result = finalBillForReady(packedOrder, []);
  assert.equal(result.totalMinor, 2700);
  assert.equal(result.finalBillAdjusted, false);
});

test("ready step preserves a previously finalized legacy order", () => {
  const legacy = {...packedOrder, items: [{...packedOrder.items[0], finalQuantity: 1.5,
    estimatedQuantity: 2, estimatedLineTotalMinor: 2000, lineTotalMinor: 1500}, packedOrder.items[1]],
  totalMinor: 2200, estimatedTotalMinor: 2700};
  const result = finalBillForReady(legacy, []);
  assert.equal(result.totalMinor, 2200);
  assert.equal(result.estimatedTotalMinor, 2700);
  assert.equal(result.items[0].estimatedLineTotalMinor, 2000);
});

test("ready step rejects changing a non-weight item", () => {
  assert.throws(() => finalBillForReady(packedOrder, [{productId: "bag", finalQuantity: 2}]));
});

test("order response includes the latest delivery slot reason", () => {
  const result = orderResponse("order-1", {...packedOrder, deliverySlotChangeReason: "Shop preparation delay"});
  assert.equal(result.deliverySlotChangeReason, "Shop preparation delay");
});
