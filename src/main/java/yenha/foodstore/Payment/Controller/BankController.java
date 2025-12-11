package yenha.foodstore.Payment.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yenha.foodstore.Payment.DTO.BankDTO;
import yenha.foodstore.Payment.DTO.BankRequestDTO;
import yenha.foodstore.Payment.DTO.QRCodeRequestDTO;
import yenha.foodstore.Payment.DTO.QRCodeResponseDTO;
import yenha.foodstore.Payment.Entity.Status;
import yenha.foodstore.Payment.Service.BankService;

import java.util.List;

@RestController
@RequestMapping("/api/banks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    /**
     * Create a new bank account with optional QR code image
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BankDTO> createBank(
            @RequestPart("bank") BankRequestDTO requestDTO,
            @RequestPart(value = "qrCodeImage", required = false) MultipartFile qrCodeImage) {
        try {
            BankDTO createdBank = bankService.createBank(requestDTO, qrCodeImage);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBank);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all bank accounts
     */
    @GetMapping
    public ResponseEntity<List<BankDTO>> getAllBanks() {
        try {
            List<BankDTO> banks = bankService.getAllBanks();
            return ResponseEntity.ok(banks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active bank accounts (convenient endpoint)
     */
    @GetMapping("/active")
    public ResponseEntity<List<BankDTO>> getActiveBanks() {
        try {
            List<BankDTO> banks = bankService.getBanksByStatus(Status.ACTIVE);
            return ResponseEntity.ok(banks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get bank accounts by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<BankDTO>> getBanksByStatus(@PathVariable Status status) {
        try {
            List<BankDTO> banks = bankService.getBanksByStatus(status);
            return ResponseEntity.ok(banks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get bank account by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BankDTO> getBankById(@PathVariable Long id) {
        try {
            BankDTO bank = bankService.getBankById(id);
            return ResponseEntity.ok(bank);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update bank account with optional QR code image
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BankDTO> updateBank(
            @PathVariable Long id,
            @RequestPart("bank") BankRequestDTO requestDTO,
            @RequestPart(value = "qrCodeImage", required = false) MultipartFile qrCodeImage) {
        try {
            BankDTO updatedBank = bankService.updateBank(id, requestDTO, qrCodeImage);
            return ResponseEntity.ok(updatedBank);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update bank status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<BankDTO> updateBankStatus(
            @PathVariable Long id,
            @RequestParam Status status) {
        try {
            BankDTO updatedBank = bankService.updateBankStatus(id, status);
            return ResponseEntity.ok(updatedBank);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate VietQR payment URL
     * Example: POST /api/banks/generate-qr
     * Body: { "amount": 30000, "addInfo": "Thanh toan HD001" }
     */
    @PostMapping("/generate-qr")
    public ResponseEntity<QRCodeResponseDTO> generateQRCode(@RequestBody QRCodeRequestDTO requestDTO) {
        try {
            QRCodeResponseDTO response = bankService.generateQRCode(requestDTO);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
