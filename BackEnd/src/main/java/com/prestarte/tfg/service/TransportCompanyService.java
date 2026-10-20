package com.prestarte.tfg.service;

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

/**
 * Servicio que gestiona las empresas de transporte y su perfil
 * público.
 *
 * Permite mostrar el listado de las que
 * están aprobadas y activas (utilizado en los selectores y en la
 * página de partners) y actualizar la ficha pública desde la propia
 * cuenta de la empresa o desde la administración.
 */
@Service
@RequiredArgsConstructor
public class TransportCompanyService {

    private final TransportCompanyRepository transportCompanyRepository;
    private final CurrentUser currentUser;

    /**
     * Devuelve las empresas de transporte aprobadas y activas. Es el
     * listado que se muestra en cualquier selector y en el catálogo
     * público de partners.
     */
    @Transactional(readOnly = true)
    public List<TransportCompany> getAllCompanies() {
        return transportCompanyRepository.findAll().stream()
                .filter(c -> c.isEnabled() && c.getStatus() == UserStatus.APPROVED)
                .toList();
    }

    /** Devuelve los perfiles públicos de todas las empresas aprobadas. */
    @Transactional(readOnly = true)
    public List<TransportCompanyProfileDto> getPublicProfiles() {
        return getAllCompanies().stream().map(this::toProfile).toList();
    }

    /** Devuelve el perfil público de una empresa concreta. */
    @Transactional(readOnly = true)
    public TransportCompanyProfileDto getPublicProfile(Long id) {
        TransportCompany c = transportCompanyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Empresa de transporte", id));
        if (!c.isEnabled() || c.getStatus() != UserStatus.APPROVED) {
            throw ResourceNotFoundException.of("Empresa de transporte", id);
        }
        return toProfile(c);
    }

    /** Devuelve la entidad completa por su identificador. */
    @Transactional(readOnly = true)
    public TransportCompany getById(Long id) {
        return transportCompanyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Empresa de transporte", id));
    }

    /**
     * Actualiza el perfil público de la empresa. Solo el titular de
     * la cuenta o un administrador pueden ejecutar la acción.
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
        if (req.getTariffBaseFee() != null) c.setTariffBaseFee(req.getTariffBaseFee());
        if (req.getTariffPerKm() != null) c.setTariffPerKm(req.getTariffPerKm());
        if (req.getTariffCrate() != null) c.setTariffCrate(req.getTariffCrate());
        if (req.getTariffBaseFee() != null) c.setTariffBaseFee(req.getTariffBaseFee());
        if (req.getTariffPerKm() != null) c.setTariffPerKm(req.getTariffPerKm());
        if (req.getTariffCrate() != null) c.setTariffCrate(req.getTariffCrate());
        return toProfile(transportCompanyRepository.save(c));
    }

    /** Elimina una empresa de transporte. Reservado a la administración. */
    @Transactional
    public void deleteCompany(Long id) {
        if (!currentUser.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo la administración puede eliminar empresas de transporte");
        }
        if (!transportCompanyRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Empresa de transporte", id);
        }
        transportCompanyRepository.deleteById(id);
    }

    /** Compone el DTO público a partir de la entidad de empresa. */
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
                .tariffBaseFee(c.getTariffBaseFee())
                .tariffPerKm(c.getTariffPerKm())
                .tariffCrate(c.getTariffCrate())
                .build();
    }
}
