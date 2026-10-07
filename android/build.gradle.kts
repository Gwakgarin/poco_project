// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // FCM: google-services.json 을 읽어주는 플러그인 (버전은 Firebase 콘솔 안내에 나온 최신으로 맞춰도 됨)
    id("com.google.gms.google-services") version "4.4.2" apply false
}