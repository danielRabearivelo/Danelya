import { AndroidCodeFile } from '../types';

export const ANDROID_PROJECT_FILES: AndroidCodeFile[] = [
  // 1. Version Catalog
  {
    path: 'gradle/libs.versions.toml',
    step: 1,
    title: 'Catalogue de versions (libs.versions.toml)',
    category: 'config',
    language: 'toml',
    content: `[versions]
agp = "8.3.0"
kotlin = "1.9.22"
coreKtx = "1.12.0"
lifecycleRuntimeKtx = "2.7.0"
activityCompose = "1.8.2"
composeBom = "2024.02.00"
navigationCompose = "2.7.7"
room = "2.6.1"
ksp = "1.9.22-1.0.17"
hilt = "2.50"
hiltNavigationCompose = "1.1.0"
datastore = "1.0.0"
workManager = "2.9.0"
junit = "4.13.2"
androidxJunit = "1.1.5"
espresso = "3.5.1"
coroutinesTest = "1.8.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# Room (SQLite local)
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hiltNavigationCompose" }

# DataStore & WorkManager
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workManager" }

# Tests
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxJunit" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }`,
  },

  // 2. Root build.gradle.kts
  {
    path: 'build.gradle.kts',
    step: 1,
    title: 'Fichier racine build.gradle.kts',
    category: 'gradle',
    language: 'gradle',
    content: `// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}`,
  },

  // 3. settings.gradle.kts
  {
    path: 'settings.gradle.kts',
    step: 1,
    title: 'Fichier settings.gradle.kts',
    category: 'gradle',
    language: 'gradle',
    content: `pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CaisseAssociation"
include(":app")`,
  },

  // 4. app/build.gradle.kts
  {
    path: 'app/build.gradle.kts',
    step: 1,
    title: 'Configuration Module app/build.gradle.kts',
    category: 'gradle',
    language: 'gradle',
    content: `plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.association.caisse"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.association.caisse"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Room export schema
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)

    // DataStore & WorkManager
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
}`,
  },

  // 5. AndroidManifest.xml
  {
    path: 'app/src/main/AndroidManifest.xml',
    step: 1,
    title: 'Manifeste Android (100% Hors-ligne, Android 14)',
    category: 'config',
    language: 'xml',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- 
      AUCUNE permission INTERNET requise (100% hors-ligne garanti).
      AUCUNE permission de stockage large (READ/WRITE_EXTERNAL_STORAGE).
      Utilisation du Storage Access Framework (SAF) natif.
      AUCUNE permission SEND_SMS : l'utilisateur valide l'envoi dans son appli SMS.
    -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Détection de WhatsApp et de l'application SMS par intent queries (Android 11+) -->
    <queries>
        <!-- WhatsApp Messenger & Business -->
        <package android:name="com.whatsapp" />
        <package android:name="com.whatsapp.w4b" />
        <!-- Application SMS standard -->
        <intent>
            <action android:name="android.intent.action.SENDTO" />
            <data android:scheme="smsto" />
        </intent>
        <!-- Application Composeur d'appels -->
        <intent>
            <action android:name="android.intent.action.DIAL" />
            <data android:scheme="tel" />
        </intent>
    </queries>

    <application
        android:name=".CaisseApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.CaisseAssociation"
        tools:targetApi="34">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"
            android:theme="@style/Theme.CaisseAssociation">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Désactivation de l'initialiseur WorkManager par défaut pour Hilt -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="\${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>

    </application>

</manifest>`,
  },

  // 6. Application class
  {
    path: 'app/src/main/java/com/association/caisse/CaisseApplication.kt',
    step: 1,
    title: 'Classe Application (Hilt & WorkManager)',
    category: 'config',
    language: 'kotlin',
    content: `package com.association.caisse

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CaisseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal pour les rappels de relance des cotisations
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.channel_reminders_desc)
            }

            // Canal pour les rappels de versement à la caisse
            val remittanceChannel = NotificationChannel(
                CHANNEL_REMITTANCE_ID,
                getString(R.string.channel_remittance_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.channel_remittance_desc)
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(remittanceChannel)
        }
    }

    companion object {
        const val CHANNEL_REMINDERS_ID = "channel_caisse_reminders"
        const val CHANNEL_REMITTANCE_ID = "channel_caisse_remittances"
    }
}`,
  },

  // 7. Room Entities
  {
    path: 'app/src/main/java/com/association/caisse/data/local/entity/MemberEntity.kt',
    step: 1,
    title: 'Entité Room : MemberEntity',
    category: 'entity',
    language: 'kotlin',
    content: `package com.association.caisse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité représentant un membre de l'association.
 * - [joinMonth] : format "YYYY-MM" (ex. "2026-01").
 * - [customMonthlyAmount] : surcharge optionnelle en espèces (Long). Si null, le taux général s'applique.
 * - [isActive] : false si le membre est archivé.
 */
@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lastName: String,
    val firstName: String,
    val phone: String = "",
    val joinMonth: String,
    val isActive: Boolean = true,
    val customMonthlyAmount: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)`,
  },

  {
    path: 'app/src/main/java/com/association/caisse/data/local/entity/MonthlyFeeRateEntity.kt',
    step: 1,
    title: 'Entité Room : MonthlyFeeRateEntity',
    category: 'entity',
    language: 'kotlin',
    content: `package com.association.caisse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Historique des montants de cotisation par défaut.
 * - [effectiveFromMonth] : mois d'effet au format "YYYY-MM" (ex. "2026-01").
 * - [amount] : montant en Long (ex. 10000).
 */
@Entity(tableName = "monthly_fee_rates")
data class MonthlyFeeRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val effectiveFromMonth: String,
    val amount: Long
)`,
  },

  {
    path: 'app/src/main/java/com/association/caisse/data/local/entity/PaymentEntity.kt',
    step: 2,
    title: 'Entités Room : PaymentEntity & PaymentAllocationEntity',
    category: 'entity',
    language: 'kotlin',
    content: `package com.association.caisse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Enregistrement d'un paiement en espèces reçu d'un membre.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["memberId"])]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val date: String, // Format "YYYY-MM-DD"
    val amount: Long, // Montant total reçu en espèces
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Ventilation exacte du montant d'un paiement sur un mois précis.
 * Permet la traçabilité des paiements partiels, multi-mois et avances.
 */
@Entity(
    tableName = "payment_allocations",
    foreignKeys = [
        ForeignKey(
            entity = PaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["paymentId"]),
        Index(value = ["memberId"]),
        Index(value = ["month"])
    ]
)
data class PaymentAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paymentId: Long,
    val memberId: Long,
    val month: String, // Format "YYYY-MM"
    val amount: Long
)`,
  },

  {
    path: 'app/src/main/java/com/association/caisse/data/local/entity/OtherIncomeEntity.kt',
    step: 3,
    title: 'Entités Room : IncomeCategoryEntity & OtherIncomeEntity',
    category: 'entity',
    language: 'kotlin',
    content: `package com.association.caisse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "income_categories")
data class IncomeCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isActive: Boolean = true
)

@Entity(
    tableName = "other_incomes",
    foreignKeys = [
        ForeignKey(
            entity = IncomeCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("memberId")]
)
data class OtherIncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val memberId: Long? = null,
    val date: String, // "YYYY-MM-DD"
    val amount: Long,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)`,
  },

  {
    path: 'app/src/main/java/com/association/caisse/data/local/entity/RemittanceAndReminderEntity.kt',
    step: 5,
    title: 'Entités Room : RemittanceEntity & ReminderLogEntity',
    category: 'entity',
    language: 'kotlin',
    content: `package com.association.caisse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Enregistrement d'un versement d'espèces à la caisse principale de l'association.
 */
@Entity(tableName = "remittances")
data class RemittanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val amount: Long, // Montant versé en espèces
    val recipient: String, // Nom de la personne ou entité qui a reçu les fonds
    val coveredPeriod: String? = null, // ex. "Janvier - Février 2026"
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReminderChannel {
    WHATSAPP,
    SMS,
    CALL
}

/**
 * Historique des relances effectuées auprès d'un membre en retard.
 */
@Entity(
    tableName = "reminder_logs",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("memberId")]
)
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val date: String, // "YYYY-MM-DD HH:mm"
    val channel: ReminderChannel,
    val amountDueAtTime: Long,
    val createdAt: Long = System.currentTimeMillis()
)`,
  },

  // 8. Room DAOs
  {
    path: 'app/src/main/java/com/association/caisse/data/local/dao/MemberDao.kt',
    step: 1,
    title: 'Room DAO : MemberDao',
    category: 'dao',
    language: 'kotlin',
    content: `package com.association.caisse.data.local.dao

import androidx.room.*
import com.association.caisse.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY lastName ASC, firstName ASC")
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE isActive = 1 ORDER BY lastName ASC, firstName ASC")
    fun getActiveMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberById(id: Long): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMember(member: MemberEntity): Long

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Query("UPDATE members SET isActive = :isActive WHERE id = :id")
    suspend fun setMemberActive(id: Long, isActive: Boolean)

    @Delete
    suspend fun deleteMember(member: MemberEntity)

    // Vérification de sécurité : un membre avec paiements ne peut pas être supprimé
    @Query("SELECT COUNT(*) FROM payments WHERE memberId = :memberId")
    suspend fun getPaymentsCountForMember(memberId: Long): Int

    @Query("SELECT COUNT(*) FROM other_incomes WHERE memberId = :memberId")
    suspend fun getOtherIncomesCountForMember(memberId: Long): Int
}`,
  },

  {
    path: 'app/src/main/java/com/association/caisse/data/local/dao/PaymentDao.kt',
    step: 2,
    title: 'Room DAO : PaymentDao',
    category: 'dao',
    language: 'kotlin',
    content: `package com.association.caisse.data.local.dao

import androidx.room.*
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import com.association.caisse.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC, id DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE memberId = :memberId ORDER BY date DESC")
    fun getPaymentsForMember(memberId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payment_allocations")
    fun getAllAllocations(): Flow<List<PaymentAllocationEntity>>

    @Query("SELECT * FROM payment_allocations WHERE memberId = :memberId")
    fun getAllocationsForMember(memberId: Long): Flow<List<PaymentAllocationEntity>>

    @Query("SELECT * FROM payment_allocations WHERE paymentId = :paymentId")
    suspend fun getAllocationsForPayment(paymentId: Long): List<PaymentAllocationEntity>

    @Insert
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Insert
    suspend fun insertAllocations(allocations: List<PaymentAllocationEntity>)

    @Transaction
    suspend fun recordPaymentWithAllocations(
        payment: PaymentEntity,
        allocations: List<PaymentAllocationEntity>
    ): Long {
        val paymentId = insertPayment(payment)
        val linkedAllocations = allocations.map { it.copy(paymentId = paymentId) }
        insertAllocations(linkedAllocations)
        return paymentId
    }

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments")
    fun getTotalPaymentsSum(): Flow<Long>
}`,
  },

  {
    path: 'app/src/main/java/com/association/caisse/data/local/dao/RemittanceAndIncomeDao.kt',
    step: 3,
    title: 'Room DAOs : FeeRateDao, IncomeDao, RemittanceDao, ReminderDao',
    category: 'dao',
    language: 'kotlin',
    content: `package com.association.caisse.data.local.dao

import androidx.room.*
import com.association.caisse.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FeeRateDao {
    @Query("SELECT * FROM monthly_fee_rates ORDER BY effectiveFromMonth DESC")
    fun getAllRates(): Flow<List<MonthlyFeeRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: MonthlyFeeRateEntity): Long
}

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<IncomeCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: IncomeCategoryEntity): Long

    @Update
    suspend fun updateCategory(category: IncomeCategoryEntity)

    @Query("SELECT * FROM other_incomes ORDER BY date DESC, id DESC")
    fun getAllOtherIncomes(): Flow<List<OtherIncomeEntity>>

    @Insert
    suspend fun insertOtherIncome(income: OtherIncomeEntity): Long

    @Delete
    suspend fun deleteOtherIncome(income: OtherIncomeEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM other_incomes")
    fun getTotalOtherIncomeSum(): Flow<Long>
}

@Dao
interface RemittanceDao {
    @Query("SELECT * FROM remittances ORDER BY date DESC, id DESC")
    fun getAllRemittances(): Flow<List<RemittanceEntity>>

    @Insert
    suspend fun insertRemittance(remittance: RemittanceEntity): Long

    @Delete
    suspend fun deleteRemittance(remittance: RemittanceEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM remittances")
    fun getTotalRemittancesSum(): Flow<Long>
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<ReminderLogEntity>>

    @Query("SELECT * FROM reminder_logs WHERE memberId = :memberId ORDER BY date DESC")
    fun getLogsForMember(memberId: Long): Flow<List<ReminderLogEntity>>

    @Insert
    suspend fun insertLog(log: ReminderLogEntity): Long

    @Delete
    suspend fun deleteLog(log: ReminderLogEntity)
}`,
  },

  // 9. Room Database
  {
    path: 'app/src/main/java/com/association/caisse/data/local/AppDatabase.kt',
    step: 1,
    title: 'Base de données Room : AppDatabase',
    category: 'database',
    language: 'kotlin',
    content: `package com.association.caisse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.association.caisse.data.local.dao.*
import com.association.caisse.data.local.entity.*

@Database(
    entities = [
        MemberEntity::class,
        MonthlyFeeRateEntity::class,
        PaymentEntity::class,
        PaymentAllocationEntity::class,
        IncomeCategoryEntity::class,
        OtherIncomeEntity::class,
        RemittanceEntity::class,
        ReminderLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun feeRateDao(): FeeRateDao
    abstract fun paymentDao(): PaymentDao
    abstract fun incomeDao(): IncomeDao
    abstract fun remittanceDao(): RemittanceDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "caisse_association.db"
    }
}`,
  },

  // 10. Fee Allocation Engine (Pure Business Logic)
  {
    path: 'app/src/main/java/com/association/caisse/domain/FeeAllocationEngine.kt',
    step: 2,
    title: 'Moteur métier de répartition des cotisations (FeeAllocationEngine.kt)',
    category: 'repository',
    language: 'kotlin',
    content: `package com.association.caisse.domain

import com.association.caisse.data.local.entity.MemberEntity
import com.association.caisse.data.local.entity.MonthlyFeeRateEntity
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Calculateur pur des cotisations et de la répartition automatique des paiements d'espèces.
 * Conforme aux règles métier :
 * - Montants en Long (pas de décimales)
 * - Affectation prioritaire aux mois impayés/partiels les plus anciens
 * - Surplus affecté aux mois futurs consécutifs (Avances)
 * - Surcharge possible via customMonthlyAmount du membre
 */
class FeeAllocationEngine {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun getExpectedFeeForMonth(
        month: String,
        member: MemberEntity,
        rates: List<MonthlyFeeRateEntity>
    ): Long {
        if (member.customMonthlyAmount != null && member.customMonthlyAmount > 0) {
            return member.customMonthlyAmount
        }
        val applicableRate = rates
            .sortedByDescending { it.effectiveFromMonth }
            .firstOrNull { it.effectiveFromMonth <= month }
            ?: rates.minByOrNull { it.effectiveFromMonth }

        return applicableRate?.amount ?: 10000L
    }

    data class AllocationProposal(
        val month: String,
        val amount: Long,
        val expectedFee: Long,
        val previouslyPaid: Long,
        val isSettled: Boolean
    )

    fun calculateAutoAllocation(
        amountReceived: Long,
        member: MemberEntity,
        existingAllocations: List<PaymentAllocationEntity>,
        rates: List<MonthlyFeeRateEntity>,
        currentMonth: String = YearMonth.now().format(formatter)
    ): List<AllocationProposal> {
        var remainingCash = amountReceived
        val result = mutableListOf<AllocationProposal>()

        val paidByMonth = existingAllocations
            .filter { it.memberId == member.id }
            .groupBy { it.month }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        var cursor = YearMonth.parse(member.joinMonth, formatter)
        val limitMonth = YearMonth.parse(currentMonth, formatter)

        // Tant qu'il reste du cash ou qu'on n'a pas atteint le mois courant
        while (remainingCash > 0 || !cursor.isAfter(limitMonth)) {
            val monthStr = cursor.format(formatter)
            val expected = getExpectedFeeForMonth(monthStr, member, rates)
            val previouslyPaid = paidByMonth[monthStr] ?: 0L
            val needed = (expected - previouslyPaid).coerceAtLeast(0L)

            if (needed > 0 && remainingCash > 0) {
                val allocate = minOf(remainingCash, needed)
                val newPaid = previouslyPaid + allocate
                remainingCash -= allocate

                result.add(
                    AllocationProposal(
                        month = monthStr,
                        amount = allocate,
                        expectedFee = expected,
                        previouslyPaid = previouslyPaid,
                        isSettled = newPaid >= expected
                    )
                )
            }

            cursor = cursor.plusMonths(1)
            // Sécurité anti-boucle infinie (ex. max 120 mois = 10 ans)
            if (result.size >= 120) break
        }

        return result
    }
}`,
  },

  // 11. Phone & Message Formatters
  {
    path: 'app/src/main/java/com/association/caisse/domain/ReminderHelper.kt',
    step: 5,
    title: 'Outils de formatage des relances & téléphone (ReminderHelper.kt)',
    category: 'repository',
    language: 'kotlin',
    content: `package com.association.caisse.domain

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ReminderHelper {

    /**
     * Normalisation du numéro selon la spécification :
     * Nettoyage des espaces, tirets, parenthèses.
     * Application de l'indicatif pays si absent (en supprimant le zéro initial).
     */
    fun normalizePhoneNumber(rawPhone: String, defaultPrefix: String = "+261"): String {
        val cleaned = rawPhone.replace(Regex("[\\s\\-\\(\\)\\.]"), "")
        if (cleaned.isBlank()) return ""
        if (cleaned.startsWith("+")) return cleaned
        if (cleaned.startsWith("00")) return "+" + cleaned.substring(2)

        val prefix = if (defaultPrefix.startsWith("+")) defaultPrefix else "+$defaultPrefix"
        val withoutLeadingZero = if (cleaned.startsWith("0")) cleaned.substring(1) else cleaned
        return "$prefix$withoutLeadingZero"
    }

    /**
     * Substitution des variables dans le modèle de message de relance :
     * {prenom}, {nom}, {montant_du}, {mois_impayes}, {association}, {devise}
     */
    fun buildReminderMessage(
        template: String,
        firstName: String,
        lastName: String,
        amountDueFormatted: String,
        unpaidMonthsFormatted: String,
        associationName: String,
        currency: String
    ): String {
        return template
            .replace("{prenom}", firstName)
            .replace("{nom}", lastName)
            .replace("{montant_du}", amountDueFormatted)
            .replace("{mois_impayes}", unpaidMonthsFormatted)
            .replace("{association}", associationName)
            .replace("{devise}", currency)
    }

    /**
     * Construit l'URL WhatsApp universelle : https://wa.me/<numéro>?text=<message>
     */
    fun buildWhatsAppUrl(normalizedPhone: String, message: String): String {
        val phoneWithoutPlus = normalizedPhone.removePrefix("+")
        val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        return "https://wa.me/$phoneWithoutPlus?text=$encodedMessage"
    }

    /**
     * Vérifie la règle anti-harcèlement (délai minimal entre deux relances)
     */
    fun isTooSoonToRemind(lastReminderTimestamp: Long?, minIntervalDays: Int = 7): Boolean {
        if (lastReminderTimestamp == null) return false
        val now = System.currentTimeMillis()
        val diffDays = (now - lastReminderTimestamp) / (1000 * 60 * 60 * 24)
        return diffDays < minIntervalDays
    }
}`,
  },

  // 12. Unit Tests
  {
    path: 'app/src/test/java/com/association/caisse/FeeAllocationEngineTest.kt',
    step: 7,
    title: 'Tests unitaires : FeeAllocationEngineTest.kt',
    category: 'test',
    language: 'kotlin',
    content: `package com.association.caisse

import com.association.caisse.data.local.entity.MemberEntity
import com.association.caisse.data.local.entity.MonthlyFeeRateEntity
import com.association.caisse.data.local.entity.PaymentAllocationEntity
import com.association.caisse.domain.FeeAllocationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeeAllocationEngineTest {

    private lateinit var engine: FeeAllocationEngine
    private lateinit var sampleMember: MemberEntity
    private lateinit var rates: List<MonthlyFeeRateEntity>

    @Before
    fun setup() {
        engine = FeeAllocationEngine()
        sampleMember = MemberEntity(
            id = 1L,
            lastName = "Ranaivo",
            firstName = "Jean",
            joinMonth = "2026-01"
        )
        rates = listOf(
            MonthlyFeeRateEntity(id = 1L, effectiveFromMonth = "2026-01", amount = 10000L)
        )
    }

    @Test
    fun testPartialPayment() {
        // Le membre doit 10000 par mois. Il paie 6000 en espèces.
        val allocations = engine.calculateAutoAllocation(
            amountReceived = 6000L,
            member = sampleMember,
            existingAllocations = emptyList(),
            rates = rates,
            currentMonth = "2026-01"
        )

        assertEquals(1, allocations.size)
        assertEquals("2026-01", allocations[0].month)
        assertEquals(6000L, allocations[0].amount)
        assertEquals(false, allocations[0].isSettled)
    }

    @Test
    fun testMultiMonthPayment() {
        // Le membre paie 25000 pour 2026-01 et 2026-02 (2x 10000) + acompte 5000 sur 2026-03
        val allocations = engine.calculateAutoAllocation(
            amountReceived = 25000L,
            member = sampleMember,
            existingAllocations = emptyList(),
            rates = rates,
            currentMonth = "2026-02"
        )

        assertEquals(3, allocations.size)
        assertEquals("2026-01", allocations[0].month)
        assertEquals(10000L, allocations[0].amount)
        assertTrue(allocations[0].isSettled)

        assertEquals("2026-02", allocations[1].month)
        assertEquals(10000L, allocations[1].amount)
        assertTrue(allocations[1].isSettled)

        assertEquals("2026-03", allocations[2].month)
        assertEquals(5000L, allocations[2].amount)
        assertEquals(false, allocations[2].isSettled)
    }

    @Test
    fun testAdvancePaymentWhenUpToDate() {
        // Janvier 2026 est déjà payé. On paie 20000 en Février et Mars (avance)
        val existing = listOf(
            PaymentAllocationEntity(id = 1L, paymentId = 1L, memberId = 1L, month = "2026-01", amount = 10000L)
        )
        val allocations = engine.calculateAutoAllocation(
            amountReceived = 20000L,
            member = sampleMember,
            existingAllocations = existing,
            rates = rates,
            currentMonth = "2026-01"
        )

        assertEquals(2, allocations.size)
        assertEquals("2026-02", allocations[0].month)
        assertEquals(10000L, allocations[0].amount)
        assertEquals("2026-03", allocations[1].month)
        assertEquals(10000L, allocations[1].amount)
    }
}`,
  },

  {
    path: 'app/src/test/java/com/association/caisse/ReminderHelperTest.kt',
    step: 7,
    title: 'Tests unitaires : ReminderHelperTest.kt',
    category: 'test',
    language: 'kotlin',
    content: `package com.association.caisse

import com.association.caisse.domain.ReminderHelper
import org.junit.Assert.*
import org.junit.Test

class ReminderHelperTest {

    @Test
    fun testNormalizePhoneNumberWithDefaultPrefix() {
        // Numéro saisi avec espaces et 0 initial
        val normalized = ReminderHelper.normalizePhoneNumber("034 11 222 33", "+261")
        assertEquals("+261341122233", normalized)

        // Numéro déjà international
        val alreadyIntl = ReminderHelper.normalizePhoneNumber("+261 33 44 555 66", "+261")
        assertEquals("+261334455566", alreadyIntl)

        // Numéro avec tirets
        val withDashes = ReminderHelper.normalizePhoneNumber("032-77-888-99", "+261")
        assertEquals("+261327788899", normalizedWithDashes = "+261327788899")
    }

    @Test
    fun testReminderMessageTemplateVariables() {
        val template = "Bonjour {prenom} {nom}, il reste {montant_du} pour {mois_impayes} – {association}."
        val result = ReminderHelper.buildReminderMessage(
            template = template,
            firstName = "Hanta",
            lastName = "Andrianina",
            amountDueFormatted = "20 000 Ar",
            unpaidMonthsFormatted = "Janv. 2026, Févr. 2026",
            associationName = "Solidarité",
            currency = "Ar"
        )

        assertEquals(
            "Bonjour Hanta Andrianina, il reste 20 000 Ar pour Janv. 2026, Févr. 2026 – Solidarité.",
            result
        )
    }

    @Test
    fun testAntiHarassmentDelay() {
        val now = System.currentTimeMillis()
        val threeDaysAgo = now - (3L * 24 * 3600 * 1000)
        val tenDaysAgo = now - (10L * 24 * 3600 * 1000)

        assertTrue("3 jours est inférieur au délai de 7 jours", ReminderHelper.isTooSoonToRemind(threeDaysAgo, 7))
        assertFalse("10 jours est supérieur au délai de 7 jours", ReminderHelper.isTooSoonToRemind(tenDaysAgo, 7))
        assertFalse("Aucune relance antérieure", ReminderHelper.isTooSoonToRemind(null, 7))
    }
}`,
  },

  // 13. French Resources (strings.xml)
  {
    path: 'app/src/main/res/values/strings.xml',
    step: 1,
    title: 'Ressources de chaînes en français (strings.xml)',
    category: 'res',
    language: 'xml',
    content: `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Caisse Association</string>
    <string name="channel_reminders_name">Rappels des cotisations</string>
    <string name="channel_reminders_desc">Notifications de rappel pour les membres ayant des retards de cotisation</string>
    <string name="channel_remittance_name">Rappels de versement à la caisse</string>
    <string name="channel_remittance_desc">Alertes quand le montant des espèces à verser dépasse le seuil configuré</string>

    <!-- Navigation -->
    <string name="nav_dashboard">Accueil</string>
    <string name="nav_members">Membres</string>
    <string name="nav_income">Recettes</string>
    <string name="nav_reminders">Relances</string>
    <string name="nav_more">Plus</string>

    <!-- Membres -->
    <string name="title_members">Gestion des membres</string>
    <string name="action_add_member">Ajouter un membre</string>
    <string name="status_active">Actif</string>
    <string name="status_inactive">Archivé</string>
    <string name="filter_all">Tous</string>
    <string name="filter_active">Actifs</string>
    <string name="filter_late">En retard</string>
    <string name="filter_archived">Archivés</string>

    <!-- Versements -->
    <string name="title_remittances">Versements à la caisse</string>
    <string name="remittance_remaining_to_pay">Reste à verser</string>
    <string name="action_new_remittance">Nouveau versement</string>
</resources>`,
  },
];
