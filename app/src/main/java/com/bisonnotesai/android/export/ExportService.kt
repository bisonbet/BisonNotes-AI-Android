package com.bisonnotesai.android.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.bisonnotesai.android.domain.model.Summary
import com.bisonnotesai.android.domain.model.Transcript
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.property.TextAlignment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Export format types
 */
enum class ExportFormat {
    PDF,
    RTF,
    MARKDOWN,
    TXT
}

/**
 * Export service for summaries and transcripts
 * Phase 6: Export & Sharing Implementation
 */
@Singleton
class ExportService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Export summary to file
     */
    suspend fun exportSummary(
        summary: Summary,
        format: ExportFormat = ExportFormat.PDF
    ): File {
        val fileName = generateFileName(summary.bestTitle(), format)
        val exportDir = getExportDirectory()
        val file = File(exportDir, fileName)

        when (format) {
            ExportFormat.PDF -> exportSummaryToPdf(summary, file)
            ExportFormat.RTF -> exportSummaryToRtf(summary, file)
            ExportFormat.MARKDOWN -> exportSummaryToMarkdown(summary, file)
            ExportFormat.TXT -> exportSummaryToText(summary, file)
        }

        return file
    }

    /**
     * Export transcript to file
     */
    suspend fun exportTranscript(
        transcript: Transcript,
        format: ExportFormat = ExportFormat.TXT
    ): File {
        val fileName = generateFileName("Transcript_${transcript.id}", format)
        val exportDir = getExportDirectory()
        val file = File(exportDir, fileName)

        when (format) {
            ExportFormat.PDF -> exportTranscriptToPdf(transcript, file)
            ExportFormat.TXT -> exportTranscriptToText(transcript, file)
            else -> exportTranscriptToText(transcript, file) // Default to text
        }

        return file
    }

    /**
     * Share file using Android share intent
     */
    fun shareFile(file: File, mimeType: String = "application/pdf"): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // MARK: - PDF Export

    private fun exportSummaryToPdf(summary: Summary, file: File) {
        val pdfWriter = PdfWriter(file)
        val pdfDoc = PdfDocument(pdfWriter)
        val document = Document(pdfDoc)

        // Title
        document.add(
            Paragraph(summary.bestTitle())
                .setFontSize(20f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
        )

        // Metadata
        document.add(
            Paragraph("Generated: ${formatDateTime(summary.generatedAt)}")
                .setFontSize(10f)
                .setItalic()
        )
        document.add(
            Paragraph("AI Engine: ${summary.aiEngine.toDisplayString()}")
                .setFontSize(10f)
                .setItalic()
        )

        document.add(Paragraph("\n"))

        // Summary content (simplified markdown to PDF conversion)
        val cleanText = summary.text
            .replace("**", "")
            .replace("##", "")
            .replace("*", "")

        document.add(Paragraph(cleanText))

        // Tasks
        if (summary.tasks.isNotEmpty()) {
            document.add(Paragraph("\n"))
            document.add(Paragraph("Tasks").setFontSize(16f).setBold())
            summary.tasks.forEach { task ->
                document.add(Paragraph("• ${task.text}"))
            }
        }

        // Reminders
        if (summary.reminders.isNotEmpty()) {
            document.add(Paragraph("\n"))
            document.add(Paragraph("Reminders").setFontSize(16f).setBold())
            summary.reminders.forEach { reminder ->
                document.add(Paragraph("• ${reminder.text}"))
            }
        }

        document.close()
    }

    private fun exportTranscriptToPdf(transcript: Transcript, file: File) {
        val pdfWriter = PdfWriter(file)
        val pdfDoc = PdfDocument(pdfWriter)
        val document = Document(pdfDoc)

        document.add(
            Paragraph("Transcript")
                .setFontSize(20f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
        )

        document.add(
            Paragraph("Transcribed: ${formatDateTime(transcript.transcribedAt)}")
                .setFontSize(10f)
                .setItalic()
        )

        document.add(Paragraph("\n"))
        document.add(Paragraph(transcript.text))

        document.close()
    }

    // MARK: - RTF Export

    private fun exportSummaryToRtf(summary: Summary, file: File) {
        val rtf = buildString {
            appendLine("{\\rtf1\\ansi\\deff0")
            appendLine("{\\fonttbl{\\f0 Times New Roman;}}")
            appendLine("{\\colortbl;\\red0\\green0\\blue0;}")

            // Title
            appendLine("\\qc\\b\\fs32 ${escapeRtf(summary.bestTitle())}\\b0\\fs24")
            appendLine("\\par")

            // Metadata
            appendLine("\\qc\\i Generated: ${formatDateTime(summary.generatedAt)}\\i0")
            appendLine("\\par\\par")

            // Summary
            appendLine("\\ql ${escapeRtf(summary.text)}")
            appendLine("\\par\\par")

            // Tasks
            if (summary.tasks.isNotEmpty()) {
                appendLine("\\b Tasks\\b0")
                appendLine("\\par")
                summary.tasks.forEach { task ->
                    appendLine("\\bullet ${escapeRtf(task.text)}")
                    appendLine("\\par")
                }
                appendLine("\\par")
            }

            // Reminders
            if (summary.reminders.isNotEmpty()) {
                appendLine("\\b Reminders\\b0")
                appendLine("\\par")
                summary.reminders.forEach { reminder ->
                    appendLine("\\bullet ${escapeRtf(reminder.text)}")
                    appendLine("\\par")
                }
            }

            appendLine("}")
        }

        file.writeText(rtf)
    }

    private fun escapeRtf(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("\n", "\\par ")
    }

    // MARK: - Markdown Export

    private fun exportSummaryToMarkdown(summary: Summary, file: File) {
        val markdown = buildString {
            appendLine("# ${summary.bestTitle()}")
            appendLine()
            appendLine("**Generated:** ${formatDateTime(summary.generatedAt)}")
            appendLine("**AI Engine:** ${summary.aiEngine.toDisplayString()}")
            appendLine("**Content Type:** ${summary.contentType.toDisplayString()}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(summary.text)
            appendLine()

            if (summary.tasks.isNotEmpty()) {
                appendLine("## Tasks")
                appendLine()
                summary.tasks.forEach { task ->
                    appendLine("- [ ] ${task.text}")
                    if (task.priority.name != "MEDIUM") {
                        appendLine("  - Priority: ${task.priority.name}")
                    }
                }
                appendLine()
            }

            if (summary.reminders.isNotEmpty()) {
                appendLine("## Reminders")
                appendLine()
                summary.reminders.forEach { reminder ->
                    appendLine("- ${reminder.text}")
                    reminder.date?.let { date ->
                        appendLine("  - Date: ${formatDateTime(date)}")
                    }
                }
            }
        }

        file.writeText(markdown)
    }

    // MARK: - Text Export

    private fun exportSummaryToText(summary: Summary, file: File) {
        val text = buildString {
            appendLine(summary.bestTitle())
            appendLine("=".repeat(summary.bestTitle().length))
            appendLine()
            appendLine("Generated: ${formatDateTime(summary.generatedAt)}")
            appendLine("AI Engine: ${summary.aiEngine.toDisplayString()}")
            appendLine()
            appendLine(summary.text.replace("**", "").replace("##", "").replace("*", ""))
            appendLine()

            if (summary.tasks.isNotEmpty()) {
                appendLine("TASKS:")
                appendLine("-".repeat(50))
                summary.tasks.forEach { task ->
                    appendLine("• ${task.text}")
                }
                appendLine()
            }

            if (summary.reminders.isNotEmpty()) {
                appendLine("REMINDERS:")
                appendLine("-".repeat(50))
                summary.reminders.forEach { reminder ->
                    appendLine("• ${reminder.text}")
                }
            }
        }

        file.writeText(text)
    }

    private fun exportTranscriptToText(transcript: Transcript, file: File) {
        file.writeText(transcript.text)
    }

    // MARK: - Helper Methods

    private fun getExportDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), "exports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun generateFileName(baseName: String, format: ExportFormat): String {
        val cleanName = baseName.replace(Regex("[^a-zA-Z0-9-_]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = when (format) {
            ExportFormat.PDF -> "pdf"
            ExportFormat.RTF -> "rtf"
            ExportFormat.MARKDOWN -> "md"
            ExportFormat.TXT -> "txt"
        }
        return "${cleanName}_${timestamp}.$extension"
    }

    private fun formatDateTime(date: Date): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
}
