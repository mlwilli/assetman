package com.github.mlwilli.assetman.identity.web

import com.github.mlwilli.assetman.identity.service.CompanySelectionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/companies")
@PreAuthorize("isAuthenticated()")
class CompanySelectionController(
    private val service: CompanySelectionService
) {

    @GetMapping("/mine")
    fun mine(): List<MyCompanyDto> = service.myCompanies()

    @PostMapping("/select")
    fun select(@Valid @RequestBody req: SelectCompanyRequest): ResponseEntity<SelectCompanyResponse> {
        val token = service.selectCompany(UUID.fromString(req.companyId))
        return ResponseEntity.ok(SelectCompanyResponse(accessToken = token))
    }
}
