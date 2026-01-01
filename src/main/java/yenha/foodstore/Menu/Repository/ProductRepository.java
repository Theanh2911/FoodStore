package yenha.foodstore.Menu.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yenha.foodstore.Menu.Entity.Category;
import yenha.foodstore.Menu.Entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Find all active products
    List<Product> findByIsActiveTrue();

    // Original methods (keep for admin panel - show all)
    List<Product> findByCategory(Category category);

    List<Product> findByCategoryCategoryId(Long categoryId);

    List<Product> findByNameContaining(String name);

    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    // Active products only
    List<Product> findByCategoryAndIsActiveTrue(Category category);

    List<Product> findByCategoryCategoryIdAndIsActiveTrue(Long categoryId);

    List<Product> findByNameContainingAndIsActiveTrue(String name);

    List<Product> findByPriceBetweenAndIsActiveTrue(Double minPrice, Double maxPrice);
}
