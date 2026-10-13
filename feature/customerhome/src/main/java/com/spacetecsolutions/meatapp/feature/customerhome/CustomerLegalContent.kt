package com.spacetecsolutions.meatapp.feature.customerhome

internal data class LegalClause(val heading: Int, val body: Int)
internal data class LegalGroup(val heading: Int, val clauses: List<LegalClause>)

internal object CustomerLegalContent {
    val version = com.spacetecsolutions.meatapp.feature.customerhome.R.string.customer_legal_version
    val lastUpdated = com.spacetecsolutions.meatapp.feature.customerhome.R.string.customer_legal_last_updated

    val terms = listOf(
        LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_group_your_account_use_bebaef3, listOf(
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_about_meat_station_c6f8180, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_meat_station_provides_fresh_meat_0f07250),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_customer_account_2cb87ac, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_keep_your_name_mobile_number_043a1da))),
        LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_group_orders_products_d844700, listOf(
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_product_information_8f093e6, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_product_descriptions_units_and_pric_6a7ed3f),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_pricing_a0d9bba, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_the_checkout_review_shows_item_f0829a6),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_order_progress_917954c, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_order_status_may_move_from_5faa237))),
        LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_group_delivery_fulfilment_9e10704, listOf(
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_scheduled_delivery_0961476, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_select_an_available_delivery_date_7358526),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_delivery_address_1f63936, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_provide_a_correct_address_location_9414316),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_receiving_perishable_goods_d011ef8, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_please_check_the_delivered_package_c4414df))),
        LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_group_payments_cod_0d36dcd, listOf(
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_payments_44357ae, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_available_methods_may_include_cash_9fd8210))),
        LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_group_cancellations_issues_d4ed200, listOf(
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_cancellation_319aaae, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_an_order_may_be_cancelled_7a681df),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_problems_with_an_order_73b3166, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_fresh_products_need_prompt_review_115ab07))),
        LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_group_privacy_legal_3a30e90, listOf(
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_personal_information_ad12e42, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_account_delivery_and_order_informat_7375e35),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_changes_8aa57de, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_these_terms_may_be_updated_77a9712))),
    )

    val privacy = listOf(
        LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_group_information_we_collect_6370b2f, listOf(
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_account_profile_8a259c8, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_we_use_your_name_mobile_f07ccb3),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_delivery_addresses_location_pin_38fa88c, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_saved_addresses_may_include_recipie_98dd63f),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_orders_fulfilment_7e8beae, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_order_records_include_items_quantit_d91dd55),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_payments_44357ae, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_we_record_the_selected_payment_678669e),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_notifications_devices_dc9cead, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_push_tokens_and_recent_in_db23eb1))),
        LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_group_how_we_use_information_c0b51a3, listOf(
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_prepare_deliver_orders_bbeb192, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_the_shop_uses_order_and_dfccbd1),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_service_updates_support_c57e68d, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_we_use_relevant_contact_and_a5624f7),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_account_safety_a78e74b, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_authentication_and_device_informati_7a04ea4))),
        LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_group_sharing_security_841a72f, listOf(
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_service_infrastructure_372a603, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_the_app_uses_its_configured_4fc1a9d),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_fulfilment_personnel_7a4ad36, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_assigned_staff_receive_the_order_2d31d29),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_protection_0f8cc4f, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_access_to_customer_records_is_0620f1c))),
        LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_group_retention_deletion_control_66783c5, listOf(
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_account_deletion_18b90b6, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_after_active_orders_are_completed_148c3dc),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_historical_business_records_25682cb, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_past_order_and_payment_facts_df14f11),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_your_controls_e33870a, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_profile_information_lets_you_update_935e004))),
        LegalGroup(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_group_policy_updates_contact_101b097, listOf(
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_updates_c76d180, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_the_version_and_last_updated_48cfe3b),
            LegalClause(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_heading_questions_9a1fc17, com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_body_contact_customer_support_from_your_a75603c))),
    )
}
