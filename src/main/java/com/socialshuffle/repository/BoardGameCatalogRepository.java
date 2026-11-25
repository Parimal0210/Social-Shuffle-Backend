package com.socialshuffle.repository;

import com.socialshuffle.model.BoardGameCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardGameCatalogRepository extends JpaRepository<BoardGameCatalogItem, String> {

    List<BoardGameCatalogItem> findAllByOrderByBggRankAsc();

    Optional<BoardGameCatalogItem> findFirstByExternalId(String externalId);
}
