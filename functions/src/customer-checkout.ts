import {randomUUID} from "node:crypto";
import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {catalogOptions, integer, number, requireCustomer, text} from "./catalog-common.js";
import {addressResponseList} from "./customer-addresses.js";
import {cartResponse} from "./customer-cart.js";

type Context = {uid: string; shopId: string};
type Slot = {id: string; label: string; startMinutes: number; endMinutes: number; active: boolean};

export const customerPrepareCheckout = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request); const firestore = getFirestore();
  const shop = firestore.collection("shops").doc(context.shopId);
  const [cart, addresses, delivery, payment, features] = await Promise.all([
    cartResponse(context), addressResponseList(context), shop.collection("config").doc("delivery").get(),
    shop.collection("config").doc("payment").get(), firestore.collection("appConfig").doc("features").get(),
  ]);
  validateCart(cart);
  const configuredSlots = slots(delivery.get("slots"));
  const scheduled = configuredSlots.length > 0;
  if (!scheduled) invalid("Configured delivery timings are unavailable", "DELIVERY_CHANGED");
  const paymentMethods = availablePaymentMethods(payment.data() ?? {}, features.data() ?? {})
    .filter((it) => it !== "UPI");
  if (!paymentMethods.length) invalid("No payment method is currently available", "PAYMENT_UNAVAILABLE");
  const deliveryDates = availableDeliveryDates(configuredSlots);
  if (!deliveryDates.length) invalid("No delivery slots are currently available", "DELIVERY_CHANGED");
  const expiresAtEpochMillis = Date.now() + 10 * 60_000; const quoteToken = randomUUID();
  await firestore.collection("users").doc(context.uid).collection("checkoutQuotes").doc(quoteToken).set({
    uid: context.uid, shopId: context.shopId, subtotalMinor: cart.subtotalMinor,
    deliveryFeeMinor: cart.deliveryFeeMinor, totalMinor: cart.totalMinor,
    itemQuantities: Object.fromEntries(cart.lines.map((line) => [line.productId, line.quantity])),
    unitPrices: Object.fromEntries(cart.lines.map((line) => [line.productId, line.unitPriceMinor])),
    createdAt: FieldValue.serverTimestamp(), expiresAt: Timestamp.fromMillis(expiresAtEpochMillis),
  });
  return {quoteToken, cart, addresses, normalDeliveryAvailable: false,
    scheduledDeliveryAvailable: true, deliveryDates, deliverySlots: configuredSlots,
    paymentMethods, currencyCode: text(delivery.get("currencyCode"), "INR"), expiresAtEpochMillis};
});

export const customerPlaceOrder = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request); const input = orderInput(request.data);
  const firestore = getFirestore(); const user = firestore.collection("users").doc(context.uid);
  const shop = firestore.collection("shops").doc(context.shopId); const orderRef = shop.collection("orders").doc();
  const requestRef = user.collection("orderRequests").doc(input.idempotencyKey);
  const result = await firestore.runTransaction(async (transaction) => {
    const previous = await transaction.get(requestRef);
    if (previous.exists) {
      const existing = await transaction.get(shop.collection("orders").doc(text(previous.get("orderId"))));
      if (!existing.exists) invalid("Order result is unavailable", "ORDER_RESULT_MISSING");
      return placedOrderResponse(existing.id, existing.data()!);
    }
    const quoteRef = user.collection("checkoutQuotes").doc(input.quoteToken);
    const addressRef = user.collection("addresses").doc(input.addressId);
    const deliveryRef = shop.collection("config").doc("delivery");
    const paymentRef = shop.collection("config").doc("payment");
    const featuresRef = firestore.collection("appConfig").doc("features");
    const counterRef = shop.collection("counters").doc("orders");
    const [quote, address, delivery, payment, features, counter, cart, profile] = await Promise.all([
      transaction.get(quoteRef), transaction.get(addressRef), transaction.get(deliveryRef),
      transaction.get(paymentRef), transaction.get(featuresRef), transaction.get(counterRef),
      transaction.get(user.collection("cartItems").limit(100)), transaction.get(user),
    ]);
    validateQuote(quote, context); if (!address.exists) notFound("Delivery address not found");
    if (!profile.exists || profile.get("active") !== true || profile.get("deletionPending") === true ||
        profile.get("role") !== "CUSTOMER" ||
        text(profile.get("shopId"), "default") !== context.shopId) {
      throw new HttpsError("permission-denied", "Active Customer access required");
    }
    if (!availablePaymentMethods(payment.data() ?? {}, features.data() ?? {}).includes(input.paymentMethod) ||
        input.paymentMethod === "UPI") {
      invalid("Selected payment method is unavailable", "PAYMENT_UNAVAILABLE");
    }
    const configuredSlots = slots(delivery.get("slots"));
    const date = availableDeliveryDates(configuredSlots).find((it) => it.id === input.deliveryDateId);
    const slot = configuredSlots.find((it) => it.id === input.deliverySlotId && date?.availableSlotIds.includes(it.id));
    if (!date || !slot) invalid("Selected delivery time is no longer available", "DELIVERY_CHANGED");
    if (cart.empty) invalid("Your cart is empty", "EMPTY_CART");
    const products = await Promise.all(cart.docs.map((line) =>
      transaction.get(shop.collection("products").doc(line.id))));
    const quoteQuantities = objectNumbers(quote.get("itemQuantities"));
    const quotePrices = objectNumbers(quote.get("unitPrices"));
    const items = cart.docs.map((line, index) => orderItem(line, products[index], quoteQuantities, quotePrices));
    const itemTotal = items.reduce((sum, item) => sum + item.lineTotalMinor, 0);
    const minimum = Math.max(0, integer(delivery.get("minimumOrderMinor")));
    if (itemTotal < minimum) invalid("Minimum order amount not reached", "DELIVERY_CHANGED");
    const configuredFee = Math.max(0, integer(delivery.get("deliveryChargeMinor")));
    const freeAt = delivery.get("freeDeliveryThresholdMinor") == null ? null :
      Math.max(0, integer(delivery.get("freeDeliveryThresholdMinor")));
    const deliveryFee = freeAt != null && itemTotal >= freeAt ? 0 : configuredFee;
    if (itemTotal !== integer(quote.get("subtotalMinor")) || deliveryFee !== integer(quote.get("deliveryFeeMinor"))) {
      invalid("Some prices have changed. Please review your updated order.", "PRICE_CHANGED");
    }
    const next = Math.max(1024, integer(counter.get("nextOrderNumber"), 1024)) + 1;
    const orderNumber = `#ORD-${String(next).padStart(4, "0")}`; const now = Date.now();
    const addressSnapshot = addressData(input.addressId, address.data()!); const totalMinor = itemTotal + deliveryFee;
    const currencyCode = text(delivery.get("currencyCode"), "INR");
    const value = {orderNumber, displayNumber: orderNumber, customerId: context.uid,
      customerName: text(profile.get("displayName"), text(address.get("name"), "Customer")),
      customerPhone: text(address.get("mobile")), customerMobile: text(address.get("mobile")),
      deliveryAddressSnapshot: addressSnapshot, addressLabel: text(address.get("type"), "Home"),
      addressSummary: addressSnapshot.formattedAddress, deliveryDate: input.deliveryDateId,
      deliverySlotSnapshot: {id: slot.id, date: input.deliveryDateId, dateLabel: date.label,
        timeLabel: slot.label, startMinutes: slot.startMinutes, endMinutes: slot.endMinutes},
      deliverySlotDateLabel: date.label, deliverySlotTimeLabel: slot.label,
      deliveryInstructions: input.instructions, instructions: input.instructions, customerNote: input.instructions,
      items, itemTotal, subtotalMinor: itemTotal, discountAmount: 0, discountMinor: 0,
      deliveryCharge: deliveryFee, deliveryFeeMinor: deliveryFee, taxAmount: 0, taxMinor: 0,
      totalAmount: totalMinor, totalMinor, amountDueMinor: totalMinor, currencyCode,
      paymentMethod: input.paymentMethod,
      paymentStatus: input.paymentMethod === "COD" ? "PENDING" : "PROCESSING", orderStatus: "PENDING",
      customerCancellationAllowed: true, revision: 0, createdAtEpochMillis: now,
      createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()};
    transaction.create(orderRef, value);
    products.forEach((product, index) => transaction.update(product.ref,
      {stockQuantity: FieldValue.increment(-items[index].quantity), updatedAt: FieldValue.serverTimestamp()}));
    cart.docs.forEach((line) => transaction.delete(line.ref));
    transaction.set(counterRef, {nextOrderNumber: next, updatedAt: FieldValue.serverTimestamp()}, {merge: true});
    transaction.create(requestRef, {orderId: orderRef.id, createdAt: FieldValue.serverTimestamp()});
    transaction.update(quoteRef, {consumedAt: FieldValue.serverTimestamp(), orderId: orderRef.id});
    return placedOrderResponse(orderRef.id, value);
  });
  return {order: result};
});

export const customerReconcileOrderCreation = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request); const key = text(request.data?.idempotencyKey);
  if (!validId(key)) throw new HttpsError("invalid-argument", "Valid order request required");
  const firestore = getFirestore(); const result = await firestore.collection("users").doc(context.uid)
    .collection("orderRequests").doc(key).get();
  if (!result.exists) return {order: null};
  const order = await firestore.collection("shops").doc(context.shopId).collection("orders")
    .doc(text(result.get("orderId"))).get();
  return {order: order.exists ? placedOrderResponse(order.id, order.data()!) : null};
});

export function availablePaymentMethods(payment: FirebaseFirestore.DocumentData, features: FirebaseFirestore.DocumentData) {
  const methods: string[] = [];
  methods.push("COD");
  if (payment.razorpayEnabled === true && payment.razorpayConfigured === true && features.razorpayAllowed === true) methods.push("RAZORPAY");
  if (payment.upiEnabled === true && features.upiAllowed === true) methods.push("UPI");
  return methods;
}

export function availableDeliveryDates(configured: Slot[], now = Date.now()) {
  const local = new Date(now + 330 * 60_000); const todayMinutes = local.getUTCHours() * 60 + local.getUTCMinutes();
  const base = Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate());
  return Array.from({length: 7}, (_, offset) => {
    const date = new Date(base + offset * 86_400_000); const id = date.toISOString().slice(0, 10);
    const day = new Intl.DateTimeFormat("en-IN", {weekday: "short", day: "numeric", month: "short", timeZone: "UTC"}).format(date);
    const prefix = offset === 0 ? "Today" : offset === 1 ? "Tomorrow" : day.split(",")[0];
    const availableSlotIds = configured.filter((slot) => offset > 0 || todayMinutes < slot.startMinutes - 60).map((slot) => slot.id);
    return {id, label: `${prefix} (${day})`, availableSlotIds};
  }).filter((date) => date.availableSlotIds.length > 0);
}

function orderInput(data: any) {
  const quoteToken = text(data?.quoteToken); const addressId = text(data?.addressId);
  const deliveryDateId = text(data?.deliveryDateId); const deliverySlotId = text(data?.deliverySlotId);
  const paymentMethod = text(data?.paymentMethod); const idempotencyKey = text(data?.idempotencyKey);
  const instructions = text(data?.instructions).trim();
  if (!validId(quoteToken) || !validId(addressId) || !/^\d{4}-\d{2}-\d{2}$/.test(deliveryDateId) ||
      !validId(deliverySlotId) || !["COD", "RAZORPAY"].includes(paymentMethod) ||
      !validId(idempotencyKey) || instructions.length > 300) {
    throw new HttpsError("invalid-argument", "Check the checkout details");
  }
  return {quoteToken, addressId, deliveryDateId, deliverySlotId, paymentMethod, idempotencyKey, instructions};
}

function orderItem(line: FirebaseFirestore.QueryDocumentSnapshot, product: FirebaseFirestore.DocumentSnapshot,
  quantities: Record<string, number>, prices: Record<string, number>) {
  const quantity = Math.max(1, integer(line.get("quantity"))); const stock = Math.floor(number(product.get("stockQuantity")));
  if (!product.exists || product.get("active") !== true || product.get("archived") === true || stock < quantity) {
    invalid("Some items are no longer available in the requested quantity.", "STOCK_CHANGED");
  }
  if (quantities[line.id] !== quantity) invalid("Your cart changed. Please review it again.", "CART_CHANGED");
  const regular = integer(product.get("priceMinor")); const offer = integer(product.get("offerPriceMinor"));
  const price = offer > 0 && offer < regular ? offer : regular;
  if (regular <= 0 || prices[line.id] == null || prices[line.id] !== price) {
    invalid("Some prices have changed. Please review your updated order.", "PRICE_CHANGED");
  }
  return {productId: line.id, name: text(product.get("name"), "Product"),
    imageUrl: Array.isArray(product.get("imageUrls")) ? product.get("imageUrls")[0] ?? null : null,
    categoryId: text(product.get("categoryId")) || null, categoryName: text(product.get("categoryName")) || null,
    unit: text(product.get("unit"), "PIECE"), quantity, unitPriceMinor: price,
    regularPriceMinor: regular, lineTotalMinor: price * quantity, attributes: stringRecord(product.get("attributes"))};
}

function validateCart(cart: any) {
  if (!cart.lines.length) invalid("Your cart is empty", "EMPTY_CART");
  if (cart.lines.some((line: any) => !line.available)) invalid("A cart item is unavailable", "STOCK_CHANGED");
  if (cart.subtotalMinor < cart.minimumOrderMinor) invalid("Minimum order amount not reached", "DELIVERY_CHANGED");
}
function validateQuote(quote: FirebaseFirestore.DocumentSnapshot, context: Context) {
  if (!quote.exists || quote.get("uid") !== context.uid || quote.get("shopId") !== context.shopId ||
      !(quote.get("expiresAt") instanceof Timestamp) || quote.get("expiresAt").toMillis() < Date.now() || quote.get("consumedAt")) {
    invalid("Checkout expired. Please review your order again.", "QUOTE_EXPIRED");
  }
}
function slots(value: unknown): Slot[] {
  if (!Array.isArray(value)) return [];
  return value.filter((slot) => slot?.active !== false).map((slot) => ({id: text(slot?.id), label: text(slot?.label),
    startMinutes: integer(slot?.startMinutes), endMinutes: integer(slot?.endMinutes), active: true}))
    .filter((slot) => slot.id && slot.label && slot.endMinutes > slot.startMinutes);
}
function addressData(id: string, value: FirebaseFirestore.DocumentData) {
  const parts = [text(value.address), text(value.landmark), text(value.city), text(value.state), text(value.postalCode)].filter(Boolean);
  return {id, type: text(value.type, "HOME"), name: text(value.name), mobile: text(value.mobile),
    address: text(value.address), landmark: text(value.landmark), city: text(value.city), state: text(value.state),
    postalCode: text(value.postalCode), formattedAddress: parts.join(", "),
    latitude: optionalCoordinate(value.latitude, -90, 90),
    longitude: optionalCoordinate(value.longitude, -180, 180)};
}
function optionalCoordinate(value: unknown, minimum: number, maximum: number): number | null {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= minimum && parsed <= maximum ? parsed : null;
}
function placedOrderResponse(id: string, value: FirebaseFirestore.DocumentData) {
  return {orderId: id, orderNumber: text(value.orderNumber ?? value.displayNumber, id),
    orderStatus: text(value.orderStatus, "PENDING"), paymentStatus: text(value.paymentStatus, "PENDING"),
    totalMinor: integer(value.totalMinor ?? value.totalAmount), currencyCode: text(value.currencyCode, "INR"),
    estimatedDelivery: [text(value.deliverySlotDateLabel), text(value.deliverySlotTimeLabel)].filter(Boolean).join(", "),
    items: Array.isArray(value.items) ? value.items : []};
}
function objectNumbers(value: unknown): Record<string, number> {
  if (!value || typeof value !== "object") return {};
  return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, integer(item)]));
}
function stringRecord(value: unknown): Record<string, string> {
  if (!value || typeof value !== "object") return {};
  return Object.fromEntries(Object.entries(value).filter((entry): entry is [string, string] => typeof entry[1] === "string"));
}
function validId(value: string) { return inRange(value.length, 1, 128) && !value.includes("/"); }
function inRange(value: number, min: number, max: number) { return value >= min && value <= max; }
function invalid(message: string, reason: string): never { throw new HttpsError("failed-precondition", message, {reason}); }
function notFound(message: string): never { throw new HttpsError("not-found", message); }
