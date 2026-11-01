package com.wchsoftworks.daltonpick

import com.wchsoftworks.daltonpick.colorengine.Lang

/** Every web address the app opens. All of them open in the user's browser; the app has no INTERNET permission. */
object Links {
    /** Plain donation page only: no memberships or shop items with perks (spec §7). */
    const val KOFI = "https://ko-fi.com/willcaphir"

    const val SOURCE = "https://github.com/WCH-Softworks/dalton-pick"

    const val APACHE_2 = "https://www.apache.org/licenses/LICENSE-2.0"

    /** Until the policy is hosted on its own page (M6), link to the file in the repository. */
    fun privacyPolicy(lang: Lang) = "$SOURCE/blob/main/docs/store/privacy-policy.${lang.tag}.md"
}
