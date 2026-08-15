package com.example.backend.user.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String avatarUrl;
    private String college;
    private String studentId;
    private String bio;
    private Integer status; // 1 enabled, 0 disabled
    private String role; // user/admin
    private Integer hic; // HIC verification status: 0 unverified, 1 verified
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public Integer getHic() { return hic; }
    public void setHic(Integer hic) { this.hic = hic; }
    public void setCollege(String college) { this.college = college; }
    public String getCollege() { return college; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentId() { return studentId; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
