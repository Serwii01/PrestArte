package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.EmailAlreadyExistsException;
import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.TransportCompanyProfileDto;
import com.prestarte.tfg.model.dto.UpdateTransportCompanyRequest;
import com.prestarte.tfg.model.entity.TransportCompany;
import com.prestarte.tfg.model.entity.UserStatus;
import com.prestarte.tfg.repository.TransportCompanyRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransportCompanyService {

    private final TransportCompanyRepository transportCompanyRepository;
    private final CurrentUser currentUser;

    @Transactional
    public TransportCompany registerCompany(TransportCompany company) {
        if (transportCompanyRepository.existsByTaxId(company.getTaxId())) {
            throw new EmailAlreadyExistsException("Ya existe una empresa registrada con el Tax ID: " + company.getTaxId());
        }
        return transportCompanyRepository.save(company);
    }

    /**
     * Lista de empresas aprobadas y activas: lo que se muestra en cualquier
     * desplegable de selección y en el catálogo público de partners.
     */
    @Transactional(readOnly = true)
    public List<TransportCompany> getAllCompanies() {
        return transportCompanyRepository.findAll().stream()
                .filter(c -> c.isEnabled() && c.getStatus() == UserStatus.APPROVED)
                .toList();
    }

    /** Perfil público de empresas aprobadas (DTO sin datos sensibles). */
    @Transactional(readOnly = true)
    public List<TransportCompanyProfileDto> getPublicProfiles() {
        return getAllCompanies().stream().map(this::toProfile).toList();
    }

    @Transactional(readOnly = true)
    public TransportCompanyProfileDto getPublicProfile(Long id) {
        TransportCompany c = transportCompanyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Empresa de transporte", id));
        if (!c.isEnabled() || c.getStatus() != UserStatus.APPROVED) {
            throw ResourceNotFoundException.of("Empresa de transporte", id);
        }
        return toProfile(c);
    }

    @Transactional(readOnly = true)
    public TransportCompany getById(Long id) {
        return transportCompanyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Empresa de transporte", id));
    }

    /**
     * El propietario de la empresa actualiza su ficha pública.
     * Solo ese usuario (o un admin) puede llamar a este endpoint.
     */
    @Transactional
    public TransportCompanyProfileDto updateProfile(Long id, UpdateTransportCompanyRequest req) {
        TransportCompany c = getById(id);
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(c.getId());
        }
        if (req.getCompanyName() != null) c.setCompanyName(req.getCompanyName());
        if (req.getContactEmail() != null) c.setContactEmail(req.getContactEmail());
        if (req.getWebsite() != null) c.setWebsite(req.getWebsite());
        if (req.getDescription() != null) c.setDescription(req.getDescription());
        if (req.getSpecialties() != null) c.setSpecialties(req.getSpecialties());
        if (req.getLocations() != null) c.setLocations(req.getLocations());
        if (req.getCoverageArea() != null) c.setCoverageArea(req.getCoverageArea());
        return toProfile(transportCompanyRepository.save(c));
    }

    @Transactional
    public void deleteCompany(Long id) {
        if (!transportCompanyRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Empresa de transporte", id);
        }
        transportCompanyRepository.deleteById(id);
    }

    private TransportCompanyProfileDto toProfile(TransportCompany c) {
        return TransportCompanyProfileDto.builder()
                .id(c.getId())
                .companyName(c.getCompanyName())
                .contactEmail(c.getContactEmail())
                .website(c.getWebsite())
                .description(c.getDescription())
                .specialties(c.getSpecialties())
                .locations(c.getLocations())
                .coverageArea(c.getCoverageArea())
                .build();
    }
}
