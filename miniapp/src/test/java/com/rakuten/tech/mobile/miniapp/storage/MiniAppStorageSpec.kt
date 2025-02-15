package com.rakuten.tech.mobile.miniapp.storage

import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import com.rakuten.tech.mobile.miniapp.*
import com.rakuten.tech.mobile.miniapp.TEST_BASE_PATH
import com.rakuten.tech.mobile.miniapp.TEST_ID_MINIAPP
import com.rakuten.tech.mobile.miniapp.TEST_ID_MINIAPP_VERSION
import com.rakuten.tech.mobile.miniapp.TEST_URL_FILE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MiniAppStorageSpec {
    private val fileWriter: FileWriter = mock()
    private val miniAppStorage: MiniAppStorage = MiniAppStorage(fileWriter, mock(), mock())
    private val zipFile = "test.zip"

    @Rule @JvmField
    val tempFolder = TemporaryFolder()

    @After
    fun onFinish() {
        tempFolder.delete()
    }

    @Test
    fun `for a given set of base path & file path, formed parent path is returned`() {
        assertTrue { miniAppStorage.getAbsoluteWritePath("a", "c") == "a/c" }
    }

    @Test
    fun `for a given url file name is returned via LocalUrlParser`() {
        val localUrlParser = getMockedLocalUrlParser()
        val miniAppStorage = MiniAppStorage(mock(), mock(), localUrlParser)
        miniAppStorage.getFileName(TEST_URL_FILE)
        verify(localUrlParser).getFileName(TEST_URL_FILE)
    }

    @Test
    fun `should get consistent path when get path for mini app version`() {
        val storage = MiniAppStorage(FileWriter(), File(TEST_BASE_PATH))

        storage.getMiniAppVersionPath(TEST_ID_MINIAPP, TEST_ID_MINIAPP_VERSION) shouldBeEqualTo
                "$TEST_BASE_PATH/miniapp/$TEST_ID_MINIAPP/$TEST_ID_MINIAPP_VERSION"
    }

    @Test
    fun `should delete all file data excluding the latest version package`() = runBlockingTest {
        val oldFile1 = tempFolder.newFolder("old_package_id_1")
        val oldFile2 = tempFolder.newFile()
        val latestPackage = tempFolder.newFolder(TEST_ID_MINIAPP_VERSION)

        miniAppStorage.removeVersions(
            TEST_ID_MINIAPP,
            TEST_ID_MINIAPP_VERSION,
            tempFolder.root.path)

        oldFile1.exists() shouldBe false
        oldFile2.exists() shouldBe false
        latestPackage.exists() shouldBe true
    }

    @Test
    fun `should delete all file data for the specified app id`() = runBlockingTest {
        val oldFile1 = tempFolder.newFolder("old_package_id_1")
        val oldFile2 = tempFolder.newFile()
        val latestPackage = tempFolder.newFolder(TEST_ID_MINIAPP_VERSION)

        miniAppStorage.removeApp(
            TEST_ID_MINIAPP,
            tempFolder.root.path)

        oldFile1.exists() shouldBe false
        oldFile2.exists() shouldBe false
        latestPackage.exists() shouldBe false
    }

    @Test
    fun `should extract file with FileWriter`() = runBlockingTest {
        val file = tempFolder.newFile()
        When calling miniAppStorage.getFileName(file.path) itReturns file.name
        val inputStream: InputStream = mock()
        miniAppStorage.saveFile(file.path, file.path, inputStream)

        verify(fileWriter)
            .unzip(inputStream, miniAppStorage.getAbsoluteWritePath(
                file.path, miniAppStorage.getFileName(file.path)))
    }

    @Test
    fun `should unzip file without exception`() = runBlockingTest {
        val file = tempFolder.newFile()
        val filePath = file.path
        val folder = tempFolder.newFolder()
        val folderPath = folder.path
        val containerPath = file.parent

        val fileWriter = FileWriter(TestCoroutineDispatcher())
        zipFiles(containerPath, arrayOf(filePath, folderPath))
        val inputStream = File("$containerPath${File.separator}$zipFile").inputStream()

        fileWriter.unzip(inputStream, containerPath)
    }

    @Test(expected = MiniAppSdkException::class)
    fun `should throw exception when file path is invalid`() = runBlockingTest {
        val file = File("")
        miniAppStorage.saveFile(file.path, file.path, mock())
    }

    @Test
    fun `deleteDirectory should delete file recursively`() {
        val file = mock(File::class)
        miniAppStorage.deleteDirectory(file)
        verify(file).deleteRecursively()
    }

    private fun getMockedLocalUrlParser() = mock<UrlToFileInfoParser>()

    @Suppress("NestedBlockDepth", "LongMethod")
    private fun zipFiles(outputPath: String, filePaths: Array<String>) {
        ZipOutputStream(BufferedOutputStream(
            FileOutputStream("$outputPath${File.separator}$zipFile"))).use { out ->
            for (filePath in filePaths) {
                val file = File(filePath)
                if (file.isDirectory) {
                    out.putNextEntry(ZipEntry(file.name + File.separator))
                } else {
                    FileInputStream(filePath).use { fi ->
                        BufferedInputStream(fi).use { origin ->
                            val entry = ZipEntry(filePath.substring(filePath.lastIndexOf(File.separator)))
                            out.putNextEntry(entry)
                            origin.copyTo(out, 1024)
                        }
                    }
                }
            }
        }
    }
}
