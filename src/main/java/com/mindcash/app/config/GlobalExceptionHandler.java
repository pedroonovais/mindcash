package com.mindcash.app.config;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ERROR_VIEW = "error/generic";
    private static final String REDIRECT_DASHBOARD = "redirect:/app/dashboard";

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(EntityNotFoundException ex, RedirectAttributes redirectAttributes,
                                 HttpServletResponse response, Model model) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        if (canRedirect(response)) {
            return REDIRECT_DASHBOARD;
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return ERROR_VIEW;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, RedirectAttributes redirectAttributes,
                                       HttpServletResponse response, Model model) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        if (canRedirect(response)) {
            return REDIRECT_DASHBOARD;
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return ERROR_VIEW;
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericError(Exception ex, RedirectAttributes redirectAttributes,
                                    HttpServletResponse response, Model model) {
        logger.error("Erro inesperado", ex);
        String message = "Ocorreu um erro inesperado. Tente novamente.";
        redirectAttributes.addFlashAttribute("error", message);
        if (canRedirect(response)) {
            return REDIRECT_DASHBOARD;
        }
        model.addAttribute("errorMessage", message);
        return ERROR_VIEW;
    }

    private static boolean canRedirect(HttpServletResponse response) {
        return !response.isCommitted();
    }
}
