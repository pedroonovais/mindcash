package com.mindcash.app.web.transaction;

import com.mindcash.app.dto.TransactionRequest;
import com.mindcash.app.model.TransactionType;
import com.mindcash.app.service.AccountService;
import com.mindcash.app.service.CategoryService;
import com.mindcash.app.service.TransactionService;
import com.mindcash.app.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public TransactionController(
            TransactionService transactionService,
            AccountService accountService,
            CategoryService categoryService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        var transactions = transactionService.findByUserId(userId, PageRequest.of(page, 20));
        model.addAttribute("transactions", transactions);
        return "app/transactions/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        model.addAttribute("transactionRequest", new TransactionRequest());
        model.addAttribute("accounts", accountService.findByUserId(userId));
        model.addAttribute("categories", categoryService.findAll(userId));
        model.addAttribute("transactionTypes", TransactionType.values());
        return "app/transactions/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute TransactionRequest transactionRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        Long userId = SecurityUtil.getCurrentUserId();

        if (bindingResult.hasErrors()) {
            model.addAttribute("accounts", accountService.findByUserId(userId));
            model.addAttribute("categories", categoryService.findAll(userId));
            model.addAttribute("transactionTypes", TransactionType.values());
            return "app/transactions/form";
        }

        transactionService.create(transactionRequest, userId);
        redirectAttributes.addFlashAttribute("success", "Transação registrada com sucesso!");
        return "redirect:/app/transactions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        transactionService.delete(id, SecurityUtil.getCurrentUserId());
        redirectAttributes.addFlashAttribute("success", "Transação excluída com sucesso!");
        return "redirect:/app/transactions";
    }
}
