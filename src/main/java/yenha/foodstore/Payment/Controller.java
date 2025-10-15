package yenha.foodstore.Payment;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
public class Controller {

    // Thông tin tài khoản cố định
    private static final String BANK_ID = "970422"; // MB Bank (MBB)
    private static final String ACCOUNT_NO = "696291102";
    private static final String ACCOUNT_NAME = "NGUYEN THE ANH";

    @GetMapping("/qr")
    public ResponseEntity<String> generateQr(
            @RequestParam long amount,
            @RequestParam String addInfo) {

        try {
            String encodedAddInfo = URLEncoder.encode(addInfo, StandardCharsets.UTF_8.toString());
            String encodedName = URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8.toString());

            String qrUrl = String.format(
                    "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                    BANK_ID, ACCOUNT_NO, amount, encodedAddInfo, encodedName
            );

            return ResponseEntity.ok(qrUrl);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error generating QR: " + e.getMessage());
        }
    }
}
