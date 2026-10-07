package com.hrms.config;

import com.hrms.entity.Permission;
import com.hrms.entity.Role;
import com.hrms.entity.User;
import com.hrms.entity.enums.UserStatus;
import com.hrms.repository.PermissionRepository;
import com.hrms.repository.RoleRepository;
import com.hrms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
public class SuperAdminBootstrapRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminBootstrapRunner.class);

    public static final String SUPER_ADMIN_ROLE_NAME = "ROLE_SUPER_ADMIN";
    public static final String SUPER_ADMIN_ROLE_ALT_NAME = "SUPER_ADMIN";

    public static final List<String> REQUIRED_PERMISSIONS = List.of(
            "PROFILE_VIEW_SELF",
            "PROFILE_UPDATE_SELF",
            "ATTENDANCE_MARK",
            "LEAVE_APPLY",
            "TEAM_VIEW",
            "LEAVE_REVIEW_TEAM",
            "ADMIN_CREATE",
            "HR_CREATE",
            "MANAGER_CREATE",
            "EMPLOYEE_CREATE",
            "EMPLOYEE_VIEW",
            "EMPLOYEE_UPDATE",
            "SALARY_MANAGE",
            "ROLE_MANAGE"
    );

    @Value("${app.bootstrap.super-admin.username:superadmin}")
    private String superAdminUsername;

    @Value("${app.bootstrap.super-admin.email:}")
    private String superAdminEmail;

    @Value("${app.bootstrap.super-admin.password:}")
    private String superAdminPassword;

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminBootstrapRunner(PermissionRepository permissionRepository,
                                     RoleRepository roleRepository,
                                     UserRepository userRepository,
                                     PasswordEncoder passwordEncoder) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Initializing HRMS Super Admin Bootstrap...");

        // 1-4. Sync Permissions
        List<Permission> permissions = syncPermissions();

        // 5-9. Sync SUPER_ADMIN Role
        Role superAdminRole = syncSuperAdminRole(permissions);

        // 10-16. Sync Super Admin User
        syncSuperAdminUser(superAdminRole);

        logger.info("HRMS Super Admin Bootstrap execution finished.");
    }

    private List<Permission> syncPermissions() {
        List<Permission> syncedPermissions = new ArrayList<>();

        for (String permName : REQUIRED_PERMISSIONS) {
            Permission permission = permissionRepository.findByName(permName)
                    .orElseGet(() -> {
                        logger.info("Creating missing permission: {}", permName);
                        return permissionRepository.save(Permission.builder()
                                .name(permName)
                                .description("Permission to " + permName.toLowerCase().replace('_', ' '))
                                .module(deriveModule(permName))
                                .build());
                    });
            syncedPermissions.add(permission);
        }

        return syncedPermissions;
    }

    private Role syncSuperAdminRole(List<Permission> permissions) {
        Role role = roleRepository.findByName(SUPER_ADMIN_ROLE_NAME)
                .or(() -> roleRepository.findByName(SUPER_ADMIN_ROLE_ALT_NAME))
                .orElseGet(() -> {
                    logger.info("Creating missing role: {}", SUPER_ADMIN_ROLE_NAME);
                    return roleRepository.save(Role.builder()
                            .name(SUPER_ADMIN_ROLE_NAME)
                            .description("Super Administrator with full system capabilities")
                            .permissions(new HashSet<>())
                            .build());
                });

        Set<Permission> currentPermissions = role.getPermissions();
        if (currentPermissions == null) {
            currentPermissions = new HashSet<>();
            role.setPermissions(currentPermissions);
        }

        boolean updated = false;
        for (Permission perm : permissions) {
            boolean exists = currentPermissions.stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(perm.getName()));
            if (!exists) {
                currentPermissions.add(perm);
                updated = true;
            }
        }

        if (updated) {
            role = roleRepository.save(role);
            logger.info("Updated {} role permissions.", role.getName());
        }

        return role;
    }

    private void syncSuperAdminUser(Role superAdminRole) {
        if (!StringUtils.hasText(superAdminEmail)) {
            // Check if any Super Admin user exists
            boolean superAdminExists = userRepository.findAll().stream()
                    .anyMatch(u -> u.getRoles().stream()
                            .anyMatch(r -> r.getName().equalsIgnoreCase(SUPER_ADMIN_ROLE_NAME) || r.getName().equalsIgnoreCase(SUPER_ADMIN_ROLE_ALT_NAME)));

            if (!superAdminExists) {
                throw new IllegalStateException("Super Admin bootstrap failed: 'app.bootstrap.super-admin.email' property must be specified.");
            }
            logger.info("Super Admin user exists. Skipping creation as email property is omitted.");
            return;
        }

        Optional<User> optionalUser = userRepository.findByEmail(superAdminEmail);

        if (optionalUser.isEmpty()) {
            if (!StringUtils.hasText(superAdminPassword)) {
                throw new IllegalStateException("Super Admin bootstrap failed: 'app.bootstrap.super-admin.password' property must be specified when creating a new Super Admin user.");
            }

            logger.info("Creating new Super Admin user with email: {}", superAdminEmail);

            String usernameToUse = StringUtils.hasText(superAdminUsername)
                    ? superAdminUsername
                    : superAdminEmail.split("@")[0];

            if (userRepository.existsByUsername(usernameToUse)) {
                usernameToUse = usernameToUse + "_" + System.currentTimeMillis();
            }

            User newSuperAdmin = User.builder()
                    .email(superAdminEmail)
                    .username(usernameToUse)
                    .password(passwordEncoder.encode(superAdminPassword))
                    .status(UserStatus.ACTIVE)
                    .roles(new HashSet<>(Set.of(superAdminRole)))
                    .build();

            userRepository.save(newSuperAdmin);
            logger.info("Super Admin user created successfully.");
        } else {
            User existingUser = optionalUser.get();
            logger.info("Super Admin user already exists with email: {}", superAdminEmail);

            Set<Role> currentRoles = existingUser.getRoles();
            if (currentRoles == null) {
                currentRoles = new HashSet<>();
                existingUser.setRoles(currentRoles);
            }

            boolean hasRole = currentRoles.stream()
                    .anyMatch(r -> r.getName().equalsIgnoreCase(superAdminRole.getName()));

            if (!hasRole) {
                currentRoles.add(superAdminRole);
                userRepository.save(existingUser);
                logger.info("Assigned role {} to existing user {}", superAdminRole.getName(), superAdminEmail);
            }
        }
    }

    private String deriveModule(String permissionName) {
        if (permissionName.startsWith("PROFILE_")) return "PROFILE";
        if (permissionName.startsWith("ATTENDANCE_")) return "ATTENDANCE";
        if (permissionName.startsWith("LEAVE_")) return "LEAVE";
        if (permissionName.startsWith("TEAM_")) return "TEAM";
        if (permissionName.startsWith("EMPLOYEE_")) return "EMPLOYEE";
        if (permissionName.startsWith("SALARY_")) return "PAYROLL";
        if (permissionName.startsWith("ROLE_") || permissionName.endsWith("_CREATE")) return "ADMIN";
        return "SYSTEM";
    }
}
