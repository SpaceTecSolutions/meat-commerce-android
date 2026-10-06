import { before, after, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';
import { ref, getBytes, uploadBytes } from 'firebase/storage';

let env;
const projectId = 'meatbush-rules-test';
const user = (role, shopId = 'shop-a', active = true) => ({ role, shopId, active, displayName: role });
const db = uid => env.authenticatedContext(uid).firestore();
const store = uid => env.authenticatedContext(uid).storage('gs://meatbush-rules-test.appspot.com');

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { host: '127.0.0.1', port: 8189, rules: readFileSync('firestore.rules', 'utf8') },
    storage: { host: '127.0.0.1', port: 9299, rules: readFileSync('storage.rules', 'utf8') },
  });
});
after(async () => env.cleanup());
beforeEach(async () => {
  await env.clearFirestore();
  await env.clearStorage();
  await env.withSecurityRulesDisabled(async context => {
    const adminDb = context.firestore();
    await setDoc(doc(adminDb, 'users/super'), user('SUPER_ADMIN', null));
    await setDoc(doc(adminDb, 'users/admin-a'), user('ADMIN'));
    await setDoc(doc(adminDb, 'users/admin-b'), user('ADMIN', 'shop-b'));
    await setDoc(doc(adminDb, 'users/driver-a'), user('DELIVERY'));
    await setDoc(doc(adminDb, 'users/driver-b'), user('DELIVERY'));
    await setDoc(doc(adminDb, 'users/customer-a'), user('CUSTOMER'));
    await setDoc(doc(adminDb, 'users/customer-b'), user('CUSTOMER'));
    await setDoc(doc(adminDb, 'shops/shop-a'), { active: true, displayName: 'A' });
    await setDoc(doc(adminDb, 'shops/shop-b'), { active: true, displayName: 'B' });
    await setDoc(doc(adminDb, 'shops/shop-a/config/features'), { maxProducts: 100, razorpayAllowed: false });
    await setDoc(doc(adminDb, 'shops/shop-a/counters/products'), { countedProducts: 1 });
    await setDoc(doc(adminDb, 'shops/shop-a/products/active'), { active: true, archived: false });
    await setDoc(doc(adminDb, 'shops/shop-a/products/inactive'), { active: false, archived: false });
    await setDoc(doc(adminDb, 'shops/shop-a/orders/assigned'), { customerId: 'customer-a', assignedDeliveryUserId: 'driver-a', orderStatus: 'CONFIRMED' });
    await setDoc(doc(adminDb, 'shops/shop-a/orders/other'), { customerId: 'customer-b', assignedDeliveryUserId: 'driver-b', orderStatus: 'CONFIRMED' });
    await setDoc(doc(adminDb, 'users/customer-a/addresses/home'), { city: 'Pune' });
    await setDoc(doc(adminDb, 'users/customer-a/notifications/own'), { recipientUid: 'customer-a', title: 'Own' });
    await setDoc(doc(adminDb, 'users/customer-b/notifications/other'), { recipientUid: 'customer-b', title: 'Other' });
    await setDoc(doc(adminDb, 'users/customer-a/devices/device-a'), { fcmToken: 'server-only' });
  });
});

test('user cannot escalate role or change tenant and may update display name only', async () => {
  await assertFails(updateDoc(doc(db('customer-a'), 'users/customer-a'), { role: 'SUPER_ADMIN' }));
  await assertFails(updateDoc(doc(db('customer-a'), 'users/customer-a'), { shopId: 'shop-b' }));
  await assertSucceeds(updateDoc(doc(db('customer-a'), 'users/customer-a'), { displayName: 'Customer A', updatedAt: new Date() }));
});

test('cross-user profile and address reads are denied', async () => {
  await assertFails(getDoc(doc(db('customer-b'), 'users/customer-a')));
  await assertFails(getDoc(doc(db('customer-b'), 'users/customer-a/addresses/home')));
  await assertSucceeds(getDoc(doc(db('customer-a'), 'users/customer-a/addresses/home')));
});

test('notification history is user scoped and notification devices are server only', async () => {
  await assertSucceeds(getDoc(doc(db('customer-a'), 'users/customer-a/notifications/own')));
  await assertFails(getDoc(doc(db('customer-a'), 'users/customer-b/notifications/other')));
  await assertFails(setDoc(doc(db('customer-a'), 'users/customer-a/notifications/spoof'), { title: 'Spoof' }));
  await assertFails(updateDoc(doc(db('customer-a'), 'users/customer-a/notifications/own'), { readAt: new Date() }));
  await assertFails(getDoc(doc(db('customer-a'), 'users/customer-a/devices/device-a')));
});

test('Super Admin can read privileged config but no client can mutate it', async () => {
  const path = 'shops/shop-a/config/features';
  await assertSucceeds(getDoc(doc(db('super'), path)));
  await assertFails(updateDoc(doc(db('super'), path), { maxProducts: 999 }));
  await assertFails(updateDoc(doc(db('admin-a'), path), { razorpayAllowed: true }));
});

test('product counter and products cannot be written directly', async () => {
  await assertFails(updateDoc(doc(db('admin-a'), 'shops/shop-a/counters/products'), { countedProducts: 0 }));
  await assertFails(setDoc(doc(db('admin-a'), 'shops/shop-a/products/bypass'), { active: true }));
});

test('public catalog exposes active products only', async () => {
  const publicDb = env.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(publicDb, 'shops/shop-a/products/active')));
  await assertFails(getDoc(doc(publicDb, 'shops/shop-a/products/inactive')));
});

test('customer reads only own order and cannot change status', async () => {
  await assertSucceeds(getDoc(doc(db('customer-a'), 'shops/shop-a/orders/assigned')));
  await assertFails(getDoc(doc(db('customer-a'), 'shops/shop-a/orders/other')));
  await assertFails(updateDoc(doc(db('customer-a'), 'shops/shop-a/orders/assigned'), { orderStatus: 'DELIVERED' }));
});

test('delivery reads assigned records only and cannot transition order', async () => {
  await assertSucceeds(getDoc(doc(db('driver-a'), 'shops/shop-a/orders/assigned')));
  await assertFails(getDoc(doc(db('driver-a'), 'shops/shop-a/orders/other')));
  await assertFails(updateDoc(doc(db('driver-a'), 'shops/shop-a/orders/assigned'), { orderStatus: 'OUT_FOR_DELIVERY' }));
});

test('Admin is restricted to own shop business data', async () => {
  await assertSucceeds(getDoc(doc(db('admin-a'), 'shops/shop-a/orders/assigned')));
  await assertFails(getDoc(doc(db('admin-b'), 'shops/shop-a/orders/assigned')));
});

test('Storage upload paths enforce shop membership and image type', async () => {
  const jpeg = new Uint8Array([0xff, 0xd8, 0xff]);
  await assertSucceeds(uploadBytes(ref(store('admin-a'), 'shops/shop-a/products/uploads/one.jpg'), jpeg, { contentType: 'image/jpeg' }));
  await assertFails(uploadBytes(ref(store('admin-b'), 'shops/shop-a/products/uploads/two.jpg'), jpeg, { contentType: 'image/jpeg' }));
  await assertFails(uploadBytes(ref(store('admin-a'), 'shops/shop-a/products/uploads/file.txt'), jpeg, { contentType: 'text/plain' }));
});

test('Storage prevents finalized catalog writes and cross-user profile writes', async () => {
  const image = new Uint8Array([1, 2, 3]);
  await assertFails(uploadBytes(ref(store('admin-a'), 'shops/shop-a/products/p1/image.jpg'), image, { contentType: 'image/jpeg' }));
  await assertSucceeds(uploadBytes(ref(store('customer-a'), 'users/customer-a/profile/avatar.jpg'), image, { contentType: 'image/jpeg' }));
  await assertFails(uploadBytes(ref(store('customer-b'), 'users/customer-a/profile/avatar2.jpg'), image, { contentType: 'image/jpeg' }));
});

test('finalized catalog media is publicly readable', async () => {
  await env.withSecurityRulesDisabled(async context => {
    await uploadBytes(ref(context.storage('gs://meatbush-rules-test.appspot.com'), 'shops/shop-a/products/p1/image.jpg'), new Uint8Array([1]), { contentType: 'image/jpeg' });
  });
  await assertSucceeds(getBytes(ref(env.unauthenticatedContext().storage('gs://meatbush-rules-test.appspot.com'), 'shops/shop-a/products/p1/image.jpg')));
  assert.ok(true);
});
