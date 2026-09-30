package com.college.complaint_portal.controller;

import com.college.complaint_portal.entity.Complaint;
import com.college.complaint_portal.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService service;

    @PostMapping("/submit")
    public ResponseEntity<Complaint> createComplaint(@RequestBody Complaint complaint) {
        return ResponseEntity.ok(service.submitComplaint(complaint));
    }

    @GetMapping("/track/{complaintId}")
    public ResponseEntity<Complaint> trackComplaint(@PathVariable String complaintId) {
        return service.getComplaintById(complaintId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<Complaint>> getAllComplaints() {
        return ResponseEntity.ok(service.getAllComplaints());
    }

    @PutMapping("/admin/update/{complaintId}")
    public ResponseEntity<Complaint> updateComplaint(
            @PathVariable String complaintId,
            @RequestParam(required = false) Complaint.Status status,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(service.updateComplaint(complaintId, status, remark, department));
    }

    @DeleteMapping("/admin/delete/{complaintId}")
    public ResponseEntity<String> deleteComplaint(@PathVariable String complaintId) {
        service.deleteComplaint(complaintId);
        return ResponseEntity.ok("Complaint deleted successfully.");
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(service.getStats());
    }
}