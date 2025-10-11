package yenha.foodstore.Menu.Controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yenha.foodstore.Constant.Error;
import yenha.foodstore.Menu.DTO.ProductDTO;
import yenha.foodstore.Menu.Entity.Category;
import yenha.foodstore.Menu.Entity.Product;
import yenha.foodstore.Menu.Service.CategoryService;
import yenha.foodstore.Menu.Service.ProductService;
import yenha.foodstore.Menu.Service.S3Service;

import java.util.HashMap;
import java.util.Map;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private S3Service s3Service;

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            return new ResponseEntity<>(categories, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        try {
            Optional<Category> category = categoryService.getCategoryById(id);
            if (category.isPresent()) {
                return new ResponseEntity<>(category.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/categories/search")
    public ResponseEntity<List<Category>> searchCategories(@RequestParam String name) {
        try {
            List<Category> categories = categoryService.searchCategoriesByName(name);
            return new ResponseEntity<>(categories, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/categories/create")
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        try {
            Category savedCategory = categoryService.saveCategory(category);
            return new ResponseEntity<>(savedCategory, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/categories/update/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        try {
            Category updatedCategory = categoryService.updateCategory(id, category);
            return new ResponseEntity<>(updatedCategory, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/categories/delete/{id}")
    public ResponseEntity<HttpStatus> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ========== PRODUCT ENDPOINTS ==========

    @GetMapping("/products/getAll")
    public ResponseEntity<List<Product>> getAllProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            return new ResponseEntity<>(products, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        try {
            Optional<Product> product = productService.getProductById(id);
            if (product.isPresent()) {
                return new ResponseEntity<>(product.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/products/category/{categoryId}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable Long categoryId) {
        try {
            List<Product> products = productService.getProductsByCategoryId(categoryId);
            return new ResponseEntity<>(products, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    @PostMapping("/products/create")
//    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductDTO productDTO, BindingResult bindingResult) {
//        // Check for validation errors
//        if (bindingResult.hasErrors()) {
//            Map<String, String> errors = new HashMap<>();
//            bindingResult.getFieldErrors().forEach(error ->
//                errors.put(error.getField(), error.getDefaultMessage())
//            );
//            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
//        }
//
//        try {
//            Product savedProduct = productService.saveProductFromDTO(productDTO);
//            return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
//        } catch (RuntimeException e) {
//            Map<String, String> error = new HashMap<>();
//            error.put("error", e.getMessage());
//            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
//        } catch (Exception e) {
//            Map<String, String> error = new HashMap<>();
//            error.put("error", "Internal server error: " + e.getMessage());
//            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @PostMapping(value = "/products/create", consumes = {"multipart/form-data"})
    public ResponseEntity<?> createProduct(
            @RequestPart("product") @Valid ProductDTO productDTO,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage()));
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = s3Service.uploadFile(imageFile);
                productDTO.setImage(imageUrl);
            }

            Product savedProduct = productService.saveProductFromDTO(productDTO);
            return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error: ", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error: ", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error: ",e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PutMapping(value = "/products/update/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") @Valid ProductDTO productDTO,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }
        
        try {
            Optional<Product> existingProductOpt = productService.getProductById(id);
            if (existingProductOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", Error.CATEGORY_NOT_FOUND);
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }
            
            Product existingProduct = existingProductOpt.get();
            String oldImageUrl = existingProduct.getImage();

            if (imageFile != null && !imageFile.isEmpty()) {
                String newImageUrl = s3Service.uploadFile(imageFile);
                productDTO.setImage(newImageUrl);

                if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                    s3Service.deleteFile(oldImageUrl);
                }
            } else if (productDTO.getImage() == null) {
                productDTO.setImage(oldImageUrl);
            }
            
            Product updatedProduct = productService.updateProductFromDTO(id, productDTO);
            return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
            
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error: ", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error: ", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error: ", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/products/update-json/{id}")
    public ResponseEntity<?> updateProductJson(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }
        
        try {
            Product updatedProduct = productService.updateProductFromDTO(id, productDTO);
            return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error",e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/products/can-delete/{id}")
    public ResponseEntity<?> canDeleteProduct(@PathVariable Long id) {
        try {
            boolean canDelete = productService.canDeleteProduct(id);
            Map<String, Object> response = new HashMap<>();
            response.put("canDelete", canDelete);
            if (!canDelete && productService.existsById(id)) {
                response.put("reason", "Product is referenced in existing orders");
            } else if (!productService.existsById(id)) {
                response.put("reason", "Product not found");
            }
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/products/create-with-image")
    public ResponseEntity<?> createProductFlexible(
            @RequestParam("name") String name,
            @RequestParam("price") Double price,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        try {
            ProductDTO productDTO = new ProductDTO();
            productDTO.setName(name);
            productDTO.setPrice(price);
            productDTO.setCategoryId(categoryId);

            if (name == null || name.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Product name is required");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
            if (price == null || price <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Product price must be positive");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
            if (categoryId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Category ID is required");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = s3Service.uploadFile(imageFile);
                productDTO.setImage(imageUrl);
            }

            Product savedProduct = productService.saveProductFromDTO(productDTO);
            return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping(value = "/products/update-with-image/{id}")
    public ResponseEntity<?> updateProductFlexible(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        try {
            Optional<Product> existingProductOpt = productService.getProductById(id);
            if (existingProductOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", Error.PRODUCT_NOT_FOUND);
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }

            Product existingProduct = existingProductOpt.get();

            ProductDTO productDTO = new ProductDTO();
            productDTO.setName(name != null ? name : existingProduct.getName());
            productDTO.setPrice(price != null ? price : existingProduct.getPrice());
            productDTO.setCategoryId(categoryId != null ? categoryId : existingProduct.getCategory().getCategoryId());
            productDTO.setImage(existingProduct.getImage()); // Keep existing image initially

            if (productDTO.getName() == null || productDTO.getName().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", Error.PRODUCT_NAME_REQUIRE);
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
            if (productDTO.getPrice() == null || productDTO.getPrice() <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", Error.PRODUCT_PRICE_NEGATIVE);
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            String oldImageUrl = existingProduct.getImage();

            if (imageFile != null && !imageFile.isEmpty()) {
                String newImageUrl = s3Service.uploadFile(imageFile);
                productDTO.setImage(newImageUrl);

                if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                    s3Service.deleteFile(oldImageUrl);
                }
            }

            Product updatedProduct = productService.updateProductFromDTO(id, productDTO);
            return new ResponseEntity<>(updatedProduct, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No file provided");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            String imageUrl = s3Service.uploadFile(file);
            
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "File uploaded successfully");
            
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/products/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            Optional<Product> productOpt = productService.getProductById(id);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                String imageUrl = product.getImage();

                productService.deleteProduct(id);

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    s3Service.deleteFile(imageUrl);
                }
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Product not found");
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }
            
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            } else if (e.getMessage().contains("referenced in existing orders")) {
                return new ResponseEntity<>(error, HttpStatus.CONFLICT);
            } else {
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

