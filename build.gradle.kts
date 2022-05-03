plugins {
    id("com.android.application") version "7.1.3" apply false
    id("org.jetbrains.kotlin.android") version "1.6.10" apply false
    id("com.google.gms.google-services") version "4.3.10" apply false
}

tasks.register("clean", Delete::class) { delete(rootProject.buildDir) }