package com.slamshoes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.PerfilCliente;

@Repository
public interface PerfilClienteRepository extends JpaRepository<PerfilCliente, Long> {

    Optional<PerfilCliente> findByIdUsuario(Long idUsuario);
}
