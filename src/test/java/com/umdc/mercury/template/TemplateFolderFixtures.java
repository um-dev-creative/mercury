package com.umdc.mercury.template;

import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import com.umdc.mercury.jpa.sql.repository.ApplicationRepository;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Builds {@link ApplicationTemplateFolders} over applications whose {@code code_name} the test chooses. */
public final class TemplateFolderFixtures {

    private TemplateFolderFixtures() {
    }

    /** An application as the database returns it: {@code code_name} is read-only on the entity. */
    public static ApplicationEntity application(UUID id, String codeName) {
        try {
            ApplicationEntity entity = new ApplicationEntity();
            entity.setId(id);
            var field = ApplicationEntity.class.getDeclaredField("codeName");
            field.setAccessible(true);
            field.set(entity, codeName);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Folders for an application with that id and code name; any other application has none. */
    public static ApplicationTemplateFolders folders(UUID id, String codeName) {
        ApplicationRepository repository = mock(ApplicationRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(application(id, codeName)));
        return new ApplicationTemplateFolders(repository);
    }

    /** Folders where no application is known, so every template stays at the root. */
    public static ApplicationTemplateFolders noFolders() {
        return new ApplicationTemplateFolders(mock(ApplicationRepository.class));
    }
}
