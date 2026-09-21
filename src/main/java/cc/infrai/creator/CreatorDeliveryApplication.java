package cc.infrai.creator;

public final class CreatorDeliveryApplication {
    private CreatorDeliveryApplication() {
    }

    public static void main(String[] args) {
        InfraiProperties properties = InfraiProperties.fromEnvironment();
        CreatorDeliveryService service = new CreatorDeliveryService(properties);
        DeliveryReceipt receipt = service.deliver("asset-2026-09", "Mina", "The signed digital workbook is available today.");
        System.out.println(receipt.assetId() + " notification=" + receipt.subscriberUpdateSent()
                + " tokens=" + receipt.countedTokens());
    }
}
