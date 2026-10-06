// One-time migration for records created before the three-day retention policy.
// Requires Application Default Credentials for the intended Firebase project.
// Dry-run by default. Explicitly pass --apply to write expiration timestamps.
const {initializeApp} = require('firebase-admin/app');
const {getFirestore, FieldPath, Timestamp} = require('firebase-admin/firestore');
const projectId = process.env.GOOGLE_CLOUD_PROJECT;
if (!projectId) throw new Error('Set GOOGLE_CLOUD_PROJECT to the intended project first.');
const db = getFirestore(initializeApp({projectId}), 'default');
async function run() {
  let cursor; let eligible = 0;
  const apply = process.argv.includes('--apply');
  while (true) {
    let query = db.collectionGroup('notifications').orderBy(FieldPath.documentId()).limit(400);
    if (cursor) query = query.startAfter(cursor);
    const page = await query.get();
    if (page.empty) break;
    const batch = db.batch(); let count = 0;
    for (const doc of page.docs) {
      const created = doc.get('createdAt');
      if (doc.ref.path.split('/').length !== 4 || !doc.ref.path.startsWith('users/') ||
          doc.get('expiresAt') || !(created instanceof Timestamp)) continue;
      batch.update(doc.ref, {expiresAt: Timestamp.fromMillis(created.toMillis() + 3 * 86400000)});
      count++;
    }
    eligible += count;
    if (apply && count) await batch.commit();
    cursor = page.docs[page.docs.length - 1];
  }
  console.log(`${apply ? 'Updated' : 'Dry run: eligible'} notifications: ${eligible}`);
}
run().catch((error) => { console.error(error.message); process.exitCode = 1; });
