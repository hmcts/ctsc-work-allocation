package uk.gov.hmcts.reform.workallocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.reform.workallocation.model.Task;

public interface TaskRepository extends JpaRepository<Task, String> {

    @Modifying
    @Query(
        value = "truncate table task",
        nativeQuery = true
    )
    void truncateTaskTable();

}
