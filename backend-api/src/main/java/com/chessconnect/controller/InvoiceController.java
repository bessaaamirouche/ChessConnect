package com.chessconnect.controller;

import com.chessconnect.model.Invoice;
import com.chessconnect.repository.InvoiceRepository;
import com.chessconnect.security.UserDetailsImpl;
import com.chessconnect.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;

    public InvoiceController(InvoiceService invoiceService, InvoiceRepository invoiceRepository) {
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Get all invoices for the current user (both received and issued).
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getMyInvoices(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<Invoice> invoices = invoiceService.getInvoicesForUser(userDetails.getId());

        List<Map<String, Object>> response = invoices.stream()
                .map(inv -> mapInvoiceToResponse(inv, userDetails.getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get invoices received by the current user (as customer).
     */
    @GetMapping("/received")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getReceivedInvoices(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<Invoice> invoices = invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(userDetails.getId());

        List<Map<String, Object>> response = invoices.stream()
                .map(inv -> mapInvoiceToResponse(inv, userDetails.getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get invoices issued by the current user (as teacher).
     */
    @GetMapping("/issued")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<Map<String, Object>>> getIssuedInvoices(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<Invoice> invoices = invoiceRepository.findByIssuerIdOrderByCreatedAtDesc(userDetails.getId());

        List<Map<String, Object>> response = invoices.stream()
                .map(inv -> mapInvoiceToResponse(inv, userDetails.getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all invoices (admin only).
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllInvoices() {
        List<Invoice> invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();

        List<Map<String, Object>> response = invoices.stream()
                .map(this::mapInvoiceToResponseAdmin)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Download invoice PDF.
     */
    @GetMapping("/{invoiceId}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            byte[] pdfBytes = invoiceService.getInvoicePdf(invoiceId, userDetails.getId());

            Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
            String filename = invoice.getInvoiceNumber().replace("/", "-") + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Error downloading invoice PDF: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get a specific invoice details.
     */
    @GetMapping("/{invoiceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getInvoice(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Security check
        boolean isCustomer = invoice.getCustomer().getId().equals(userDetails.getId());
        boolean isIssuer = invoice.getIssuer() != null && invoice.getIssuer().getId().equals(userDetails.getId());

        if (!isCustomer && !isIssuer) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        return ResponseEntity.ok(mapInvoiceToResponse(invoice, userDetails.getId()));
    }

    /**
     * Map Invoice entity to API response.
     */
    private Map<String, Object> mapInvoiceToResponse(Invoice invoice, Long currentUserId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        boolean isReceived = invoice.getCustomer().getId().equals(currentUserId);

        String issuerName;
        if (invoice.getIssuer() != null) {
            issuerName = invoice.getIssuer().getFirstName() + " " + invoice.getIssuer().getLastName();
            if (invoice.getIssuer().getCompanyName() != null) {
                issuerName = invoice.getIssuer().getCompanyName();
            }
        } else {
            issuerName = "mychess";
        }

        String customerName = invoice.getCustomer().getFirstName() + " " + invoice.getCustomer().getLastName();
        if (invoice.getCustomer().getCompanyName() != null && !invoice.getCustomer().getCompanyName().isBlank()) {
            customerName = invoice.getCustomer().getCompanyName();
        }

        return Map.ofEntries(
                Map.entry("id", invoice.getId()),
                Map.entry("invoiceNumber", invoice.getInvoiceNumber()),
                Map.entry("invoiceType", invoice.getInvoiceType().name()),
                Map.entry("isReceived", isReceived),
                Map.entry("issuerName", issuerName),
                Map.entry("customerName", customerName),
                Map.entry("description", invoice.getDescription()),
                Map.entry("subtotalCents", invoice.getSubtotalCents()),
                Map.entry("vatCents", invoice.getVatCents()),
                Map.entry("totalCents", invoice.getTotalCents()),
                Map.entry("vatRate", invoice.getVatRate() != null ? invoice.getVatRate() : 0),
                Map.entry("commissionRate", invoice.getCommissionRate() != null ? invoice.getCommissionRate() : 0),
                Map.entry("promoApplied", invoice.getPromoApplied() != null && invoice.getPromoApplied()),
                Map.entry("status", invoice.getStatus()),
                Map.entry("hasPdf", invoice.getPdfPath() != null),
                Map.entry("issuedAt", invoice.getIssuedAt().format(formatter)),
                Map.entry("createdAt", invoice.getCreatedAt().format(formatter)),
                Map.entry("lessonId", invoice.getLesson() != null ? invoice.getLesson().getId() : null)
        );
    }

    /**
     * Map Invoice entity to API response for admin (no currentUserId needed).
     */
    private Map<String, Object> mapInvoiceToResponseAdmin(Invoice invoice) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        String issuerName;
        if (invoice.getIssuer() != null) {
            issuerName = invoice.getIssuer().getFirstName() + " " + invoice.getIssuer().getLastName();
            if (invoice.getIssuer().getCompanyName() != null) {
                issuerName = invoice.getIssuer().getCompanyName();
            }
        } else {
            issuerName = "mychess";
        }

        String customerName = invoice.getCustomer().getFirstName() + " " + invoice.getCustomer().getLastName();
        if (invoice.getCustomer().getCompanyName() != null && !invoice.getCustomer().getCompanyName().isBlank()) {
            customerName = invoice.getCustomer().getCompanyName();
        }

        return Map.ofEntries(
                Map.entry("id", invoice.getId()),
                Map.entry("invoiceNumber", invoice.getInvoiceNumber()),
                Map.entry("invoiceType", invoice.getInvoiceType().name()),
                Map.entry("isReceived", false),
                Map.entry("issuerName", issuerName),
                Map.entry("customerName", customerName),
                Map.entry("description", invoice.getDescription()),
                Map.entry("subtotalCents", invoice.getSubtotalCents()),
                Map.entry("vatCents", invoice.getVatCents()),
                Map.entry("totalCents", invoice.getTotalCents()),
                Map.entry("vatRate", invoice.getVatRate() != null ? invoice.getVatRate() : 0),
                Map.entry("commissionRate", invoice.getCommissionRate() != null ? invoice.getCommissionRate() : 0),
                Map.entry("promoApplied", invoice.getPromoApplied() != null && invoice.getPromoApplied()),
                Map.entry("status", invoice.getStatus()),
                Map.entry("hasPdf", invoice.getPdfPath() != null),
                Map.entry("issuedAt", invoice.getIssuedAt().format(formatter)),
                Map.entry("createdAt", invoice.getCreatedAt().format(formatter)),
                Map.entry("lessonId", invoice.getLesson() != null ? invoice.getLesson().getId() : null)
        );
    }
}
