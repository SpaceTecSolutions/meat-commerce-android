package com.spacetecsolutions.meatapp.feature.customerhome

internal data class LegalClause(val heading: String, val body: String)
internal data class LegalGroup(val heading: String, val clauses: List<LegalClause>)

internal object CustomerLegalContent {
    const val version = "1.1"
    const val lastUpdated = "23 September 2026"

    val terms = listOf(
        LegalGroup("Your Account & Use", listOf(
            LegalClause("About Meat Station", "Meat Station provides fresh meat ordering through this mobile app and its public website, meatstation.org. Product availability, prices, delivery areas, and checkout terms shown in the app apply to mobile orders."),
            LegalClause("Customer Account", "Keep your name, mobile number, and delivery details accurate. Access to your account is protected by the sign-in method provided in the app."))),
        LegalGroup("Orders & Products", listOf(
            LegalClause("Product Information", "Product descriptions, units, and prices are displayed before you add items to your cart. Availability may change before an order is confirmed."),
            LegalClause("Pricing", "The checkout review shows item prices, any delivery charge, and the total payable before confirmation."),
            LegalClause("Order Progress", "Order status may move from Placed to Confirmed, Preparing, Out for Delivery, and Delivered, or to Cancelled when cancellation is permitted."))),
        LegalGroup("Delivery & Fulfilment", listOf(
            LegalClause("Scheduled Delivery", "Select an available delivery date and time slot at checkout. The slot is an expected delivery window, not an exact-minute guarantee."),
            LegalClause("Delivery Address", "Provide a correct address, location pin, contact number, and any access instructions needed to complete delivery."),
            LegalClause("Receiving Perishable Goods", "Please check the delivered package promptly and contact support if there is an issue with your order."))),
        LegalGroup("Payments & COD", listOf(
            LegalClause("Payments", "Available methods may include Cash on Delivery and a configured online payment provider. The final amount and selected method are shown before confirmation. Online orders are created only after verified payment."))),
        LegalGroup("Cancellations & Issues", listOf(
            LegalClause("Cancellation", "An order may be cancelled from Order Details only while the app offers that action. Once preparation or delivery begins, cancellation may no longer be available."),
            LegalClause("Problems with an Order", "Fresh products need prompt review. Contact Customer Support about missing, damaged, or incorrect items so the shop can review the issue and available resolution."))),
        LegalGroup("Privacy & Legal", listOf(
            LegalClause("Personal Information", "Account, delivery, and order information is handled as described in the Privacy Policy. You can request account deletion from Profile Information."),
            LegalClause("Changes", "These terms may be updated when the service changes. The version and last-updated date shown here identify this published text."))),
    )

    val privacy = listOf(
        LegalGroup("Information We Collect", listOf(
            LegalClause("Account & Profile", "We use your name, mobile number, sign-in information, and account status to provide your customer account and contact you about orders."),
            LegalClause("Delivery Addresses & Location Pin", "Saved addresses may include recipient details, landmarks, and a selected map pin. The pin identifies a delivery destination; this screen does not require continuous or background customer location tracking."),
            LegalClause("Orders & Fulfilment", "Order records include items, quantities, prices, totals, selected delivery slots, instructions, and progress status."),
            LegalClause("Payments", "We record the selected payment method, provider references where applicable, amount due, and payment status. Sensitive card or UPI credentials are handled by the payment provider and are not stored by this app."),
            LegalClause("Notifications & Devices", "Push tokens and recent in-app notices support account and order alerts."))),
        LegalGroup("How We Use Information", listOf(
            LegalClause("Prepare & Deliver Orders", "The shop uses order and address details to prepare products and deliver them in the selected slot."),
            LegalClause("Service Updates & Support", "We use relevant contact and order details to send operational notices and respond to support requests."),
            LegalClause("Account Safety", "Authentication and device information helps protect accounts and prevent misuse."))),
        LegalGroup("Sharing & Security", listOf(
            LegalClause("Service Infrastructure", "The app uses its configured cloud, notification, and mapping providers to operate these services."),
            LegalClause("Fulfilment Personnel", "Assigned staff receive the order and recipient details needed to prepare and deliver an active order."),
            LegalClause("Protection", "Access to customer records is restricted by account and role permissions, and app traffic uses encrypted connections."))),
        LegalGroup("Retention, Deletion & Control", listOf(
            LegalClause("Account Deletion", "After active orders are completed or cancelled, you can delete your account in the app. The account profile, saved addresses, cart, notifications, device registrations, and sign-in identity are removed."),
            LegalClause("Historical Business Records", "Past order and payment facts may be retained without your name, contact details, address, or account identifier for accounting, legal, security, or fraud-prevention purposes."),
            LegalClause("Your Controls", "Profile Information lets you update your name and request deletion. Saved addresses and notifications can be managed in their respective screens."))),
        LegalGroup("Policy Updates & Contact", listOf(
            LegalClause("Updates", "The version and last-updated date on this page identify this policy text. Changes to data practices require an updated policy."),
            LegalClause("Questions", "Contact Customer Support from your Profile & Account screen for privacy questions."))),
    )
}
