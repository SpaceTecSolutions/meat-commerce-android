import assert from "node:assert/strict";
import test from "node:test";
import {availableDeliveryDates, availablePaymentMethods} from "./customer-checkout.js";

test("checkout always offers COD and gates optional payment methods", () => {
  assert.deepEqual(availablePaymentMethods(
    {codEnabled: true, upiEnabled: true, razorpayEnabled: true, razorpayConfigured: false},
    {codAllowed: true, upiAllowed: false, razorpayAllowed: true},
  ), ["COD"]);
});

test("configured Razorpay is available only when Super Admin allows it", () => {
  assert.deepEqual(availablePaymentMethods(
    {razorpayEnabled: true, razorpayConfigured: true}, {razorpayAllowed: true},
  ), ["COD", "RAZORPAY"]);
});

test("delivery dates omit today's slots after their one-hour cutoff", () => {
  const now = Date.UTC(2026, 8, 5, 6, 0); // 11:30 AM in Asia/Kolkata.
  const dates = availableDeliveryDates([
    {id: "morning", label: "9 AM - 11 AM", startMinutes: 540, endMinutes: 660, active: true},
    {id: "evening", label: "5 PM - 7 PM", startMinutes: 1020, endMinutes: 1140, active: true},
  ], now);
  assert.deepEqual(dates[0].availableSlotIds, ["evening"]);
  assert.deepEqual(dates[1].availableSlotIds, ["morning", "evening"]);
  assert.equal(dates.length, 7);
});
