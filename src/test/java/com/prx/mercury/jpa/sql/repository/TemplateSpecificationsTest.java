package com.prx.mercury.jpa.sql.repository;

import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.repository.TemplateSpecifications;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TemplateSpecifications unit tests")
class TemplateSpecificationsTest {

    @SuppressWarnings("unchecked")
    private Root<TemplateEntity> mockRoot() {
        return mock(Root.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("applies application scope, active, templateType, severityType and q predicates when all are provided")
    void search_allFiltersProvided() {
        Root<TemplateEntity> root = mockRoot();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Object> applicationPath = mock(Path.class);
        Path<Object> applicationIdPath = mock(Path.class);
        when(root.get("application")).thenReturn(applicationPath);
        when(applicationPath.get("id")).thenReturn(applicationIdPath);

        Path<Object> activePath = mock(Path.class);
        when(root.get("active")).thenReturn(activePath);

        Path<Object> templateTypePath = mock(Path.class);
        Path<Object> templateTypeIdPath = mock(Path.class);
        when(root.get("templateType")).thenReturn(templateTypePath);
        when(templateTypePath.get("id")).thenReturn(templateTypeIdPath);

        Path<Object> severityTypePath = mock(Path.class);
        Path<Object> severityTypeIdPath = mock(Path.class);
        when(root.get("severityType")).thenReturn(severityTypePath);
        when(severityTypePath.get("id")).thenReturn(severityTypeIdPath);

        Path<Object> descriptionPath = mock(Path.class);
        Path<Object> locationPath = mock(Path.class);
        when(root.get("description")).thenReturn(descriptionPath);
        when(root.get("location")).thenReturn(locationPath);

        Expression<String> lowerDesc = mock(Expression.class);
        Expression<String> lowerLoc = mock(Expression.class);
        when(cb.lower(any())).thenReturn(lowerDesc, lowerLoc);

        UUID applicationId = UUID.randomUUID();
        UUID templateTypeId = UUID.randomUUID();
        UUID severityTypeId = UUID.randomUUID();

        Predicate appPred = mock(Predicate.class);
        Predicate activePred = mock(Predicate.class);
        Predicate typePred = mock(Predicate.class);
        Predicate severityPred = mock(Predicate.class);
        Predicate likeDescPred = mock(Predicate.class);
        Predicate likeLocPred = mock(Predicate.class);
        Predicate orPred = mock(Predicate.class);
        Predicate andPred = mock(Predicate.class);

        when(cb.equal(applicationIdPath, applicationId)).thenReturn(appPred);
        when(cb.equal(activePath, Boolean.TRUE)).thenReturn(activePred);
        when(cb.equal(templateTypeIdPath, templateTypeId)).thenReturn(typePred);
        when(cb.equal(severityTypeIdPath, severityTypeId)).thenReturn(severityPred);
        when(cb.like(lowerDesc, "%welcome%")).thenReturn(likeDescPred);
        when(cb.like(lowerLoc, "%welcome%")).thenReturn(likeLocPred);
        when(cb.or(likeDescPred, likeLocPred)).thenReturn(orPred);
        when(cb.and(any(Predicate[].class))).thenReturn(andPred);

        Specification<TemplateEntity> spec =
                TemplateSpecifications.search(applicationId, "Welcome", templateTypeId, severityTypeId, true);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(andPred);
        verify(cb).and(new Predicate[]{appPred, activePred, typePred, severityPred, orPred});
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("only the mandatory application predicate is applied when every other filter is null")
    void search_onlyApplicationScope_whenNoOtherFilters() {
        Root<TemplateEntity> root = mockRoot();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Object> applicationPath = mock(Path.class);
        Path<Object> applicationIdPath = mock(Path.class);
        when(root.get("application")).thenReturn(applicationPath);
        when(applicationPath.get("id")).thenReturn(applicationIdPath);

        UUID applicationId = UUID.randomUUID();
        Predicate appPred = mock(Predicate.class);
        Predicate andPred = mock(Predicate.class);
        when(cb.equal(applicationIdPath, applicationId)).thenReturn(appPred);
        when(cb.and(any(Predicate[].class))).thenReturn(andPred);

        Specification<TemplateEntity> spec = TemplateSpecifications.search(applicationId, null, null, null, null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(andPred);
        verify(cb).and(new Predicate[]{appPred});
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("a blank q does not add a text-search predicate")
    void search_blankQ_isIgnored() {
        Root<TemplateEntity> root = mockRoot();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Object> applicationPath = mock(Path.class);
        Path<Object> applicationIdPath = mock(Path.class);
        when(root.get("application")).thenReturn(applicationPath);
        when(applicationPath.get("id")).thenReturn(applicationIdPath);

        UUID applicationId = UUID.randomUUID();
        Predicate appPred = mock(Predicate.class);
        Predicate andPred = mock(Predicate.class);
        when(cb.equal(applicationIdPath, applicationId)).thenReturn(appPred);
        when(cb.and(any(Predicate[].class))).thenReturn(andPred);

        Specification<TemplateEntity> spec = TemplateSpecifications.search(applicationId, "   ", null, null, null);
        spec.toPredicate(root, query, cb);

        verify(cb).and(new Predicate[]{appPred});
    }
}
