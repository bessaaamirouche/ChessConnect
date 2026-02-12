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
        try {
            List<Invoice> invoices = invoiceService.getInvoicesForUser(userDetails.getId());

            List<Map<String, Object>> response = invoices.stream()
                    .map(inv -> {
                        try {
                            return mapInvoiceToResponse(inv, userDetails.getId());
                        } catch (Exception e) {
                            log.error("Error mapping invoice {}: {}", inv.getId(), e.getMessage(), e);
                            // Return a minimal response for problematic invoices
                            return Map.<String, Object>ofEntries(
                                    Map.entry("id", inv.getId()),
                                    Map.entry("invoiceNumber", inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "N/A"),
                                    Map.entry("invoiceType", inv.getInvoiceType() != null ? inv.getInvoiceType().name() : ""),
                                    Map.entry("isReceived", false),
                                    Map.entry("issuerName", ""),
                                    Map.entry("customerName", ""),
                                    Map.entry("description", inv.getDescription() != null ? inv.getDescription() : "Erreur de chargement"),
                                    Map.entry("subtotalCents", inv.getSubtotalCents() != null ? inv.getSubtotalCents() : 0),
                                    Map.entry("vatCents", inv.getVatCents() != null ? inv.getVatCents() : 0),
                                    Map.entry("totalCents", inv.getTotalCents() != null ? inv.getTotalCents() : 0),
                                    Map.entry("vatRate", 0),
                                    Map.entry("commissionRate", 0),
                                    Map.entry("promoApplied", false),
                                    Map.entry("status", inv.getStatus() != null ? inv.getStatus() : "PENDING"),
                                    Map.entry("hasPdf", false),
                                    Map.entry("issuedAt", ""),
                                    Map.entry("createdAt", "")
                            );
                        }
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching invoices for user {}: {}", userDetails.getId(), e.getMessage(), e);
            return ResponseEntity.ok(List.of()); // Return empty list instead of 500
        }
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
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            byte[] pdfBytes = invoiceService.getInvoicePdf(invoiceId, userDetails.getId(), isAdmin);

            Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
            String filename = invoice.getInvoiceNumber().replace("/", "-") + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Error downloading invoice PDF: {}", e.getMessage(), e);
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
        boolean isCustomer = invoice.getCustomer() != null && invoice.getCustomer().getId().equals(userDetails.getId());
        boolean isIssuer = invoice.getIssuer() != null && invoice.getIssuer().getId().equals(userDetails.getId());

        if (!isCustomer && !isIssuer) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        return ResponseEntity.ok(mapInvoiceToResponse(invoice, userDetails.getId()));
    }

    /**
     * Map Invoice entity to API response.
     * Uses denormalized fields as fallback when user accounts have been deleted.
     */
    private Map<String, Object> mapInvoiceToResponse(Invoice invoice, Long currentUserId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        boolean isReceived = invoice.getCustomer() != null && invoice.getCustomer().getId().equals(currentUserId);

        String issuerName;
        if (invoice.getIssuer() != null) {
            issuerName = invoice.getIssuer().getFirstName() + " " + invoice.getIssuer().getLastName();
            if (invoice.getIssuer().getCompanyName() != null) {
                issuerName = invoice.getIssuer().getCompanyName();
            }
        } else if (invoice.getIssuerName() != null) {
            issuerName = invoice.getIssuerName();
        } else {
            issuerName = "mychess";
        }

        String customerName = "Inconnu";
        if (invoice.getCustomer() != null) {
            customerName = invoice.getCustomer().getFirstName() + " " + invoice.getCustomer().getLastName();
            if (invoice.getCustomer().getCompanyName() != null && !invoice.getCustomer().getCompanyName().isBlank()) {
                customerName = invoice.getCustomer().getCompanyName();
            }
        } else if (invoice.getCustomerName() != null) {
            customerName = invoice.getCustomerName();
        }

        // Handle potentially null dates
        String issuedAtStr = invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(formatter) : "";
        String createdAtStr = invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(formatter) : "";

        return Map.ofEntries(
                Map.entry("id", invoice.getId()),
                Map.entry("invoiceNumber", invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : ""),
                Map.entry("invoiceType", invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : ""),
                Map.entry("isReceived", isReceived),
                Map.entry("issuerName", issuerName),
                Map.entry("customerName", customerName),
                Map.entry("description", invoice.getDescription() != null ? invoice.getDescription() : ""),
                Map.entry("subtotalCents", invoice.getSubtotalCents() != null ? invoice.getSubtotalCents() : 0),
                Map.entry("vatCents", invoice.getVatCents() != null ? invoice.getVatCents() : 0),
                Map.entry("totalCents", invoice.getTotalCents() != null ? invoice.getTotalCents() : 0),
                Map.entry("vatRate", invoice.getVatRate() != null ? invoice.getVatRate() : 0),
                Map.entry("commissionRate", invoice.getCommissionRate() != null ? invoice.getCommissionRate() : 0),
                Map.entry("promoApplied", invoice.getPromoApplied() != null && invoice.getPromoApplied()),
                Map.entry("status", invoice.getStatus() != null ? invoice.getStatus() : ""),
                Map.entry("hasPdf", true), // PDFs can now be generated on-demand
                Map.entry("issuedAt", issuedAtStr),
                Map.entry("createdAt", createdAtStr),
                Map.entry("lessonId", invoice.getLesson() != null ? invoice.getLesson().getId() : null)
        );
    }

    /**
     * Map Invoice entity to API response for admin (no currentUserId needed).
     * Uses denormalized fields as fallback when user accounts have been deleted.
     */
    private Map<String, Object> mapInvoiceToResponseAdmin(Invoice invoice) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        String issuerName;
        if (invoice.getIssuer() != null) {
            issuerName = invoice.getIssuer().getFirstName() + " " + invoice.getIssuer().getLastName();
            if (invoice.getIssuer().getCompanyName() != null) {
                issuerName = invoice.getIssuer().getCompanyName();
            }
        } else if (invoice.getIssuerName() != null) {
            issuerName = invoice.getIssuerName();
        } else {
            issuerName = "mychess";
        }

        String customerName = "Inconnu";
        if (invoice.getCustomer() != null) {
            customerName = invoice.getCustomer().getFirstName() + " " + invoice.getCustomer().getLastName();
            if (invoice.getCustomer().getCompanyName() != null && !invoice.getCustomer().getCompanyName().isBlank()) {
                customerName = invoice.getCustomer().getCompanyName();
            }
        } else if (invoice.getCustomerName() != null) {
            customerName = invoice.getCustomerName();
        }

        // Handle potentially null dates
        String issuedAtStr = invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(formatter) : "";
        String createdAtStr = invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(formatter) : "";

        return Map.ofEntries(
                Map.entry("id", invoice.getId()),
                Map.entry("invoiceNumber", invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : ""),
                Map.entry("invoiceType", invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : ""),
                Map.entry("isReceived", false),
                Map.entry("issuerName", issuerName),
                Map.entry("customerName", customerName),
                Map.entry("description", invoice.getDescription() != null ? invoice.getDescription() : ""),
                Map.entry("subtotalCents", invoice.getSubtotalCents() != null ? invoice.getSubtotalCents() : 0),
                Map.entry("vatCents", invoice.getVatCents() != null ? invoice.getVatCents() : 0),
                Map.entry("totalCents", invoice.getTotalCents() != null ? invoice.getTotalCents() : 0),
                Map.entry("vatRate", invoice.getVatRate() != null ? invoice.getVatRate() : 0),
                Map.entry("commissionRate", invoice.getCommissionRate() != null ? invoice.getCommissionRate() : 0),
                Map.entry("promoApplied", invoice.getPromoApplied() != null && invoice.getPromoApplied()),
                Map.entry("status", invoice.getStatus() != null ? invoice.getStatus() : ""),
                Map.entry("hasPdf", true), // PDFs can now be generated on-demand
                Map.entry("issuedAt", issuedAtStr),
                Map.entry("createdAt", createdAtStr),
                Map.entry("lessonId", invoice.getLesson() != null ? invoice.getLesson().getId() : null)
        );
    }
}
