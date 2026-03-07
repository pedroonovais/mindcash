package com.mindcash.app.repository;

import com.mindcash.app.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Retorna categorias globais (user_id IS NULL) e do usuário, apenas raízes (sem parent) */
    @Query("SELECT c FROM Category c WHERE (c.user IS NULL OR c.user.id = :userId) AND c.parent IS NULL ORDER BY c.name")
    List<Category> findRootCategoriesByUserId(Long userId);

    /** Raízes com filhos carregados (evita LazyInitializationException na view quando open-in-view é false) */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE (c.user IS NULL OR c.user.id = :userId) AND c.parent IS NULL ORDER BY c.name")
    List<Category> findRootCategoriesWithChildrenByUserId(Long userId);

    /** Todas as categorias visíveis para o usuário (globais + próprias) */
    @Query("SELECT c FROM Category c WHERE c.user IS NULL OR c.user.id = :userId ORDER BY c.name")
    List<Category> findAllByUserId(Long userId);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    /** Subcategorias de uma categoria pai */
    List<Category> findByParentId(Long parentId);
}
