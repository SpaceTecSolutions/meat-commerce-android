import assert from "node:assert/strict";
import test from "node:test";
import {strongPassword, supportsOtpSignInRole} from "./customer-auth.js";

test("customer password policy requires mixed case and a digit", () => {
  assert.equal(strongPassword("Customer1"), true);
  assert.equal(strongPassword("customer1"), false);
  assert.equal(strongPassword("CUSTOMER1"), false);
  assert.equal(strongPassword("Customer"), false);
  assert.equal(strongPassword("Short1"), false);
});

test("OTP sign-in accepts only the approved app roles", () => {
  assert.equal(supportsOtpSignInRole("CUSTOMER"), true);
  assert.equal(supportsOtpSignInRole("ADMIN"), true);
  assert.equal(supportsOtpSignInRole("SUPER_ADMIN"), true);
  assert.equal(supportsOtpSignInRole("STAFF"), true);
  assert.equal(supportsOtpSignInRole("DELIVERY"), false);
  assert.equal(supportsOtpSignInRole("customer"), false);
  assert.equal(supportsOtpSignInRole(undefined), false);
});
