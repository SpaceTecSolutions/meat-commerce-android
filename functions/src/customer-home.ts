import {getFirestore} from "./database.js";
import {catalogOptions, categoryResponse, integer, productResponse, requireCustomer, publicCustomerShop, text} from "./catalog-common.js";
import {onCall} from "firebase-functions/v2/https";

type HomeDocument = {id: string; data: FirebaseFirestore.DocumentData};

export const customerGetHome = onCall(catalogOptions, async (request) => {
  const context = request.auth ? await requireCustomer(request) : null;
  const firestore = getFirestore();
  const shop = firestore.collection("shops").doc(await publicCustomerShop(request));
  const user = context ? firestore.collection("users").doc(context.uid) : null;
  const notificationsEnabled = (await firestore.collection("appConfig").doc("features").get())
    .get("inAppNotificationsEnabled") !== false;
  const thirtyDaysAgo = Date.now() - 30 * 24 * 60 * 60 * 1000;
  const [categorySnapshot, productSnapshot, bannerSnapshot, addressSnapshot, cartSnapshot, unreadSnapshot,
    recentOrdersSnapshot, customerOrdersSnapshot] =
    await Promise.all([
      shop.collection("categories").limit(40).get(),
      shop.collection("products").where("active", "==", true).limit(120).get(),
      shop.collection("banners").limit(20).get(),
      user?.collection("addresses").limit(50).get() ?? null,
      user?.collection("cartItems").limit(100).get() ?? null,
      notificationsEnabled ? user?.collection("notifications")
        .where("readAt", "==", null).count().get().catch(() => null) ?? null : null,
      shop.collection("orders").where("createdAtEpochMillis", ">=", thirtyDaysAgo).limit(150).get(),
      context ? shop.collection("orders").where("customerId", "==", context.uid).limit(1).get() : null,
    ]);

  const categories = selectHomeCategories(categorySnapshot.docs.map((doc) => ({id: doc.id, data: doc.data()})));
  const categoryIds = new Set(categories.map((category) => category.id));
  const products = productSnapshot.docs.map((doc) => ({id: doc.id, data: doc.data()}))
    .filter((item) => item.data.active === true && item.data.archived !== true && categoryIds.has(text(item.data.categoryId)));
  const sales = aggregateProductSales(recentOrdersSnapshot.docs.map((doc) => doc.data()), thirtyDaysAgo);
  const bestSellers = selectHomeBestSellers(products, sales.thirtyDay);
  const recommended = customerOrdersSnapshot && !customerOrdersSnapshot.empty ?
    selectRecommended(products, new Set(bestSellers.map((item) => item.id))) : [];
  const popularThisWeek = rankBySales(products, sales.sevenDay).slice(0, 8);
  const quickPicks = selectQuickPicks(products).slice(0, 8);
  const address = addressSnapshot?.docs.sort((a, b) =>
    Number(b.get("isDefault") === true) - Number(a.get("isDefault") === true))[0];

  return {
    deliveryLocationLabel: address ? homeLocationLabel(address.data()) : "Select location",
    deliveryAddressLabel: address ? addressTypeLabel(address.data()) : "Select location",
    unreadNotifications: unreadSnapshot?.data().count ?? 0,
    cartQuantity: cartSnapshot?.size ?? 0,
    banners: bannerSnapshot.docs.filter((doc) => doc.get("active") === true)
      .sort((a, b) => integer(a.get("sortOrder")) - integer(b.get("sortOrder")))
      .slice(0, 5).map((doc) => bannerResponse(doc.id, doc.data())),
    categories: categories.map((category) => categoryResponse(category.id, category.data)),
    bestSellers: bestSellers.map((product) => productResponse(product.id, product.data)),
    recommended: recommended.map((product) => productResponse(product.id, product.data)),
    popularThisWeek: popularThisWeek.map((product) => productResponse(product.id, product.data)),
    quickPicks: quickPicks.map((product) => productResponse(product.id, product.data)),
    offers: [],
    newProducts: [],
    promotions: [],
    offersEnabled: false,
  };
});

export function selectHomeCategories(documents: HomeDocument[]) {
  return documents.filter((item) => item.data.active === true)
    .sort((a, b) => integer(a.data.sortOrder) - integer(b.data.sortOrder) ||
      text(a.data.name).localeCompare(text(b.data.name)));
}

export function selectHomeBestSellers(documents: HomeDocument[], sales = new Map<string, number>()) {
  const curated = documents.filter((item) => item.data.isFeatured === true || item.data.bestSeller === true ||
      integer(item.data.salesCount) > 0)
    .sort((a, b) => integer(b.data.salesCount) - integer(a.data.salesCount) ||
      text(a.data.name).localeCompare(text(b.data.name))).slice(0, 10);
  return curated.length ? curated : rankBySales(documents, sales).slice(0, 10);
}

export function aggregateProductSales(orders: FirebaseFirestore.DocumentData[], thirtyDaysAgo: number) {
  const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
  const thirtyDay = new Map<string, number>(); const sevenDay = new Map<string, number>();
  orders.filter((order) => text(order.orderStatus) !== "CANCELLED").forEach((order) => {
    const createdAt = integer(order.createdAtEpochMillis);
    if (createdAt < thirtyDaysAgo) return;
    const items = Array.isArray(order.items) ? order.items : [];
    items.forEach((item: FirebaseFirestore.DocumentData) => {
      const id = text(item.productId); const quantity = Math.max(0, Number(item.quantity) || 0);
      if (!id || quantity <= 0) return;
      thirtyDay.set(id, (thirtyDay.get(id) ?? 0) + quantity);
      if (createdAt >= sevenDaysAgo) sevenDay.set(id, (sevenDay.get(id) ?? 0) + quantity);
    });
  });
  return {thirtyDay, sevenDay};
}

export function rankBySales(documents: HomeDocument[], sales: Map<string, number>) {
  return documents.filter((item) => (sales.get(item.id) ?? 0) > 0)
    .sort((a, b) => (sales.get(b.id) ?? 0) - (sales.get(a.id) ?? 0) ||
      text(a.data.name).localeCompare(text(b.data.name)));
}

function selectRecommended(documents: HomeDocument[], excluded: Set<string>) {
  const curated = documents.filter((item) => !excluded.has(item.id) &&
    (item.data.recommended === true || item.data.isFeatured === true));
  const unfeatured = documents.filter((item) => !excluded.has(item.id));
  const candidates = curated.length ? curated : unfeatured.length ? unfeatured : documents;
  return candidates.sort((a, b) => text(a.data.name).localeCompare(text(b.data.name))).slice(0, 8);
}

function selectQuickPicks(documents: HomeDocument[]) {
  return documents.filter((item) => {
    const searchable = [item.data.categoryName, item.data.type, item.data.subtype,
      ...Object.values(item.data.attributes ?? {})].map((value) => text(value)).join(" ").toLowerCase();
    return item.data.readyToCook === true || /ready.?to.?cook|quick.?pick|marinated/.test(searchable);
  }).sort((a, b) => text(a.data.name).localeCompare(text(b.data.name)));
}

export function homeLocationLabel(address: FirebaseFirestore.DocumentData) {
  const area = text(address.area) || text(address.address).split(",")[0]?.trim();
  const city = text(address.city);
  return [area, city].filter(Boolean).join(", ") || "Select location";
}

function addressTypeLabel(address: FirebaseFirestore.DocumentData) {
  const type = text(address.type, "OTHER").trim().toLocaleLowerCase("en");
  return type ? type.charAt(0).toUpperCase() + type.slice(1) : "Other";
}

function bannerResponse(id: string, value: FirebaseFirestore.DocumentData) {
  const actionRoute = text(value.actionRoute) ||
    (text(value.actionType).toUpperCase() === "PRODUCT" && text(value.actionTargetId) ?
      `product:${text(value.actionTargetId)}` : null);
  return {
    id, title: text(value.title), subtitle: text(value.subtitle), imageUrl: value.imageUrl ?? null,
    actionRoute, active: value.active === true, sortOrder: integer(value.sortOrder),
    contentAlignment: text(value.contentAlignment, "BOTTOM_START"),
    textPlacement: value.textPlacement ?? null, buttonPlacement: value.buttonPlacement ?? null,
    subtitlePlacement: value.subtitlePlacement ?? null,
    buttonColor: text(value.buttonColor, "RED"), buttonShape: text(value.buttonShape, "ROUNDED"),
    buttonArrow: value.buttonArrow === true,
    buttonEnabled: value.buttonEnabled === true, buttonText: text(value.buttonText, "Shop Now"),
    revision: integer(value.revision),
  };
}
