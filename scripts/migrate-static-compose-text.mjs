import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";

// Conservative UI-only migration. Protocol constants require manual review.
const root = path.resolve(import.meta.dirname, "..");
const apply = process.argv.includes("--apply");
const modules = ["app", "core/designsystem", ...fs.readdirSync(path.join(root, "feature"))
  .filter((name) => fs.existsSync(path.join(root, "feature", name, "build.gradle.kts")))
  .map((name) => `feature/${name}`)];

function filesUnder(directory) {
  if (!fs.existsSync(directory)) return [];
  return fs.readdirSync(directory, {withFileTypes: true}).flatMap((entry) => {
    const full = path.join(directory, entry.name);
    return entry.isDirectory() ? filesUnder(full) : full.endsWith(".kt") ? [full] : [];
  });
}

function nameFor(value) {
  const slug = value.toLowerCase().normalize("NFKD").replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "").split("_").slice(0, 5).join("_") || "text";
  const digest = crypto.createHash("sha1").update(value).digest("hex").slice(0, 7);
  return `ui_${slug.slice(0, 45)}_${digest}`;
}

function xmlText(value) {
  return value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/'/g, "\\'").replace(/%/g, "%%").replace(/@/g, "\\@");
}

function formatTemplate(literal) {
  if (literal.includes("\\") || !literal.includes("$")) return null;
  const token = /\$\{([^{}"']+)\}|\$([A-Za-z_]\w*)/g;
  const argumentsFound = [];
  const parts = [];
  let offset = 0;
  for (const match of literal.matchAll(token)) {
    const expression = match[1] ?? match[2];
    if (!expression?.trim()) return null;
    parts.push(xmlText(literal.slice(offset, match.index)));
    argumentsFound.push(expression.trim());
    parts.push(`%${argumentsFound.length}$s`);
    offset = match.index + match[0].length;
  }
  if (!argumentsFound.length || literal.slice(offset).includes("$")) return null;
  parts.push(xmlText(literal.slice(offset)));
  if (parts.every((part) => /^%\d+\$s$/.test(part) || !part.trim())) return null;
  return {value: parts.join(""), argumentsFound};
}

let fileCount = 0;
let literalCount = 0;
for (const module of modules) {
  const moduleRoot = path.join(root, module);
  const gradle = fs.readFileSync(path.join(moduleRoot, "build.gradle.kts"), "utf8");
  const namespace = gradle.match(/namespace\s*=\s*"([^"]+)"/)?.[1];
  if (!namespace) throw new Error(`No namespace in ${module}`);
  const resources = new Map();
  for (const file of filesUnder(path.join(moduleRoot, "src", "main"))) {
    const before = fs.readFileSync(file, "utf8");
    function reference(literal) {
      if (!literal || literal.includes("\\")) return null;
      const key = nameFor(literal);
      const template = literal.includes("$") ? formatTemplate(literal) : null;
      if (literal.includes("$") && !template) return null;
      resources.set(key, template?.value ?? xmlText(literal));
      literalCount++;
      const args = template?.argumentsFound.map((expression) => `, ${expression}`).join("") ?? "";
      return `androidx.compose.ui.res.stringResource(${namespace}.R.string.${key}${args})`;
    }
    let after = before.replace(/\bText\("((?:\\.|[^"\\])*)"/g, (match, literal) => {
      const resource = reference(literal);
      return resource ? `Text(${resource}` : match;
    });
    after = after.replace(/\bText\((if\s*\([^)]*\)\s*)"((?:\\.|[^"\\])*)"\s*else\s*"((?:\\.|[^"\\])*)"/g,
      (match, condition, yes, no) => {
        const yesResource = reference(yes);
        const noResource = reference(no);
        return yesResource && noResource ? `Text(${condition}${yesResource} else ${noResource}` : match;
      });
    after = after.replace(/\bText\(([^\n]*?\?\s*:)\s*"((?:\\.|[^"\\])*)"/g,
      (match, expression, literal) => {
        const resource = reference(literal);
        return resource ? `Text(${expression} ${resource}` : match;
      });
    after = after.replace(/\bcontentDescription\s*=\s*"((?:\\.|[^"\\])*)"/g, (match, literal) => {
      const resource = reference(literal);
      return resource ? `contentDescription = ${resource}` : match;
    });
    after = after.replace(/\bIcon\(([^,\n]+),\s*"((?:\\.|[^"\\])*)"/g, (match, icon, literal) => {
      const resource = reference(literal);
      return resource ? `Icon(${icon}, ${resource}` : match;
    });
    after = after.replace(/\bAppBackTopBar\("((?:\\.|[^"\\])*)"/g, (match, literal) => {
      const resource = reference(literal);
      return resource ? `AppBackTopBar(${resource}` : match;
    });
    if (before.includes("@Composable")) {
      after = after.replace(/\b(title|description|actionLabel|placeholder)\s*=\s*"((?:\\.|[^"\\])*)"/g,
        (match, field, literal) => {
          const resource = reference(literal);
          return resource ? `${field} = ${resource}` : match;
        });
    }
    if (before !== after) {
      fileCount++;
      if (apply) fs.writeFileSync(file, after);
    }
  }
  if (!resources.size) continue;
  const xmlPath = path.join(moduleRoot, "src", "main", "res", "values", "strings.xml");
  const existing = fs.existsSync(xmlPath) ? fs.readFileSync(xmlPath, "utf8") :
    '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n</resources>\n';
  const additions = [...resources].filter(([key]) => !existing.includes(`name="${key}"`))
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, value]) => `    <string name="${key}">${value}</string>`).join("\n");
  if (additions && apply) {
    fs.mkdirSync(path.dirname(xmlPath), {recursive: true});
    fs.writeFileSync(xmlPath, existing.replace("</resources>", `${additions}\n</resources>`));
  }
  process.stdout.write(`${module}: ${resources.size} resources\n`);
}
process.stdout.write(`${apply ? "Migrated" : "Would migrate"} ${literalCount} literals in ${fileCount} files.\n`);
