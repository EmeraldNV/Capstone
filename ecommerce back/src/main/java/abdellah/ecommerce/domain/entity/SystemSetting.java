package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import abdellah.ecommerce.domain.enums.SystemSettingValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "system_setting")
public class SystemSetting extends TimestampedEntity {

    @NotBlank
    @Size(max = 100)
    @Column(name = "setting_key", nullable = false, length = 100, unique = true)
    private String settingKey;

    @NotBlank
    @Column(name = "setting_value", nullable = false, columnDefinition = "text")
    private String settingValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private  SystemSettingValueType valueType;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @NotNull
    @Column(name = "is_sensitive", nullable = false)
    private Boolean sensitive = Boolean.FALSE;
}
