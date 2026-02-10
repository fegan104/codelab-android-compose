package com.fitnow.vegas.ui

interface PromotionCreativeClickListener {

    /**
     * Called when the creative is shown.
     *
     * @param response The [VegasResponse] that was shown.
     */
    fun onShown(response: VegasResponse<*>)

    /**
     * Called when the user clicks the "Open Action" button.
     *
     * @param actionUrl The URL to open
     */
    fun onOpenAction(actionUrl: String?)

    /**
     * Called when the user dismisses the creative.
     */
    fun onDismiss()
}