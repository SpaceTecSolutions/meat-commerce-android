import {readFileSync} from "node:fs";
import {cert, deleteApp, initializeApp} from "firebase-admin/app";

const credentialsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
const packageName = process.env.ANDROID_PACKAGE_NAME?.trim();
const sha1 = process.env.ANDROID_SHA1?.trim();
if (!credentialsPath || !packageName || !sha1) {
  throw new Error("GOOGLE_APPLICATION_CREDENTIALS, ANDROID_PACKAGE_NAME, and ANDROID_SHA1 are required");
}

const credentials = JSON.parse(readFileSync(credentialsPath, "utf8"));
const credential = cert(credentials);
const app = initializeApp({credential}, "android-auth-configuration");

try {
  const token = (await credential.getAccessToken()).access_token;
  const headers = {Authorization: `Bearer ${token}`, "Content-Type": "application/json"};
  const appsResponse = await fetch(
    `https://firebase.googleapis.com/v1beta1/projects/${credentials.project_id}/androidApps`, {headers},
  );
  if (!appsResponse.ok) throw new Error(`Android apps request failed (${appsResponse.status})`);
  const androidApp = ((await appsResponse.json()).apps ?? []).find((item) => item.packageName === packageName);
  if (!androidApp) throw new Error(`Firebase Android app not found for ${packageName}`);
  const listResponse = await fetch(`https://firebase.googleapis.com/v1beta1/${androidApp.name}/sha`, {headers});
  if (!listResponse.ok) throw new Error(`Certificate list request failed (${listResponse.status})`);
  const certificates = (await listResponse.json()).certificates ?? [];
  if (certificates.some((item) => item.certType === "SHA_1" && item.shaHash === sha1)) {
    console.log(`SHA-1 is already registered for ${packageName}.`);
  } else {
    const createResponse = await fetch(`https://firebase.googleapis.com/v1beta1/${androidApp.name}/sha`, {
      method: "POST", headers, body: JSON.stringify({shaHash: sha1, certType: "SHA_1"}),
    });
    if (!createResponse.ok) throw new Error(`Certificate registration failed (${createResponse.status})`);
    console.log(`Registered debug SHA-1 for ${packageName}.`);
  }
} finally {
  await deleteApp(app);
}
