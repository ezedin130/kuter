package com.kuter.identity;

import com.kuter.identity.dto.AddMemberDto;
import com.kuter.identity.dto.TenantMemberDetailResponse;
import com.kuter.identity.dto.TransferOwnershipDto;
import com.kuter.identity.dto.UpdateMemberDto;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing tenant staff memberships and RBAC roles.
 */
public interface TenantMembershipService {

    List<TenantMemberDetailResponse> getTenantMembers(UUID tenantId, KuterUserPrincipal principal);

    TenantMemberDetailResponse addMember(UUID tenantId, AddMemberDto dto, KuterUserPrincipal principal, HttpServletRequest httpRequest);

    TenantMemberDetailResponse updateMember(UUID tenantId, UUID memberId, UpdateMemberDto dto, KuterUserPrincipal principal, HttpServletRequest httpRequest);

    TenantMemberDetailResponse updateMemberStatus(UUID tenantId, UUID memberId, MembershipStatus status, KuterUserPrincipal principal, HttpServletRequest httpRequest);

    TenantMemberDetailResponse transferOwnership(UUID tenantId, TransferOwnershipDto dto, KuterUserPrincipal principal, HttpServletRequest httpRequest);

    void removeMember(UUID tenantId, UUID memberId, KuterUserPrincipal principal, HttpServletRequest httpRequest);
}
