# Android text resources

- Put every new user-visible label, message, placeholder, accessibility description, notification, and validation error in the owning Android module's `src/main/res/values/strings.xml`.
- In Compose, read text with `stringResource(R.string.name)`; outside Compose, use `Context.getString(R.string.name)`. Use formatted resources or `plurals` for dynamic values, never Kotlin interpolation for user-visible sentences.
- Do not move protocol keys, Firestore field names, navigation routes, IDs, logging tags, or other machine-readable constants into Android string resources.
- Before completing an Android UI change, search the touched files for new hardcoded user-facing text and verify it was extracted.
