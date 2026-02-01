package org.refit.refitbackend.domain.task.repository;

import org.refit.refitbackend.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, String> {
}
