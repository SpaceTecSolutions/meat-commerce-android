import assert from "node:assert/strict";
import test from "node:test";
import {calculateDeliveryFee} from "./customer-cart.js";

test("delivery charge is applied below the configured threshold", () => {
  assert.equal(calculateDeliveryFee(49_900, 5_000, 50_000), 5_000);
});

test("delivery is free at and above the configured threshold", () => {
  assert.equal(calculateDeliveryFee(50_000, 5_000, 50_000), 0);
  assert.equal(calculateDeliveryFee(75_000, 5_000, 50_000), 0);
});

test("delivery charge remains configured when no free threshold exists", () => {
  assert.equal(calculateDeliveryFee(75_000, 5_000, null), 5_000);
});
