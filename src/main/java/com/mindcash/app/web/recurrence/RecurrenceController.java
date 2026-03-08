package com.mindcash.app.web.recurrence;

import com.mindcash.app.service.TransactionService;
import com.mindcash.app.util.SecurityUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/recurrences")
public class RecurrenceController {

    private final TransactionService transactionService;

    public RecurrenceController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        var templates = transactionService.findRecurrenceTemplatesByUserId(userId, PageRequest.of(page, 20));
        model.addAttribute("templates", templates);
        return "app/recurrences/index";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        transactionService.cancelRecurrence(id, SecurityUtil.getCurrentUserId());
        redirectAttributes.addFlashAttribute("success", "Recorrência cancelada. Não serão geradas novas transações.");
        return "redirect:/app/recurrences";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        transactionService.delete(id, SecurityUtil.getCurrentUserId());
        redirectAttributes.addFlashAttribute("success", "Recorrência excluída.");
        return "redirect:/app/recurrences";
    }
}
