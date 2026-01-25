package com.chessconnect.dto.wallet;

import com.chessconnect.model.StudentWallet;

public record WalletResponse(
        Long id,
        Integer balanceCents,
        Integer totalTopUpsCents,
        Integer totalUsedCents,
        Integer totalRefundedCents
) {
    public static WalletResponse from(StudentWallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalanceCents(),
                wallet.getTotalTopUpsCents(),
                wallet.getTotalUsedCents(),
                wallet.getTotalRefundedCents()
        );
    }

    public static WalletResponse empty() {
        return new WalletResponse(null, 0, 0, 0, 0);
    }
}
