package com.chessconnect.service;

import com.chessconnect.model.User;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Transfer;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.TransferCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeConnectService {

    private static final Logger log = LoggerFactory.getLogger(StripeConnectService.class);

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Create a Stripe Express Connect account for a teacher.
     * For France, we must use minimal parameters and let Stripe collect all info via hosted onboarding.
     */
    public String createConnectAccount(User teacher) throws StripeException {
        // For France (and other regulated countries), create minimal account
        // All user info will be collected via Stripe-hosted onboarding
        AccountCreateParams params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .putMetadata("user_id", teacher.getId().toString())
                .setCapabilities(
                        AccountCreateParams.Capabilities.builder()
                                .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                                        .setRequested(true)
                                        .build())
                                .build()
                )
                .build();

        Account account = Account.create(params);
        log.info("Created Stripe Connect account {} for teacher {}", account.getId(), teacher.getId());
        return account.getId();
    }

    /**
     * Create an account link for the onboarding flow.
     * The teacher will be redirected to Stripe to complete their account setup.
     */
    public String createAccountLink(String accountId, String refreshUrl, String returnUrl) throws StripeException {
        AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setRefreshUrl(refreshUrl)
                .setReturnUrl(returnUrl)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();

        AccountLink accountLink = AccountLink.create(params);
        log.info("Created account link for Stripe Connect account {}", accountId);
        return accountLink.getUrl();
    }

    /**
     * Create an onboarding URL for a teacher.
     * Returns both the URL and the account ID (in case a new account was created).
     */
    public OnboardingResult createOnboardingUrl(User teacher) throws StripeException {
        String accountId = teacher.getStripeConnectAccountId();
        boolean newAccount = false;

        // Create account if it doesn't exist
        if (accountId == null || accountId.isBlank()) {
            accountId = createConnectAccount(teacher);
            newAccount = true;
        }

        String refreshUrl = frontendUrl + "/settings?stripe_connect=refresh";
        String returnUrl = frontendUrl + "/settings?stripe_connect=return";

        String url = createAccountLink(accountId, refreshUrl, returnUrl);
        return new OnboardingResult(url, accountId, newAccount);
    }

    /**
     * Result of onboarding URL creation.
     */
    public record OnboardingResult(String url, String accountId, boolean newAccount) {}

    /**
     * Check if a Stripe Connect account is fully onboarded and ready to receive transfers.
     */
    public boolean isAccountReady(String accountId) throws StripeException {
        if (accountId == null || accountId.isBlank()) {
            return false;
        }

        Account account = Account.retrieve(accountId);

        // Check if the account can receive payouts
        boolean chargesEnabled = account.getChargesEnabled() != null && account.getChargesEnabled();
        boolean payoutsEnabled = account.getPayoutsEnabled() != null && account.getPayoutsEnabled();
        boolean detailsSubmitted = account.getDetailsSubmitted() != null && account.getDetailsSubmitted();

        log.debug("Stripe Connect account {} status: chargesEnabled={}, payoutsEnabled={}, detailsSubmitted={}",
                accountId, chargesEnabled, payoutsEnabled, detailsSubmitted);

        // For transfers to work, we need payouts to be enabled and details submitted
        return payoutsEnabled && detailsSubmitted;
    }

    /**
     * Get the status details of a Stripe Connect account.
     */
    public AccountStatus getAccountStatus(String accountId) throws StripeException {
        if (accountId == null || accountId.isBlank()) {
            return new AccountStatus(false, false, false, false, null);
        }

        Account account = Account.retrieve(accountId);

        boolean chargesEnabled = account.getChargesEnabled() != null && account.getChargesEnabled();
        boolean payoutsEnabled = account.getPayoutsEnabled() != null && account.getPayoutsEnabled();
        boolean detailsSubmitted = account.getDetailsSubmitted() != null && account.getDetailsSubmitted();
        boolean isReady = payoutsEnabled && detailsSubmitted;

        // Get any pending requirements
        String pendingReason = null;
        if (account.getRequirements() != null &&
            account.getRequirements().getCurrentlyDue() != null &&
            !account.getRequirements().getCurrentlyDue().isEmpty()) {
            pendingReason = "Documents ou informations requises";
        }

        return new AccountStatus(true, isReady, chargesEnabled, payoutsEnabled, pendingReason);
    }

    /**
     * Create a transfer to a connected account.
     * This moves funds from the platform's Stripe balance to the connected account.
     */
    public Transfer createTransfer(String connectedAccountId, long amountCents, String description) throws StripeException {
        TransferCreateParams params = TransferCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency("eur")
                .setDestination(connectedAccountId)
                .setDescription(description)
                .build();

        Transfer transfer = Transfer.create(params);
        log.info("Created transfer {} of {} cents to account {}",
                transfer.getId(), amountCents, connectedAccountId);
        return transfer;
    }

    /**
     * Create a transfer for a teacher payout.
     */
    public Transfer payTeacher(User teacher, long amountCents, String yearMonth) throws StripeException {
        if (teacher.getStripeConnectAccountId() == null) {
            throw new IllegalStateException("Le coach n'a pas de compte Stripe Connect configure");
        }

        if (!isAccountReady(teacher.getStripeConnectAccountId())) {
            throw new IllegalStateException("Le compte Stripe Connect du coach n'est pas pret a recevoir des paiements");
        }

        String description = String.format("Paiement mychess - %s %s - %s",
                teacher.getFirstName(), teacher.getLastName(), yearMonth);

        return createTransfer(teacher.getStripeConnectAccountId(), amountCents, description);
    }

    /**
     * Account status record.
     */
    public record AccountStatus(
            boolean accountExists,
            boolean isReady,
            boolean chargesEnabled,
            boolean payoutsEnabled,
            String pendingReason
    ) {}
}
