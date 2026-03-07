package com.mindcash.app.web.category;

import com.mindcash.app.dto.CategoryRequest;
import com.mindcash.app.service.CategoryService;
import com.mindcash.app.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        model.addAttribute("categories", categoryService.findRootCategoriesWithChildren(userId));
        model.addAttribute("categoryRequest", new CategoryRequest());
        model.addAttribute("allCategories", categoryService.findAll(userId));
        return "app/categories/index";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute CategoryRequest categoryRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        Long userId = SecurityUtil.getCurrentUserId();

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findRootCategoriesWithChildren(userId));
            model.addAttribute("allCategories", categoryService.findAll(userId));
            return "app/categories/index";
        }

        categoryService.create(categoryRequest, userId);
        redirectAttributes.addFlashAttribute("success", "Categoria criada com sucesso!");
        return "redirect:/app/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id, SecurityUtil.getCurrentUserId());
            redirectAttributes.addFlashAttribute("success", "Categoria excluída com sucesso!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/app/categories";
    }
}
