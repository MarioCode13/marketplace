package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, java.util.UUID> {
}
