package com.chessconnect.service;

import com.chessconnect.model.Invoice;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.InvoiceType;
import com.chessconnect.repository.InvoiceRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    // Platform info for commission invoices
    private static final String PLATFORM_NAME = "mychess";
    private static final String PLATFORM_ADDRESS = "Paris, France";
    private static final String PLATFORM_SIRET = ""; // To be configured
    private static final String PLATFORM_EMAIL = "contact@mychess.fr";

    // Commission rates
    private static final double STANDARD_COMMISSION_RATE = 12.5; // 12.5%
    private static final double PROMO_COMMISSION_RATE = 2.5;     // 2.5% with CHESS2026

    @Value("${app.invoices.storage-path:/var/invoices}")
    private String invoiceStoragePath;

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            UserRepository userRepository,
            LessonRepository lessonRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
    }

    /**
     * Generate both invoices for a completed payment.
     * Called by the Stripe webhook handler.
     */
    @Transactional
    public List<Invoice> generateInvoicesForPayment(
            String paymentIntentId,
            Long studentId,
            Long teacherId,
            Long lessonId,
            int totalAmountCents,
            boolean promoApplied
    ) {
        // Check if invoices already exist for this payment
        if (invoiceRepository.existsByStripePaymentIntentId(paymentIntentId)) {
            log.warn("Invoices already exist for payment intent: {}", paymentIntentId);
            return invoiceRepository.findByStripePaymentIntentId(paymentIntentId);
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found: " + teacherId));
        Lesson lesson = lessonId != null ? lessonRepository.findById(lessonId).orElse(null) : null;

        // Calculate commission
        double commissionRate = promoApplied ? PROMO_COMMISSION_RATE : STANDARD_COMMISSION_RATE;
        int commissionCents = (int) Math.round(totalAmountCents * commissionRate / 100);
        int teacherEarningsCents = totalAmountCents - commissionCents;

        // Generate Invoice 1: Teacher -> Student (lesson service)
        Invoice lessonInvoice = createLessonInvoice(
                student, teacher, lesson, paymentIntentId, totalAmountCents
        );

        // Generate Invoice 2: Platform -> Teacher (commission fees)
        Invoice commissionInvoice = createCommissionInvoice(
                teacher, lesson, paymentIntentId, commissionCents, commissionRate, promoApplied
        );

        // Generate PDFs
        try {
            generateLessonInvoicePdf(lessonInvoice, student, teacher, lesson);
            generateCommissionInvoicePdf(commissionInvoice, teacher, lesson);
        } catch (Exception e) {
            log.error("Error generating PDF invoices", e);
        }

        log.info("Generated invoices for payment {}: lesson invoice #{}, commission invoice #{}",
                paymentIntentId, lessonInvoice.getInvoiceNumber(), commissionInvoice.getInvoiceNumber());

        return List.of(lessonInvoice, commissionInvoice);
    }

    /**
     * Create the lesson invoice (Teacher -> Student).
     */
    private Invoice createLessonInvoice(
            User student,
            User teacher,
            Lesson lesson,
            String paymentIntentId,
            int totalAmountCents
    ) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber("FAC"));
        invoice.setInvoiceType(InvoiceType.LESSON_INVOICE);
        invoice.setCustomer(student);
        invoice.setIssuer(teacher);
        invoice.setLesson(lesson);
        invoice.setStripePaymentIntentId(paymentIntentId);
        invoice.setSubtotalCents(totalAmountCents);
        invoice.setVatCents(0); // Teachers usually not VAT registered for small amounts
        invoice.setTotalCents(totalAmountCents);
        invoice.setVatRate(0);
        invoice.setDescription("Cours d'echecs - 1 heure");
        invoice.setStatus("PAID");
        invoice.setIssuedAt(LocalDateTime.now());

        return invoiceRepository.save(invoice);
    }

    /**
     * Create the commission invoice (Platform -> Teacher).
     */
    private Invoice createCommissionInvoice(
            User teacher,
            Lesson lesson,
            String paymentIntentId,
            int commissionCents,
            double commissionRate,
            boolean promoApplied
    ) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber("COM"));
        invoice.setInvoiceType(InvoiceType.COMMISSION_INVOICE);
        invoice.setCustomer(teacher); // Teacher receives this invoice
        invoice.setIssuer(null); // Platform is the issuer
        invoice.setLesson(lesson);
        invoice.setStripePaymentIntentId(paymentIntentId);
        invoice.setSubtotalCents(commissionCents);
        invoice.setVatCents(0);
        invoice.setTotalCents(commissionCents);
        invoice.setVatRate(0);
        invoice.setCommissionRate(commissionRate);
        invoice.setPromoApplied(promoApplied);

        String description = promoApplied
                ? "Frais techniques de mise en relation (2,5%)"
                : "Frais de mise en relation et frais de service (12,5%)";
        invoice.setDescription(description);
        invoice.setStatus("PAID");
        invoice.setIssuedAt(LocalDateTime.now());

        return invoiceRepository.save(invoice);
    }

    /**
     * Generate a unique sequential invoice number.
     */
    private String generateInvoiceNumber(String prefix) {
        Long maxId = invoiceRepository.findMaxId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        int year = LocalDateTime.now().getYear();
        return String.format("%s-%d-%06d", prefix, year, nextId);
    }

    /**
     * Generate PDF for lesson invoice.
     */
    private void generateLessonInvoicePdf(Invoice invoice, User student, User teacher, Lesson lesson) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content
            addInvoiceHeader(document, "FACTURE", invoice.getInvoiceNumber(), invoice.getIssuedAt());
            addIssuerInfo(document, teacher);
            addCustomerInfo(document, student, "Client");
            addLessonInvoiceTable(document, invoice, lesson);
            addPaymentInfo(document, "Paye par carte bancaire via Stripe");
            addFooter(document);

            document.close();

            // Save PDF
            String pdfPath = savePdf(baos.toByteArray(), invoice.getInvoiceNumber());
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);

        } catch (DocumentException e) {
            log.error("Error creating lesson invoice PDF", e);
            throw new IOException("Failed to generate PDF", e);
        }
    }

    /**
     * Generate PDF for commission invoice.
     */
    private void generateCommissionInvoicePdf(Invoice invoice, User teacher, Lesson lesson) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content
            addInvoiceHeader(document, "FACTURE DE COMMISSION", invoice.getInvoiceNumber(), invoice.getIssuedAt());
            addPlatformIssuerInfo(document);
            addCustomerInfo(document, teacher, "Prestataire");
            addCommissionInvoiceTable(document, invoice, lesson);
            addPaymentInfo(document, "Preleve automatiquement sur le paiement du cours");
            addFooter(document);

            document.close();

            // Save PDF
            String pdfPath = savePdf(baos.toByteArray(), invoice.getInvoiceNumber());
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);

        } catch (DocumentException e) {
            log.error("Error creating commission invoice PDF", e);
            throw new IOException("Failed to generate PDF", e);
        }
    }

    /**
     * Add invoice header with logo and title.
     */
    private void addInvoiceHeader(Document document, String title, String invoiceNumber, LocalDateTime date) throws DocumentException {
        // Title
        Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, new Color(51, 51, 51));
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        document.add(titlePara);

        document.add(new Paragraph(" "));

        // Invoice number and date
        Font infoFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(102, 102, 102));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Phrase("Facture N° : " + invoiceNumber, infoFont));
        infoTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Phrase datePhrase = new Phrase("Date : " + date.format(formatter), infoFont);
        rightCell.addElement(datePhrase);
        infoTable.addCell(rightCell);

        document.add(infoTable);
        document.add(new Paragraph(" "));
    }

    /**
     * Add issuer (teacher) information.
     */
    private void addIssuerInfo(Document document, User teacher) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(212, 168, 75));
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        document.add(new Paragraph("EMETTEUR", headerFont));

        String name = teacher.getFirstName() + " " + teacher.getLastName();
        if (teacher.getCompanyName() != null && !teacher.getCompanyName().isBlank()) {
            name = teacher.getCompanyName() + "\n" + name;
        }
        document.add(new Paragraph(name, normalFont));
        document.add(new Paragraph(teacher.getEmail(), normalFont));

        if (teacher.getSiret() != null && !teacher.getSiret().isBlank()) {
            document.add(new Paragraph("SIRET : " + teacher.getSiret(), normalFont));
        }

        document.add(new Paragraph(" "));
    }

    /**
     * Add platform issuer information.
     */
    private void addPlatformIssuerInfo(Document document) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(212, 168, 75));
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        document.add(new Paragraph("EMETTEUR", headerFont));
        document.add(new Paragraph(PLATFORM_NAME, normalFont));
        document.add(new Paragraph(PLATFORM_ADDRESS, normalFont));
        document.add(new Paragraph(PLATFORM_EMAIL, normalFont));
        if (!PLATFORM_SIRET.isBlank()) {
            document.add(new Paragraph("SIRET : " + PLATFORM_SIRET, normalFont));
        }

        document.add(new Paragraph(" "));
    }

    /**
     * Add customer information.
     */
    private void addCustomerInfo(Document document, User customer, String label) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(212, 168, 75));
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        document.add(new Paragraph(label.toUpperCase(), headerFont));
        document.add(new Paragraph(customer.getFirstName() + " " + customer.getLastName(), normalFont));
        document.add(new Paragraph(customer.getEmail(), normalFont));

        if (customer.getCompanyName() != null && !customer.getCompanyName().isBlank()) {
            document.add(new Paragraph(customer.getCompanyName(), normalFont));
        }
        if (customer.getSiret() != null && !customer.getSiret().isBlank()) {
            document.add(new Paragraph("SIRET : " + customer.getSiret(), normalFont));
        }

        document.add(new Paragraph(" "));
    }

    /**
     * Add lesson invoice table with details.
     */
    private void addLessonInvoiceTable(Document document, Invoice invoice, Lesson lesson) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 1.5f, 1.5f});
        table.setSpacingBefore(20);

        // Header row
        Color goldColor = new Color(212, 168, 75);
        String[] headers = {"Description", "Qte", "Prix unitaire", "Total"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(goldColor);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Data row
        String description = invoice.getDescription();
        if (lesson != null && lesson.getScheduledAt() != null) {
            description += "\n" + lesson.getScheduledAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        table.addCell(createCell(description, normalFont, Element.ALIGN_LEFT));
        table.addCell(createCell("1", normalFont, Element.ALIGN_CENTER));
        table.addCell(createCell(formatCents(invoice.getTotalCents()), normalFont, Element.ALIGN_RIGHT));
        table.addCell(createCell(formatCents(invoice.getTotalCents()), normalFont, Element.ALIGN_RIGHT));

        document.add(table);

        // Total section
        addTotalSection(document, invoice);
    }

    /**
     * Add commission invoice table.
     */
    private void addCommissionInvoiceTable(Document document, Invoice invoice, Lesson lesson) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 1.5f, 1.5f});
        table.setSpacingBefore(20);

        // Header row
        Color goldColor = new Color(212, 168, 75);
        String[] headers = {"Description", "Taux", "Base", "Total"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(goldColor);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Calculate base amount (reverse from commission)
        int baseAmount = (int) Math.round(invoice.getTotalCents() / (invoice.getCommissionRate() / 100));

        table.addCell(createCell(invoice.getDescription(), normalFont, Element.ALIGN_LEFT));
        table.addCell(createCell(String.format("%.1f%%", invoice.getCommissionRate()), normalFont, Element.ALIGN_CENTER));
        table.addCell(createCell(formatCents(baseAmount), normalFont, Element.ALIGN_RIGHT));
        table.addCell(createCell(formatCents(invoice.getTotalCents()), normalFont, Element.ALIGN_RIGHT));

        document.add(table);

        // Total section
        addTotalSection(document, invoice);
    }

    /**
     * Add total section.
     */
    private void addTotalSection(Document document, Invoice invoice) throws DocumentException {
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setSpacingBefore(15);

        // Subtotal
        totalTable.addCell(createCell("Sous-total HT", normalFont, Element.ALIGN_LEFT));
        totalTable.addCell(createCell(formatCents(invoice.getSubtotalCents()), normalFont, Element.ALIGN_RIGHT));

        // VAT
        if (invoice.getVatRate() != null && invoice.getVatRate() > 0) {
            totalTable.addCell(createCell("TVA (" + invoice.getVatRate() + "%)", normalFont, Element.ALIGN_LEFT));
            totalTable.addCell(createCell(formatCents(invoice.getVatCents()), normalFont, Element.ALIGN_RIGHT));
        }

        // Total
        PdfPCell totalLabelCell = createCell("TOTAL TTC", boldFont, Element.ALIGN_LEFT);
        totalLabelCell.setBackgroundColor(new Color(245, 245, 245));
        totalTable.addCell(totalLabelCell);

        PdfPCell totalValueCell = createCell(formatCents(invoice.getTotalCents()), boldFont, Element.ALIGN_RIGHT);
        totalValueCell.setBackgroundColor(new Color(245, 245, 245));
        totalTable.addCell(totalValueCell);

        document.add(totalTable);
    }

    /**
     * Add payment information.
     */
    private void addPaymentInfo(Document document, String paymentMethod) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(212, 168, 75));
        Font normalFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(102, 102, 102));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("PAIEMENT", headerFont));
        document.add(new Paragraph(paymentMethod, normalFont));
        document.add(new Paragraph("Statut : PAYE", normalFont));
    }

    /**
     * Add footer.
     */
    private void addFooter(Document document) throws DocumentException {
        Font footerFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(153, 153, 153));

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        Paragraph footer = new Paragraph(
                "Facture generee automatiquement par mychess - Plateforme de cours d'echecs en ligne",
                footerFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    /**
     * Helper to create table cell.
     */
    private PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(new Color(220, 220, 220));
        return cell;
    }

    /**
     * Format cents to EUR string.
     */
    private String formatCents(int cents) {
        return String.format("%.2f EUR", cents / 100.0);
    }

    /**
     * Save PDF to storage and return path.
     */
    private String savePdf(byte[] pdfBytes, String invoiceNumber) throws IOException {
        // Ensure storage directory exists
        Path storagePath = Paths.get(invoiceStoragePath);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        // Generate filename
        String filename = invoiceNumber.replace("/", "-") + ".pdf";
        Path filePath = storagePath.resolve(filename);

        // Write file
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(pdfBytes);
        }

        log.info("Saved invoice PDF: {}", filePath);
        return filePath.toString();
    }

    /**
     * Get PDF bytes for download.
     */
    public byte[] getInvoicePdf(Long invoiceId, Long userId) throws IOException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Security check: user must be customer or issuer
        boolean isCustomer = invoice.getCustomer().getId().equals(userId);
        boolean isIssuer = invoice.getIssuer() != null && invoice.getIssuer().getId().equals(userId);

        if (!isCustomer && !isIssuer) {
            throw new RuntimeException("Access denied to this invoice");
        }

        if (invoice.getPdfPath() == null) {
            throw new RuntimeException("PDF not available for this invoice");
        }

        return Files.readAllBytes(Paths.get(invoice.getPdfPath()));
    }

    /**
     * Get invoices for a user.
     */
    public List<Invoice> getInvoicesForUser(Long userId) {
        // Get invoices where user is customer (received) or issuer (sent)
        List<Invoice> received = invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(userId);
        List<Invoice> issued = invoiceRepository.findByIssuerIdOrderByCreatedAtDesc(userId);

        // Merge and sort by date
        received.addAll(issued);
        received.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return received;
    }

    /**
     * Generate invoice for subscription payment.
     */
    @Transactional
    public Invoice generateSubscriptionInvoice(Long studentId, int amountCents, String stripePaymentIntentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber("ABO"));
        invoice.setInvoiceType(InvoiceType.SUBSCRIPTION);
        invoice.setCustomer(student);
        invoice.setIssuer(null); // Platform is the issuer
        invoice.setStripePaymentIntentId(stripePaymentIntentId);
        invoice.setSubtotalCents(amountCents);
        invoice.setVatCents(0);
        invoice.setTotalCents(amountCents);
        invoice.setVatRate(0);
        invoice.setDescription("Abonnement Premium Mychess - 1 mois");
        invoice.setStatus("PAID");
        invoice.setIssuedAt(LocalDateTime.now());

        invoice = invoiceRepository.save(invoice);

        // Generate PDF
        try {
            generateSubscriptionInvoicePdf(invoice, student);
        } catch (Exception e) {
            log.error("Error generating subscription invoice PDF", e);
        }

        log.info("Generated subscription invoice #{} for student {}", invoice.getInvoiceNumber(), studentId);

        return invoice;
    }

    /**
     * Generate PDF for subscription invoice.
     */
    private void generateSubscriptionInvoicePdf(Invoice invoice, User student) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content
            addInvoiceHeader(document, "FACTURE D'ABONNEMENT", invoice.getInvoiceNumber(), invoice.getIssuedAt());
            addPlatformIssuerInfo(document);
            addCustomerInfo(document, student, "Client");
            addSubscriptionInvoiceTable(document, invoice);
            addPaymentInfo(document, "Paye par carte bancaire via Stripe");
            addFooter(document);

            document.close();

            // Save PDF
            String pdfPath = savePdf(baos.toByteArray(), invoice.getInvoiceNumber());
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);

        } catch (DocumentException e) {
            log.error("Error creating subscription invoice PDF", e);
            throw new IOException("Failed to generate PDF", e);
        }
    }

    /**
     * Add subscription invoice table.
     */
    private void addSubscriptionInvoiceTable(Document document, Invoice invoice) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 1.5f, 1.5f});
        table.setSpacingBefore(20);

        // Header row
        Color goldColor = new Color(212, 168, 75);
        String[] headers = {"Description", "Qte", "Prix unitaire", "Total"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(goldColor);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Data row
        table.addCell(createCell(invoice.getDescription(), normalFont, Element.ALIGN_LEFT));
        table.addCell(createCell("1", normalFont, Element.ALIGN_CENTER));
        table.addCell(createCell(formatCents(invoice.getTotalCents()), normalFont, Element.ALIGN_RIGHT));
        table.addCell(createCell(formatCents(invoice.getTotalCents()), normalFont, Element.ALIGN_RIGHT));

        document.add(table);

        // Total section
        addTotalSection(document, invoice);
    }
}
