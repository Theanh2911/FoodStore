package yenha.foodstore.Payment.Service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yenha.foodstore.Menu.Service.S3Service;
import yenha.foodstore.Payment.DTO.BankDTO;
import yenha.foodstore.Payment.DTO.BankRequestDTO;
import yenha.foodstore.Payment.DTO.QRCodeRequestDTO;
import yenha.foodstore.Payment.DTO.QRCodeResponseDTO;
import yenha.foodstore.Payment.Entity.Bank;
import yenha.foodstore.Payment.Entity.Status;
import yenha.foodstore.Payment.Repository.BankRepository;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankService {

    private static final Logger logger = LoggerFactory.getLogger(BankService.class);
    
    private final BankRepository bankRepository;
    private final S3Service s3Service;

    @Transactional
    public BankDTO createBank(BankRequestDTO requestDTO, MultipartFile qrCodeImage) {
        logger.info("Creating new bank account for: {}", requestDTO.getBankName());

        bankRepository.findByAccountNumber(requestDTO.getAccountNumber())
                .ifPresent(bank -> {
                    throw new IllegalArgumentException("Account number already exists");
                });
        
        String qrCodeImageUrl = null;
        if (qrCodeImage != null && !qrCodeImage.isEmpty()) {
            logger.info("Uploading QR code image to S3");
            qrCodeImageUrl = s3Service.uploadFile(qrCodeImage);
        }
        
        Bank bank = Bank.builder()
                .bankName(requestDTO.getBankName())
                .accountNumber(requestDTO.getAccountNumber())
                .accountHolder(requestDTO.getAccountHolder())
                .qrCodeImageUrl(qrCodeImageUrl)
                .status(requestDTO.getStatus() != null ? requestDTO.getStatus() : Status.ACTIVE)
                .build();
        
        Bank savedBank = bankRepository.save(bank);
        logger.info("Bank account created successfully with ID: {}", savedBank.getId());
        
        return convertToDTO(savedBank);
    }

    @Transactional(readOnly = true)
    public List<BankDTO> getAllBanks() {
        logger.info("Retrieving all bank accounts");
        return bankRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BankDTO> getBanksByStatus(Status status) {
        logger.info("Retrieving bank accounts by status: {}", status);
        return bankRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BankDTO getBankById(Long id) {
        logger.info("Retrieving bank account with ID: {}", id);
        Bank bank = bankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found with ID: " + id));
        return convertToDTO(bank);
    }

    @Transactional
    public BankDTO updateBank(Long id, BankRequestDTO requestDTO, MultipartFile qrCodeImage) {
        logger.info("Updating bank account with ID: {}", id);
        
        Bank bank = bankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found with ID: " + id));
        
        // Check if account number is being changed and if it already exists
        if (!bank.getAccountNumber().equals(requestDTO.getAccountNumber())) {
            bankRepository.findByAccountNumber(requestDTO.getAccountNumber())
                    .ifPresent(existingBank -> {
                        throw new IllegalArgumentException("Account number already exists");
                    });
        }
        
        // Update basic fields
        bank.setBankName(requestDTO.getBankName());
        bank.setAccountNumber(requestDTO.getAccountNumber());
        bank.setAccountHolder(requestDTO.getAccountHolder());
        bank.setStatus(requestDTO.getStatus());
        
        // Handle QR code image update
        if (qrCodeImage != null && !qrCodeImage.isEmpty()) {
            logger.info("Updating QR code image");
            // Delete old image if exists
            if (bank.getQrCodeImageUrl() != null && !bank.getQrCodeImageUrl().isEmpty()) {
                s3Service.deleteFile(bank.getQrCodeImageUrl());
            }
            // Upload new image
            String newQrCodeImageUrl = s3Service.uploadFile(qrCodeImage);
            bank.setQrCodeImageUrl(newQrCodeImageUrl);
        }
        
        Bank updatedBank = bankRepository.save(bank);
        logger.info("Bank account updated successfully with ID: {}", updatedBank.getId());
        
        return convertToDTO(updatedBank);
    }

    @Transactional
    public BankDTO updateQRCodeImage(Long id, MultipartFile qrCodeImage) {
        logger.info("Updating QR code image for bank ID: {}", id);
        
        Bank bank = bankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found with ID: " + id));
        
        if (qrCodeImage == null || qrCodeImage.isEmpty()) {
            throw new IllegalArgumentException("QR code image is required");
        }
        
        // Delete old image if exists
        if (bank.getQrCodeImageUrl() != null && !bank.getQrCodeImageUrl().isEmpty()) {
            s3Service.deleteFile(bank.getQrCodeImageUrl());
        }
        
        // Upload new image
        String newQrCodeImageUrl = s3Service.uploadFile(qrCodeImage);
        bank.setQrCodeImageUrl(newQrCodeImageUrl);
        
        Bank updatedBank = bankRepository.save(bank);
        logger.info("QR code image updated successfully for bank ID: {}", updatedBank.getId());
        
        return convertToDTO(updatedBank);
    }

    @Transactional
    public void deleteQRCodeImage(Long id) {
        logger.info("Deleting QR code image for bank ID: {}", id);
        
        Bank bank = bankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found with ID: " + id));
        
        if (bank.getQrCodeImageUrl() != null && !bank.getQrCodeImageUrl().isEmpty()) {
            s3Service.deleteFile(bank.getQrCodeImageUrl());
            bank.setQrCodeImageUrl(null);
            bankRepository.save(bank);
            logger.info("QR code image deleted successfully for bank ID: {}", id);
        }
    }

    @Transactional
    public BankDTO updateBankStatus(Long id, Status status) {
        logger.info("Updating bank status for ID: {} to {}", id, status);
        
        Bank bank = bankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found with ID: " + id));
        
        bank.setStatus(status);
        Bank updatedBank = bankRepository.save(bank);
        
        logger.info("Bank status updated successfully for ID: {}", id);
        return convertToDTO(updatedBank);
    }

    /**
     * Generate VietQR payment URL
     */
    @Transactional(readOnly = true)
    public QRCodeResponseDTO generateQRCode(QRCodeRequestDTO requestDTO) {
        logger.info("Generating QR code with amount: {} and info: {}", requestDTO.getAmount(), requestDTO.getAddInfo());
        
        // Get active bank account
        List<BankDTO> activeBanks = getBanksByStatus(Status.ACTIVE);
        
        if (activeBanks.isEmpty()) {
            throw new RuntimeException("No active bank account found");
        }
        
        // Use the first active bank account
        BankDTO activeBank = activeBanks.get(0);
        
        // Validate amount
        if (requestDTO.getAmount() == null || requestDTO.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        
        // Build QR code URL
        String qrCodeUrl = buildVietQRUrl(
            activeBank.getAccountNumber(),
            activeBank.getAccountHolder(),
            requestDTO.getAmount(),
            requestDTO.getAddInfo()
        );
        
        logger.info("QR code URL generated successfully: {}", qrCodeUrl);
        
        return QRCodeResponseDTO.builder()
                .qrCodeUrl(qrCodeUrl)
                .amount(requestDTO.getAmount())
                .addInfo(requestDTO.getAddInfo())
                .bankName(activeBank.getBankName())
                .accountNumber(activeBank.getAccountNumber())
                .accountHolder(activeBank.getAccountHolder())
                .build();
    }
    
    /**
     * Build VietQR URL
     * Format: https://img.vietqr.io/image/mbbank-{accountNumber}-compact.png?amount={amount}&addInfo={info}&accountName={accountHolder}
     */
    private String buildVietQRUrl(String accountNumber, String accountHolder, Double amount, String addInfo) {
        try {
            StringBuilder urlBuilder = new StringBuilder("https://img.vietqr.io/image/mbbank-");
            urlBuilder.append(accountNumber);
            urlBuilder.append("-compact.png?");
            
            // Add amount parameter
            urlBuilder.append("amount=").append(amount.longValue());
            
            // Add addInfo parameter (URL encoded)
            if (addInfo != null && !addInfo.isEmpty()) {
                String encodedInfo = URLEncoder.encode(addInfo, StandardCharsets.UTF_8.toString());
                urlBuilder.append("&addInfo=").append(encodedInfo);
            }
            
            // Add accountName parameter (URL encoded)
            if (accountHolder != null && !accountHolder.isEmpty()) {
                String encodedName = URLEncoder.encode(accountHolder, StandardCharsets.UTF_8.toString());
                urlBuilder.append("&accountName=").append(encodedName);
            }
            
            return urlBuilder.toString();
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding URL parameters", e);
            throw new RuntimeException("Error generating QR code URL", e);
        }
    }

    private BankDTO convertToDTO(Bank bank) {
        return BankDTO.builder()
                .id(bank.getId())
                .bankName(bank.getBankName())
                .accountNumber(bank.getAccountNumber())
                .accountHolder(bank.getAccountHolder())
                .qrCodeImageUrl(bank.getQrCodeImageUrl())
                .status(bank.getStatus())
                .build();
    }
}

