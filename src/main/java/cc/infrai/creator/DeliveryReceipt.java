package cc.infrai.creator;

public record DeliveryReceipt(String assetId, boolean subscriberUpdateSent, int countedTokens, String copy) {
}
