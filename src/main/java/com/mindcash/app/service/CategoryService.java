package com.mindcash.app.service;

import com.mindcash.app.dto.CategoryRequest;
import com.mindcash.app.model.Category;
import com.mindcash.app.model.User;
import com.mindcash.app.repository.CategoryRepository;
import com.mindcash.app.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<Category> findRootCategories(Long userId) {
        return categoryRepository.findRootCategoriesByUserId(userId);
    }

    /** Raízes com filhos já carregados para uso em view (evita LazyInitializationException). */
    public List<Category> findRootCategoriesWithChildren(Long userId) {
        return categoryRepository.findRootCategoriesWithChildrenByUserId(userId);
    }

    public List<Category> findAll(Long userId) {
        return categoryRepository.findAllByUserId(userId);
    }

    @Transactional
    public Category create(CategoryRequest request, Long userId) {
        User user = userRepository.getReferenceById(userId);

        Category category = new Category();
        category.setUser(user);
        category.setName(request.getName().trim());
        category.setIcon(request.getIcon());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoria pai não encontrada"));
            category.setParent(parent);
        }

        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryRequest request, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));

        if (category.isGlobal()) {
            throw new IllegalArgumentException("Categorias globais não podem ser editadas");
        }

        category.setName(request.getName().trim());
        category.setIcon(request.getIcon());
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));

        if (category.isGlobal()) {
            throw new IllegalArgumentException("Categorias globais não podem ser excluídas");
        }

        categoryRepository.delete(category);
    }
}
