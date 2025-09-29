package yenha.foodstore.Menu.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> getProductsByCategory(Category category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategoryCategoryId(categoryId);
    }

    public List<Product> searchProductsByName(String name) {
        return productRepository.findByNameContaining(name);
    }

    public List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }

    public List<Product> getProductsWithImages() {
        return productRepository.findByImageIsNotNull();
    }

    public List<Product> getProductsWithoutImages() {
        return productRepository.findByImageIsNull();
    }

    public List<Product> searchProductsByImage(String imagePattern) {
        return productRepository.findByImageContaining(imagePattern);
    }

    public Product saveProduct(Product product) {
        // Validate that the category exists
        if (product.getCategory() != null && product.getCategory().getCategoryId() != null) {
            Optional<Category> category = categoryService.getCategoryById(product.getCategory().getCategoryId());
            if (category.isPresent()) {
                product.setCategory(category.get());
            } else {
                throw new RuntimeException("Category not found with id: " + product.getCategory().getCategoryId());
            }
        }
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product productDetails) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isPresent()) {
            Product existingProduct = optionalProduct.get();
            existingProduct.setName(productDetails.getName());
            existingProduct.setPrice(productDetails.getPrice());
            existingProduct.setImage(productDetails.getImage());
            
            // Update category if provided
            if (productDetails.getCategory() != null && productDetails.getCategory().getCategoryId() != null) {
                Optional<Category> category = categoryService.getCategoryById(productDetails.getCategory().getCategoryId());
                if (category.isPresent()) {
                    existingProduct.setCategory(category.get());
                } else {
                    throw new RuntimeException("Category not found with id: " + productDetails.getCategory().getCategoryId());
                }
            }
            
            return productRepository.save(existingProduct);
        }
        throw new RuntimeException("Product not found with id: " + id);
    }

    public boolean existsById(Long id) {
        return productRepository.existsById(id);
    }

    public boolean canDeleteProduct(Long id) {
        return productRepository.existsById(id) && !orderItemRepository.existsByProductProductId(id);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }

        if (orderItemRepository.existsByProductProductId(id)) {
            throw new RuntimeException("Cannot delete product with id: " + id + ". Product is referenced in existing orders.");
        }
        
        productRepository.deleteById(id);
    }

    public Product saveProductFromDTO(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setPrice(productDTO.getPrice());
        product.setImage(productDTO.getImage());
        
        // Get and set the category
        if (productDTO.getCategoryId() != null) {
            Optional<Category> category = categoryService.getCategoryById(productDTO.getCategoryId());
            if (category.isPresent()) {
                product.setCategory(category.get());
            } else {
                throw new RuntimeException("Category not found with id: " + productDTO.getCategoryId());
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
            existingProduct.setImage(productDTO.getImage());
            
            // Update category if provided
            if (productDTO.getCategoryId() != null) {
                Optional<Category> category = categoryService.getCategoryById(productDTO.getCategoryId());
                if (category.isPresent()) {
                    existingProduct.setCategory(category.get());
                } else {
                    throw new RuntimeException("Category not found with id: " + productDTO.getCategoryId());
                }
            }
            
            return productRepository.save(existingProduct);
        }
        throw new RuntimeException("Product not found with id: " + id);
    }

    public ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setProductId(product.getProductId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setImage(product.getImage());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getCategoryId());
        }
        return dto;
    }
}

