# Customer Address Backend Contract

Phase 18 uses these callable Cloud Functions:

- `customerGetAddresses`
- `customerCreateAddress`
- `customerUpdateAddress`
- `customerDeleteAddress`
- `customerSetDefaultAddress`

Every callable requires Firebase Authentication, valid App Check, and an active `CUSTOMER`. The customer ID and shop ID must be derived from the authenticated profile. Never accept an owner/customer ID from Android. Address IDs must be resolved only inside that customer's address collection, preventing enumeration or cross-customer access.

Validate and normalize all fields on the server: type (`HOME`, `WORK`, `OTHER`), name, configurable-country mobile, address, optional landmark, city, state, and postal code. Enforce the same or stricter length bounds as the client and cap the number of saved addresses per customer.

Create, edit, delete, and default mutations return the complete ordered address list. Use revision checks for edits/deletes/default changes and return `ABORTED` on stale writes. Exactly one address is default whenever the list is non-empty. The first address becomes default automatically. Setting a default must atomically clear the previous default. If the default is deleted while other addresses remain, select a deterministic replacement in the same transaction.

Direct client Firestore writes should remain denied. Address reads and mutations go through these authorized functions so fields cannot be reassigned to another customer. Do not expose addresses to ADMIN, DELIVERY, or SUPER_ADMIN through these customer endpoints; later order workflows should copy only the selected delivery snapshot into an authorized order.

Test cross-customer and cross-shop IDs, non-CUSTOMER roles, inactive users, invalid App Check, malformed fields, address limits, stale revisions, concurrent default changes, deletion of the current default, and deterministic single-default invariants.
