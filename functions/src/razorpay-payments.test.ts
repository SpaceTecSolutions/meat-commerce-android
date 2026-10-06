import assert from "node:assert/strict";
import {createHmac} from "node:crypto";
import {test} from "node:test";
import {isRazorpaySignatureValid} from "./razorpay-payments.js";

test("Razorpay callback signature verifies the server order id and payment id", () => {
  const secret = "test_secret"; const orderId = "order_123"; const paymentId = "pay_456";
  const signature = createHmac("sha256", secret).update(`${orderId}|${paymentId}`).digest("hex");
  assert.equal(isRazorpaySignatureValid(orderId, paymentId, signature, secret), true);
  assert.equal(isRazorpaySignatureValid(orderId, "pay_changed", signature, secret), false);
  assert.equal(isRazorpaySignatureValid("order_changed", paymentId, signature, secret), false);
});
