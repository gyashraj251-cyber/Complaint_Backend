package com.college.complaint_portal.service;

import com.college.complaint_portal.dto.AdminRegistrationRequest;
import com.college.complaint_portal.dto.OtpVerificationRequest;
import com.college.complaint_portal.entity.AdminUser;
import com.college.complaint_portal.repository.AdminUserRepository;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AdminService {

    @Autowired
    private AdminUserRepository adminUserRepository;

    // Resend API key injected from application.properties
    @Value("${resend.api.key}")
    private String resendApiKey;

    // Master Secret Key for creating admin accounts
    @Value("${admin.creation.secret-key:COLLEGE_ADMIN_SECRET_2026}")
    private String masterSecretKey;

    public void registerAdmin(AdminRegistrationRequest request) {
        // Step 1: Validate Secret Creation Key
        if (!masterSecretKey.equals(request.getSecretKey())) {
            throw new IllegalArgumentException("Invalid Admin Creation Secret Key!");
        }

        // Step 2: Check if admin already exists
        if (adminUserRepository.existsByEmail(request.getEmail())) {
            AdminUser existingUser = adminUserRepository.findByEmail(request.getEmail()).get();
            if (existingUser.isVerified()) {
                throw new IllegalStateException("An admin account with this email already exists and is active.");
            }
        }

        // Step 3: Generate 6-Digit Random OTP
        String otp = String.format("%06d", new Random().nextInt(900000) + 100000);

        // Step 4: Save or Update Admin Entity
        AdminUser admin = adminUserRepository.findByEmail(request.getEmail())
                .orElse(new AdminUser());

        admin.setEmail(request.getEmail());
        admin.setPassword(request.getPassword()); // Recommended: Use BCryptPasswordEncoder here for security
        admin.setOtp(otp);
        admin.setOtpGeneratedTime(LocalDateTime.now());
        admin.setVerified(false);

        adminUserRepository.save(admin);

        // Step 5: Send OTP Email via Resend HTTPS API
        sendOtpEmail(request.getEmail(), otp);
    }

    public boolean verifyOtp(OtpVerificationRequest request) {
        AdminUser admin = adminUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No pending registration found for this email."));

        if (admin.getOtp() == null || !admin.getOtp().equals(request.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP entered.");
        }

        // Check if OTP has expired (valid for 5 minutes)
        if (admin.getOtpGeneratedTime().plusMinutes(5).isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("OTP has expired. Please register again to generate a new OTP.");
        }

        // Mark as verified and clear OTP
        admin.setVerified(true);
        admin.setOtp(null);
        adminUserRepository.save(admin);

        return true;
    }

    public boolean validateLogin(String email, String password) {
        Optional<AdminUser> adminOptional = adminUserRepository.findByEmail(email);

        if (adminOptional.isPresent()) {
            AdminUser admin = adminOptional.get();
            // Verify both that the account is active/verified and that passwords match
            return admin.isVerified() && admin.getPassword().equals(password);
        }

        return false;
    }

    private void sendOtpEmail(String recipientEmail, String otp) {
        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Campus Portal <onboarding@resend.dev>") // Free default testing sender on Resend
                .to(recipientEmail)
                .subject("Admin Registration OTP Verification")
                .html("<p>Welcome to the Admin Panel.</p><p>Your OTP for verification is: <strong>" + otp + "</strong></p><p>This code will expire in 5 minutes.</p>")
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            System.out.println("OTP email dispatched successfully. Resend ID: " + response.getId());
        } catch (ResendException e) {
            System.err.println("Resend email delivery failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }
}