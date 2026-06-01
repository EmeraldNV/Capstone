package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.taglia.TagliaRequest;
import abdellah.ecommerce.api.dto.taglia.TagliaResponse;
import abdellah.ecommerce.domain.entity.Taglia;
import abdellah.ecommerce.exception.BadRequestApiException;
import abdellah.ecommerce.exception.ConflictApiException;
import abdellah.ecommerce.exception.NotFoundApiException;
import abdellah.ecommerce.repository.TagliaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class TagliaService {

    private final TagliaRepository tagliaRepository;

    public TagliaService(TagliaRepository tagliaRepository) {
        this.tagliaRepository = tagliaRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "taglie")
    public List<TagliaResponse> listTaglie() {
        log.debug("Loading taglie from database");
        return tagliaRepository.findAllByOrderByNomeAscCodiceAsc().stream()
                .map(TagliaService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Taglia getTagliaEntity(Long id) {
        if (id == null) {
            throw new ValidationException("Taglia id is required.");
        }
        return tagliaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Taglia not found with id " + id));
    }

    @Transactional(readOnly = true)
    public TagliaResponse getTaglia(Long id) {
        return toResponse(getTagliaEntity(id));
    }

    @Transactional
    @CacheEvict(cacheNames = "taglie", allEntries = true)
    public TagliaResponse createTaglia(TagliaRequest request) {
        validateRequest(request, null);
        Taglia taglia = new Taglia();
        taglia.setNome(normalizeNome(request.nome()));
        taglia.setCodice(normalizeCodice(request.codice()));
        log.info("Creating taglia codice={} nome={}", taglia.getCodice(), taglia.getNome());
        return toResponse(tagliaRepository.saveAndFlush(taglia));
    }

    @Transactional
    @CacheEvict(cacheNames = "taglie", allEntries = true)
    public TagliaResponse updateTaglia(Long id, TagliaRequest request) {
        Taglia taglia = getTagliaEntity(id);
        validateRequest(request, id);
        taglia.setNome(normalizeNome(request.nome()));
        taglia.setCodice(normalizeCodice(request.codice()));
        log.info("Updating taglia id={} codice={}", id, taglia.getCodice());
        return toResponse(tagliaRepository.saveAndFlush(taglia));
    }

    @Transactional
    @CacheEvict(cacheNames = "taglie", allEntries = true)
    public void deleteTaglia(Long id) {
        Taglia taglia = getTagliaEntity(id);
        if (taglia.getProducts() != null && !taglia.getProducts().isEmpty()) {
            throw new ConflictApiException("TAGLIA_IN_USE", "La taglia non puo essere eliminata perche e associata a uno o piu prodotti.");
        }
        log.info("Deleting taglia id={} codice={}", id, taglia.getCodice());
        tagliaRepository.delete(taglia);
    }

    @Transactional(readOnly = true)
    public Set<Taglia> resolveTaglieByIds(List<Long> tagliaIds) {
        if (tagliaIds == null || tagliaIds.isEmpty()) {
            throw new BadRequestApiException("TAGLIE_REQUIRED", "Seleziona almeno una taglia.");
        }
        Set<Long> distinctIds = new LinkedHashSet<>(tagliaIds);
        List<Taglia> taglie = new ArrayList<>();
        for (Taglia taglia : tagliaRepository.findAllById(distinctIds)) {
            taglie.add(taglia);
        }
        if (taglie.size() != distinctIds.size()) {
            throw new NotFoundApiException("TAGLIA_NOT_FOUND", "Una o piu taglie selezionate non esistono.");
        }
        return new LinkedHashSet<>(taglie);
    }

    private void validateRequest(TagliaRequest request, Long currentId) {
        if (request == null) {
            throw new ValidationException("Taglia payload is required.");
        }
        String nome = normalizeNome(request.nome());
        String codice = normalizeCodice(request.codice());

        boolean duplicate = currentId == null
                ? tagliaRepository.existsByCodiceIgnoreCase(codice)
                : tagliaRepository.existsByCodiceIgnoreCaseAndIdNot(codice, currentId);
        if (duplicate) {
            throw new ConflictApiException("TAGLIA_CODICE_EXISTS", "Esiste gia una taglia con questo codice.");
        }
        if (nome.isBlank() || codice.isBlank()) {
            throw new ValidationException("Taglia nome e codice sono obbligatori.");
        }
    }

    private String normalizeNome(String nome) {
        if (nome == null) {
            return "";
        }
        return nome.trim();
    }

    private String normalizeCodice(String codice) {
        if (codice == null) {
            return "";
        }
        return codice.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static TagliaResponse toResponse(Taglia taglia) {
        return new TagliaResponse(
                taglia.getId(),
                taglia.getNome(),
                taglia.getCodice(),
                taglia.getCreatedAt(),
                taglia.getUpdatedAt()
        );
    }
}
