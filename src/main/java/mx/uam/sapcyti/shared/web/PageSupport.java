package mx.uam.sapcyti.shared.web;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Helper to build a {@link Page} from an already materialised, filtered list.
 *
 * <p>Catalog listings enrich aggregates with data owned by another bounded context (the
 * {@code identity} {@code User}), so filtering and slicing happen in memory after that join
 * rather than in a single cross-context query. This keeps the slicing logic in one place.
 */
public final class PageSupport {

    private PageSupport() {
    }

    public static <T> Page<T> paginate(List<T> content, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= content.size()) {
            return new PageImpl<>(List.of(), pageable, content.size());
        }
        int end = Math.min(start + pageable.getPageSize(), content.size());
        return new PageImpl<>(content.subList(start, end), pageable, content.size());
    }
}
