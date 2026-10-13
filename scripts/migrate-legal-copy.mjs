import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";

const root = path.resolve(import.meta.dirname, "..");
const sourcePath = path.join(root, "feature/customerhome/src/main/java/com/spacetecsolutions/meatapp/feature/customerhome/CustomerLegalContent.kt");
const resourcePath = path.join(root, "feature/customerhome/src/main/res/values/strings.xml");
let source = fs.readFileSync(sourcePath, "utf8");
let resources = fs.readFileSync(resourcePath, "utf8");

function resourceName(value, prefix) {
  const slug = value.toLowerCase().normalize("NFKD").replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "").split("_").slice(0, 5).join("_") || "text";
  const hash = crypto.createHash("sha1").update(value).digest("hex").slice(0, 7);
  return `${prefix}_${slug.slice(0, 35)}_${hash}`;
}

function xmlEscape(value) {
  return value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/'/g, "\\'").replace(/@/g, "\\@");
}

function addString(name, value) {
  if (!resources.includes(`name="${name}"`)) {
    resources = resources.replace("</resources>", `    <string name="${name}">${xmlEscape(value)}</string>\n</resources>`);
  }
  return name;
}

source = source.replace(/internal data class LegalClause\(val heading: String, val body: String\)/,
  "internal data class LegalClause(val heading: Int, val body: Int)");
source = source.replace(/internal data class LegalGroup\(val heading: String, val clauses: List<LegalClause>\)/,
  "internal data class LegalGroup(val heading: Int, val clauses: List<LegalClause>)");
source = source.replace(/const val version = "([^"]+)"/, (_, value) => `val version = com.spacetecsolutions.meatapp.feature.customerhome.R.string.${addString("customer_legal_version", value)}`);
source = source.replace(/const val lastUpdated = "([^"]+)"/, (_, value) => `val lastUpdated = com.spacetecsolutions.meatapp.feature.customerhome.R.string.${addString("customer_legal_last_updated", value)}`);
source = source.replace(/LegalGroup\("((?:\\.|[^"\\])*)",\s*listOf\(/g, (_, heading) => {
  const key = addString(resourceName(heading, "legal_group"), heading);
  return `LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.${key}, listOf(`;
});
source = source.replace(/LegalClause\("((?:\\.|[^"\\])*)",\s*"((?:\\.|[^"\\])*)"\)/g, (_, heading, body) => {
  const headingKey = addString(resourceName(heading, "legal_heading"), heading);
  const bodyKey = addString(resourceName(body, "legal_body"), body);
  return `LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.${headingKey}, com.spacetecsolutions.meatapp.feature.customerhome.R.string.${bodyKey})`;
});
fs.writeFileSync(sourcePath, source);
fs.writeFileSync(resourcePath, resources);
