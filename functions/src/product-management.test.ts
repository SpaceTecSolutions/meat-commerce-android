import assert from "node:assert/strict";
import test from "node:test";
import {cartProductCount, validCartQuantity, validateRetainedImages} from "./product-management.js";

test("product gallery retains only existing unique images in requested order", () => {
  const existing = ["one.jpg", "two.jpg", "three.jpg"];
  assert.deepEqual(
    validateRetainedImages(existing, ["three.jpg", "one.jpg", "three.jpg"]),
    ["three.jpg", "one.jpg"],
  );
});

test("product gallery rejects a client supplied external retained URL", () => {
  assert.throws(
    () => validateRetainedImages(["trusted.jpg"], ["external.jpg"]),
    /Invalid retained product image/,
  );
});

test("cart quantity is additive and stock bounded", () => {
  assert.equal(validCartQuantity(2, 3, 10), 5);
  assert.throws(() => validCartQuantity(2, 3, 4), /out of stock/);
  assert.throws(() => validCartQuantity(0, 1, 0), /out of stock/);
});

test("cart badge counts distinct products instead of summed quantity", () => {
  assert.equal(cartProductCount(2, true), 2);
  assert.equal(cartProductCount(2, false), 3);
});
