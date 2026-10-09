# World Gone Mad — Android / Android TV

Free Android app for https://ua-stena.info/. It displays recent WordPress posts, featured images and excerpts, and opens full articles on the original website. Interface labels are available in English, Russian and Ukrainian.

## Build APK
Open **Actions → Build World Gone Mad APK**. A push to main also starts a build. When it finishes successfully, open the run and download the `World-Gone-Mad-APK` artifact, then extract `app-debug.apk`.

No paid APIs, backend, account, or API keys are used. This is a test/debug APK for installation outside Google Play.

REST endpoints used:
- English: https://ua-stena.info/wp-json/wp/v2/posts?per_page=30&_embed=1
- Russian: https://ua-stena.info/ru/wp-json/wp/v2/posts?per_page=30&_embed=1
- Ukrainian: https://ua-stena.info/uk/wp-json/wp/v2/posts?per_page=30&_embed=1

If the localized REST endpoints differ on the site, update `apiUrl()` in `MainActivity.java`.
