import {readFileSync} from "node:fs";
import {cert, deleteApp, initializeApp} from "firebase-admin/app";
import {DocumentReference, getFirestore} from "firebase-admin/firestore";

const sourceDatabaseId = "(default)";
const destinationDatabaseId = "default";
const execute = process.argv.includes("--execute");
const credentialsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;

if (!credentialsPath) {
  throw new Error("Set GOOGLE_APPLICATION_CREDENTIALS to the Firebase service-account JSON path.");
}

const credentials = JSON.parse(readFileSync(credentialsPath, "utf8"));
const app = initializeApp({credential: cert(credentials)}, "firestore-region-migration");
const source = getFirestore(app, sourceDatabaseId);
const destination = getFirestore(app, destinationDatabaseId);
const sourceDocuments = [];

function remapReferences(value) {
  if (value instanceof DocumentReference) return destination.doc(value.path);
  if (Array.isArray(value)) return value.map(remapReferences);
  if (value && Object.getPrototypeOf(value) === Object.prototype) {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, remapReferences(item)]));
  }
  return value;
}

async function visitCollection(collection) {
  const references = await collection.listDocuments();
  for (const reference of references) {
    const snapshot = await reference.get();
    if (snapshot.exists) sourceDocuments.push({path: reference.path, data: snapshot.data()});
    for (const child of await reference.listCollections()) await visitCollection(child);
  }
}

async function discover() {
  for (const collection of await source.listCollections()) await visitCollection(collection);
}

async function copy() {
  const writer = destination.bulkWriter();
  for (const document of sourceDocuments) {
    writer.set(destination.doc(document.path), remapReferences(document.data));
  }
  await writer.close();
}

async function verify() {
  let verified = 0;
  for (let offset = 0; offset < sourceDocuments.length; offset += 100) {
    const group = sourceDocuments.slice(offset, offset + 100);
    const snapshots = await destination.getAll(...group.map(({path}) => destination.doc(path)));
    verified += snapshots.filter((snapshot) => snapshot.exists).length;
  }
  if (verified !== sourceDocuments.length) {
    throw new Error(`Verification failed: ${verified}/${sourceDocuments.length} documents found.`);
  }
  return verified;
}

try {
  await discover();
  const roots = [...new Set(sourceDocuments.map(({path}) => path.split("/")[0]))].sort();
  const productPaths = sourceDocuments
    .map(({path}) => path)
    .filter((path) => /^shops\/[^/]+\/products\/[^/]+$/.test(path));
  console.log(`Source: ${sourceDatabaseId}; destination: ${destinationDatabaseId}`);
  console.log(`Discovered ${sourceDocuments.length} documents in: ${roots.join(", ")}`);
  console.log(`Products (${productPaths.length}): ${productPaths.join(", ") || "none"}`);
  if (!execute) {
    console.log("Dry run only. Re-run with --execute to copy and verify documents.");
  } else {
    await copy();
    console.log(`Copied and verified ${await verify()} documents. Source data was not deleted.`);
  }
} finally {
  await deleteApp(app);
}
