import assert from "node:assert/strict";
import test from "node:test";
import {realtimeOrderEligible} from "./customer-orders.js";

test("realtime order requires status, assignment and active session", () => {
  const eligible = {
    orderStatus: "OUT_FOR_DELIVERY",
    assignedDeliveryUserId: "driver-1",
    assignedDeliveryRole: "DELIVERY",
    deliveryStartedByUserId: "driver-1",
    trackingSessionId: "session-1",
    trackingLifecycle: "ACTIVE",
  };
  assert.equal(realtimeOrderEligible(eligible), true);
  assert.equal(realtimeOrderEligible({...eligible, orderStatus: "PREPARING"}), false);
  assert.equal(realtimeOrderEligible({...eligible, trackingSessionId: null}), false);
  assert.equal(realtimeOrderEligible({...eligible, assignedDeliveryRole: "CUSTOMER"}), false);
});

test("personally delivering admin is a valid tracking assignment", () => {
  assert.equal(realtimeOrderEligible({
    orderStatus: "OUT_FOR_DELIVERY", adminDeliveringPersonally: true,
    assignedDeliveryRole: "ADMIN", deliveryStartedByUserId: "admin-1",
    trackingSessionId: "session-2", trackingLifecycle: "ACTIVE",
  }), true);
});
