package com.mindcash.app.web.account;

import com.mindcash.app.dto.AccountRequest;
import com.mindcash.app.model.Account;
import com.mindcash.app.model.AccountType;
import com.mindcash.app.service.AccountService;
import com.mindcash.app.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/app/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public String list(Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Account> accounts = accountService.findByUserId(userId);
        model.addAttribute("accounts", accounts);
        model.addAttribute("accountRequest", new AccountRequest());
        model.addAttribute("accountTypes", AccountType.values());
        return "app/accounts/index";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute AccountRequest accountRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            Long userId = SecurityUtil.getCurrentUserId();
            model.addAttribute("accounts", accountService.findByUserId(userId));
            model.addAttribute("accountTypes", AccountType.values());
            return "app/accounts/index";
        }

        accountService.create(accountRequest, SecurityUtil.getCurrentUserId());
        redirectAttributes.addFlashAttribute("success", "Conta criada com sucesso!");
        return "redirect:/app/accounts";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        Account account = accountService.findByIdAndUserId(id, userId);

        AccountRequest request = new AccountRequest();
        request.setName(account.getName());
        request.setType(account.getType());
        request.setCurrency(account.getCurrency());

        model.addAttribute("accountRequest", request);
        model.addAttribute("account", account);
        model.addAttribute("accountTypes", AccountType.values());
        return "app/accounts/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute AccountRequest accountRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            Account account = accountService.findByIdAndUserId(id, SecurityUtil.getCurrentUserId());
            model.addAttribute("account", account);
            model.addAttribute("accountTypes", AccountType.values());
            return "app/accounts/edit";
        }

        accountService.update(id, accountRequest, SecurityUtil.getCurrentUserId());
        redirectAttributes.addFlashAttribute("success", "Conta atualizada com sucesso!");
        return "redirect:/app/accounts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        accountService.softDelete(id, SecurityUtil.getCurrentUserId());
        redirectAttributes.addFlashAttribute("success", "Conta excluída com sucesso!");
        return "redirect:/app/accounts";
    }
}
