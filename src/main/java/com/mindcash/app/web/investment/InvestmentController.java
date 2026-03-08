package com.mindcash.app.web.investment;

import com.mindcash.app.dto.InvestmentRequest;
import com.mindcash.app.model.Account;
import com.mindcash.app.model.AccountType;
import com.mindcash.app.model.Investment;
import com.mindcash.app.model.InvestmentType;
import com.mindcash.app.model.RentabilityKind;
import com.mindcash.app.service.AccountService;
import com.mindcash.app.service.InvestmentService;
import com.mindcash.app.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/app/investments")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final AccountService accountService;

    public InvestmentController(InvestmentService investmentService, AccountService accountService) {
        this.investmentService = investmentService;
        this.accountService = accountService;
    }

    @GetMapping
    public String index(Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Investment> investments = investmentService.findByUserId(userId);
        BigDecimal totalInvested = investmentService.totalInvestedByUserId(userId);
        List<Object[]> allocationByType = investmentService.sumAmountByType(userId);
        var evolutionData = investmentService.getEvolutionData(userId);

        List<String> evolutionLabels = evolutionData.stream().map(Map.Entry::getKey).toList();
        List<Double> evolutionValues = evolutionData.stream().map(e -> e.getValue().doubleValue()).toList();

        List<String> allocationLabels = new ArrayList<>();
        List<Double> allocationPercents = new ArrayList<>();
        List<Map.Entry<String, Double>> allocationLegend = new ArrayList<>();
        if (totalInvested != null && totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            for (Object[] row : allocationByType) {
                InvestmentType type = (InvestmentType) row[0];
                BigDecimal sum = (BigDecimal) row[1];
                String label = type.getLabel();
                double pct = sum.divide(totalInvested, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue();
                allocationLabels.add(label);
                allocationPercents.add(pct);
                allocationLegend.add(Map.entry(label, pct));
            }
        }

        model.addAttribute("allocationLegend", allocationLegend);

        Map<String, BigDecimal> allocationMap = allocationByType.stream()
                .collect(Collectors.toMap(
                        row -> ((InvestmentType) row[0]).name(),
                        row -> (BigDecimal) row[1]
                ));

        model.addAttribute("investments", investments);
        model.addAttribute("totalInvested", totalInvested);
        model.addAttribute("allocationByType", allocationByType);
        model.addAttribute("allocationMap", allocationMap);
        model.addAttribute("evolutionData", evolutionData);
        model.addAttribute("evolutionLabels", evolutionLabels);
        model.addAttribute("evolutionValues", evolutionValues);
        model.addAttribute("allocationLabels", allocationLabels);
        model.addAttribute("allocationPercents", allocationPercents);
        return "app/investments/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Account> accounts = accountService.findByUserId(userId);
        List<Account> investmentAccounts = accounts.stream()
                .filter(a -> a.getType() == AccountType.CORRETORA || a.getType() == AccountType.INVESTIMENTO)
                .toList();

        InvestmentRequest request = new InvestmentRequest();
        request.setDate(LocalDate.now());

        model.addAttribute("investmentRequest", request);
        model.addAttribute("accounts", accounts);
        model.addAttribute("investmentAccounts", investmentAccounts);
        model.addAttribute("investmentTypes", InvestmentType.values());
        model.addAttribute("rentabilityKinds", RentabilityKind.values());
        return "app/investments/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute InvestmentRequest investmentRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        Long userId = SecurityUtil.getCurrentUserId();

        if (bindingResult.hasErrors()) {
            List<Account> accounts = accountService.findByUserId(userId);
            List<Account> investmentAccounts = accounts.stream()
                    .filter(a -> a.getType() == AccountType.CORRETORA || a.getType() == AccountType.INVESTIMENTO)
                    .toList();
            model.addAttribute("accounts", accounts);
            model.addAttribute("investmentAccounts", investmentAccounts);
            model.addAttribute("investmentTypes", InvestmentType.values());
            model.addAttribute("rentabilityKinds", RentabilityKind.values());
            return "app/investments/form";
        }

        try {
            investmentService.create(investmentRequest, userId);
            redirectAttributes.addFlashAttribute("success", "Investimento cadastrado com sucesso!");
            return "redirect:/app/investments";
        } catch (IllegalArgumentException e) {
            List<Account> accounts = accountService.findByUserId(userId);
            List<Account> investmentAccounts = accounts.stream()
                    .filter(a -> a.getType() == AccountType.CORRETORA || a.getType() == AccountType.INVESTIMENTO)
                    .toList();
            model.addAttribute("accounts", accounts);
            model.addAttribute("investmentAccounts", investmentAccounts);
            model.addAttribute("investmentTypes", InvestmentType.values());
            model.addAttribute("rentabilityKinds", RentabilityKind.values());
            model.addAttribute("error", e.getMessage());
            return "app/investments/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        investmentService.delete(id, SecurityUtil.getCurrentUserId());
        redirectAttributes.addFlashAttribute("success", "Investimento excluído com sucesso!");
        return "redirect:/app/investments";
    }
}
