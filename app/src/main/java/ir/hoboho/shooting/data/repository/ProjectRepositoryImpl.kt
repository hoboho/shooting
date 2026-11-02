package ir.hoboho.shooting.data.repository

import ir.hoboho.shooting.core.common.Clock
import ir.hoboho.shooting.core.common.NotFoundException
import ir.hoboho.shooting.core.common.ValidationException
import ir.hoboho.shooting.data.local.ProjectDao
import ir.hoboho.shooting.data.mapper.toDomain
import ir.hoboho.shooting.data.mapper.toEntity
import ir.hoboho.shooting.domain.Validator
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val dao: ProjectDao,
    private val clock: Clock,
) : ProjectRepository {

    override fun observeProjects(): Flow<List<Project>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeActiveProjects(): Flow<List<Project>> =
        dao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getProjects(): List<Project> = dao.getAll().map { it.toDomain() }

    override suspend fun getProject(id: Long): Project? = dao.getById(id)?.toDomain()

    override suspend fun save(project: Project): Long {
        val name = project.name.trim()
        val issues = Validator.project(name, dao.countByName(name, project.id) > 0)
        if (issues.isNotEmpty()) throw ValidationException(issues)
        val now = clock.nowEpochMilli()
        val entity = project.copy(
            name = name,
            uuid = project.uuid.ifBlank { UUID.randomUUID().toString() },
            createdAt = if (project.id == 0L) now else project.createdAt,
            updatedAt = now,
        ).toEntity()
        val id = dao.upsert(entity)
        return if (id == -1L) project.id else id
    }

    override suspend fun setActive(id: Long, active: Boolean) {
        if (dao.getById(id) == null) throw NotFoundException("project $id")
        dao.setActive(id, active, clock.nowEpochMilli())
    }

    override suspend fun delete(id: Long) {
        if (dao.getById(id) == null) throw NotFoundException("project $id")
        // Room cascades workdays/operations; expenses keep their history via
        // ON DELETE SET NULL. The UI asks for confirmation before calling this.
        dao.deleteById(id)
    }
}
