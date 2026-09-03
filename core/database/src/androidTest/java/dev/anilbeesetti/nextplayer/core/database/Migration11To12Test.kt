package dev.anilbeesetti.nextplayer.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration11To12Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MediaDatabase::class.java,
    )

    @Test
    fun migrationAddsEmptyDirectoryPasswordWithoutChangingConnection() {
        helper.createDatabase(TEST_DATABASE, 11).apply {
            execSQL(
                "INSERT INTO network_connection " +
                    "(id,name,protocol,host,port,path,username,password,use_https,authentication," +
                    "private_key_file_name,private_key_passphrase,host_key_fingerprint,created_at) " +
                    "VALUES (1,'Server','WEBDAV','example.com',80,'/','','',0,'PASSWORD','','','',123)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            12,
            true,
            MediaDatabase.MIGRATION_11_12,
        ).use { db ->
            db.query("SELECT directory_password FROM network_connection WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("", cursor.getString(0))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-11-12"
    }
}
