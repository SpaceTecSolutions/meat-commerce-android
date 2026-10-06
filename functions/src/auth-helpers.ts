import {createHmac} from "node:crypto";

const E164_PATTERN = /^\+[1-9]\d{7,14}$/;

export function canonicalMobile(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const normalized = value.replace(/[\s()-]/g, "");
  return E164_PATTERN.test(normalized) ? normalized : null;
}

export function mobileLookupId(mobile: string, secret: string): string {
  return createHmac("sha256", secret).update(mobile, "utf8").digest("hex");
}

export function validPassword(value: unknown): value is string {
  return typeof value === "string" && value.length >= 8 && value.length <= 128;
}
