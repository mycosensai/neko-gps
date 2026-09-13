package com.nekogps.app.features.bookmarks

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nekogps.app.features.gamification.GamificationDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression guard for Room code generation.
 *
 * The build previously used `annotationProcessor` instead of `kapt`, so no
 * `*_Impl` classes were generated and *every* database access threw
 * `IllegalStateException: Cannot find implementation for ... AppDatabase_Impl`
 * at runtime. Building the database here fails loudly if codegen regresses.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DatabaseCodegenTest {

    private lateinit var context: Context
    private var appDb: AppDatabase? = null
    private var gamificationDb: GamificationDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        appDb?.close()
        gamificationDb?.close()
    }

    @Test
    fun `AppDatabase opens with generated implementation`() {
        appDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val db = appDb!!
        assertNotNull("bookmarkDao missing", db.bookmarkDao())
        assertNotNull("parkingDao missing", db.parkingDao())
        assertNotNull("waypointDao missing", db.waypointDao())
        assertNotNull("odometerDao missing", db.odometerDao())
        assertNotNull("achievementDao missing", db.achievementDao())
        assertNotNull("leaderboardDao missing", db.leaderboardDao())
        assertNotNull("locationChallengeDao missing", db.locationChallengeDao())
        assertNotNull("photoWaypointDao missing", db.photoWaypointDao())
    }

    @Test
    fun `GamificationDatabase opens with generated implementation`() {
        gamificationDb = Room.inMemoryDatabaseBuilder(context, GamificationDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        assertNotNull(gamificationDb)
    }

    @Test
    fun `generated implementation classes are loadable`() {
        // Direct check that kapt produced the Room artefacts.
        val impls = listOf(
            "com.nekogps.app.features.bookmarks.AppDatabase_Impl",
            "com.nekogps.app.features.bookmarks.BookmarkDao_Impl",
            "com.nekogps.app.features.findmycar.ParkingDao_Impl",
            "com.nekogps.app.features.waypoints.WaypointDao_Impl",
            "com.nekogps.app.features.stats.OdometerDao_Impl"
        )
        for (name in impls) {
            val loaded = runCatching { Class.forName(name) }
            assertTrue("Room generated class missing: $name", loaded.isSuccess)
        }
    }

    @Test
    fun `bookmark round trip persists through the generated dao`() = runBlocking {
        appDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = appDb!!.bookmarkDao()

        val entity = BookmarkEntity(
            name = "Home",
            latitude = 51.5074,
            longitude = -0.1278
        )
        val id = dao.insert(entity)
        assertTrue("insert returned no row id", id > 0)
        assertEquals(1, dao.getCount())

        val loaded = dao.getBookmarkById(id)
        assertNotNull("bookmark was not persisted", loaded)
        assertEquals("Home", loaded!!.name)
        assertEquals(51.5074, loaded.latitude, 0.00001)
    }
}
