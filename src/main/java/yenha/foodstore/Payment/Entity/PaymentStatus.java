package yenha.foodstore.Payment.Entity;

public enum PaymentStatus {
    PENDING,           // Đang chờ thanh toán
    SUCCESS,           // Thanh toán thành công
    FAILED,            // Thanh toán thất bại
    AMOUNT_MISMATCH,   // Số tiền không khớp
    ORDER_NOT_FOUND    // Không tìm thấy đơn hàng
}

