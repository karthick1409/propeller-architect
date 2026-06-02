package com.corplearning.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class InterventionService {

    private final InterventionRepository interventionRepository;
    private final InterventionOutcomeRepository outcomeRepository;
    private final EmployeeRepository employeeRepository;

    public InterventionService(InterventionRepository interventionRepository,
                               InterventionOutcomeRepository outcomeRepository,
                               EmployeeRepository employeeRepository) {
        this.interventionRepository = interventionRepository;
        this.outcomeRepository = outcomeRepository;
        this.employeeRepository = employeeRepository;
    }

    public InterventionEntity assign(Long employeeId, String type, String assignee,
                                     LocalDate scheduledDate, Long riskAssessmentId, String notes) {
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        InterventionEntity i = new InterventionEntity();
        i.setEmployeeId(employeeId);
        i.setType(type);
        i.setAssignee(assignee);
        i.setScheduledDate(scheduledDate);
        i.setRiskAssessmentId(riskAssessmentId);
        i.setStatus("ASSIGNED");
        i.setNotes(notes);
        return interventionRepository.save(i);
    }

    public InterventionEntity updateStatus(Long id, String status, String notes) {
        InterventionEntity i = interventionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Intervention not found"));
        i.setStatus(status);
        if (notes != null) i.setNotes(notes);
        return interventionRepository.save(i);
    }

    @Transactional
    public InterventionOutcomeEntity recordOutcome(Long interventionId, String outcome, String summary) {
        InterventionEntity i = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new IllegalArgumentException("Intervention not found"));
        i.setStatus("COMPLETED");
        interventionRepository.save(i);
        InterventionOutcomeEntity o = new InterventionOutcomeEntity();
        o.setInterventionId(interventionId);
        o.setOutcome(outcome);
        o.setSummary(summary);
        o.setEffectiveness("PENDING_REEVALUATION");
        return outcomeRepository.save(o);
    }

    public List<Map<String, Object>> listByEmployee(Long employeeId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (InterventionEntity i : interventionRepository.findByEmployeeId(employeeId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("type", i.getType());
            m.put("assignee", i.getAssignee());
            m.put("scheduledDate", i.getScheduledDate());
            m.put("status", i.getStatus());
            m.put("notes", i.getNotes());
            list.add(m);
        }
        return list;
    }

    public List<Map<String, Object>> listAll() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (InterventionEntity i : interventionRepository.findAll()) {
            employeeRepository.findById(i.getEmployeeId()).ifPresent(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", i.getId());
                m.put("employeeNumber", e.getEmployeeNumber());
                m.put("fullName", e.getFullName());
                m.put("type", i.getType());
                m.put("status", i.getStatus());
                m.put("assignee", i.getAssignee());
                list.add(m);
            });
        }
        return list;
    }
}

