package de.pdfwerkzeugkasten.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import de.pdfwerkzeugkasten.domain.model.*
import de.pdfwerkzeugkasten.domain.usecase.FileNameGenerator
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.util.PDFBoxResourceLoader
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PdfEngine {
    suspend fun pageCount(uri: Uri, password: String? = null): Int
    suspend fun compress(uri: Uri, originalName: String, level: CompressionLevel): PdfOperationResult
    suspend fun merge(uris: List<Uri>): PdfOperationResult
    suspend fun split(uri: Uri, originalName: String, pages: List<Int>, rangeLabel: String): PdfOperationResult
    suspend fun imagesToPdf(imageUris: List<Uri>, options: ImageToPdfOptions): PdfOperationResult
    suspend fun rotate(uri: Uri, originalName: String, degrees: Int): PdfOperationResult
    suspend fun protect(uri: Uri, originalName: String, password: String, allowPrint: Boolean, allowCopy: Boolean): PdfOperationResult
    suspend fun unlock(uri: Uri, originalName: String, password: String): PdfOperationResult
    suspend fun renderPreview(uri: Uri, pageIndex: Int, width: Int): Bitmap
}

class PdfBoxEngine(private val context: Context, private val names: FileNameGenerator = FileNameGenerator()) : PdfEngine {
    init { PDFBoxResourceLoader.init(context) }
    override suspend fun pageCount(uri: Uri, password: String?): Int = withContext(Dispatchers.IO) { openDocument(uri, password).use { it.numberOfPages } }

    override suspend fun compress(uri: Uri, originalName: String, level: CompressionLevel) = withContext(Dispatchers.IO) {
        val input = cacheInput(uri, "compress_input.pdf"); val output = outputFile(names.compressed(originalName)); val before = input.length()
        PDDocument.load(input).use { doc -> doc.documentInformation.producer = "PDF Werkzeugkasten"; doc.save(output) }
        // PDFBox performs structural cleanup. Stronger image recompression is reserved for Pro roadmap to avoid destructive surprises.
        result(output, before)
    }

    override suspend fun merge(uris: List<Uri>) = withContext(Dispatchers.IO) {
        require(uris.isNotEmpty()) { "Bitte mindestens eine PDF auswählen." }
        val output = outputFile(names.merged()); val merger = PDFMergerUtility(); var inputSize = 0L
        uris.forEachIndexed { index, uri -> val f = cacheInput(uri, "merge_$index.pdf"); inputSize += f.length(); merger.addSource(f) }
        merger.destinationFileName = output.absolutePath; merger.mergeDocuments(null); result(output, inputSize)
    }

    override suspend fun split(uri: Uri, originalName: String, pages: List<Int>, rangeLabel: String) = withContext(Dispatchers.IO) {
        val input = cacheInput(uri, "split_input.pdf"); val output = outputFile(names.split(originalName, rangeLabel)); val before = input.length()
        PDDocument.load(input).use { src ->
            require(src.numberOfPages > 0) { "PDF ohne Seiten." }
            val out = PDDocument(); out.use {
                pages.forEach { page -> require(page in 1..src.numberOfPages) { "Seite außerhalb des Dokuments: $page" }; it.addPage(src.getPage(page - 1)) }
                it.save(output)
            }
        }
        result(output, before)
    }

    override suspend fun imagesToPdf(imageUris: List<Uri>, options: ImageToPdfOptions) = withContext(Dispatchers.IO) {
        val output = outputFile(names.imagesToPdf()); var inputSize = 0L
        PDDocument().use { doc ->
            imageUris.forEachIndexed { index, uri ->
                val imgFile = cacheInput(uri, "image_$index"); inputSize += imgFile.length(); val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath) ?: error("Bild kann nicht gelesen werden.")
                val pageSize = pageSize(options, bitmap); val page = PDPage(pageSize); doc.addPage(page)
                val margin = options.margin.points; val image = JPEGFactory.createFromImage(doc, bitmap, options.quality.jpegQuality / 100f)
                val maxW = pageSize.width - margin * 2; val maxH = pageSize.height - margin * 2; val scale = minOf(maxW / bitmap.width, maxH / bitmap.height)
                val w = bitmap.width * scale; val h = bitmap.height * scale; val x = (pageSize.width - w) / 2; val y = (pageSize.height - h) / 2
                PDPageContentStream(doc, page).use { cs -> cs.drawImage(image, x, y, w, h) }
                bitmap.recycle()
            }
            doc.save(output)
        }
        result(output, inputSize)
    }

    override suspend fun rotate(uri: Uri, originalName: String, degrees: Int) = withContext(Dispatchers.IO) {
        val input = cacheInput(uri, "rotate_input.pdf"); val output = outputFile(names.rotated(originalName)); val before = input.length()
        PDDocument.load(input).use { doc -> doc.pages.forEach { it.rotation = (it.rotation + degrees) % 360 }; doc.save(output) }
        result(output, before)
    }

    override suspend fun protect(uri: Uri, originalName: String, password: String, allowPrint: Boolean, allowCopy: Boolean) = withContext(Dispatchers.IO) {
        require(password.length >= 4) { "Bitte ein Passwort mit mindestens 4 Zeichen wählen." }
        val input = cacheInput(uri, "protect_input.pdf"); val output = outputFile(names.protected(originalName)); val before = input.length()
        PDDocument.load(input).use { doc ->
            val ap = AccessPermission().apply { setCanPrint(allowPrint); setCanExtractContent(allowCopy) }
            doc.protect(StandardProtectionPolicy(password, password, ap).apply { encryptionKeyLength = 128 })
            doc.save(output)
        }
        result(output, before)
    }

    override suspend fun unlock(uri: Uri, originalName: String, password: String) = withContext(Dispatchers.IO) {
        val input = cacheInput(uri, "unlock_input.pdf"); val output = outputFile(names.unlocked(originalName)); val before = input.length()
        openDocument(Uri.fromFile(input), password).use { doc -> doc.isAllSecurityToBeRemoved = true; doc.save(output) }
        result(output, before)
    }

    override suspend fun renderPreview(uri: Uri, pageIndex: Int, width: Int): Bitmap = withContext(Dispatchers.IO) {
        context.contentResolver.openFileDescriptor(uri, "r")!!.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val page = renderer.openPage(pageIndex); page.use {
                    val scale = width.toFloat() / page.width; val bmp = Bitmap.createBitmap(width, (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); bmp
                }
            }
        }
    }

    private fun openDocument(uri: Uri, password: String? = null): PDDocument = context.contentResolver.openInputStream(uri).use { input -> if (password.isNullOrBlank()) PDDocument.load(input) else PDDocument.load(input, password) }
    private fun cacheInput(uri: Uri, name: String): File { val file = File(context.cacheDir, "input_${System.nanoTime()}_$name"); context.contentResolver.openInputStream(uri)!!.use { input -> FileOutputStream(file).use { input.copyTo(it) } }; return file }
    private fun outputFile(name: String): File { val dir = File(context.cacheDir, "outputs").apply { mkdirs() }; return uniqueFile(dir, name) }
    private fun uniqueFile(dir: File, name: String): File { var file = File(dir, name); var i = 1; while (file.exists()) { file = File(dir, name.removeSuffix(".pdf") + "_$i.pdf"); i++ }; return file }
    private fun result(output: File, inputSize: Long) = PdfOperationResult(output.name, Uri.fromFile(output), inputSize, output.length())
    private fun pageSize(options: ImageToPdfOptions, bitmap: Bitmap): PDRectangle = when (options.paperFormat) { PaperFormat.A4 -> if (options.orientation == PageOrientation.LANDSCAPE) PDRectangle.A4.rotate() else PDRectangle.A4; PaperFormat.LETTER -> if (options.orientation == PageOrientation.LANDSCAPE) PDRectangle.LETTER.rotate() else PDRectangle.LETTER; PaperFormat.ORIGINAL -> PDRectangle(bitmap.width.toFloat(), bitmap.height.toFloat()) }
}
