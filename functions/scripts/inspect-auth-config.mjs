import {readFileSync} from "node:fs";
import {cert, deleteApp, initializeApp} from "firebase-admin/app";

const credentialsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
if (!credentialsPath) throw new Error("GOOGLE_APPLICATION_CREDENTIALS is required");
const credentials = JSON.parse(readFileSync(credentialsPath, "utf8"));
const credential = cert(credentials);
const app = initializeApp({credential}, "auth-config-inspection");

try {
  const accessToken = await credential.getAccessToken();
  const response = await fetch(
    `https://identitytoolkit.googleapis.com/admin/v2/projects/${credentials.project_id}/config`,
    {headers: {Authorization: `Bearer ${accessToken.access_token}`}},
  );
  if (!response.ok) throw new Error(`Auth configuration request failed (${response.status})`);
  const config = await response.json();
  const appsResponse = await fetch(
    `https://firebase.googleapis.com/v1beta1/projects/${credentials.project_id}/androidApps`,
    {headers: {Authorization: `Bearer ${accessToken.access_token}`}},
  );
  if (!appsResponse.ok) throw new Error(`Android apps request failed (${appsResponse.status})`);
  const apps = (await appsResponse.json()).apps ?? [];
  const androidApps = [];
  for (const androidApp of apps) {
    const shaResponse = await fetch(
      `https://firebase.googleapis.com/v1beta1/${androidApp.name}/sha`,
      {headers: {Authorization: `Bearer ${accessToken.access_token}`}},
    );
    const certificates = shaResponse.ok ? (await shaResponse.json()).certificates ?? [] : [];
    androidApps.push({
      packageName: androidApp.packageName,
      sha1Certificates: certificates.filter((item) => item.certType === "SHA_1").length,
      sha256Certificates: certificates.filter((item) => item.certType === "SHA_256").length,
    });
  }
  console.log(JSON.stringify({
    emailPasswordEnabled: config.signIn?.email?.enabled === true,
    phoneEnabled: config.signIn?.phoneNumber?.enabled === true,
    testPhoneNumbersConfigured: Object.keys(config.signIn?.phoneNumber?.testPhoneNumbers ?? {}).length,
    smsRegionPolicy: config.smsRegionConfig ?? null,
    androidApps,
  }, null, 2));
} finally {
  await deleteApp(app);
}
