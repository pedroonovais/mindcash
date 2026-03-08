package com.mindcash.app.web.investment;

import com.mindcash.app.dto.InvestmentRequest;
import com.mindcash.app.dto.InitialCompositionRequest;
import com.mindcash.app.dto.PositionLineDto;
import com.mindcash.app.model.Account;
import com.mindcash.app.model.AccountType;
import com.mindcash.app.model.Investment;
import com.mindcash.app.model.InvestmentType;
import com.mindcash.app.model.RentabilityKind;
import com.mindcash.app.service.AccountService;
import com.mindcash.app.service.InvestmentService;
import com.mindcash.app.service.InvestmentYieldUpdateService;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/app/investments")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final AccountService accountService;
    private final InvestmentYieldUpdateService investmentYieldUpdateService;

    public InvestmentController(InvestmentService investmentService, AccountService accountService,
                                 InvestmentYieldUpdateService investmentYieldUpdateService) {
        this.investmentService = investmentService;
        this.accountService = accountService;
        this.investmentYieldUpdateService = investmentYieldUpdateService;
    }

    @GetMapping
    public String index(Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        investmentYieldUpdateService.ensureYieldTransactionsUpToToday(userId);

        List<Investment> investments = investmentService.findByUserId(userId);
        BigDecimal totalCurrentValue = investmentService.totalCurrentValueByUserId(userId);
        List<Object[]> allocationByType = investmentService.sumCurrentValueByType(userId);
        var evolutionData = investmentService.getEvolutionData(userId);

        List<String> evolutionLabels = evolutionData.stream().map(Map.Entry::getKey).toList();
        List<Double> evolutionValues = evolutionData.stream().map(e -> e.getValue().doubleValue()).toList();

        List<String> allocationLabels = new ArrayList<>();
        List<Double> allocationPercents = new ArrayList<>();
        List<Map.Entry<String, Double>> allocationLegend = new ArrayList<>();
        if (totalCurrentValue != null && totalCurrentValue.compareTo(BigDecimal.ZERO) > 0) {
            for (Object[] row : allocationByType) {
                InvestmentType type = (InvestmentType) row[0];
                BigDecimal sum = (BigDecimal) row[1];
                String label = type.getLabel();
                double pct = sum.divide(totalCurrentValue, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue();
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
        model.addAttribute("totalCurrentValue", totalCurrentValue);
        model.addAttribute("totalInvested", investmentService.totalInvestedByUserId(userId));
        model.addAttribute("allocationByType", allocationByType);
        model.addAttribute("allocationMap", allocationMap);
        model.addAttribute("evolutionData", evolutionData);
        model.addAttribute("evolutionLabels", evolutionLabels);
        model.addAttribute("evolutionValues", evolutionValues);
        model.addAttribute("allocationLabels", allocationLabels);
        model.addAttribute("allocationPercents", allocationPercents);
        double cdiAnnualRate = 12.0;
        model.addAttribute("cdiAnnualRate", cdiAnnualRate);
        List<Double> projectionValues = new ArrayList<>();
        List<Double> projectionRates = new ArrayList<>();
        for (Investment inv : investments) {
            projectionValues.add(inv.getCurrentValue().doubleValue());
            projectionRates.add(investmentService.getEffectiveAnnualRate(inv.getRentabilityKind(), inv.getRentabilityValue(), cdiAnnualRate));
        }
        model.addAttribute("projectionValues", projectionValues);
        model.addAttribute("projectionRates", projectionRates);
        try {
            Map<String, List<Double>> projectionData = Map.of("values", projectionValues, "rates", projectionRates);
            model.addAttribute("projectionDataJson", new ObjectMapper().writeValueAsString(projectionData));
        } catch (JsonProcessingException e) {
            model.addAttribute("projectionDataJson", "{\"values\":[],\"rates\":[]}");
        }

        List<Account> investmentOnlyAccounts = accountService.findByUserId(userId).stream()
                .filter(a -> a.getType() == AccountType.INVESTIMENTO)
                .toList();
        boolean canRegisterExistingBalance = investmentOnlyAccounts.size() == 1
                && investmentOnlyAccounts.get(0).getBalance().compareTo(BigDecimal.ZERO) > 0
                && investments.isEmpty();
        model.addAttribute("canRegisterExistingBalance", canRegisterExistingBalance);
        model.addAttribute("singleInvestmentAccount", canRegisterExistingBalance ? investmentOnlyAccounts.get(0) : null);

        return "app/investments/index";
    }

    @GetMapping("/composition")
    public String compositionForm(@RequestParam Long accountId, Model model, RedirectAttributes redirectAttributes) {
        Long userId = SecurityUtil.getCurrentUserId();
        var accountOpt = accountService.findByUserId(userId).stream()
                .filter(a -> a.getType() == AccountType.INVESTIMENTO && a.getId().equals(accountId))
                .findFirst();
        if (accountOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Conta de investimento não encontrada.");
            return "redirect:/app/investments";
        }
        Account account = accountOpt.get();
        if (account.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "A conta não possui saldo para compor.");
            return "redirect:/app/investments";
        }
        if (!investmentService.findByUserId(userId).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Já existem posições. Não é possível definir composição.");
            return "redirect:/app/investments";
        }

        InitialCompositionRequest request = new InitialCompositionRequest();
        request.setDestinationAccountId(accountId);
        request.setPositions(new ArrayList<>(List.of(new PositionLineDto())));

        model.addAttribute("compositionRequest", request);
        model.addAttribute("account", account);
        model.addAttribute("investmentTypes", InvestmentType.values());
        model.addAttribute("rentabilityKinds", RentabilityKind.values());
        return "app/investments/composition";
    }

    @PostMapping("/register-composition")
    public String registerComposition(
            @Valid @ModelAttribute("compositionRequest") InitialCompositionRequest compositionRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        Long userId = SecurityUtil.getCurrentUserId();

        if (bindingResult.hasErrors()) {
            Long accountId = compositionRequest.getDestinationAccountId();
            var accountOpt = accountService.findByUserId(userId).stream()
                    .filter(a -> a.getType() == AccountType.INVESTIMENTO && a.getId().equals(accountId))
                    .findFirst();
            if (accountOpt.isPresent()) {
                model.addAttribute("account", accountOpt.get());
            }
            model.addAttribute("investmentTypes", InvestmentType.values());
            model.addAttribute("rentabilityKinds", RentabilityKind.values());
            return "app/investments/composition";
        }

        try {
            investmentService.registerComposition(userId, compositionRequest.getDestinationAccountId(), compositionRequest.getPositions());
            redirectAttributes.addFlashAttribute("success", "Composição do saldo registrada com sucesso!");
            return "redirect:/app/investments";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/app/investments";
        } catch (jakarta.persistence.EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Conta de investimento não encontrada.");
            return "redirect:/app/investments";
        }
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
            repopulateForm(model, userId);
            return "app/investments/form";
        }

        try {
            investmentService.create(investmentRequest, userId);
            redirectAttributes.addFlashAttribute("success", "Investimento cadastrado com sucesso!");
            return "redirect:/app/investments";
        } catch (IllegalArgumentException e) {
            repopulateForm(model, userId);
            model.addAttribute("error", e.getMessage());
            return "app/investments/form";
        } catch (jakarta.persistence.EntityNotFoundException e) {
            repopulateForm(model, userId);
            model.addAttribute("error", "Conta de origem ou destino não encontrada.");
            return "app/investments/form";
        }
    }

    private void repopulateForm(Model model, Long userId) {
        List<Account> accounts = accountService.findByUserId(userId);
        List<Account> investmentAccounts = accounts.stream()
                .filter(a -> a.getType() == AccountType.CORRETORA || a.getType() == AccountType.INVESTIMENTO)
                .toList();
        model.addAttribute("accounts", accounts);
        model.addAttribute("investmentAccounts", investmentAccounts);
        model.addAttribute("investmentTypes", InvestmentType.values());
        model.addAttribute("rentabilityKinds", RentabilityKind.values());
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        investmentService.delete(id, SecurityUtil.getCurrentUserId());
        redirectAttributes.addFlashAttribute("success", "Investimento excluído com sucesso!");
        return "redirect:/app/investments";
    }

    @PostMapping("/register-existing-balance")
    public String registerExistingBalance(RedirectAttributes redirectAttributes, Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        try {
            investmentService.registerExistingBalance(userId);
            redirectAttributes.addFlashAttribute("success", "Saldo existente registrado como posição inicial. Edite ou adicione mais posições para detalhar.");
            return "redirect:/app/investments";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/app/investments";
        }
    }
}
