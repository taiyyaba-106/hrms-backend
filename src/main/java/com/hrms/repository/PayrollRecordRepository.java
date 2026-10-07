package com.hrms.repository;

import com.hrms.entity.PayrollRecord;
import com.hrms.entity.enums.PayrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {

    List<PayrollRecord> findByEmployeeId(Long employeeId);

    Optional<PayrollRecord> findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(Long employeeId, Integer month, Integer year);

    List<PayrollRecord> findByPayPeriodMonthAndPayPeriodYear(Integer month, Integer year);

    List<PayrollRecord> findByStatus(PayrollStatus status);
}
