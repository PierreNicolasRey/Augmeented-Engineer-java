package com.exalt.it.belair.infrastructure.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository interface for Order persistence.
 * Handles CRUD operations on OrderJpaEntity in the database.
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, String> {
}
