package com.example.car_maintenance.utils

import android.content.Context
import android.os.Environment
import com.example.car_maintenance.data.model.Activity
import com.example.car_maintenance.data.model.ActivityType
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    suspend fun exportToPdf(
        context: Context,
        carName: String,
        activities: List<Activity>,
        totalCost: Double,
        costByType: Map<ActivityType, Double>,
        currency: String,
        startDate: Long?,
        endDate: Long?
    ): File? = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "CarMaintenance"
            )
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfFile = File(exportDir, "report_${carName}_$timestamp.pdf")
            
            val pdfWriter = PdfWriter(pdfFile)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Title
            document.add(
                Paragraph("Car Maintenance Report")
                    .setFontSize(20f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
            )
            
            document.add(Paragraph("Car: $carName").setFontSize(14f))
            
            if (startDate != null && endDate != null) {
                document.add(
                    Paragraph(
                        "Period: ${dateOnlyFormat.format(Date(startDate))} to ${dateOnlyFormat.format(Date(endDate))}"
                    ).setFontSize(12f)
                )
            }
            
            document.add(Paragraph("\n"))
            
            // Summary
            document.add(Paragraph("Summary").setFontSize(16f).setBold())
            document.add(Paragraph("Total Activities: ${activities.size}"))
            document.add(Paragraph("Total Cost: $currency ${"%.2f".format(totalCost)}"))
            document.add(Paragraph("\n"))
            
            // Cost by type
            document.add(Paragraph("Cost by Activity Type").setFontSize(16f).setBold())
            costByType.forEach { (type, cost) ->
                document.add(
                    Paragraph("${type.getDisplayName()}: $currency ${"%.2f".format(cost)}")
                )
            }
            document.add(Paragraph("\n"))
            
            // Activities table
            document.add(Paragraph("Activity Details").setFontSize(16f).setBold())
            
            val table = Table(floatArrayOf(2f, 2f, 1.5f, 1.5f, 3f))
            table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100f))
            
            // Header
            table.addHeaderCell("Date")
                        table.addHeaderCell("Type")
            table.addHeaderCell("Mileage")
            table.addHeaderCell("Cost")
            table.addHeaderCell("Notes")
            
            // Data rows
            activities.forEach { activity ->
                table.addCell(dateFormat.format(Date(activity.date)))
                table.addCell(activity.type.getDisplayName())
                table.addCell("%.1f".format(activity.mileage))
                table.addCell("$currency ${"%.2f".format(activity.cost)}")
                table.addCell(activity.notes.take(50))
            }
            
            document.add(table)
            
            // Footer
            document.add(Paragraph("\n\n"))
            document.add(
                Paragraph("Generated on ${dateFormat.format(Date())}")
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            
            document.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun exportToExcel(
        context: Context,
        carName: String,
        activities: List<Activity>,
        totalCost: Double,
        costByType: Map<ActivityType, Double>,
        currency: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "CarMaintenance"
            )
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val excelFile = File(exportDir, "report_${carName}_$timestamp.xlsx")
            
            val workbook = XSSFWorkbook()
            
            // Summary sheet
            val summarySheet = workbook.createSheet("Summary")
            var rowNum = 0
            
            summarySheet.createRow(rowNum++).apply {
                createCell(0).setCellValue("Car Maintenance Report")
            }
            summarySheet.createRow(rowNum++).apply {
                createCell(0).setCellValue("Car: $carName")
            }
            rowNum++
            
            summarySheet.createRow(rowNum++).apply {
                createCell(0).setCellValue("Total Activities:")
                createCell(1).setCellValue(activities.size.toDouble())
            }
            summarySheet.createRow(rowNum++).apply {
                createCell(0).setCellValue("Total Cost:")
                createCell(1).setCellValue("$currency ${"%.2f".format(totalCost)}")
            }
            rowNum++
            
            summarySheet.createRow(rowNum++).apply {
                createCell(0).setCellValue("Cost by Type")
            }
            costByType.forEach { (type, cost) ->
                summarySheet.createRow(rowNum++).apply {
                    createCell(0).setCellValue(type.getDisplayName())
                    createCell(1).setCellValue("$currency ${"%.2f".format(cost)}")
                }
            }
            
            // Activities sheet
            val activitiesSheet = workbook.createSheet("Activities")
            rowNum = 0
            
            // Header
            activitiesSheet.createRow(rowNum++).apply {
                createCell(0).setCellValue("Date")
                createCell(1).setCellValue("Type")
                createCell(2).setCellValue("Mileage")
                createCell(3).setCellValue("Cost ($currency)")
                createCell(4).setCellValue("Notes")
            }
            
            // Data
            activities.forEach { activity ->
                activitiesSheet.createRow(rowNum++).apply {
                    createCell(0).setCellValue(dateFormat.format(Date(activity.date)))
                    createCell(1).setCellValue(activity.type.getDisplayName())
                    createCell(2).setCellValue(activity.mileage)
                    createCell(3).setCellValue(activity.cost)
                    createCell(4).setCellValue(activity.notes)
                }
            }
            
            // Auto-size columns
            for (i in 0..4) {
                summarySheet.autoSizeColumn(i)
                activitiesSheet.autoSizeColumn(i)
            }
            
            FileOutputStream(excelFile).use { fileOut ->
                workbook.write(fileOut)
            }
            workbook.close()
            
            excelFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun exportToCsv(
        context: Context,
        carName: String,
        activities: List<Activity>,
        currency: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "CarMaintenance"
            )
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val csvFile = File(exportDir, "report_${carName}_$timestamp.csv")
            
            csvFile.bufferedWriter().use { writer ->
                // Header
                writer.write("Date,Type,Mileage,Cost ($currency),Notes\n")
                
                // Data
                activities.forEach { activity ->
                    val line = "${dateFormat.format(Date(activity.date))}," +
                            "${activity.type.getDisplayName()}," +
                            "${activity.mileage}," +
                            "${activity.cost}," +
                            "\"${activity.notes.replace("\"", "\"\"")}\"\n"
                    writer.write(line)
                }
            }
            
            csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}