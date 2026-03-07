package com.mindcash.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

    @NotBlank(message = "Nome da categoria é obrigatório")
    @Size(max = 80, message = "Nome deve ter no máximo 80 caracteres")
    private String name;

    @Size(max = 50)
    private String icon;

    private Long parentId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
