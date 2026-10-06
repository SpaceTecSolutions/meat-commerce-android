import assert from "node:assert/strict";
import test from "node:test";
import {integer, normalize, number, text} from "./catalog-common.js";

test("catalog text normalization is stable and case insensitive", () => {
  assert.equal(normalize("  Fresh   Chicken  "), "fresh chicken");
  assert.equal(normalize("MUTTON"), "mutton");
});

test("catalog primitive parsing rejects invalid values", () => {
  assert.equal(text(null, "fallback"), "fallback");
  assert.equal(integer("12"), 12);
  assert.equal(integer("12.5", 3), 3);
  assert.equal(number("12.5"), 12.5);
  assert.equal(number("invalid", 4), 4);
});
