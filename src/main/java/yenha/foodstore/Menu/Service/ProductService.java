package yenha.foodstore.Menu.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yenha.foodstore.Constant.Error;
import yenha.foodstore.Menu.DTO.ProductDTO;
import yenha.foodstore.Menu.Entity.Category;
import yenha.foodstore.Menu.Entity.Product;
import yenha.foodstore.Menu.Repository.ProductRepository;
import yenha.foodstore.Order.Repository.OrderItemRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    // For customer-facing endpoints - only active products
    public List<Product> getAllProducts() {
        return productRepository.findByIsActiveTrue();
    }

    // For admin panel - all products including inactive
    public List<Product> getAllProductsIncludingInactive() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategoryCategoryIdAndIsActiveTrue(categoryId);
    }

    public void deleteProduct(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (!productOpt.isPresent()) {
            throw new RuntimeException(Error.PRODUCT_NOT_FOUND + id);
        }

        Product product = productOpt.get();
        product.setIsActive(false);
        productRepository.save(product);
    }

    public void hardDeleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException(Error.PRODUCT_NOT_FOUND + id);
        }

        if (orderItemRepository.existsByProductProductId(id)) {
            throw new RuntimeException(Error.PRODUCT_CANNOT_BE_DELETED + id);
        }

        productRepository.deleteById(id);
    }

    public Product saveProductFromDTO(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setPrice(productDTO.getPrice());
        product.setCost(productDTO.getCost());
        product.setDefaultDailyLimit(productDTO.getDefaultDailyLimit());
        product.setImage(productDTO.getImage());
        product.setIsActive(true); // New products are active by default

        if (productDTO.getCategoryId() != null) {
            Optional<Category> category = categoryService.getCategoryById(productDTO.getCategoryId());
            if (category.isPresent()) {
                product.setCategory(category.get());
            } else {
                throw new RuntimeException(Error.CATEGORY_NOT_FOUND + productDTO.getCategoryId());
            }
        }

        return productRepository.save(product);
    }

    public Product updateProductFromDTO(Long id, ProductDTO productDTO) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isPresent()) {
            Product existingProduct = optionalProduct.get();
            existingProduct.setName(productDTO.getName());
            existingProduct.setPrice(productDTO.getPrice());
            existingProduct.setCost(productDTO.getCost());
            existingProduct.setDefaultDailyLimit(productDTO.getDefaultDailyLimit());
            existingProduct.setImage(productDTO.getImage());

            if (productDTO.getCategoryId() != null) {
                Optional<Category> category = categoryService.getCategoryById(productDTO.getCategoryId());
                if (category.isPresent()) {
                    existingProduct.setCategory(category.get());
                } else {
                    throw new RuntimeException(Error.CATEGORY_NOT_FOUND + productDTO.getCategoryId());
                }
            }

            return productRepository.save(existingProduct);
        }
        throw new RuntimeException(Error.PRODUCT_NOT_FOUND + id);
    }

    public ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setProductId(product.getProductId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setCost(product.getCost());
        dto.setDefaultDailyLimit(product.getDefaultDailyLimit());
        dto.setImage(product.getImage());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getCategoryId());
        }
        return dto;
    }

}
