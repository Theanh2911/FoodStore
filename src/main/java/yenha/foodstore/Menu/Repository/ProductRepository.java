package yenha.foodstore.Menu.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yenha.foodstore.Menu.Entity.Category;
import yenha.foodstore.Menu.Entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(Category category);
    List<Product> findByCategoryCategoryId(Long categoryId);
    List<Product> findByNameContaining(String name);
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
    
    // Image-related queries
    List<Product> findByImageIsNotNull();
    List<Product> findByImageIsNull();
    List<Product> findByImageContaining(String imagePattern);
}

