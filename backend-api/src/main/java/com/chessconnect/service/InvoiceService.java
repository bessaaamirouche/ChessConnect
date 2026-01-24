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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    // Platform info for invoices (from KBIS)
    private static final String PLATFORM_NAME = "Mychess.fr";
    private static final String PLATFORM_OWNER = "CANDLE";
    private static final String PLATFORM_ADDRESS = "10 rue de Penthievre, 75008 Paris";
    private static final String PLATFORM_SIREN = "834 446 510";
    private static final String PLATFORM_RCS = "R.C.S. Paris";
    private static final String PLATFORM_EMAIL = "support@mychess.fr";

    // Commission rates (12.5% platform + 2.5% Stripe = 15% total)
    private static final double STANDARD_COMMISSION_RATE = 12.5; // 12.5% platform
    private static final double PROMO_COMMISSION_RATE = 2.5;     // 2.5% with CHESS2026

    @Value("${app.invoices.storage-path:/app/uploads/invoices}")
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
        // Logo (1024x329 original, scaled to fit header, aligned left)
        try {
            ClassPathResource logoResource = new ClassPathResource("static/logo.png");
            if (logoResource.exists()) {
                try (InputStream is = logoResource.getInputStream()) {
                    byte[] logoBytes = is.readAllBytes();
                    Image logo = Image.getInstance(logoBytes);
                    logo.scaleToFit(180, 58); // Preserve aspect ratio for horizontal logo
                    logo.setAlignment(Element.ALIGN_LEFT);
                    document.add(logo);
                    document.add(new Paragraph(" "));
                }
            }
        } catch (Exception e) {
            log.warn("Could not add logo to invoice PDF: {}", e.getMessage());
        }

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
        document.add(new Paragraph(PLATFORM_OWNER, normalFont));
        document.add(new Paragraph(PLATFORM_ADDRESS, normalFont));
        document.add(new Paragraph(PLATFORM_EMAIL, normalFont));
        document.add(new Paragraph("SIREN : " + PLATFORM_SIREN + " " + PLATFORM_RCS, normalFont));

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
     * Add footer with legal mentions.
     */
    private void addFooter(Document document) throws DocumentException {
        Font legalFont = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(102, 102, 102));
        Font footerFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(153, 153, 153));

        document.add(new Paragraph(" "));

        // Mandatory VAT mention for micro-entreprise (franchise en base de TVA)
        Paragraph vatMention = new Paragraph(
                "TVA non applicable, art. 293 B du CGI",
                legalFont
        );
        vatMention.setAlignment(Element.ALIGN_CENTER);
        document.add(vatMention);

        document.add(new Paragraph(" "));

        Paragraph footer = new Paragraph(
                "Facture generee automatiquement par Mychess.fr - Plateforme de cours d'echecs en ligne",
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
    public byte[] getInvoicePdf(Long invoiceId, Long userId, boolean isAdmin) throws IOException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Security check: user must be customer, issuer, or admin
        boolean isCustomer = invoice.getCustomer().getId().equals(userId);
        boolean isIssuer = invoice.getIssuer() != null && invoice.getIssuer().getId().equals(userId);

        if (!isCustomer && !isIssuer && !isAdmin) {
            throw new RuntimeException("Access denied to this invoice");
        }

        // Generate PDF on-demand if it doesn't exist
        if (invoice.getPdfPath() == null || !Files.exists(Paths.get(invoice.getPdfPath()))) {
            log.info("Generating PDF on-demand for invoice {}", invoiceId);
            generatePdfOnDemand(invoice);
        }

        return Files.readAllBytes(Paths.get(invoice.getPdfPath()));
    }

    /**
     * Generate PDF on-demand for existing invoices that don't have a PDF.
     */
    private void generatePdfOnDemand(Invoice invoice) throws IOException {
        User customer = invoice.getCustomer();
        User issuer = invoice.getIssuer();
        Lesson lesson = invoice.getLesson();

        switch (invoice.getInvoiceType()) {
            case LESSON_INVOICE:
                if (issuer != null) {
                    generateLessonInvoicePdf(invoice, customer, issuer, lesson);
                }
                break;
            case COMMISSION_INVOICE:
                generateCommissionInvoicePdf(invoice, customer, lesson);
                break;
            case SUBSCRIPTION:
                generateSubscriptionInvoicePdf(invoice, customer);
                break;
            case PAYOUT_INVOICE:
                String yearMonth = extractYearMonthFromDescription(invoice.getDescription());
                generatePayoutInvoicePdf(invoice, customer, yearMonth);
                break;
            default:
                throw new RuntimeException("Unknown invoice type: " + invoice.getInvoiceType());
        }
    }

    /**
     * Extract year-month from payout description like "Virement des gains - 2026-01"
     */
    private String extractYearMonthFromDescription(String description) {
        if (description != null && description.contains(" - ")) {
            return description.substring(description.lastIndexOf(" - ") + 3);
        }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
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
        // Check if invoice already exists for this payment
        if (stripePaymentIntentId != null && invoiceRepository.existsByStripePaymentIntentId(stripePaymentIntentId)) {
            log.info("Subscription invoice already exists for payment intent: {}", stripePaymentIntentId);
            List<Invoice> existing = invoiceRepository.findByStripePaymentIntentId(stripePaymentIntentId);
            return existing.isEmpty() ? null : existing.get(0);
        }

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
     * Generate invoice for coach payout/transfer.
     */
    @Transactional
    public Invoice generatePayoutInvoice(User teacher, int amountCents, String yearMonth, String stripeTransferId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber("VIR"));
        invoice.setInvoiceType(InvoiceType.PAYOUT_INVOICE);
        invoice.setCustomer(teacher); // Teacher receives this invoice
        invoice.setIssuer(null); // Platform is the issuer
        invoice.setStripePaymentIntentId(stripeTransferId);
        invoice.setSubtotalCents(amountCents);
        invoice.setVatCents(0);
        invoice.setTotalCents(amountCents);
        invoice.setVatRate(0);
        invoice.setDescription("Virement des gains - " + yearMonth);
        invoice.setStatus("PAID");
        invoice.setIssuedAt(LocalDateTime.now());

        invoice = invoiceRepository.save(invoice);

        // Generate PDF
        try {
            generatePayoutInvoicePdf(invoice, teacher, yearMonth);
        } catch (Exception e) {
            log.error("Error generating payout invoice PDF", e);
        }

        log.info("Generated payout invoice #{} for teacher {}", invoice.getInvoiceNumber(), teacher.getId());

        return invoice;
    }

    /**
     * Generate PDF for payout invoice.
     */
    private void generatePayoutInvoicePdf(Invoice invoice, User teacher, String yearMonth) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content
            addInvoiceHeader(document, "RELEVE DE VIREMENT", invoice.getInvoiceNumber(), invoice.getIssuedAt());
            addPlatformIssuerInfo(document);
            addCustomerInfo(document, teacher, "Beneficiaire");
            addPayoutInvoiceTable(document, invoice, yearMonth);
            addPaymentInfo(document, "Virement Stripe Connect vers votre compte bancaire");
            addFooter(document);

            document.close();

            // Save PDF
            String pdfPath = savePdf(baos.toByteArray(), invoice.getInvoiceNumber());
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);

        } catch (DocumentException e) {
            log.error("Error creating payout invoice PDF", e);
            throw new IOException("Failed to generate PDF", e);
        }
    }

    /**
     * Add payout invoice table.
     */
    private void addPayoutInvoiceTable(Document document, Invoice invoice, String yearMonth) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2, 2});
        table.setSpacingBefore(20);

        // Header row
        Color goldColor = new Color(212, 168, 75);
        String[] headers = {"Description", "Periode", "Montant"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(goldColor);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Data row
        table.addCell(createCell("Virement des gains de cours", normalFont, Element.ALIGN_LEFT));
        table.addCell(createCell(yearMonth, normalFont, Element.ALIGN_CENTER));
        table.addCell(createCell(formatCents(invoice.getTotalCents()), normalFont, Element.ALIGN_RIGHT));

        document.add(table);

        // Total section
        addTotalSection(document, invoice);
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

    /**
     * Generate a credit note (avoir) for a refunded lesson.
     * This creates a CREDIT_NOTE invoice that references the original invoice.
     *
     * @param lesson The cancelled lesson
     * @param stripeRefundId The Stripe refund ID
     * @param refundPercentage The percentage refunded (0-100)
     * @param refundAmountCents The amount refunded in cents
     * @return The generated credit note invoice
     */
    @Transactional
    public Invoice generateCreditNote(Lesson lesson, String stripeRefundId, int refundPercentage, int refundAmountCents) {
        // Find the original lesson invoice
        List<Invoice> lessonInvoices = invoiceRepository.findByLessonId(lesson.getId());
        Invoice originalInvoice = lessonInvoices.stream()
                .filter(inv -> inv.getInvoiceType() == InvoiceType.LESSON_INVOICE)
                .findFirst()
                .orElse(null);

        if (originalInvoice == null) {
            log.warn("No original invoice found for lesson {} - cannot generate credit note", lesson.getId());
            return null;
        }

        // Check if credit note already exists for this refund
        if (stripeRefundId != null) {
            boolean exists = lessonInvoices.stream()
                    .anyMatch(inv -> inv.getInvoiceType() == InvoiceType.CREDIT_NOTE
                            && stripeRefundId.equals(inv.getStripeRefundId()));
            if (exists) {
                log.info("Credit note already exists for refund {}", stripeRefundId);
                return null;
            }
        }

        User student = lesson.getStudent();
        User teacher = lesson.getTeacher();

        // Create credit note
        Invoice creditNote = new Invoice();
        creditNote.setInvoiceNumber(generateInvoiceNumber("AV"));
        creditNote.setInvoiceType(InvoiceType.CREDIT_NOTE);
        creditNote.setCustomer(student);
        creditNote.setIssuer(teacher);
        creditNote.setLesson(lesson);
        creditNote.setOriginalInvoice(originalInvoice);
        creditNote.setStripeRefundId(stripeRefundId);
        creditNote.setRefundPercentage(refundPercentage);
        creditNote.setSubtotalCents(-refundAmountCents); // Negative for credit note
        creditNote.setVatCents(0);
        creditNote.setTotalCents(-refundAmountCents);
        creditNote.setVatRate(0);

        String refundDescription = refundPercentage == 100
                ? "Avoir - Annulation de cours"
                : String.format("Avoir - Remboursement partiel (%d%%)", refundPercentage);
        creditNote.setDescription(refundDescription);
        creditNote.setStatus("REFUNDED");
        creditNote.setIssuedAt(LocalDateTime.now());

        creditNote = invoiceRepository.save(creditNote);

        // Update original invoice status
        String newStatus = refundPercentage == 100 ? "REFUNDED" : "PARTIALLY_REFUNDED";
        originalInvoice.setStatus(newStatus);
        invoiceRepository.save(originalInvoice);

        // Generate PDF for credit note
        try {
            generateCreditNotePdf(creditNote, student, teacher, lesson, originalInvoice);
        } catch (Exception e) {
            log.error("Error generating credit note PDF", e);
        }

        log.info("Generated credit note #{} for lesson {} ({}% refund)",
                creditNote.getInvoiceNumber(), lesson.getId(), refundPercentage);

        return creditNote;
    }

    /**
     * Generate PDF for credit note.
     */
    private void generateCreditNotePdf(Invoice creditNote, User student, User teacher,
                                        Lesson lesson, Invoice originalInvoice) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content
            addInvoiceHeader(document, "AVOIR", creditNote.getInvoiceNumber(), creditNote.getIssuedAt());
            addIssuerInfo(document, teacher);
            addCustomerInfo(document, student, "Client");

            // Reference to original invoice
            Font refFont = new Font(Font.HELVETICA, 10, Font.ITALIC);
            Paragraph refParagraph = new Paragraph(
                    "Reference facture originale : " + originalInvoice.getInvoiceNumber(),
                    refFont
            );
            refParagraph.setSpacingBefore(15);
            document.add(refParagraph);

            addCreditNoteTable(document, creditNote, lesson);
            addCreditNoteTotalSection(document, creditNote);
            addPaymentInfo(document, "Remboursement via Stripe");
            addFooter(document);

            document.close();

            // Save PDF
            String pdfPath = savePdf(baos.toByteArray(), creditNote.getInvoiceNumber());
            creditNote.setPdfPath(pdfPath);
            invoiceRepository.save(creditNote);

        } catch (DocumentException e) {
            log.error("Error creating credit note PDF", e);
            throw new IOException("Failed to generate PDF", e);
        }
    }

    /**
     * Add credit note table.
     */
    private void addCreditNoteTable(Document document, Invoice creditNote, Lesson lesson) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1.5f, 1.5f, 1.5f});
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

        // Data row - show negative amount for credit
        String description = creditNote.getDescription();
        int refundAmount = Math.abs(creditNote.getTotalCents());

        table.addCell(createCell(description, normalFont, Element.ALIGN_LEFT));
        table.addCell(createCell("1", normalFont, Element.ALIGN_CENTER));
        table.addCell(createCell("-" + formatCents(refundAmount), normalFont, Element.ALIGN_RIGHT));
        table.addCell(createCell("-" + formatCents(refundAmount), normalFont, Element.ALIGN_RIGHT));

        document.add(table);
    }

    /**
     * Add credit note total section (negative amounts).
     */
    private void addCreditNoteTotalSection(Document document, Invoice creditNote) throws DocumentException {
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setSpacingBefore(20);

        int refundAmount = Math.abs(creditNote.getTotalCents());

        // Subtotal
        totalTable.addCell(createCell("Sous-total HT:", normalFont, Element.ALIGN_LEFT));
        totalTable.addCell(createCell("-" + formatCents(refundAmount), normalFont, Element.ALIGN_RIGHT));

        // TVA
        totalTable.addCell(createCell("TVA (0%):", normalFont, Element.ALIGN_LEFT));
        totalTable.addCell(createCell("0,00 EUR", normalFont, Element.ALIGN_RIGHT));

        // Total - highlighted in red for credit
        Font redBoldFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(220, 38, 38));
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total rembourse:", boldFont));
        totalLabelCell.setBorder(0);
        totalLabelCell.setPadding(5);
        totalLabelCell.setBackgroundColor(new Color(254, 226, 226)); // Light red
        totalTable.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase("-" + formatCents(refundAmount), redBoldFont));
        totalValueCell.setBorder(0);
        totalValueCell.setPadding(5);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setBackgroundColor(new Color(254, 226, 226)); // Light red
        totalTable.addCell(totalValueCell);

        document.add(totalTable);
    }
}
