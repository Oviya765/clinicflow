package com.HospitalManagement.controller;

import com.HospitalManagement.requestdto.LabOrderRequestDto;
import com.HospitalManagement.requestdto.LabResultRequestDto;
import com.HospitalManagement.responsedto.LabOrderResponseDto;
import com.HospitalManagement.responsedto.LabResultResponseDto;
import com.HospitalManagement.service.LabOrderService;
import com.HospitalManagement.service.LabResultService;
import com.HospitalManagement.service.AuthService;
import com.HospitalManagement.service.PatientService;
import com.HospitalManagement.entity.User;
import com.HospitalManagement.enums.Roles;
import com.HospitalManagement.responsedto.PatientResponseDto;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lab")
public class LabController {

    private final LabOrderService labOrderService;
    private final LabResultService labResultService;
    private final AuthService authService;
    private final PatientService patientService;

    public LabController(LabOrderService labOrderService,
                         LabResultService labResultService,
                         AuthService authService,
                         PatientService patientService) {
        this.labOrderService = labOrderService;
        this.labResultService = labResultService;
        this.authService = authService;
        this.patientService = patientService;
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'CLINICIAN', 'ADMIN', 'PATIENT')")
    public List<LabOrderResponseDto> getAllOrders() {
        User currentUser = authService.getAuthenticatedUser();
        if (currentUser.getRole() == Roles.PATIENT) {
            PatientResponseDto patient = patientService.getPatientByEmail(currentUser.getEmail());
            if (patient == null) {
                return List.of();
            }
            return labOrderService.getOrdersByPatientId(patient.patientId());
        }
        return labOrderService.getAllOrders();
    }

    @GetMapping("/orders/{labOrderId}")
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'CLINICIAN', 'ADMIN', 'PATIENT')")
    public LabOrderResponseDto getOrderById(@PathVariable Long labOrderId) {
        LabOrderResponseDto order = labOrderService.getOrderById(labOrderId);
        User currentUser = authService.getAuthenticatedUser();
        if (currentUser.getRole() == Roles.PATIENT) {
            PatientResponseDto patient = patientService.getPatientByEmail(currentUser.getEmail());
            if (patient == null || !order.patientId().equals(patient.patientId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You are not authorized to view this lab order.");
            }
        }
        return order;
    }

    @GetMapping("/orders/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'CLINICIAN', 'ADMIN', 'PATIENT')")
    public List<LabOrderResponseDto> getOrdersByPatientId(@PathVariable Long patientId) {
        User currentUser = authService.getAuthenticatedUser();
        if (currentUser.getRole() == Roles.PATIENT) {
            PatientResponseDto patient = patientService.getPatientByEmail(currentUser.getEmail());
            if (patient == null || !patientId.equals(patient.patientId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You are not authorized to view other patient's lab orders.");
            }
        }
        return labOrderService.getOrdersByPatientId(patientId);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLINICIAN')")
    public LabOrderResponseDto createOrder(@Valid @RequestBody LabOrderRequestDto requestDto) {
        return labOrderService.createOrder(requestDto);
    }

    @PutMapping("/orders/{labOrderId}")
    @PreAuthorize("hasAuthority('CLINICIAN')")
    public LabOrderResponseDto updateOrder(
            @PathVariable Long labOrderId,
            @Valid @RequestBody LabOrderRequestDto requestDto
    ) {
        return labOrderService.updateOrder(labOrderId, requestDto);
    }

    @DeleteMapping("/orders/{labOrderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public LabOrderResponseDto cancelOrder(@PathVariable Long labOrderId) {
        return labOrderService.cancelOrder(labOrderId);
    }

    @PatchMapping("/orders/{labOrderId}/collect")
    @PreAuthorize("hasAuthority('LAB_TECHNICIAN')")
    public LabOrderResponseDto collectSample(@PathVariable Long labOrderId) {
        return labOrderService.collectSample(labOrderId);
    }

    @GetMapping("/results")
    @PreAuthorize("hasAuthority('LAB_TECHNICIAN')")
    public List<LabResultResponseDto> getAllResults() {
        return labResultService.getAllResults();
    }

    @GetMapping("/results/{resultId}")
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'CLINICIAN', 'ADMIN', 'PATIENT')")
    public LabResultResponseDto getResultById(@PathVariable Long resultId) {
        LabResultResponseDto result = labResultService.getResultById(resultId);
        LabOrderResponseDto order = labOrderService.getOrderById(result.labOrderId());
        User currentUser = authService.getAuthenticatedUser();
        if (currentUser.getRole() == Roles.PATIENT) {
            PatientResponseDto patient = patientService.getPatientByEmail(currentUser.getEmail());
            if (patient == null || !order.patientId().equals(patient.patientId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You are not authorized to view this lab result.");
            }
        }
        return result;
    }

    @GetMapping("/orders/{labOrderId}/results")
    @PreAuthorize("hasAnyAuthority('LAB_TECHNICIAN', 'CLINICIAN', 'ADMIN', 'PATIENT')")
    public List<LabResultResponseDto> getResultsByOrderId(@PathVariable Long labOrderId) {
        LabOrderResponseDto order = labOrderService.getOrderById(labOrderId);
        User currentUser = authService.getAuthenticatedUser();
        if (currentUser.getRole() == Roles.PATIENT) {
            PatientResponseDto patient = patientService.getPatientByEmail(currentUser.getEmail());
            if (patient == null || !order.patientId().equals(patient.patientId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You are not authorized to view results for this lab order.");
            }
        }
        return labResultService.getResultsByOrderId(labOrderId);
    }

    @PostMapping("/results")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LAB_TECHNICIAN')")
    public LabResultResponseDto createResult(@Valid @RequestBody LabResultRequestDto requestDto) {
        return labResultService.createResult(requestDto);
    }

    @PutMapping("/results/{resultId}")
    @PreAuthorize("hasAuthority('LAB_TECHNICIAN')")
    public LabResultResponseDto updateResult(
            @PathVariable Long resultId,
            @Valid @RequestBody LabResultRequestDto requestDto
    ) {
        return labResultService.updateResult(resultId, requestDto);
    }

    @DeleteMapping("/results/{resultId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteResult(@PathVariable Long resultId) {
        labResultService.deleteResult(resultId);
    }
}