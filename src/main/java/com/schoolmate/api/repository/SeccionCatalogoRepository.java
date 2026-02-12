package com.schoolmate.api.repository;

import com.schoolmate.api.entity.SeccionCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeccionCatalogoRepository extends JpaRepository<SeccionCatalogo, String> {

    List<SeccionCatalogo> findByActivoTrueOrderByOrdenAsc();
}
