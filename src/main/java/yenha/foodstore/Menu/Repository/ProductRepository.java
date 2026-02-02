package yenha.foodstore.Menu.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yenha.foodstore.Menu.Entity.Category;
import yenha.foodstore.Menu.Entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Find all active products with JOIN FETCH to avoid N+1 query
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.isActive = true")
    List<Product> findByIsActiveTrue();

    // Original methods (keep for admin panel - show all)
    List<Product> findByCategory(Category category);

    List<Product> findByCategoryCategoryId(Long categoryId);

    List<Product> findByNameContaining(String name);

    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    // Active products only with JOIN FETCH
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.category = :category AND p.isActive = true")
    List<Product> findByCategoryAndIsActiveTrue(@Param("category") Category category);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.category.categoryId = :categoryId AND p.isActive = true")
    List<Product> findByCategoryCategoryIdAndIsActiveTrue(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.isActive = true")
    List<Product> findByNameContainingAndIsActiveTrue(@Param("name") String name);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.isActive = true")
    List<Product> findByPriceBetweenAndIsActiveTrue(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);
}
