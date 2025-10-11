package yenha.foodstore.Menu.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yenha.foodstore.Constant.Error;
import yenha.foodstore.Menu.Entity.Category;
import yenha.foodstore.Menu.Repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public List<Category> searchCategoriesByName(String name) {
        return categoryRepository.findByNameContaining(name);
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category categoryDetails) {
        Optional<Category> optionalCategory = categoryRepository.findById(id);
        if (optionalCategory.isPresent()) {
            Category existingCategory = optionalCategory.get();
            existingCategory.setName(categoryDetails.getName());
            return categoryRepository.save(existingCategory);
        }
        throw new RuntimeException(Error.CATEGORY_NOT_FOUND + id);
    }

    public void deleteCategory(Long id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
        } else {
            throw new RuntimeException(Error.CATEGORY_NOT_FOUND + id);
        }
    }

    public boolean existsById(Long id) {
        return categoryRepository.existsById(id);
    }

}
