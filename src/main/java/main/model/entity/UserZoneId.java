package main.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_zone_id")
public class UserZoneId {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "zone_id")
    private String zoneId;

    public void setZoneId(String zoneId) {
        this.zoneId = "UTC".concat(zoneId);
    }
}