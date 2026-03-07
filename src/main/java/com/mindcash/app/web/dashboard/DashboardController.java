package com.mindcash.app.web.dashboard;

import com.mindcash.app.dto.DashboardData;
import com.mindcash.app.model.Account;
import com.mindcash.app.service.AccountService;
import com.mindcash.app.service.DashboardService;
import com.mindcash.app.service.TransactionService;
import com.mindcash.app.util.SecurityUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

@Controller
@RequestMapping("/app/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public DashboardController(
            DashboardService dashboardService,
            AccountService accountService,
            TransactionService transactionService) {
        this.dashboardService = dashboardService;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping
    public String dashboard(Model model) {
        Long userId = SecurityUtil.getCurrentUserId();

        DashboardData data = dashboardService.buildDashboard(userId);
        List<Account> accounts = accountService.findByUserId(userId);
        var recentTransactions = transactionService.findByUserId(userId, PageRequest.of(0, 5));

        model.addAttribute("dashboard", data);
        model.addAttribute("accounts", accounts);
        model.addAttribute("recentTransactions", recentTransactions.getContent());
        model.addAttribute("userName", SecurityUtil.getCurrentUser().getName());

        return "app/dashboard";
    }
}
