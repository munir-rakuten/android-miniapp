import java.util.Date
import java.text.SimpleDateFormat
import kotlin.collections.mutableMapOf
apply {
    from("../config/android/application.gradle")
}
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("androidx.navigation.safeargs")
}

var property = { key: String ->
    if (System.getenv(key) != null) {
         System.getenv(key)
    } else {
        if (project.hasProperty(key)) {
            project.properties[key]
        } else {
             null
        }
    }
}

var numProperty = { key: String ->
    if (System.getenv(key) != null) {
         System.getenv(key)
    } else {
        if (project.hasProperty(key)) {
            project.properties[key]
        } else {
             "0"
        }
    }
}

android {
    defaultConfig {
        versionCode = 1
        versionName = project.version as String
        buildConfigField ("int", "ADDITIONAL_ANALYTICS_ACC", numProperty("ADDITIONAL_ANALYTICS_ACC").toString())
        buildConfigField ("int", "ADDITIONAL_ANALYTICS_AID", numProperty("ADDITIONAL_ANALYTICS_AID").toString())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Set signing config only if the keystore file exists
    val keystoreFile = file("release-keystore.jks")
    if (keystoreFile.exists()) {
        signingConfigs {
            create("release") {
                keyAlias = System.getenv("MINIAPP_RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("MINIAPP_RELEASE_KEY_PASSWORD")

                storeFile = keystoreFile
                storePassword = System.getenv("MINIAPP_KEYSTORE_PASSWORD")
            }
        }

        buildTypes.getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    val defaultAppName = "Mini App Sample"
    val commonPlaceholders = mutableMapOf(
        "appCenterSecret" to (project.properties["APPCENTER_SECRET"] ?: ""),
        "ratAcc"          to (project.properties["ANALYTICS_ACC"]  ?: "0"),
        "ratAid"          to (project.properties["ANALYTICS_AID"]  ?: "0")
    )
    var debugManifestPlaceholders = mutableMapOf(
        "baseUrl"                      to (project.properties["MINIAPP_SERVER_BASE_URL"] ?: "https://www.example.com/"),
        "isPreviewMode"                to (project.properties["IS_PREVIEW_MODE"] ?: true),
        "requireSignatureVerification" to (project.properties["REQUIRE_SIGNATURE_VERIFICATION"] ?: false),
        "hostAppUserAgentInfo"         to (project.properties["HOST_APP_UA_INFO"] ?: "MiniApp Demo App/$project.version"),
        "projectId"                    to (project.properties["HOST_PROJECT_ID"] ?: "test-host-project-id"),
        "subscriptionKey"              to (project.properties["HOST_APP_SUBSCRIPTION_KEY"] ?: "test-subs-key"),
        "adMobAppId"                   to (project.properties["ADMOB_APP_ID"] ?: ""),
        "appName"                      to "$defaultAppName DEBUG",
        "ratEndpoint"                  to (project.properties["ANALYTICS_ENDPOINT"] ?: ""),
        "sslPublicKey"                 to (project.properties["CERTIFICATE_PUBLIC_KEY"] ?: ""),
        "sslPublicKeyBackup"           to (project.properties["CERTIFICATE_PUBLIC_KEY_BACKUP"] ?: ""),
        "enableH5Ads"                  to (project.properties["ENABLE_H5_ADS"] ?: true),
        "maxStorageSizeLimitInBytes"   to (project.properties["MAX_STORAGE_SIZE_LIMIT_IN_BYTES"] ?: 5242880)
        ) + commonPlaceholders

    var stagingManifestPlaceholders = debugManifestPlaceholders
    //stagingManifestPlaceholders.appName = defaultAppName + " STG"

    var prodManifestPlaceholders = mutableMapOf(
        "baseUrl"                      to (project.properties["MINIAPP_PROD_SERVER_BASE_URL"] ?: "https://www.example.com/"),
        "isPreviewMode"                to (project.properties["PROD_IS_PREVIEW_MODE"] ?: true),
        "requireSignatureVerification" to (project.properties["PROD_REQUIRE_SIGNATURE_VERIFICATION"] ?: false),
        "hostAppUserAgentInfo"         to (project.properties["HOST_APP_PROD_UA_INFO"] ?: "MiniApp Demo App/$project.version"),
        "projectId"                    to (project.properties["HOST_PROJECT_PROD_ID"] ?: "test-host-project-id"),
        "subscriptionKey"              to (project.properties["HOST_APP_PROD_SUBSCRIPTION_KEY"] ?: "test-subs-key"),
        "adMobAppId"                   to (project.properties["PROD_ADMOB_APP_ID"] ?: ""),
        "appName"                      to  defaultAppName,
        "ratEndpoint"                  to (project.properties["PROD_ANALYTICS_ENDPOINT"] ?: "https://www.example.com/"),
        "sslPublicKey"                 to (project.properties["PROD_CERTIFICATE_PUBLIC_KEY"] ?: ""),
        "sslPublicKeyBackup"           to (project.properties["PROD_CERTIFICATE_PUBLIC_KEY_BACKUP"] ?: ""),
        "enableH5Ads"                  to (project.properties["ENABLE_H5_ADS"] ?: true),
        "maxStorageSizeLimitInBytes"   to (project.properties["MAX_STORAGE_SIZE_LIMIT_IN_BYTES"] ?: 5242880)
    ) + commonPlaceholders

    val buildVersion = System.getenv("CIRCLE_BUILD_NUM") ?:  SimpleDateFormat("yyMMddHHmm").format(Date())

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix ="-DEBUG"
            resValue ("string", "build_version", buildVersion)
            resValue ("string", "miniapp_sdk_version", project.version as String)
            resValue ("string", "appcenter_secret", "${debugManifestPlaceholders["appCenterSecret"]}")
            resValue ("string", "sslPublicKey", "${debugManifestPlaceholders["sslPublicKey"]}")
            resValue ("string", "sslPublicKeyBackup", "${debugManifestPlaceholders["sslPublicKeyBackup"]}")
            resValue ("string", "sslPublicKeyProd", "${prodManifestPlaceholders["sslPublicKey"]}")
            resValue ("string", "sslPublicKeyProdBackup", "${prodManifestPlaceholders["sslPublicKeyBackup"]}")
            resValue ("string", "stagingBaseUrl", "${stagingManifestPlaceholders["baseUrl"]}")
            resValue ("string", "stagingProjectId", "${stagingManifestPlaceholders["projectId"]}")
            resValue ("string", "stagingSubscriptionKey", "${stagingManifestPlaceholders["subscriptionKey"]}")
            resValue ("string", "prodBaseUrl", "${prodManifestPlaceholders["baseUrl"]}")
            resValue ("string", "prodProjectId", "${prodManifestPlaceholders["projectId"]}")
            resValue ("string", "prodSubscriptionKey", "${prodManifestPlaceholders["subscriptionKey"]}")
            isDebuggable = true
            isMinifyEnabled = false
            buildConfigField ("Boolean", "ENABLE_APPCENTER_CRASHLYTICS", "false")

            manifestPlaceholders = debugManifestPlaceholders
        }
        getByName("release") {
            resValue ("string", "build_version", buildVersion)
            resValue ("string", "miniapp_sdk_version", project.version as String)
            resValue ("string", "appcenter_secret", "${prodManifestPlaceholders["appCenterSecret"]}")
            resValue ("string", "sslPublicKey", "${debugManifestPlaceholders["sslPublicKey"]}")
            resValue ("string", "sslPublicKeyBackup", "${debugManifestPlaceholders["sslPublicKeyBackup"]}")
            resValue ("string", "sslPublicKeyProd", "${prodManifestPlaceholders["sslPublicKey"]}")
            resValue ("string", "sslPublicKeyProdBackup", "${prodManifestPlaceholders["sslPublicKeyBackup"]}")
            resValue ("string", "stagingBaseUrl", "${stagingManifestPlaceholders["baseUrl"]}")
            resValue ("string", "stagingProjectId", "${stagingManifestPlaceholders["projectId"]}")
            resValue ("string", "stagingSubscriptionKey", "${stagingManifestPlaceholders["subscriptionKey"]}")
            resValue ("string", "prodBaseUrl", "${prodManifestPlaceholders["baseUrl"]}")
            resValue ("string", "prodProjectId", "${prodManifestPlaceholders["projectId"]}")
            resValue ("string", "prodSubscriptionKey", "${prodManifestPlaceholders["subscriptionKey"]}")
            isDebuggable = true
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField ("Boolean", "ENABLE_APPCENTER_CRASHLYTICS", "true")
            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            manifestPlaceholders = prodManifestPlaceholders
        }
        create("staging") {
            initWith(getByName("release"))
            resValue ("string", "sslPublicKey", "${stagingManifestPlaceholders["sslPublicKey"]}")
            resValue ("string", "sslPublicKeyBackup", "${stagingManifestPlaceholders["sslPublicKeyBackup"]}")
            resValue ("string", "sslPublicKeyProd", "${prodManifestPlaceholders["sslPublicKey"]}")
            resValue ("string", "sslPublicKeyProdBackup", "${prodManifestPlaceholders["sslPublicKeyBackup"]}")
            resValue ("string", "stagingBaseUrl", "${stagingManifestPlaceholders["baseUrl"]}")
            resValue ("string", "stagingProjectId", "${stagingManifestPlaceholders["projectId"]}")
            resValue ("string", "stagingSubscriptionKey", "${stagingManifestPlaceholders["subscriptionKey"]}")
            resValue ("string", "prodBaseUrl", "${prodManifestPlaceholders["baseUrl"]}")
            resValue ("string", "prodProjectId", "${prodManifestPlaceholders["projectId"]}")
            resValue ("string", "prodSubscriptionKey", "${prodManifestPlaceholders["subscriptionKey"]}")
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-STG-build-$buildVersion"
            buildConfigField ("Boolean", "ENABLE_APPCENTER_CRASHLYTICS", "true")
            matchingFallbacks = listOf("release", "debug")

            manifestPlaceholders = stagingManifestPlaceholders
        }
        create("rc") {
            initWith(getByName("release"))
            resValue ("string", "sslPublicKey", "${stagingManifestPlaceholders["sslPublicKey"]}")
            resValue ("string", "sslPublicKeyBackup", "${stagingManifestPlaceholders["sslPublicKeyBackup"]}")
            resValue ("string", "sslPublicKeyProd", "${prodManifestPlaceholders["sslPublicKey"]}")
            resValue ("string", "sslPublicKeyProdBackup", "${prodManifestPlaceholders["sslPublicKeyBackup"]}")
            resValue ("string", "stagingBaseUrl", "${stagingManifestPlaceholders["baseUrl"]}")
            resValue ("string", "stagingProjectId", "${stagingManifestPlaceholders["projectId"]}")
            resValue ("string", "stagingSubscriptionKey", "${stagingManifestPlaceholders["subscriptionKey"]}")
            resValue ("string", "prodBaseUrl", "${prodManifestPlaceholders["baseUrl"]}")
            resValue ("string", "prodProjectId", "${prodManifestPlaceholders["projectId"]}")
            resValue ("string", "prodSubscriptionKey", "${prodManifestPlaceholders["subscriptionKey"]}")
            applicationIdSuffix = ".rc"
            versionNameSuffix = "-RC-build-$buildVersion"
            buildConfigField ("Boolean", "ENABLE_APPCENTER_CRASHLYTICS", "true")
            matchingFallbacks = listOf("release", "debug")

            manifestPlaceholders = stagingManifestPlaceholders
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    compileOnly ("javax.annotation:jsr250-api:${rootProject.ext["jsr250"]}")

    implementation (project(":miniapp"))
    implementation ("androidx.appcompat:appcompat:${rootProject.ext["androidx_appcompat"]}")
    implementation ("androidx.constraintlayout:constraintlayout:${rootProject.ext["androidx_constraintLayout"]}")
    implementation ("androidx.core:core-ktx:${rootProject.ext["androidx_coreKtx"]}")
    implementation ("androidx.lifecycle:lifecycle-extensions:${rootProject.ext["androidx_lifecycle"]}")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:${rootProject.ext["androidx_lifecycle_viewmodel"]}")
    implementation ("androidx.activity:activity-ktx:${rootProject.ext["androidx_activity"]}")

    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${rootProject.ext["kotlin_version"]}")
    implementation ("com.google.code.gson:gson:${rootProject.ext["gson"]}")
    if (property("GITHUB_USERNAME") != null) {
        implementation ("com.rakuten.tech.mobile.analytics:analytics:${rootProject.ext["analytics_sdk"]}")
    } else {
        implementation (project(":analytics"))
    }
    implementation (project(":admob-latest"))

    implementation ("com.github.bumptech.glide:glide:${rootProject.ext["glide"]}")
    annotationProcessor ("com.github.bumptech.glide:compiler:${rootProject.ext["glide"]}")
    implementation ("com.google.android.material:material:${rootProject.ext["material"]}")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:${rootProject.ext["swipe_refresh_layout"]}")
    implementation ("androidx.recyclerview:recyclerview:${rootProject.ext["recyclerview"]}")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.google.android.gms:play-services-ads:${rootProject.ext["google_ads"]}")
    implementation ("com.microsoft.appcenter:appcenter-crashes:${rootProject.ext["appCenterSdkVersion"]}")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.4.1")
    implementation ("androidx.navigation:navigation-ui-ktx:2.4.1")
    testImplementation ("junit:junit:${rootProject.ext["junit"]}")
}

// Some of our dependencies also use manifest-config-processor, but they use the old `com.rakutentech.mobile` version
configurations.all {
    exclude(group = "com.rakuten.tech.mobile", module = "manifest-config-processor")
    exclude(group = "com.rakuten.tech.mobile", module = "manifest-config-annotations")
}
