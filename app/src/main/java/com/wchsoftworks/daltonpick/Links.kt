package com.wchsoftworks.daltonpick

import com.wchsoftworks.daltonpick.colorengine.Lang

/** Every web address the app opens. All of them open in the user's browser; the app has no INTERNET permission. */
object Links {
    /** Plain donation page only: no memberships or shop items with perks (spec §7). */
    const val KOFI = "https://ko-fi.com/willcaphir"

    const val SOURCE = "https://github.com/WCH-Softworks/dalton-pick"

    const val APACHE_2 = "https://www.apache.org/licenses/LICENSE-2.0"

    const val SITE = "https://willcaphir.com/daltonpick/"

    /** The policy page on willcaphir.com (generated from docs/store/privacy-policy.*.md by scripts/export-privacy.mjs). */
    fun privacyPolicy(lang: Lang) = when (lang) {
        Lang.EN -> "${SITE}privacy/"
        else -> "$SITE${lang.tag}/privacy/"
    }
}
