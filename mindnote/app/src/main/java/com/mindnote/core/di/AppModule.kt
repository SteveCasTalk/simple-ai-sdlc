package com.mindnote.core.di

import android.net.Uri
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log
import java.io.File
import com.mindnote.BuildConfig
import com.mindnote.core.auth.AuthEvents
import com.mindnote.core.storage.UserPrefs
import com.mindnote.data.db.MindNoteDatabase
import com.mindnote.data.remote.AuthApi
import com.mindnote.data.remote.AuthRevoked
import com.mindnote.data.remote.BearerAuth
import com.mindnote.data.remote.ChatApi
import com.mindnote.data.remote.NotesApi
import com.mindnote.data.remote.OcrApi
import kotlinx.coroutines.flow.first
import com.mindnote.data.repository.LocalUserRepository
import com.mindnote.data.repository.RoomFavoritesRepository
import com.mindnote.data.repository.RoomNotesRepository
import com.mindnote.domain.repository.FavoritesRepository
import com.mindnote.domain.repository.NotesRepository
import com.mindnote.domain.repository.UserRepository
import com.mindnote.features.capture.CaptureViewModel
import com.mindnote.features.chat.ChatViewModel
import com.mindnote.features.home.HomeViewModel
import com.mindnote.features.notedetail.NoteDetailViewModel
import com.mindnote.features.notes.NotesViewModel
import com.mindnote.features.onboarding.OnboardingViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { UserPrefs(androidContext()) }
    single { AuthEvents() }

    single {
        Room.databaseBuilder(androidContext(), MindNoteDatabase::class.java, MindNoteDatabase.DB_NAME)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL(
                        "INSERT OR IGNORE INTO users (id, username) VALUES (?, ?)",
                        arrayOf(MindNoteDatabase.LOCAL_USER_ID, ""),
                    )
                }
            })
            .addMigrations(MindNoteDatabase.MIGRATION_1_2)
            .build()
    }
    single { get<MindNoteDatabase>().userDao() }
    single { get<MindNoteDatabase>().noteDao() }
    single { get<MindNoteDatabase>().topicDao() }
    single { get<MindNoteDatabase>().favoriteDao() }

    single {
        val userPrefs = get<UserPrefs>()
        val authEvents = get<AuthEvents>()
        val deviceId = userPrefs.deviceIdBlocking()
        Log.i("MindNote", "Device ID: $deviceId")
        HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 30_000
            }
            install(Logging) {
                level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
            }
            install(SSE)
            install(BearerAuth) {
                tokenSource = { userPrefs.authTokenFlow.first() }
            }
            install(AuthRevoked) {
                this.userPrefs = userPrefs
                this.authEvents = authEvents
            }
            defaultRequest {
                url(BuildConfig.BASE_URL)
                contentType(ContentType.Application.Json)
                header("X-Device-Id", deviceId)
            }
        }
    }
    single { NotesApi(get()) }
    single { ChatApi(get()) }
    single { OcrApi(get()) }
    single { AuthApi(get()) }

    single<NotesRepository> { RoomNotesRepository(get(), get(), get(), get()) }
    single<FavoritesRepository> { RoomFavoritesRepository(get(), get()) }
    single<UserRepository> { LocalUserRepository(get(), get()) }

    viewModel { OnboardingViewModel(get()) }
    viewModel {
        val chatApi: ChatApi = get()
        HomeViewModel(
            userRepository = get(),
            notesRepository = get(),
            latestConversationId = { chatApi.latestConversation() },
        )
    }
    viewModel { NotesViewModel(get(), get()) }
    viewModel { CaptureViewModel(get()) }
    viewModel {
        val ctx = androidContext()
        val deviceId = get<UserPrefs>().deviceIdBlocking()
        val ocrApi: com.mindnote.data.remote.OcrApi = get()
        com.mindnote.features.scan.ScanViewModel(
            ocrCall = { d, b, c -> ocrApi.ocr(d, b, c) },
            notesRepository = get(),
            deviceId = deviceId,
            readBytes = { uriString ->
                val uri = Uri.parse(uriString)
                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Could not open image stream")
            },
            contentTypeOf = { uriString ->
                val uri = Uri.parse(uriString)
                ctx.contentResolver.getType(uri) ?: "image/jpeg"
            },
            saveImage = { noteId, bytes ->
                val dir = File(ctx.filesDir, "ocr").apply { mkdirs() }
                val file = File(dir, "$noteId.jpg")
                file.writeBytes(bytes)
                file.absolutePath
            },
        )
    }
    viewModel { (conversationId: String) -> ChatViewModel(conversationId, get()) }
    viewModel { (noteId: String) -> NoteDetailViewModel(noteId, get(), get()) }
}
