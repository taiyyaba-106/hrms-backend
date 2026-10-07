package com.hrms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "leave_balance",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_leave_balance_emp_type_year", columnNames = {"employee_id", "leave_type_id", "year"})
    },
    indexes = {
        @Index(name = "idx_leave_balance_employee", columnList = "employee_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_leave_balance_employee"))
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_leave_balance_leave_type"))
    private LeaveType leaveType;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "total_allocated", nullable = false)
    private Double totalAllocated;

    @Column(name = "used_days", nullable = false)
    @Builder.Default
    private Double usedDays = 0.0;

    @Column(name = "pending_days", nullable = false)
    @Builder.Default
    private Double pendingDays = 0.0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
