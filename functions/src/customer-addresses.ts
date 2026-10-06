import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {catalogOptions, integer, number, requireCustomer, text} from "./catalog-common.js";

type Context = {uid: string; shopId: string};

export const customerGetAddresses = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request);
  return {addresses: await addressResponseList(context)};
});

export const customerCreateAddress = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request); const input = addressInput(request.data);
  const collection = addresses(context.uid); const reference = collection.doc();
  await getFirestore().runTransaction(async (transaction) => {
    const existing = await transaction.get(collection.limit(50));
    const makeDefault = input.makeDefault || existing.empty;
    if (makeDefault) existing.docs.forEach((item) => transaction.update(item.ref, {isDefault: false}));
    transaction.create(reference, {...addressFields(input), isDefault: makeDefault,
      revision: 0, createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()});
  });
  return {addresses: await addressResponseList(context)};
});

export const customerUpdateAddress = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request); const input = addressInput(request.data);
  const addressId = text(request.data?.addressId); const expected = integer(request.data?.expectedRevision, -1);
  if (!addressId || expected < 0) invalid("Address revision required");
  const collection = addresses(context.uid); const reference = collection.doc(addressId);
  await getFirestore().runTransaction(async (transaction) => {
    const [current, existing] = await Promise.all([transaction.get(reference), transaction.get(collection.limit(50))]);
    if (!current.exists) throw new HttpsError("not-found", "Address not found");
    if (integer(current.get("revision")) !== expected) throw new HttpsError("aborted", "Address changed");
    if (input.makeDefault) existing.docs.filter((item) => item.id !== addressId)
      .forEach((item) => transaction.update(item.ref, {isDefault: false}));
    transaction.update(reference, {...addressFields(input),
      isDefault: input.makeDefault || current.get("isDefault") === true, revision: expected + 1,
      updatedAt: FieldValue.serverTimestamp()});
  });
  return {addresses: await addressResponseList(context)};
});

export const customerSetDefaultAddress = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request); const addressId = text(request.data?.addressId);
  const expected = integer(request.data?.expectedRevision, -1); const collection = addresses(context.uid);
  if (!addressId || expected < 0) invalid("Address revision required");
  await getFirestore().runTransaction(async (transaction) => {
    const [target, existing] = await Promise.all([
      transaction.get(collection.doc(addressId)), transaction.get(collection.limit(50)),
    ]);
    if (!target.exists) throw new HttpsError("not-found", "Address not found");
    if (integer(target.get("revision")) !== expected) throw new HttpsError("aborted", "Address changed");
    existing.docs.forEach((item) => transaction.update(item.ref, {
      isDefault: item.id === addressId,
      revision: FieldValue.increment(item.id === addressId ? 1 : 0), updatedAt: FieldValue.serverTimestamp(),
    }));
  });
  return {addresses: await addressResponseList(context)};
});

export const customerDeleteAddress = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request); const addressId = text(request.data?.addressId);
  const expected = integer(request.data?.expectedRevision, -1); const collection = addresses(context.uid);
  if (!addressId || expected < 0) invalid("Address revision required");
  await getFirestore().runTransaction(async (transaction) => {
    const [target, existing] = await Promise.all([
      transaction.get(collection.doc(addressId)), transaction.get(collection.limit(50)),
    ]);
    if (!target.exists) throw new HttpsError("not-found", "Address not found");
    if (integer(target.get("revision")) !== expected) throw new HttpsError("aborted", "Address changed");
    transaction.delete(target.ref);
    if (target.get("isDefault") === true) {
      const replacement = existing.docs.find((item) => item.id !== addressId);
      if (replacement) transaction.update(replacement.ref, {isDefault: true,
        revision: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp()});
    }
  });
  return {addresses: await addressResponseList(context)};
});

function addressInput(data: any) {
  const type = text(data?.type, "OTHER"); const name = text(data?.name).trim();
  const mobile = text(data?.mobile).trim(); const address = text(data?.address).trim();
  const landmark = text(data?.landmark).trim(); const city = text(data?.city).trim();
  const state = text(data?.state).trim(); const postalCode = text(data?.postalCode).trim();
  if (!["HOME", "WORK", "OTHER"].includes(type) || name.length < 2 || name.length > 80 ||
      !/^\+[1-9]\d{7,14}$/.test(mobile) || address.length < 5 || address.length > 250 ||
      landmark.length > 100 || city.length < 2 || city.length > 80 || state.length < 2 ||
      state.length > 80 || !/^\d{6}$/.test(postalCode)) invalid("Check the address details");
  const latitude = data?.latitude == null ? null : number(data.latitude, 999);
  const longitude = data?.longitude == null ? null : number(data.longitude, 999);
  if (latitude == null || longitude == null) invalid("Choose the delivery location on the map before saving");
  if ((latitude == null) !== (longitude == null) || (latitude != null && (latitude < -90 || latitude > 90 ||
      longitude! < -180 || longitude! > 180))) invalid("Invalid address coordinates");
  return {type, name, mobile, address, landmark, city, state, postalCode,
    makeDefault: data?.makeDefault === true, latitude, longitude};
}

function addressFields(input: ReturnType<typeof addressInput>) {
  return {type: input.type, name: input.name, mobile: input.mobile, address: input.address,
    landmark: input.landmark, city: input.city, state: input.state, postalCode: input.postalCode,
    latitude: input.latitude, longitude: input.longitude};
}

export async function addressResponseList(context: Context) {
  const snapshot = await addresses(context.uid).limit(50).get();
  return snapshot.docs.map((item) => ({id: item.id, type: text(item.get("type"), "OTHER"),
    name: text(item.get("name")), mobile: text(item.get("mobile")), address: text(item.get("address")),
    landmark: text(item.get("landmark")), city: text(item.get("city")), state: text(item.get("state")),
    postalCode: text(item.get("postalCode")), isDefault: item.get("isDefault") === true,
    revision: integer(item.get("revision")), latitude: item.get("latitude") ?? null,
    longitude: item.get("longitude") ?? null})).sort((a, b) => Number(b.isDefault) - Number(a.isDefault));
}
function addresses(uid: string) { return getFirestore().collection("users").doc(uid).collection("addresses"); }
function invalid(message: string): never { throw new HttpsError("invalid-argument", message); }
