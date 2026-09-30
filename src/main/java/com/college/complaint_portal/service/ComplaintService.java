package com.college.complaint_portal.service;

import com.college.complaint_portal.entity.Complaint;
import com.college.complaint_portal.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository repository;

    public Complaint submitComplaint(Complaint complaint) {
        String uniqueId = "CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        complaint.setComplaintId(uniqueId);
        complaint.setStatus(Complaint.Status.PENDING);
        return repository.save(complaint);
    }

    public Optional<Complaint> getComplaintById(String complaintId) {
        return repository.findByComplaintId(complaintId);
    }

    public List<Complaint> getAllComplaints() {
        return repository.findAll();
    }

    public Complaint updateComplaint(String complaintId, Complaint.Status status, String remark, String department) {
        Complaint complaint = repository.findByComplaintId(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found with ID: " + complaintId));

        if (status != null) {
            complaint.setStatus(status);
        }
        if (remark != null) {
            complaint.setAdminRemark(remark);
        }
        if (department != null && !department.isBlank()) {
            complaint.setAssignedDepartment(department);
        }
        return repository.save(complaint);
    }

    public void deleteComplaint(String complaintId) {
        Complaint complaint = repository.findByComplaintId(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found with ID: " + complaintId));
        repository.delete(complaint);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", repository.count());
        stats.put("pending", repository.countByStatus(Complaint.Status.PENDING));
        stats.put("inProgress", repository.countByStatus(Complaint.Status.IN_PROGRESS));
        stats.put("resolved", repository.countByStatus(Complaint.Status.RESOLVED));
        return stats;
    }
}