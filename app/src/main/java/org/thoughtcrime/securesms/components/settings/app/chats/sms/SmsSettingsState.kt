package org.thoughtcrime.securesms.components.settings.app.chats.sms

data class SmsSettingsState(
  val useAsDefaultSmsApp: Boolean,
  val smsDeliveryReportsEnabled: Boolean,
  val wifiCallingCompatibilityEnabled: Boolean,
  val smsMmsFromSelfEnabled: Boolean
)
