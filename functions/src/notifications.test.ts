import assert from "node:assert/strict";
import test from "node:test";
import {statusNotification, isOrderAccepted} from "./notifications.js";
import {deliveryMoment} from "./notification-schedules.js";

test("online order acceptance requires verified payment, while COD is immediate", () => {
  assert.equal(isOrderAccepted({paymentMethod: "COD", paymentStatus: "PENDING"}), true);
  for (const paymentStatus of ["PROCESSING", "PENDING", "FAILED", "CANCELLED"]) {
    assert.equal(isOrderAccepted({paymentMethod: "RAZORPAY", paymentStatus}), false);
  }
  assert.equal(isOrderAccepted({paymentMethod: "RAZORPAY", paymentStatus: "PAID"}), true);
});

test("status notifications are emitted only for customer-visible transitions", () => {
  assert.equal(statusNotification("PENDING", "#ORD-1"), undefined);
  assert.equal(statusNotification("CONFIRMED", "#ORD-1").event, "ORDER_CONFIRMED");
  assert.match(statusNotification("OUT_FOR_DELIVERY", "#ORD-1").body, /on the way/);
  assert.equal(statusNotification("DELIVERED", "#ORD-1").title, "Delivered");
});

test("delivery slot moments use the configured India business offset", () => {
  const value = {deliveryDate: "2026-09-11", deliverySlotSnapshot: {startMinutes: 600, endMinutes: 720}};
  assert.equal(new Date(deliveryMoment(value, "startMinutes")).toISOString(), "2026-09-11T04:30:00.000Z");
  assert.equal(new Date(deliveryMoment(value, "endMinutes")).toISOString(), "2026-09-11T06:30:00.000Z");
});
