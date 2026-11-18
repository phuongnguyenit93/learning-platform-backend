package com.learningplatform.backend.module.knowledge.model;

import com.learningplatform.backend.module.menu.model.MenuNodeType;
import com.learningplatform.backend.module.menu.model.ModuleMenu;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.learningplatform.backend.shared.persistence.NumericBooleanConverter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Immutable;

/** Language-independent Knowledge metadata attached to one Section menu node. */
@Entity
@Immutable
@Check(constraints = "IS_ACTIVE IN (0, 1) AND MENU_NODE_TYPE = 'SECTION' AND (DIFFICULT IS NULL OR DIFFICULT IN ('BASIC', 'INTERMEDIATE', 'ADVANCED'))")
@Table(name = "MODULE_KNOWLEDGE",
        uniqueConstraints = @UniqueConstraint(name = "UQ_MK_MENU", columnNames = "MENU_ID"))
public class ModuleKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "MENU_ID", nullable = false)
    private Long menuId;

    @Enumerated(EnumType.STRING)
    @Column(name = "MENU_NODE_TYPE", nullable = false, length = 20)
    private MenuNodeType menuNodeType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "MENU_ID", referencedColumnName = "ID",
                    insertable = false, updatable = false),
            @JoinColumn(name = "MENU_NODE_TYPE", referencedColumnName = "NODE_TYPE",
                    insertable = false, updatable = false)
    })
    private ModuleMenu menu;

    @Enumerated(EnumType.STRING)
    @Column(name = "DIFFICULT", length = 20)
    private Difficulty difficult;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active;

    protected ModuleKnowledge() {
    }

    public Long getId() { return id; }
    public Long getMenuId() { return menuId; }
    public MenuNodeType getMenuNodeType() { return menuNodeType; }
    public ModuleMenu getMenu() { return menu; }
    public Difficulty getDifficult() { return difficult; }
    public Boolean getActive() { return active; }
}
