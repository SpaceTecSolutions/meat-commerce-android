import assert from "node:assert/strict";
import test from "node:test";
import {canonicalMobile, mobileLookupId, validPassword} from "./auth-helpers.js";

test("canonicalMobile accepts canonical E164 and removes presentation separators", () => {
  assert.equal(canonicalMobile("+91 98765-43210"), "+919876543210");
  assert.equal(canonicalMobile("9876543210"), null);
});

test("mobile lookup is keyed and stable", () => {
  const first = mobileLookupId("+919876543210", "secret-one");
  assert.equal(first, mobileLookupId("+919876543210", "secret-one"));
  assert.notEqual(first, mobileLookupId("+919876543210", "secret-two"));
  assert.equal(first.length, 64);
});

test("password input is bounded", () => {
  assert.equal(validPassword("Password1"), true);
  assert.equal(validPassword("short"), false);
  assert.equal(validPassword("x".repeat(129)), false);
});
