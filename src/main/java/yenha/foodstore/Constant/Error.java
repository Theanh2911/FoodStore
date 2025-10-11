package yenha.foodstore.Constant;

public class Error {

    public static final String CATEGORY_NOT_FOUND = "Category not found";

    public static final String ORDER_NOT_FOUND = "Order not found";

    public static final String PRODUCT_NOT_FOUND = "Product not found";
    public static final String PRODUCT_CANNOT_BE_DELETED = "This product is appeared in a order so it cannot be deleted";
    public static final String PRODUCT_NAME_REQUIRE = "Product name is required";
    public static final String PRODUCT_PRICE_NEGATIVE = "Product price must be greater than or equal to 0";

    public static final String ORDER_CUSTOMER_NAME_BLANK = "Customer name in order is required";
    public static final String ORDER_TOTAL_INVALID = "Total amount in order must be greater than 0";
    public static final String ORDER_ITEMS_EMPTY = "Order must contain at least one item";
    public static final String ORDER_ITEM_PRODUCT_ID_NULL = "Product ID is required for each item in order";
    public static final String ORDER_ITEM_QUANTITY_INVALID = "Quantity must be greater than 0 for each item in order";
}
