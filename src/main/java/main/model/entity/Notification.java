package main.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @Column(name = "user_id_long", nullable = false)
    private String userIdLong;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", columnDefinition = "enum('ACCEPT', 'DENY')", nullable = false)
    private NotificationStatus notificationStatus;

    public enum NotificationStatus {
        ACCEPT,
        DENY
    }
}
