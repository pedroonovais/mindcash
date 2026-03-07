package com.mindcash.app.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injeta atributos globais no model para todas as views Thymeleaf.
 * Evita uso de #httpServletRequest (removido no Thymeleaf 3.1).
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }
}
