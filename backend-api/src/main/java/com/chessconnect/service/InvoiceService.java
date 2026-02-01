package com.chessconnect.service;

import com.chessconnect.model.Invoice;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.InvoiceType;
import com.chessconnect.model.Payment;
import com.chessconnect.repository.InvoiceRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.PaymentRepository;
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
    private static final String PLATFORM_WEBSITE = "www.mychess.fr";

    // Commission rates (10% platform + 2.5% Stripe = 12.5% total)
    private static final double STANDARD_COMMISSION_RATE = 10.0; // 10% platform
    private static final double PROMO_COMMISSION_RATE = 2.5;     // 2.5% with CHESS2026

    // PDF Design colors - Blue theme
    private static final Color BLUE_DARK = new Color(26, 79, 122);      // #1a4f7a - Headers, titles
    private static final Color BLUE_MEDIUM = new Color(41, 128, 185);   // #2980b9 - Gradient end
    private static final Color BLUE_ACCENT = new Color(52, 152, 219);   // #3498db - Links, accents
    private static final Color BLUE_VERY_LIGHT = new Color(234, 242, 248); // #eaf2f8 - Alternating rows
    private static final Color GRAY_TEXT = new Color(85, 85, 85);       // #555555 - Body text
    private static final Color GRAY_LIGHT = new Color(149, 165, 166);   // #95a5a6 - Secondary text
    private static final Color GRAY_BORDER = new Color(220, 220, 220);  // #dcdcdc - Table borders
    private static final Color WHITE = Color.WHITE;

    // Border radius for rounded corners
    private static final float BORDER_RADIUS = 10f;

    /**
     * Custom cell event to draw rounded rectangle backgrounds.
     */
    private static class RoundedCellEvent implements PdfPCellEvent {
        private final Color backgroundColor;
        private final float radius;

        public RoundedCellEvent(Color backgroundColor, float radius) {
            this.backgroundColor = backgroundColor;
            this.radius = radius;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
            float x = position.getLeft();
            float y = position.getBottom();
            float w = position.getWidth();
            float h = position.getHeight();

            canvas.saveState();
            canvas.setColorFill(backgroundColor);
            canvas.roundRectangle(x, y, w, h, radius);
            canvas.fill();
            canvas.restoreState();
        }
    }

    /**
     * Custom table event to draw rounded rectangle around entire table.
     */
    private static class RoundedTableEvent implements PdfPTableEvent {
        private final Color backgroundColor;
        private final float radius;

        public RoundedTableEvent(Color backgroundColor, float radius) {
            this.backgroundColor = backgroundColor;
            this.radius = radius;
        }

        @Override
        public void tableLayout(PdfPTable table, float[][] widths, float[] heights, int headerRows, int rowStart, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
            float x = widths[0][0];
            float y = heights[heights.length - 1];
            float w = widths[0][widths[0].length - 1] - x;
            float h = heights[0] - y;

            canvas.saveState();
            canvas.setColorFill(backgroundColor);
            canvas.roundRectangle(x, y, w, h, radius);
            canvas.fill();
            canvas.restoreState();
        }
    }

    @Value("${app.invoices.storage-path:/app/uploads/invoices}")
    private String invoiceStoragePath;

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            UserRepository userRepository,
            LessonRepository lessonRepository,
            PaymentRepository paymentRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.paymentRepository = paymentRepository;
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

        // Generate Invoice: Teacher -> Student (lesson service)
        // Note: Commission invoice is generated only when coach withdraws earnings
        Invoice lessonInvoice = createLessonInvoice(
                student, teacher, lesson, paymentIntentId, totalAmountCents
        );

        // Generate PDF
        try {
            generateLessonInvoicePdf(lessonInvoice, student, teacher, lesson);
        } catch (Exception e) {
            log.error("Error generating PDF invoice", e);
        }

        log.info("Generated lesson invoice #{} for payment {}",
                lessonInvoice.getInvoiceNumber(), paymentIntentId);

        return List.of(lessonInvoice);
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
                ? "Frais techniques de paiement (2,5%)"
                : "Frais plateforme (10%) + Frais Stripe (2,5%)";
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
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content with new blue design
            addInvoiceHeader(document, "FACTURE", invoice.getInvoiceNumber(), invoice.getIssuedAt());
            addTwoColumnParties(document, teacher, student, false);
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
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content with new blue design
            addInvoiceHeader(document, "FACTURE DE COMMISSION", invoice.getInvoiceNumber(), invoice.getIssuedAt());
            addTwoColumnParties(document, null, teacher, true); // Platform as issuer
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
     * Add professional invoice header with blue gradient band, logo and title.
     */
    private void addInvoiceHeader(Document document, String title, String invoiceNumber, LocalDateTime date) throws DocumentException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Blue header band with logo on left and title/info on right (with rounded corners)
        PdfPTable headerBand = new PdfPTable(2);
        headerBand.setWidthPercentage(100);
        headerBand.setWidths(new float[]{1, 1.5f});
        headerBand.setTableEvent(new RoundedTableEvent(BLUE_DARK, BORDER_RADIUS));

        // Left cell: Logo image only (transparent background, table event handles bg)
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(null); // Transparent, table event draws bg
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(15);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        try {
            ClassPathResource logoResource = new ClassPathResource("static/logo.png");
            if (logoResource.exists()) {
                try (InputStream is = logoResource.getInputStream()) {
                    byte[] logoBytes = is.readAllBytes();
                    Image logo = Image.getInstance(logoBytes);
                    logo.scaleToFit(160, 160);
                    logoCell.addElement(logo);
                }
            }
        } catch (Exception e) {
            // If logo fails to load, leave cell empty
            log.warn("Failed to load logo for invoice: {}", e.getMessage());
        }

        headerBand.addCell(logoCell);

        // Right cell: Title and invoice info
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(null); // Transparent, table event draws bg
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(15);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        // Title
        Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, WHITE);
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(titlePara);

        // Invoice number
        Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(180, 200, 220));
        Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD, WHITE);

        Paragraph invoiceNumPara = new Paragraph();
        invoiceNumPara.setAlignment(Element.ALIGN_RIGHT);
        invoiceNumPara.add(new Chunk("N° ", labelFont));
        invoiceNumPara.add(new Chunk(invoiceNumber, valueFont));
        titleCell.addElement(invoiceNumPara);

        // Date
        Paragraph datePara = new Paragraph();
        datePara.setAlignment(Element.ALIGN_RIGHT);
        datePara.add(new Chunk("Date : ", labelFont));
        datePara.add(new Chunk(date.format(formatter), valueFont));
        titleCell.addElement(datePara);

        headerBand.addCell(titleCell);

        document.add(headerBand);

        // Small spacing after header
        document.add(new Paragraph(" "));
    }

    /**
     * Add issuer (teacher) and customer information side by side.
     */
    private void addIssuerInfo(Document document, User teacher) throws DocumentException {
        // This will be called separately when needed - see addTwoColumnParties for combined layout
        addSinglePartyInfo(document, teacher, "EMETTEUR", true);
    }

    /**
     * Add platform issuer information.
     */
    private void addPlatformIssuerInfo(Document document) throws DocumentException {
        // This will be called separately when needed
        addSinglePlatformInfo(document, "EMETTEUR");
    }

    /**
     * Add customer information.
     */
    private void addCustomerInfo(Document document, User customer, String label) throws DocumentException {
        // This will be called separately when needed
        addSinglePartyInfo(document, customer, label.toUpperCase(), false);
    }

    /**
     * Add two-column layout for issuer (left) and customer (right).
     */
    private void addTwoColumnParties(Document document, User issuer, User customer, boolean issuerIsPlatform) throws DocumentException {
        PdfPTable partiesTable = new PdfPTable(2);
        partiesTable.setWidthPercentage(100);
        partiesTable.setWidths(new float[]{1, 1});
        partiesTable.setSpacingBefore(10);
        partiesTable.setSpacingAfter(25);

        // Left column: Issuer
        PdfPCell issuerCell = new PdfPCell();
        issuerCell.setBorder(Rectangle.NO_BORDER);
        issuerCell.setPaddingRight(15);

        // Header with blue underline
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD, BLUE_DARK);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);
        Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_LIGHT);

        Paragraph issuerHeader = new Paragraph("EMETTEUR", headerFont);
        issuerCell.addElement(issuerHeader);

        // Blue underline
        PdfPTable underline1 = new PdfPTable(1);
        underline1.setWidthPercentage(40);
        underline1.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell lineCell1 = new PdfPCell();
        lineCell1.setBackgroundColor(BLUE_ACCENT);
        lineCell1.setFixedHeight(2);
        lineCell1.setBorder(Rectangle.NO_BORDER);
        underline1.addCell(lineCell1);
        issuerCell.addElement(underline1);

        // Issuer details
        if (issuerIsPlatform) {
            issuerCell.addElement(new Paragraph(PLATFORM_NAME, normalFont));
            issuerCell.addElement(new Paragraph(PLATFORM_OWNER, normalFont));
            issuerCell.addElement(new Paragraph(PLATFORM_ADDRESS, smallFont));
            issuerCell.addElement(new Paragraph(PLATFORM_EMAIL, smallFont));
            issuerCell.addElement(new Paragraph("SIREN : " + PLATFORM_SIREN, smallFont));
        } else if (issuer != null) {
            String name = issuer.getFirstName() + " " + issuer.getLastName();
            if (issuer.getCompanyName() != null && !issuer.getCompanyName().isBlank()) {
                issuerCell.addElement(new Paragraph(issuer.getCompanyName(), normalFont));
            }
            issuerCell.addElement(new Paragraph(name, normalFont));
            issuerCell.addElement(new Paragraph(issuer.getEmail(), smallFont));
            if (issuer.getSiret() != null && !issuer.getSiret().isBlank()) {
                issuerCell.addElement(new Paragraph("SIRET : " + issuer.getSiret(), smallFont));
            }
        }

        partiesTable.addCell(issuerCell);

        // Right column: Customer
        PdfPCell customerCell = new PdfPCell();
        customerCell.setBorder(Rectangle.NO_BORDER);
        customerCell.setPaddingLeft(15);

        Paragraph customerHeader = new Paragraph("CLIENT", headerFont);
        customerCell.addElement(customerHeader);

        // Blue underline
        PdfPTable underline2 = new PdfPTable(1);
        underline2.setWidthPercentage(40);
        underline2.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell lineCell2 = new PdfPCell();
        lineCell2.setBackgroundColor(BLUE_ACCENT);
        lineCell2.setFixedHeight(2);
        lineCell2.setBorder(Rectangle.NO_BORDER);
        underline2.addCell(lineCell2);
        customerCell.addElement(underline2);

        // Customer details
        if (customer != null) {
            String customerName = customer.getFirstName() + " " + customer.getLastName();
            if (customer.getCompanyName() != null && !customer.getCompanyName().isBlank()) {
                customerCell.addElement(new Paragraph(customer.getCompanyName(), normalFont));
            }
            customerCell.addElement(new Paragraph(customerName, normalFont));
            customerCell.addElement(new Paragraph(customer.getEmail(), smallFont));
            if (customer.getSiret() != null && !customer.getSiret().isBlank()) {
                customerCell.addElement(new Paragraph("SIRET : " + customer.getSiret(), smallFont));
            }
        }

        partiesTable.addCell(customerCell);
        document.add(partiesTable);
    }

    /**
     * Add single party info (for backwards compatibility).
     */
    private void addSinglePartyInfo(Document document, User user, String label, boolean isIssuer) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD, BLUE_DARK);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);
        Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_LIGHT);

        document.add(new Paragraph(label, headerFont));

        // Blue underline
        PdfPTable underline = new PdfPTable(1);
        underline.setWidthPercentage(20);
        underline.setHorizontalAlignment(Element.ALIGN_LEFT);
        underline.setSpacingAfter(5);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(BLUE_ACCENT);
        lineCell.setFixedHeight(2);
        lineCell.setBorder(Rectangle.NO_BORDER);
        underline.addCell(lineCell);
        document.add(underline);

        if (user != null) {
            String name = user.getFirstName() + " " + user.getLastName();
            if (user.getCompanyName() != null && !user.getCompanyName().isBlank()) {
                document.add(new Paragraph(user.getCompanyName(), normalFont));
            }
            document.add(new Paragraph(name, normalFont));
            document.add(new Paragraph(user.getEmail(), smallFont));
            if (user.getSiret() != null && !user.getSiret().isBlank()) {
                document.add(new Paragraph("SIRET : " + user.getSiret(), smallFont));
            }
        }

        document.add(new Paragraph(" "));
    }

    /**
     * Add single platform info (for backwards compatibility).
     */
    private void addSinglePlatformInfo(Document document, String label) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD, BLUE_DARK);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);
        Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_LIGHT);

        document.add(new Paragraph(label, headerFont));

        // Blue underline
        PdfPTable underline = new PdfPTable(1);
        underline.setWidthPercentage(20);
        underline.setHorizontalAlignment(Element.ALIGN_LEFT);
        underline.setSpacingAfter(5);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(BLUE_ACCENT);
        lineCell.setFixedHeight(2);
        lineCell.setBorder(Rectangle.NO_BORDER);
        underline.addCell(lineCell);
        document.add(underline);

        document.add(new Paragraph(PLATFORM_NAME, normalFont));
        document.add(new Paragraph(PLATFORM_OWNER, normalFont));
        document.add(new Paragraph(PLATFORM_ADDRESS, smallFont));
        document.add(new Paragraph(PLATFORM_EMAIL, smallFont));
        document.add(new Paragraph("SIREN : " + PLATFORM_SIREN + " " + PLATFORM_RCS, smallFont));

        document.add(new Paragraph(" "));
    }

    /**
     * Add lesson invoice table with details.
     */
    private void addLessonInvoiceTable(Document document, Invoice invoice, Lesson lesson) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 1.5f, 1.5f});
        table.setSpacingBefore(20);

        // Header row with blue background
        String[] headers = {"Description", "Qte", "Prix unitaire", "Total"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BLUE_DARK);
            cell.setPadding(10);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(BLUE_DARK);
            table.addCell(cell);
        }

        // Data row (white background for single row)
        String description = invoice.getDescription();
        if (lesson != null && lesson.getScheduledAt() != null) {
            description += "\nDate : " + lesson.getScheduledAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy a HH:mm"));
        }

        table.addCell(createStyledCell(description, normalFont, Element.ALIGN_LEFT, WHITE, true));
        table.addCell(createStyledCell("1", normalFont, Element.ALIGN_CENTER, WHITE, true));
        table.addCell(createStyledCell(formatCents(invoice.getTotalCents()), normalFont, Element.ALIGN_RIGHT, WHITE, true));
        table.addCell(createStyledCell(formatCents(invoice.getTotalCents()), normalFont, Element.ALIGN_RIGHT, WHITE, true));

        document.add(table);

        // Total section
        addTotalSection(document, invoice);
    }

    /**
     * Add commission invoice table.
     */
    private void addCommissionInvoiceTable(Document document, Invoice invoice, Lesson lesson) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);
        Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD, BLUE_DARK);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 1.5f, 1.5f});
        table.setSpacingBefore(20);

        // Header row with blue background
        String[] headers = {"Description", "Taux", "Base", "Total"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BLUE_DARK);
            cell.setPadding(10);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(BLUE_DARK);
            table.addCell(cell);
        }

        // Calculate base amount (lesson price) from total commission
        double totalRate = invoice.getCommissionRate();
        int baseAmount;
        int totalCommissionCents;
        int rowIndex = 0;

        // For promo (2.5% only), the rate is already 2.5%
        if (invoice.getPromoApplied() != null && invoice.getPromoApplied()) {
            // Promo: only 2.5% technical fee (Stripe only)
            baseAmount = (int) Math.round(invoice.getTotalCents() / (totalRate / 100));
            totalCommissionCents = invoice.getTotalCents();

            Color rowBg = WHITE;
            table.addCell(createStyledCell("Frais techniques de paiement", normalFont, Element.ALIGN_LEFT, rowBg, true));
            table.addCell(createStyledCell("2.5%", normalFont, Element.ALIGN_CENTER, rowBg, true));
            table.addCell(createStyledCell(formatCents(baseAmount), normalFont, Element.ALIGN_RIGHT, rowBg, true));
            table.addCell(createStyledCell(formatCents(totalCommissionCents), normalFont, Element.ALIGN_RIGHT, rowBg, true));
        } else {
            // Standard: 10% platform + 2.5% Stripe = 12.5%
            baseAmount = (int) Math.round(invoice.getTotalCents() / (totalRate / 100));

            int platformFeeCents = (int) Math.round(baseAmount * 10.0 / 100);
            int stripeFeeCents = (int) Math.round(baseAmount * 2.5 / 100);
            totalCommissionCents = platformFeeCents + stripeFeeCents;

            // Line 1: Platform fee (10%) - white background
            table.addCell(createStyledCell("Frais de mise en relation", normalFont, Element.ALIGN_LEFT, WHITE, true));
            table.addCell(createStyledCell("10%", normalFont, Element.ALIGN_CENTER, WHITE, true));
            table.addCell(createStyledCell(formatCents(baseAmount), normalFont, Element.ALIGN_RIGHT, WHITE, true));
            table.addCell(createStyledCell(formatCents(platformFeeCents), normalFont, Element.ALIGN_RIGHT, WHITE, true));

            // Line 2: Stripe fee (2.5%) - light blue background (alternating)
            table.addCell(createStyledCell("Frais de paiement Stripe", normalFont, Element.ALIGN_LEFT, BLUE_VERY_LIGHT, true));
            table.addCell(createStyledCell("2.5%", normalFont, Element.ALIGN_CENTER, BLUE_VERY_LIGHT, true));
            table.addCell(createStyledCell(formatCents(baseAmount), normalFont, Element.ALIGN_RIGHT, BLUE_VERY_LIGHT, true));
            table.addCell(createStyledCell(formatCents(stripeFeeCents), normalFont, Element.ALIGN_RIGHT, BLUE_VERY_LIGHT, true));
        }

        document.add(table);

        // Custom total section for commission invoice with blue gradient
        addCommissionTotalSection(document, totalCommissionCents);
    }

    /**
     * Add commission total section with blue styling.
     */
    private void addCommissionTotalSection(Document document, int totalCommissionCents) throws DocumentException {
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);
        Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD, WHITE);

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setSpacingBefore(15);

        // Subtotal
        PdfPCell subtotalLabel = new PdfPCell(new Phrase("Sous-total HT", normalFont));
        subtotalLabel.setBorder(Rectangle.NO_BORDER);
        subtotalLabel.setPadding(8);
        totalTable.addCell(subtotalLabel);

        PdfPCell subtotalValue = new PdfPCell(new Phrase(formatCents(totalCommissionCents), normalFont));
        subtotalValue.setBorder(Rectangle.NO_BORDER);
        subtotalValue.setPadding(8);
        subtotalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.addCell(subtotalValue);

        document.add(totalTable);

        // Total with blue background and rounded corners
        PdfPTable totalRow = new PdfPTable(2);
        totalRow.setWidthPercentage(50);
        totalRow.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalRow.setSpacingBefore(5);
        totalRow.setTableEvent(new RoundedTableEvent(BLUE_DARK, BORDER_RADIUS));

        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL TTC", boldFont));
        totalLabelCell.setBackgroundColor(null);
        totalLabelCell.setBorder(Rectangle.NO_BORDER);
        totalLabelCell.setPadding(10);
        totalRow.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCents(totalCommissionCents), boldFont));
        totalValueCell.setBackgroundColor(null);
        totalValueCell.setBorder(Rectangle.NO_BORDER);
        totalValueCell.setPadding(10);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalRow.addCell(totalValueCell);

        document.add(totalRow);
    }

    /**
     * Add total section with blue gradient styling.
     */
    private void addTotalSection(Document document, Invoice invoice) throws DocumentException {
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);
        Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD, WHITE);
        Font totalAmountFont = new Font(Font.HELVETICA, 14, Font.BOLD, WHITE);

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setSpacingBefore(15);

        // Subtotal
        PdfPCell subtotalLabel = new PdfPCell(new Phrase("Sous-total HT", normalFont));
        subtotalLabel.setBorder(Rectangle.NO_BORDER);
        subtotalLabel.setPadding(8);
        totalTable.addCell(subtotalLabel);

        PdfPCell subtotalValue = new PdfPCell(new Phrase(formatCents(invoice.getSubtotalCents()), normalFont));
        subtotalValue.setBorder(Rectangle.NO_BORDER);
        subtotalValue.setPadding(8);
        subtotalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.addCell(subtotalValue);

        // VAT
        if (invoice.getVatRate() != null && invoice.getVatRate() > 0) {
            PdfPCell vatLabel = new PdfPCell(new Phrase("TVA (" + invoice.getVatRate() + "%)", normalFont));
            vatLabel.setBorder(Rectangle.NO_BORDER);
            vatLabel.setPadding(8);
            totalTable.addCell(vatLabel);

            PdfPCell vatValue = new PdfPCell(new Phrase(formatCents(invoice.getVatCents()), normalFont));
            vatValue.setBorder(Rectangle.NO_BORDER);
            vatValue.setPadding(8);
            vatValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.addCell(vatValue);
        }

        document.add(totalTable);

        // Total with blue background and rounded corners (separate table for rounded effect)
        PdfPTable totalRow = new PdfPTable(2);
        totalRow.setWidthPercentage(50);
        totalRow.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalRow.setSpacingBefore(5);
        totalRow.setTableEvent(new RoundedTableEvent(BLUE_DARK, BORDER_RADIUS));

        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL TTC", boldFont));
        totalLabelCell.setBackgroundColor(null);
        totalLabelCell.setBorder(Rectangle.NO_BORDER);
        totalLabelCell.setPadding(12);
        totalRow.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCents(invoice.getTotalCents()), totalAmountFont));
        totalValueCell.setBackgroundColor(null);
        totalValueCell.setBorder(Rectangle.NO_BORDER);
        totalValueCell.setPadding(12);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalRow.addCell(totalValueCell);

        document.add(totalRow);
    }

    /**
     * Add payment information with blue styling.
     */
    private void addPaymentInfo(Document document, String paymentMethod) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD, BLUE_DARK);
        Font normalFont = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);
        Font statusFont = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(39, 174, 96)); // Green for paid

        document.add(new Paragraph(" "));

        // Header with blue underline
        Paragraph paymentHeader = new Paragraph("INFORMATIONS DE PAIEMENT", headerFont);
        document.add(paymentHeader);

        // Blue underline
        PdfPTable underline = new PdfPTable(1);
        underline.setWidthPercentage(30);
        underline.setHorizontalAlignment(Element.ALIGN_LEFT);
        underline.setSpacingAfter(8);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(BLUE_ACCENT);
        lineCell.setFixedHeight(2);
        lineCell.setBorder(Rectangle.NO_BORDER);
        underline.addCell(lineCell);
        document.add(underline);

        document.add(new Paragraph(paymentMethod, normalFont));

        Paragraph statusPara = new Paragraph();
        statusPara.add(new Chunk("Statut : ", normalFont));
        statusPara.add(new Chunk("PAYE", statusFont));
        document.add(statusPara);
    }

    /**
     * Add footer with legal mentions and thank you message.
     */
    private void addFooter(Document document) throws DocumentException {
        Font legalHeaderFont = new Font(Font.HELVETICA, 11, Font.BOLD, BLUE_DARK);
        Font legalFont = new Font(Font.HELVETICA, 8, Font.NORMAL, GRAY_TEXT);
        Font thankYouFont = new Font(Font.HELVETICA, 12, Font.BOLD, WHITE);

        document.add(new Paragraph(" "));

        // Legal mentions section
        Paragraph legalHeader = new Paragraph("MENTIONS LEGALES", legalHeaderFont);
        document.add(legalHeader);

        // Blue underline
        PdfPTable underline = new PdfPTable(1);
        underline.setWidthPercentage(25);
        underline.setHorizontalAlignment(Element.ALIGN_LEFT);
        underline.setSpacingAfter(8);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(BLUE_ACCENT);
        lineCell.setFixedHeight(2);
        lineCell.setBorder(Rectangle.NO_BORDER);
        underline.addCell(lineCell);
        document.add(underline);

        // VAT mention
        Paragraph vatMention = new Paragraph(
                "TVA non applicable, art. 293 B du CGI",
                legalFont
        );
        document.add(vatMention);

        // Additional legal info
        Paragraph legalInfo = new Paragraph(
                "En cas de retard de paiement, une penalite de 3 fois le taux d'interet legal sera appliquee, " +
                "ainsi qu'une indemnite forfaitaire de 40€ pour frais de recouvrement.",
                legalFont
        );
        legalInfo.setSpacingBefore(5);
        document.add(legalInfo);

        document.add(new Paragraph(" "));

        // Blue footer band with thank you message (rounded corners)
        PdfPTable footerBand = new PdfPTable(1);
        footerBand.setWidthPercentage(100);
        footerBand.setSpacingBefore(20);
        footerBand.setTableEvent(new RoundedTableEvent(BLUE_DARK, BORDER_RADIUS));

        PdfPCell footerCell = new PdfPCell();
        footerCell.setBackgroundColor(null); // Transparent, table event draws bg
        footerCell.setBorder(Rectangle.NO_BORDER);
        footerCell.setPadding(15);
        footerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph thankYou = new Paragraph("MERCI POUR VOTRE CONFIANCE", thankYouFont);
        thankYou.setAlignment(Element.ALIGN_CENTER);
        footerCell.addElement(thankYou);

        Font footerInfoFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(200, 220, 240));
        Paragraph footerInfo = new Paragraph(
                PLATFORM_WEBSITE + " - " + PLATFORM_EMAIL,
                footerInfoFont
        );
        footerInfo.setAlignment(Element.ALIGN_CENTER);
        footerCell.addElement(footerInfo);

        footerBand.addCell(footerCell);
        document.add(footerBand);
    }

    /**
     * Helper to create table cell with default styling.
     */
    private PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(10);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(GRAY_BORDER);
        return cell;
    }

    /**
     * Helper to create styled table cell with background color.
     */
    private PdfPCell createStyledCell(String text, Font font, int alignment, Color backgroundColor, boolean hasBorder) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(10);
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(backgroundColor);
        if (hasBorder) {
            cell.setBorderColor(GRAY_BORDER);
            cell.setBorderWidth(0.5f);
        } else {
            cell.setBorder(Rectangle.NO_BORDER);
        }
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
        List<Invoice> received = new java.util.ArrayList<>(
                invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(userId)
        );
        List<Invoice> issued = invoiceRepository.findByIssuerIdOrderByCreatedAtDesc(userId);

        // Merge and sort by date (null-safe)
        received.addAll(issued);
        received.sort((a, b) -> {
            LocalDateTime dateA = a.getCreatedAt();
            LocalDateTime dateB = b.getCreatedAt();
            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1; // nulls last
            if (dateB == null) return -1;
            return dateB.compareTo(dateA);
        });

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
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content with new blue design
            addInvoiceHeader(document, "FACTURE D'ABONNEMENT", invoice.getInvoiceNumber(), invoice.getIssuedAt());
            addTwoColumnParties(document, null, student, true); // Platform as issuer
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
     * Generate invoices for coach payout/transfer.
     * Creates both:
     * 1. PAYOUT_INVOICE: Shows the amount transferred to coach
     * 2. COMMISSION_INVOICE: Shows the commission taken by platform
     */
    @Transactional
    public Invoice generatePayoutInvoice(User teacher, int netAmountCents, String yearMonth, String stripeTransferId) {
        // Calculate gross amount and commission from net amount
        // Net = Gross * (1 - 0.125) = Gross * 0.875
        // Gross = Net / 0.875
        int grossAmountCents = (int) Math.round(netAmountCents / 0.875);
        int totalCommissionCents = grossAmountCents - netAmountCents;
        int platformFeeCents = (int) Math.round(grossAmountCents * 0.10);
        int stripeFeeCents = totalCommissionCents - platformFeeCents;

        // 1. Generate COMMISSION_INVOICE (Platform -> Teacher)
        Invoice commissionInvoice = new Invoice();
        commissionInvoice.setInvoiceNumber(generateInvoiceNumber("COM"));
        commissionInvoice.setInvoiceType(InvoiceType.COMMISSION_INVOICE);
        commissionInvoice.setCustomer(teacher);
        commissionInvoice.setIssuer(null); // Platform is the issuer
        commissionInvoice.setStripePaymentIntentId(stripeTransferId + "-COM");
        commissionInvoice.setSubtotalCents(totalCommissionCents);
        commissionInvoice.setVatCents(0);
        commissionInvoice.setTotalCents(totalCommissionCents);
        commissionInvoice.setVatRate(0);
        commissionInvoice.setCommissionRate(12.5);
        commissionInvoice.setPromoApplied(false);
        commissionInvoice.setDescription("Commissions sur les cours - " + yearMonth);
        commissionInvoice.setStatus("PAID");
        commissionInvoice.setIssuedAt(LocalDateTime.now());
        commissionInvoice = invoiceRepository.save(commissionInvoice);

        // Generate commission PDF
        try {
            generateCommissionInvoicePdfForPayout(commissionInvoice, teacher, yearMonth, grossAmountCents, platformFeeCents, stripeFeeCents);
        } catch (Exception e) {
            log.error("Error generating commission invoice PDF", e);
        }

        log.info("Generated commission invoice #{} for teacher {} - {}€ commission on {}€ gross",
                commissionInvoice.getInvoiceNumber(), teacher.getId(), totalCommissionCents / 100.0, grossAmountCents / 100.0);

        // 2. Generate PAYOUT_INVOICE (for the transfer)
        Invoice payoutInvoice = new Invoice();
        payoutInvoice.setInvoiceNumber(generateInvoiceNumber("VIR"));
        payoutInvoice.setInvoiceType(InvoiceType.PAYOUT_INVOICE);
        payoutInvoice.setCustomer(teacher);
        payoutInvoice.setIssuer(null); // Platform is the issuer
        payoutInvoice.setStripePaymentIntentId(stripeTransferId);
        payoutInvoice.setSubtotalCents(netAmountCents);
        payoutInvoice.setVatCents(0);
        payoutInvoice.setTotalCents(netAmountCents);
        payoutInvoice.setVatRate(0);
        payoutInvoice.setDescription("Virement des gains - " + yearMonth);
        payoutInvoice.setStatus("PAID");
        payoutInvoice.setIssuedAt(LocalDateTime.now());

        payoutInvoice = invoiceRepository.save(payoutInvoice);

        // Generate payout PDF
        try {
            generatePayoutInvoicePdf(payoutInvoice, teacher, yearMonth);
        } catch (Exception e) {
            log.error("Error generating payout invoice PDF", e);
        }

        log.info("Generated payout invoice #{} for teacher {} - {}€ net",
                payoutInvoice.getInvoiceNumber(), teacher.getId(), netAmountCents / 100.0);

        return payoutInvoice;
    }

    /**
     * Generate PDF for commission invoice during payout.
     */
    private void generateCommissionInvoicePdfForPayout(
            Invoice invoice,
            User teacher,
            String yearMonth,
            int grossAmountCents,
            int platformFeeCents,
            int stripeFeeCents
    ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content with new blue design
            addInvoiceHeader(document, "FACTURE DE COMMISSION", invoice.getInvoiceNumber(), invoice.getIssuedAt());
            addTwoColumnParties(document, null, teacher, true); // Platform as issuer

            // Commission table with breakdown
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, WHITE);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 1, 1.5f, 1.5f});
            table.setSpacingBefore(20);

            // Header row with blue background
            String[] headers = {"Description", "Taux", "Base", "Total"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(BLUE_DARK);
                cell.setPadding(10);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(BLUE_DARK);
                table.addCell(cell);
            }

            // Line 1: Platform fee (10%) - white background
            table.addCell(createStyledCell("Frais de mise en relation", normalFont, Element.ALIGN_LEFT, WHITE, true));
            table.addCell(createStyledCell("10%", normalFont, Element.ALIGN_CENTER, WHITE, true));
            table.addCell(createStyledCell(formatCents(grossAmountCents), normalFont, Element.ALIGN_RIGHT, WHITE, true));
            table.addCell(createStyledCell(formatCents(platformFeeCents), normalFont, Element.ALIGN_RIGHT, WHITE, true));

            // Line 2: Stripe fee (2.5%) - alternating light blue background
            table.addCell(createStyledCell("Frais de paiement Stripe", normalFont, Element.ALIGN_LEFT, BLUE_VERY_LIGHT, true));
            table.addCell(createStyledCell("2.5%", normalFont, Element.ALIGN_CENTER, BLUE_VERY_LIGHT, true));
            table.addCell(createStyledCell(formatCents(grossAmountCents), normalFont, Element.ALIGN_RIGHT, BLUE_VERY_LIGHT, true));
            table.addCell(createStyledCell(formatCents(stripeFeeCents), normalFont, Element.ALIGN_RIGHT, BLUE_VERY_LIGHT, true));

            document.add(table);

            // Total section with blue styling
            int totalCommission = platformFeeCents + stripeFeeCents;
            addCommissionTotalSection(document, totalCommission);

            // Period info
            Font infoFont = new Font(Font.HELVETICA, 9, Font.ITALIC, GRAY_LIGHT);
            Paragraph periodInfo = new Paragraph(
                    "Periode : " + yearMonth + " - Montant brut des cours : " + formatCents(grossAmountCents),
                    infoFont
            );
            periodInfo.setSpacingBefore(15);
            document.add(periodInfo);

            addPaymentInfo(document, "Preleve automatiquement sur les paiements des cours");
            addFooter(document);

            document.close();

            // Save PDF
            String pdfPath = savePdf(baos.toByteArray(), invoice.getInvoiceNumber());
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);

        } catch (DocumentException e) {
            log.error("Error creating commission invoice PDF for payout", e);
            throw new IOException("Failed to generate PDF", e);
        }
    }

    /**
     * Generate PDF for payout invoice.
     */
    private void generatePayoutInvoicePdf(Invoice invoice, User teacher, String yearMonth) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content with new blue design
            addInvoiceHeader(document, "RELEVE DE VIREMENT", invoice.getInvoiceNumber(), invoice.getIssuedAt());
            addTwoColumnParties(document, null, teacher, true); // Platform as issuer, teacher as beneficiary
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
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2, 2});
        table.setSpacingBefore(20);

        // Header row with blue background
        String[] headers = {"Description", "Periode", "Montant"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BLUE_DARK);
            cell.setPadding(10);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(BLUE_DARK);
            table.addCell(cell);
        }

        // Data row with white background
        table.addCell(createStyledCell("Virement des gains de cours", normalFont, Element.ALIGN_LEFT, WHITE, true));
        table.addCell(createStyledCell(yearMonth, normalFont, Element.ALIGN_CENTER, WHITE, true));
        table.addCell(createStyledCell(formatCents(invoice.getTotalCents()), normalFont, Element.ALIGN_RIGHT, WHITE, true));

        document.add(table);

        // Total section
        addTotalSection(document, invoice);
    }

    /**
     * Add subscription invoice table.
     */
    private void addSubscriptionInvoiceTable(Document document, Invoice invoice) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 1.5f, 1.5f});
        table.setSpacingBefore(20);

        // Header row with blue background
        String[] headers = {"Description", "Qte", "Prix unitaire", "Total"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BLUE_DARK);
            cell.setPadding(10);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(BLUE_DARK);
            table.addCell(cell);
        }

        // Data row with white background
        table.addCell(createStyledCell(invoice.getDescription(), normalFont, Element.ALIGN_LEFT, WHITE, true));
        table.addCell(createStyledCell("1", normalFont, Element.ALIGN_CENTER, WHITE, true));
        table.addCell(createStyledCell(formatCents(invoice.getTotalCents()), normalFont, Element.ALIGN_RIGHT, WHITE, true));
        table.addCell(createStyledCell(formatCents(invoice.getTotalCents()), normalFont, Element.ALIGN_RIGHT, WHITE, true));

        document.add(table);

        // Total section
        addTotalSection(document, invoice);
    }

    /**
     * Generate invoice for credit top-up.
     */
    @Transactional
    public Invoice generateTopUpInvoice(Long studentId, int amountCents, String stripePaymentIntentId) {
        // Check if invoice already exists for this payment
        if (stripePaymentIntentId != null && invoiceRepository.existsByStripePaymentIntentId(stripePaymentIntentId)) {
            log.info("Top-up invoice already exists for payment intent: {}", stripePaymentIntentId);
            List<Invoice> existing = invoiceRepository.findByStripePaymentIntentId(stripePaymentIntentId);
            return existing.isEmpty() ? null : existing.get(0);
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber("REC"));
        invoice.setInvoiceType(InvoiceType.SUBSCRIPTION); // Using SUBSCRIPTION type for platform payments
        invoice.setCustomer(student);
        invoice.setIssuer(null); // Platform is the issuer
        invoice.setStripePaymentIntentId(stripePaymentIntentId);
        invoice.setSubtotalCents(amountCents);
        invoice.setVatCents(0);
        invoice.setTotalCents(amountCents);
        invoice.setVatRate(0);
        invoice.setDescription("Recharge de crédit Mychess");
        invoice.setStatus("PAID");
        invoice.setIssuedAt(LocalDateTime.now());

        invoice = invoiceRepository.save(invoice);

        // Generate PDF
        try {
            generateTopUpInvoicePdf(invoice, student);
        } catch (Exception e) {
            log.error("Error generating top-up invoice PDF", e);
        }

        log.info("Generated top-up invoice #{} for student {}", invoice.getInvoiceNumber(), studentId);

        return invoice;
    }

    /**
     * Generate PDF for top-up invoice.
     */
    private void generateTopUpInvoicePdf(Invoice invoice, User student) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content with new blue design
            addInvoiceHeader(document, "FACTURE DE RECHARGE", invoice.getInvoiceNumber(), invoice.getIssuedAt());
            addTwoColumnParties(document, null, student, true); // Platform as issuer
            addSubscriptionInvoiceTable(document, invoice); // Reuse subscription table format
            addPaymentInfo(document, "Paye par carte bancaire via Stripe");
            addFooter(document);

            document.close();

            // Save PDF
            String pdfPath = savePdf(baos.toByteArray(), invoice.getInvoiceNumber());
            invoice.setPdfPath(pdfPath);
            invoiceRepository.save(invoice);

        } catch (DocumentException e) {
            log.error("Error creating top-up invoice PDF", e);
            throw new IOException("Failed to generate PDF", e);
        }
    }

    /**
     * Generate invoice for a lesson paid with credit (no Stripe payment).
     * Note: Commission invoice is generated only when coach withdraws earnings.
     */
    @Transactional
    public List<Invoice> generateInvoicesForCreditPayment(
            Long studentId,
            Long teacherId,
            Long lessonId,
            int totalAmountCents
    ) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found: " + teacherId));
        Lesson lesson = lessonId != null ? lessonRepository.findById(lessonId).orElse(null) : null;

        // Generate a unique reference for credit payment (no Stripe ID)
        String creditPaymentRef = "CREDIT-" + lessonId + "-" + System.currentTimeMillis();

        // Generate Invoice: Teacher -> Student (lesson service)
        Invoice lessonInvoice = new Invoice();
        lessonInvoice.setInvoiceNumber(generateInvoiceNumber("FAC"));
        lessonInvoice.setInvoiceType(InvoiceType.LESSON_INVOICE);
        lessonInvoice.setCustomer(student);
        lessonInvoice.setIssuer(teacher);
        lessonInvoice.setLesson(lesson);
        lessonInvoice.setStripePaymentIntentId(creditPaymentRef);
        lessonInvoice.setSubtotalCents(totalAmountCents);
        lessonInvoice.setVatCents(0);
        lessonInvoice.setTotalCents(totalAmountCents);
        lessonInvoice.setVatRate(0);
        lessonInvoice.setDescription("Cours d'echecs - 1 heure (paye par credit)");
        lessonInvoice.setStatus("PAID");
        lessonInvoice.setIssuedAt(LocalDateTime.now());
        lessonInvoice = invoiceRepository.save(lessonInvoice);

        // Generate PDF
        try {
            generateLessonInvoicePdf(lessonInvoice, student, teacher, lesson);
        } catch (Exception e) {
            log.error("Error generating PDF invoice for credit payment", e);
        }

        log.info("Generated lesson invoice #{} for credit payment", lessonInvoice.getInvoiceNumber());

        return List.of(lessonInvoice);
    }

    /**
     * Generate a credit note for wallet refund (no Stripe refund).
     */
    @Transactional
    public Invoice generateCreditNoteForWalletRefund(Lesson lesson, int refundPercentage, int refundAmountCents) {
        return generateCreditNote(lesson, null, refundPercentage, refundAmountCents);
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
            // Create the LESSON_INVOICE first using Payment info
            Payment payment = paymentRepository.findByLessonId(lesson.getId()).orElse(null);
            if (payment == null) {
                log.warn("No payment found for lesson {} - cannot generate credit note", lesson.getId());
                return null;
            }

            log.info("Creating missing LESSON_INVOICE for lesson {} before credit note", lesson.getId());
            originalInvoice = createLessonInvoice(
                    lesson.getStudent(),
                    lesson.getTeacher(),
                    lesson,
                    payment.getStripePaymentIntentId(),
                    payment.getAmountCents()
            );

            // Generate PDF for the original invoice
            try {
                generateLessonInvoicePdf(originalInvoice, lesson.getStudent(), lesson.getTeacher(), lesson);
            } catch (Exception e) {
                log.error("Error generating lesson invoice PDF", e);
            }
        }

        // Check if credit note already exists for this lesson (idempotency for both Stripe and wallet refunds)
        boolean creditNoteExists = lessonInvoices.stream()
                .anyMatch(inv -> inv.getInvoiceType() == InvoiceType.CREDIT_NOTE
                        && inv.getOriginalInvoice() != null
                        && inv.getOriginalInvoice().getInvoiceType() == InvoiceType.LESSON_INVOICE);
        if (creditNoteExists) {
            log.info("Credit note already exists for lesson {} - skipping duplicate", lesson.getId());
            return null;
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

        // Also update the COMMISSION_INVOICE status and create credit note for commission
        Invoice commissionInvoice = lessonInvoices.stream()
                .filter(inv -> inv.getInvoiceType() == InvoiceType.COMMISSION_INVOICE)
                .findFirst()
                .orElse(null);
        if (commissionInvoice != null) {
            // Check if commission credit note already exists
            boolean commissionCreditNoteExists = lessonInvoices.stream()
                    .anyMatch(inv -> inv.getInvoiceType() == InvoiceType.CREDIT_NOTE
                            && inv.getOriginalInvoice() != null
                            && inv.getOriginalInvoice().getInvoiceType() == InvoiceType.COMMISSION_INVOICE);

            if (!commissionCreditNoteExists) {
                // Calculate commission refund amount
                int commissionRefundCents = (commissionInvoice.getTotalCents() * refundPercentage) / 100;

                // Create credit note for commission
                Invoice commissionCreditNote = new Invoice();
                commissionCreditNote.setInvoiceNumber(generateInvoiceNumber("AV"));
                commissionCreditNote.setInvoiceType(InvoiceType.CREDIT_NOTE);
                commissionCreditNote.setCustomer(teacher); // Teacher receives the credit
                commissionCreditNote.setIssuer(null); // Platform is the issuer
                commissionCreditNote.setLesson(lesson);
                commissionCreditNote.setOriginalInvoice(commissionInvoice);
                commissionCreditNote.setRefundPercentage(refundPercentage);
                commissionCreditNote.setSubtotalCents(-commissionRefundCents);
                commissionCreditNote.setVatCents(0);
                commissionCreditNote.setTotalCents(-commissionRefundCents);
                commissionCreditNote.setVatRate(0);

                String commissionRefundDescription = refundPercentage == 100
                        ? "Avoir - Annulation commission plateforme"
                        : String.format("Avoir - Remboursement partiel commission (%d%%)", refundPercentage);
                commissionCreditNote.setDescription(commissionRefundDescription);
                commissionCreditNote.setStatus("REFUNDED");
                commissionCreditNote.setIssuedAt(LocalDateTime.now());

                invoiceRepository.save(commissionCreditNote);
                log.info("Generated commission credit note #{} for lesson {} (-{} cents)",
                        commissionCreditNote.getInvoiceNumber(), lesson.getId(), commissionRefundCents);
            }

            commissionInvoice.setStatus(newStatus);
            invoiceRepository.save(commissionInvoice);
            log.info("Updated commission invoice #{} status to {}", commissionInvoice.getInvoiceNumber(), newStatus);
        }

        // Determine refund method
        boolean isWalletRefund = stripeRefundId == null;

        // Generate PDF for credit note
        try {
            generateCreditNotePdf(creditNote, student, teacher, lesson, originalInvoice, isWalletRefund);
        } catch (Exception e) {
            log.error("Error generating credit note PDF", e);
        }

        log.info("Generated credit note #{} for lesson {} ({}% refund, {})",
                creditNote.getInvoiceNumber(), lesson.getId(), refundPercentage,
                isWalletRefund ? "wallet" : "Stripe");

        return creditNote;
    }

    /**
     * Generate PDF for credit note.
     */
    private void generateCreditNotePdf(Invoice creditNote, User student, User teacher,
                                        Lesson lesson, Invoice originalInvoice, boolean isWalletRefund) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add content with new blue design
            addInvoiceHeader(document, "AVOIR", creditNote.getInvoiceNumber(), creditNote.getIssuedAt());
            addTwoColumnParties(document, teacher, student, false);

            // Reference to original invoice with blue styling
            Font refLabelFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);
            Font refValueFont = new Font(Font.HELVETICA, 10, Font.BOLD, BLUE_DARK);
            Paragraph refParagraph = new Paragraph();
            refParagraph.add(new Chunk("Reference facture originale : ", refLabelFont));
            refParagraph.add(new Chunk(originalInvoice.getInvoiceNumber(), refValueFont));
            refParagraph.setSpacingBefore(10);
            refParagraph.setSpacingAfter(5);
            document.add(refParagraph);

            addCreditNoteTable(document, creditNote, lesson);
            addCreditNoteTotalSection(document, creditNote);
            String paymentMethod = isWalletRefund
                    ? "Remboursement credite sur le portefeuille"
                    : "Remboursement via Stripe";
            addPaymentInfo(document, paymentMethod);
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
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1.5f, 1.5f, 1.5f});
        table.setSpacingBefore(20);

        // Header row with blue background
        String[] headers = {"Description", "Qte", "Prix unitaire", "Total"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BLUE_DARK);
            cell.setPadding(10);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(BLUE_DARK);
            table.addCell(cell);
        }

        // Data row - show negative amount for credit with white background
        String description = creditNote.getDescription();
        int refundAmount = Math.abs(creditNote.getTotalCents());

        table.addCell(createStyledCell(description, normalFont, Element.ALIGN_LEFT, WHITE, true));
        table.addCell(createStyledCell("1", normalFont, Element.ALIGN_CENTER, WHITE, true));
        table.addCell(createStyledCell("-" + formatCents(refundAmount), normalFont, Element.ALIGN_RIGHT, WHITE, true));
        table.addCell(createStyledCell("-" + formatCents(refundAmount), normalFont, Element.ALIGN_RIGHT, WHITE, true));

        document.add(table);
    }

    /**
     * Add credit note total section (negative amounts).
     */
    private void addCreditNoteTotalSection(Document document, Invoice creditNote) throws DocumentException {
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);
        Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setSpacingBefore(15);

        int refundAmount = Math.abs(creditNote.getTotalCents());

        // Subtotal
        PdfPCell subtotalLabel = new PdfPCell(new Phrase("Sous-total HT", normalFont));
        subtotalLabel.setBorder(Rectangle.NO_BORDER);
        subtotalLabel.setPadding(8);
        totalTable.addCell(subtotalLabel);

        PdfPCell subtotalValue = new PdfPCell(new Phrase("-" + formatCents(refundAmount), normalFont));
        subtotalValue.setBorder(Rectangle.NO_BORDER);
        subtotalValue.setPadding(8);
        subtotalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.addCell(subtotalValue);

        // TVA
        PdfPCell vatLabel = new PdfPCell(new Phrase("TVA (0%)", normalFont));
        vatLabel.setBorder(Rectangle.NO_BORDER);
        vatLabel.setPadding(8);
        totalTable.addCell(vatLabel);

        PdfPCell vatValue = new PdfPCell(new Phrase("0,00 EUR", normalFont));
        vatValue.setBorder(Rectangle.NO_BORDER);
        vatValue.setPadding(8);
        vatValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.addCell(vatValue);

        document.add(totalTable);

        // Total - highlighted with amber/orange color for refund (rounded corners)
        Color refundBgColor = new Color(254, 243, 199);  // Light amber
        Color refundTextColor = new Color(180, 83, 9);   // Amber-700
        Font refundBoldFont = new Font(Font.HELVETICA, 14, Font.BOLD, refundTextColor);

        PdfPTable totalRow = new PdfPTable(2);
        totalRow.setWidthPercentage(50);
        totalRow.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalRow.setSpacingBefore(5);
        totalRow.setTableEvent(new RoundedTableEvent(refundBgColor, BORDER_RADIUS));

        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL REMBOURSE", new Font(Font.HELVETICA, 12, Font.BOLD, refundTextColor)));
        totalLabelCell.setBorder(Rectangle.NO_BORDER);
        totalLabelCell.setPadding(12);
        totalLabelCell.setBackgroundColor(null);
        totalRow.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase("-" + formatCents(refundAmount), refundBoldFont));
        totalValueCell.setBorder(Rectangle.NO_BORDER);
        totalValueCell.setPadding(12);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setBackgroundColor(null);
        totalRow.addCell(totalValueCell);

        document.add(totalRow);
    }
}
