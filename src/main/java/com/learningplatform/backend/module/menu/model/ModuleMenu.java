package com.learningplatform.backend.module.menu.model;

import com.learningplatform.backend.module.core.model.LearningModule;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.learningplatform.backend.shared.persistence.NumericBooleanConverter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Immutable;

/**
 * A Chapter or Section node. Only Chapter rows point directly to a Module.
 * Oracle's function-based unique indexes for scoped slugs remain database-owned.
 */
@Entity
@Immutable
@Check(constraints = "IS_ACTIVE IN (0, 1) AND NODE_TYPE IN ('CHAPTER', 'SECTION') AND ((NODE_TYPE = 'CHAPTER' AND MODULE_ID IS NOT NULL AND MENU_PARENT_ID IS NULL AND MENU_PARENT_TYPE IS NULL) OR (NODE_TYPE = 'SECTION' AND MODULE_ID IS NULL AND MENU_PARENT_ID IS NOT NULL AND MENU_PARENT_TYPE = 'CHAPTER'))")
@Table(name = "MODULE_MENU",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_MM_ID_TYPE", columnNames = {"ID", "NODE_TYPE"}),
        indexes = {
                @Index(name = "IX_MM_MODULE_SORT", columnList = "MODULE_ID, SORT_ORDER, ID"),
                @Index(name = "IX_MM_PARENT_SORT", columnList = "MENU_PARENT_ID, SORT_ORDER, ID")
        })
public class ModuleMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MODULE_ID", foreignKey = @ForeignKey(name = "FK_MM_MODULE"))
    private LearningModule module;

    @Column(name = "MENU_PARENT_ID")
    private Long menuParentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "NODE_TYPE", nullable = false, length = 20)
    private MenuNodeType nodeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "MENU_PARENT_TYPE", length = 20)
    private MenuNodeType menuParentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "MENU_PARENT_ID", referencedColumnName = "ID",
                    insertable = false, updatable = false),
            @JoinColumn(name = "MENU_PARENT_TYPE", referencedColumnName = "NODE_TYPE",
                    insertable = false, updatable = false)
    })
    private ModuleMenu parentMenu;

    @Column(name = "SLUG", nullable = false, length = 150)
    private String slug;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean active;

    protected ModuleMenu() {
    }

    public Long getId() { return id; }
    public LearningModule getModule() { return module; }
    public Long getMenuParentId() { return menuParentId; }
    public MenuNodeType getNodeType() { return nodeType; }
    public MenuNodeType getMenuParentType() { return menuParentType; }
    public ModuleMenu getParentMenu() { return parentMenu; }
    public String getSlug() { return slug; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getActive() { return active; }
}
