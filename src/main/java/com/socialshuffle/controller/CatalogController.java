package com.socialshuffle.controller;

import com.socialshuffle.service.BoardGameCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
@CrossOrigin(origins = "*")
public class CatalogController {

    private final BoardGameCatalogService catalogService;

    public CatalogController(BoardGameCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/boardgames")
    public ResponseEntity<?> searchBoardGames(@RequestParam(value = "q", required = false) String query) {
        return ResponseEntity.ok(catalogService.searchCatalog(query));
    }

    @GetMapping("/boardgames/{externalId}")
    public ResponseEntity<?> getBoardGameDetails(@PathVariable("externalId") String externalId) {
        return ResponseEntity.ok(catalogService.fetchGameDetails(externalId));
    }
}
