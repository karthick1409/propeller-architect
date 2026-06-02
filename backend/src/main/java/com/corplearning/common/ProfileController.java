package com.corplearning.common;

import com.corplearning.common.Employee;
import com.corplearning.common.EmployeeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final EmployeeRepository employeeRepository;

    public ProfileController(ProfileService profileService, EmployeeRepository employeeRepository) {
        this.profileService = profileService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return employeeRepository.findAll().stream()
                .map(e -> profileService.getProfileView(e.getId())).collect(Collectors.toList());
    }

    @GetMapping("/{employeeId}")
    public Map<String, Object> get(@PathVariable Long employeeId) {
        return profileService.getProfileView(employeeId);
    }

    @PostMapping("/{employeeId}/refresh")
    public Map<String, Object> refresh(@PathVariable Long employeeId) {
        profileService.refreshProfile(employeeId);
        return profileService.getProfileView(employeeId);
    }
}

