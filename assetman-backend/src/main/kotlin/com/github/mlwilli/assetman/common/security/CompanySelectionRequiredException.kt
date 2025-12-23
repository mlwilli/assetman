package com.github.mlwilli.assetman.common.security

import org.springframework.security.access.AccessDeniedException

/**
 * Thrown when an authenticated user attempts to call application APIs
 * without selecting an active company.
 *
 * This is handled specially by JsonAccessDeniedHandler to return 409.
 */
class CompanySelectionRequiredException(
    message: String = "Company selection required. Select a company to continue."
) : AccessDeniedException(message)
