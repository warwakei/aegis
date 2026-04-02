package rich.util.modules.autobuy;

public class BuyRequest {
    public int price;
    public String itemId;
    public String displayName;
    public int count;
    public String loreHash;
    public int maxPrice;
    public int minQuantity;

    public BuyRequest(int price, String itemId, String displayName, int count, String loreHash, int maxPrice, int minQuantity) {
        this.price = price;
        this.itemId = itemId;
        this.displayName = displayName;
        this.count = count;
        this.loreHash = loreHash;
        this.maxPrice = maxPrice;
        this.minQuantity = minQuantity;
    }
}