package kr.ac.knue.achievement;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import kr.ac.knue.achievement.codes.CodeGroupManagementService;
import kr.ac.knue.achievement.codes.DetailCodeManagementService;
import kr.ac.knue.achievement.menus.MenuInformationManagementService;
import kr.ac.knue.achievement.menus.MenuPermissionManagementService;
import kr.ac.knue.achievement.menus.MenuStructureManagementService;
import kr.ac.knue.achievement.organizations.OrganizationManagementService;
import kr.ac.knue.achievement.roles.RoleManagementService;
import kr.ac.knue.achievement.userroles.UserRoleManagementService;
import kr.ac.knue.achievement.users.UserManagementService;

@SpringBootApplication
@Import(FacultyAchievementApplication.TestProfileFallbackConfiguration.class)
public class FacultyAchievementApplication {
    public static void main(String[] args) {
        SpringApplication.run(FacultyAchievementApplication.class, args);
    }

    @Configuration
    @Profile("test")
    static class TestProfileFallbackConfiguration {
        @Bean(name = "testFallbackCodeGroupManagementService") @ConditionalOnMissingBean
        CodeGroupManagementService codeGroupManagementService() { return fallback(CodeGroupManagementService.class); }
        @Bean(name = "testFallbackDetailCodeManagementService") @ConditionalOnMissingBean
        DetailCodeManagementService detailCodeManagementService() { return fallback(DetailCodeManagementService.class); }
        @Bean(name = "testFallbackMenuInformationManagementService") @ConditionalOnMissingBean
        MenuInformationManagementService menuInformationManagementService() { return fallback(MenuInformationManagementService.class); }
        @Bean(name = "testFallbackMenuPermissionManagementService") @ConditionalOnMissingBean
        MenuPermissionManagementService menuPermissionManagementService() { return fallback(MenuPermissionManagementService.class); }
        @Bean(name = "testFallbackMenuStructureManagementService") @ConditionalOnMissingBean
        MenuStructureManagementService menuStructureManagementService() { return fallback(MenuStructureManagementService.class); }
        @Bean(name = "testFallbackOrganizationManagementService") @ConditionalOnMissingBean
        OrganizationManagementService organizationManagementService() { return fallback(OrganizationManagementService.class); }
        @Bean(name = "testFallbackRoleManagementService") @ConditionalOnMissingBean
        RoleManagementService roleManagementService() { return fallback(RoleManagementService.class); }
        @Bean(name = "testFallbackUserRoleManagementService") @ConditionalOnMissingBean
        UserRoleManagementService userRoleManagementService() { return fallback(UserRoleManagementService.class); }
        @Bean(name = "testFallbackUserManagementService") @ConditionalOnMissingBean
        UserManagementService userManagementService() { return fallback(UserManagementService.class); }

        @SuppressWarnings("unchecked")
        private <T> T fallback(Class<T> type) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> type.getSimpleName() + " test fallback";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == boolean.class) return false;
                if (returnType == int.class) return 0;
                if (returnType == long.class) return 0L;
                if (returnType == double.class) return 0D;
                if (returnType == float.class) return 0F;
                if (List.class.isAssignableFrom(returnType)) return List.of();
                if (Set.class.isAssignableFrom(returnType)) return Set.of();
                if (Map.class.isAssignableFrom(returnType)) return Collections.emptyMap();
                return null;
            });
        }
    }
}
