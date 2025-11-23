package com.bisonnotesai.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bisonnotesai.android.data.transcription.aws.AWSCredentials
import com.bisonnotesai.android.data.transcription.aws.AWSTranscribeConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore extension for AWS preferences
 */
private val Context.awsPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aws_preferences"
)

/**
 * Manager for AWS settings persistence
 * Uses DataStore for secure credentials storage
 */
@Singleton
class AWSPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.awsPreferencesDataStore

    companion object {
        private val ACCESS_KEY_ID = stringPreferencesKey("access_key_id")
        private val SECRET_ACCESS_KEY = stringPreferencesKey("secret_access_key")
        private val REGION = stringPreferencesKey("region")
        private val BUCKET_NAME = stringPreferencesKey("bucket_name")
        private val LANGUAGE_CODE = stringPreferencesKey("language_code")

        private const val DEFAULT_REGION = "us-east-1"
        private const val DEFAULT_LANGUAGE_CODE = "en-US"
    }

    /**
     * Get AWS credentials as Flow
     */
    val credentials: Flow<AWSCredentials> = dataStore.data.map { preferences ->
        AWSCredentials(
            accessKeyId = preferences[ACCESS_KEY_ID] ?: "",
            secretAccessKey = preferences[SECRET_ACCESS_KEY] ?: "",
            region = preferences[REGION] ?: DEFAULT_REGION
        )
    }

    /**
     * Get complete AWS Transcribe configuration as Flow
     */
    val config: Flow<AWSTranscribeConfig> = dataStore.data.map { preferences ->
        AWSTranscribeConfig(
            credentials = AWSCredentials(
                accessKeyId = preferences[ACCESS_KEY_ID] ?: "",
                secretAccessKey = preferences[SECRET_ACCESS_KEY] ?: "",
                region = preferences[REGION] ?: DEFAULT_REGION
            ),
            bucketName = preferences[BUCKET_NAME] ?: "",
            languageCode = preferences[LANGUAGE_CODE] ?: DEFAULT_LANGUAGE_CODE
        )
    }

    /**
     * Get access key ID
     */
    val accessKeyId: Flow<String> = dataStore.data.map { preferences ->
        preferences[ACCESS_KEY_ID] ?: ""
    }

    /**
     * Get secret access key
     */
    val secretAccessKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[SECRET_ACCESS_KEY] ?: ""
    }

    /**
     * Get region
     */
    val region: Flow<String> = dataStore.data.map { preferences ->
        preferences[REGION] ?: DEFAULT_REGION
    }

    /**
     * Get bucket name
     */
    val bucketName: Flow<String> = dataStore.data.map { preferences ->
        preferences[BUCKET_NAME] ?: ""
    }

    /**
     * Get language code
     */
    val languageCode: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_CODE] ?: DEFAULT_LANGUAGE_CODE
    }

    /**
     * Save access key ID
     */
    suspend fun saveAccessKeyId(accessKeyId: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_KEY_ID] = accessKeyId
        }
    }

    /**
     * Save secret access key
     */
    suspend fun saveSecretAccessKey(secretAccessKey: String) {
        dataStore.edit { preferences ->
            preferences[SECRET_ACCESS_KEY] = secretAccessKey
        }
    }

    /**
     * Save region
     */
    suspend fun saveRegion(region: String) {
        dataStore.edit { preferences ->
            preferences[REGION] = region
        }
    }

    /**
     * Save bucket name
     */
    suspend fun saveBucketName(bucketName: String) {
        dataStore.edit { preferences ->
            preferences[BUCKET_NAME] = bucketName
        }
    }

    /**
     * Save language code
     */
    suspend fun saveLanguageCode(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_CODE] = languageCode
        }
    }

    /**
     * Save complete credentials
     */
    suspend fun saveCredentials(credentials: AWSCredentials) {
        dataStore.edit { preferences ->
            preferences[ACCESS_KEY_ID] = credentials.accessKeyId
            preferences[SECRET_ACCESS_KEY] = credentials.secretAccessKey
            preferences[REGION] = credentials.region
        }
    }

    /**
     * Save complete configuration
     */
    suspend fun saveConfig(config: AWSTranscribeConfig) {
        dataStore.edit { preferences ->
            preferences[ACCESS_KEY_ID] = config.credentials.accessKeyId
            preferences[SECRET_ACCESS_KEY] = config.credentials.secretAccessKey
            preferences[REGION] = config.credentials.region
            preferences[BUCKET_NAME] = config.bucketName
            preferences[LANGUAGE_CODE] = config.languageCode
        }
    }

    /**
     * Reset to defaults
     */
    suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Check if credentials are configured
     */
    suspend fun isConfigured(): Boolean {
        var configured = false
        dataStore.data.collect { preferences ->
            val hasAccessKey = !preferences[ACCESS_KEY_ID].isNullOrBlank()
            val hasSecretKey = !preferences[SECRET_ACCESS_KEY].isNullOrBlank()
            val hasBucket = !preferences[BUCKET_NAME].isNullOrBlank()
            configured = hasAccessKey && hasSecretKey && hasBucket
        }
        return configured
    }
}
