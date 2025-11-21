# Passkeys Setup – Server + Android Linking (Simple Guide)

## 1. Get the SHA-256 fingerprint

Run:

    keytool -list -v \
      -alias <ALIAS> \
      -keystore <path-to-keystore>.jks

Copy the `SHA256:` value, e.g.:

    3A:55:F2:90:AF:...:9C

**Why?**  
This identifies the exact Android app that is allowed to use passkeys.

---

## 2. Convert SHA-256 → `android:apk-key-hash` and set `expectedOrigin`

1. Remove all colons:

       3A55F290AF...9C

2. Convert hex → Base64URL:

       https://cryptii.com/pipes/hex-to-base64  
       (choose **Base64url**)

3. Result example:

       H8aaJx3lOZCaxVnsZU5__ALkVjXJALA11rtegEE0Ldc

4. Build the Android origin:

       android:apk-key-hash:H8aaJx3lOZCaxVnsZU5__ALkVjXJALA11rtegEE0Ldc

5. Add both web + Android origins to your backend:

       export const expectedOrigin = [
         "https://your-domain",
         "android:apk-key-hash:<your-base64url-hash>",
       ];

**Why?**  
Your WebAuthn server must accept only these two origins.

---

## 3. Generate and host `assetlinks.json`

Required location:

    https://your-domain/.well-known/assetlinks.json

Use Google’s generator:

    https://developers.google.com/digital-asset-links/tools/generator

Fill in package name + SHA-256, then include **both** relations:

- `delegate_permission/common.get_login_creds`
- `delegate_permission/common.handle_all_urls`

**Why?**  
Android must confirm that your app is linked to your domain
before passkeys can work.

---

## 4. Enable the domain in Google Play Console

In your app’s Play Console:

    Grow users → Deep links → Domains

Add your domain and **enable credential sharing**.

**Why?**  
This lets Google Play Services share passkeys between your website and your app.
