package de.tehmanu.skybad.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.Arrays;
import java.util.List;

/**
 * @author TehManu
 * @since 11.10.2024
 */
@Getter
@AllArgsConstructor
public enum SkyBadRole {

    OWNER(1294000068415717376L),
    CO_OWNER(1295407373443399862L),
    SENIOR_MANAGER(1295406880381734912L),
    MANAGER(1294276299283632220L),
    TECHNICIAN(1295418756662562918L),
    BUILDING(1295419393840517221L),
    SENIOR_LIFEGUARD(1295409391733837844L),
    SENIOR_CASHIER(1295410218372436009L),
    LIFEGUARD(1295410660254945311L),
    CASHIER(1295411075029794880L),
    TRIAL_LIFEGUARD(1295412326761234443L),
    TRIAL_CASHIER(1295412618797781083L),
    ASSISTANCE(1295413070386171924L),
    CHECKOUT_RIGHTS(1295412869478744074L),
    CURRENTLY_ONLINE(1293585327285338122L),
    ;

    private final long roleId;

    public static boolean hasAtLeastRole(final Member member, final SkyBadRole skyBadRole) {
        List<Role> skyBadRoles = Arrays.stream(SkyBadRole.values())
                .filter(role -> role.ordinal() <= skyBadRole.ordinal())
                .map(role -> member.getGuild().getRoleById(role.getRoleId()))
                .toList();

        return member.getRoles().stream().anyMatch(skyBadRoles::contains);
    }

    public static boolean hasAnyRole(final Member member, final SkyBadRole... skyBadRole) {
        return Arrays.stream(skyBadRole).anyMatch(sbRole -> member.getRoles().stream().anyMatch(r -> r.getIdLong() == sbRole.getRoleId()));
    }

    public boolean hasRole(final Member member) {
        final Role role = member.getGuild().getRoleById(this.roleId);
        return role != null && member.getRoles().contains(role);
    }
}
