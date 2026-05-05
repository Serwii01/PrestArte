package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.TransportCompany;
import com.prestarte.tfg.repository.TransportCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransportCompanyService {

    private final TransportCompanyRepository transportCompanyRepository;

    /**
     * Registra una empresa verificando que el TaxID no exista ya.
     */
    @Transactional
    public TransportCompany registerCompany(TransportCompany company) {
        if (transportCompanyRepository.existsByTaxId(company.getTaxId())) {
            throw new RuntimeException("Ya existe una empresa registrada con el Tax ID: " + company.getTaxId());
        }
        // Aquí podrías setear el rol por defecto si no viene en el JSON
        // company.setRole(Role.TRANSPORT_COMPANY);
        return transportCompanyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public List<TransportCompany> getAllCompanies() {
        return transportCompanyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TransportCompany getById(Long id) {
        return transportCompanyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa de transporte no encontrada con ID: " + id));
    }

    /**
     * Actualiza los datos operativos de la empresa[cite: 8].
     */
    @Transactional
    public TransportCompany updateCompany(Long id, TransportCompany details) {
        TransportCompany company = getById(id);

        // Actualizamos campos específicos del transportista[cite: 5, 8]
        company.setCompanyName(details.getCompanyName());
        company.setContactEmail(details.getContactEmail());
        company.setCoverageArea(details.getCoverageArea());

        // Actualizamos campos heredados de User si es necesario[cite: 8]
        company.setName(details.getName());
        company.setEmail(details.getEmail());
        company.setPhone(details.getPhone());

        return transportCompanyRepository.save(company);
    }

    /**
     * Elimina la empresa del sistema[cite: 8].
     */
    @Transactional
    public void deleteCompany(Long id) {
        if (!transportCompanyRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: la empresa no existe.");
        }
        transportCompanyRepository.deleteById(id);
    }
}