## iMe Messenger is an unofficial Telegram client

The development of the main functionality is made by the [Telegram](https://telegram.org) team which creates the most advanced and sophisticated messenger in the world.
Telegram provides the opportunity to use its [open code](https://telegram.org/apps#source-code) and [API](https://core.telegram.org/#tdlib-build-your-own-telegram) to implement brand-new solutions. 
The iMe team develops and adds new features in the messenger without breaking user privacy and violating security principles, enjoying the benefits provided by Telegram. 

## Features

## In-house developments and integrations:
- iMe Wallet and internal AiCoin
- Neurobots assistants for chatting
- Neurobots Store
- Telegram channels, groups, and chatbots’ collection
- Animated stickers with the addition of a text
- Integrated Google Translator
- Text from a Photo
- Photo Description
- Transcribing voice messages to text
- Fast forwarding messages to WhatsApp, WA Business, and Viber

## Features based on the Telegram functionality:

- Efficient Cloud based on the Saved Messages chat
- Multi-panel in chats
- Automatic sorting of chats’ lists by types: Unread; Personal; Groups; Channels; Bots
- Customized Telegram folders
- Management. Includes tabs: Owner; Administrator; Undelivered; Drafts
- Forward messages without quotes
- Compact view of a chat list
- Set of iMe app icons

## Additional Telegram configurations:

- Going to the first message in a chat
- Opportunity to pin chats’ previews to view their content without opening them
- Hiding a pinned message
- Display accounts on the home screen for quick switching between them
- Opportunity to choose what account data to display in the sidebar
- “Move to archive” option in the chat’s setting of the hamburger menu
- Disable/enable the lower panel in channels
- Disable/enable rounding the number of users in chats
- Confirmation before a call
- Sending GIFs and stickers without sound
- Clickable links in profile description
- User ID in profile
- Stickers, GIF, voice and video messages preview before sending

Download iMe from the [App Store](https://apps.apple.com/ru/app/ime-messenger-%D0%B4%D0%BB%D1%8F-telegram/id1450480822) or [Google Play](https://play.google.com/store/apps/details?id=com.iMe.android&hl)!

Our channel [@iMe_en](https://t.me/ime_en) / [@iMe_ru](https://t.me/ime_ru)

Our group [@iMe_ai](https://t.me/ime_ai)

[Privacy policy](https://www.imem.app/privacy-policy.html)

## Creating your Telegram Application


We welcome all developers to use our API and source code to create applications on our platform.
There are several things we require from **all developers** for the moment.

1. [**Obtain your own api_id**](https://core.telegram.org/api/obtaining_api_id) for your application.
2. Please **do not** use the name Telegram for your app — or make sure your users understand that it is unofficial.
3. Kindly **do not** use our standard logo (white paper plane in a blue circle) as your app's logo.
3. Please study our [**security guidelines**](https://core.telegram.org/mtproto/security_guidelines) and take good care of your users' data and privacy.
4. Please remember to publish **your** code too in order to comply with the licences.

### API, Protocol documentation

Telegram API manuals: https://core.telegram.org/api

MTproto protocol manuals: https://core.telegram.org/mtproto

### Compilation Guide

You will require Android Studio 3.4, Android NDK rev. 20 and Android SDK 8.1

1. Download the Telegram source code from https://github.com/DrKLO/Telegram ( git clone https://github.com/DrKLO/Telegram.git )
2. Copy your release.keystore into TMessagesProj/config
3. Fill out RELEASE_KEY_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_STORE_PASSWORD in gradle.properties to access your  release.keystore
4.  Go to https://console.firebase.google.com/, create two android apps with application IDs org.telegram.messenger and org.telegram.messenger.beta, turn on firebase messaging and download google-services.json, which should be copied to the same folder as TMessagesProj.
5. Open the project in the Studio (note that it should be opened, NOT imported).
6. Fill out values in TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java – there’s a link for each of the variables showing where and which data to obtain.
7. You are ready to compile Telegram.

### Localization

We moved all translations to https://translations.telegram.org/en/android/. Please use it.
