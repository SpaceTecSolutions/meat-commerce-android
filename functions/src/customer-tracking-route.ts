import {defineSecret} from "firebase-functions/params";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {logger} from "firebase-functions";
import {getDatabase} from "firebase-admin/database";
import {catalogOptions, requireCustomer, text} from "./catalog-common.js";
import {getFirestore} from "./database.js";

const routesApiKey = defineSecret("MAPS_ROUTES_API_KEY");
const cacheMillis = 30_000;
const cacheMovementMeters = 80;

export const customerGetTrackingRoute = onCall(
  {...catalogOptions, secrets: [routesApiKey]},
  async (request) => {
    const context = await requireCustomer(request);
    const orderId = requiredId(request.data?.orderId);
    const firestore = getFirestore();
    const order = await firestore.collection("shops").doc(context.shopId)
      .collection("orders").doc(orderId).get();
    if (!order.exists || text(order.get("customerId")) !== context.uid) denied();
    const value = order.data()!;
    const sessionId = requiredId(value.trackingSessionId);
    const [features, delivery, authorization, location, cache] = await Promise.all([
      firestore.collection("appConfig").doc("features").get(),
      firestore.collection("shops").doc(context.shopId).collection("config").doc("delivery").get(),
      getDatabase().ref(`trackingAuthorizations/${sessionId}`).get(),
      getDatabase().ref(`activeDeliveryTracking/${sessionId}/location`).get(),
      getDatabase().ref(`trackingRouteCache/${sessionId}`).get(),
    ]);
    const auth = authorization.val();
    if (features.get("realtimeTrackingAllowed") !== true ||
        delivery.get("realtimeTrackingEnabled") !== true || value.orderStatus !== "OUT_FOR_DELIVERY" ||
        value.trackingLifecycle !== "ACTIVE" || auth?.active !== true ||
        auth?.customerUid !== context.uid || auth?.orderId !== orderId ||
        !["ADMIN", "DELIVERY"].includes(text(auth?.writerRole))) denied();
    const origin = coordinate(location.val()?.latitude, location.val()?.longitude);
    const addressSnapshot = value.deliveryAddressSnapshot ?? value.addressSnapshot ?? {};
    let destination = coordinate(
      value.deliveryDestinationLatitude ?? addressSnapshot.latitude,
      value.deliveryDestinationLongitude ?? addressSnapshot.longitude,
    );
    if (!destination) {
      const addressId = text(addressSnapshot.id);
      if (addressId) {
        const savedAddress = await firestore.collection("users").doc(context.uid)
          .collection("addresses").doc(addressId).get();
        destination = coordinate(savedAddress.get("latitude"), savedAddress.get("longitude"));
      }
    }
    const destinationAddress = addressText(value, addressSnapshot);
    if (!origin || (!destination && !destinationAddress)) {
      throw new HttpsError("failed-precondition", "Delivery destination is unavailable");
    }
    const cached = cache.val();
    if (validCache(cached, origin)) return {route: cached};
    const routeDestination: RouteDestination = destination ?? destinationAddress;
    let response = await computeRoute(origin, routeDestination, "TWO_WHEELER");
    if (response.retryWithDrive) {
      logger.info("Two-wheeler route unavailable; retrying with road-driving route", {orderId});
      response = await computeRoute(origin, routeDestination, "DRIVE");
    }
    const first = response.payload.routes?.[0];
    const encodedPolyline = first?.polyline?.encodedPolyline;
    if (!encodedPolyline) {
      logger.warn("Routes API returned no route", {orderId, status: response.status});
      throw new HttpsError("not-found", "No delivery route found");
    }
    destination = destination ?? coordinate(
      first?.legs?.at(-1)?.endLocation?.latLng?.latitude,
      first?.legs?.at(-1)?.endLocation?.latLng?.longitude,
    );
    if (!destination) {
      throw new HttpsError("not-found", "Delivery destination could not be resolved");
    }
    const route = {
      encodedPolyline, distanceMeters: Math.max(0, first.distanceMeters ?? 0),
      durationSeconds: durationSeconds(first.duration), generatedAtEpochMillis: Date.now(),
      originLatitude: origin.latitude, originLongitude: origin.longitude,
      destinationLatitude: destination.latitude, destinationLongitude: destination.longitude,
    };
    await getDatabase().ref(`trackingRouteCache/${sessionId}`).set(route);
    return {route};
  },
);

async function computeRoute(origin: Coordinate, destination: RouteDestination, travelMode: string) {
  const destinationWaypoint = typeof destination === "string" ?
    {address: destination} : {location: {latLng: destination}};
  const response = await fetch("https://routes.googleapis.com/directions/v2:computeRoutes", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Goog-Api-Key": routesApiKey.value(),
        "X-Goog-FieldMask": [
          "routes.duration", "routes.distanceMeters", "routes.polyline.encodedPolyline",
          "routes.legs.endLocation",
        ].join(","),
      },
      body: JSON.stringify({
        origin: {location: {latLng: origin}}, destination: destinationWaypoint,
        travelMode, routingPreference: "TRAFFIC_AWARE",
        polylineQuality: "OVERVIEW", polylineEncoding: "ENCODED_POLYLINE",
      }),
  });
  const payload = await response.json() as RoutesResponse & {error?: {code?: number; status?: string}};
  if (!response.ok) {
    logger.error("Routes API request failed", {
      status: response.status, apiStatus: payload.error?.status, apiCode: payload.error?.code,
      travelMode,
    });
    if (travelMode !== "TWO_WHEELER" || response.status === 401 || response.status === 403) {
      throw new HttpsError("unavailable", "Unable to calculate delivery route");
    }
  }
  return {payload, status: response.status,
    retryWithDrive: !response.ok || !payload.routes?.[0]?.polyline?.encodedPolyline};
}

type Coordinate = {latitude: number; longitude: number};
type RouteDestination = Coordinate | string;
type RoutesResponse = {routes?: Array<{duration?: string; distanceMeters?: number;
  polyline?: {encodedPolyline?: string}; legs?: Array<{endLocation?: {latLng?: Coordinate}}>}>};

function addressText(order: Record<string, any>, snapshot: Record<string, any>): string {
  const direct = text(snapshot.formattedAddress) || text(snapshot.address) ||
    text(order.addressSummary);
  if (direct) return direct;
  return [snapshot.addressLine1, snapshot.addressLine2, snapshot.landmark,
    snapshot.area, snapshot.city, snapshot.state, snapshot.postalCode]
    .map((part) => text(part)).filter(Boolean).join(", ");
}

function coordinate(latitude: unknown, longitude: unknown): Coordinate | null {
  if (typeof latitude !== "number" || typeof longitude !== "number" ||
      latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return null;
  return {latitude, longitude};
}
function validCache(value: any, origin: Coordinate): boolean {
  return typeof value?.encodedPolyline === "string" &&
    coordinate(value?.destinationLatitude, value?.destinationLongitude) !== null &&
    Date.now() - Number(value.generatedAtEpochMillis) < cacheMillis &&
    distance(origin, {latitude: Number(value.originLatitude), longitude: Number(value.originLongitude)}) <
      cacheMovementMeters;
}
function distance(a: Coordinate, b: Coordinate): number {
  const radians = (degrees: number) => degrees * Math.PI / 180;
  const dLat = radians(b.latitude - a.latitude); const dLon = radians(b.longitude - a.longitude);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(radians(a.latitude)) *
    Math.cos(radians(b.latitude)) * Math.sin(dLon / 2) ** 2;
  return 6371000 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
}
function durationSeconds(value?: string): number {
  return Math.max(0, Math.round(Number(value?.replace(/s$/, "")) || 0));
}
function requiredId(value: unknown): string {
  const id = text(value); if (!id || id.includes("/") || id.length > 128) denied(); return id;
}
function denied(): never {
  throw new HttpsError("failed-precondition", "Live tracking route is unavailable");
}
