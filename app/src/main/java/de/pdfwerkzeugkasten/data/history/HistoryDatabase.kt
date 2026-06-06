package de.pdfwerkzeugkasten.data.history

import android.content.Context
import androidx.room.*
import de.pdfwerkzeugkasten.domain.model.HistoryItem
import de.pdfwerkzeugkasten.domain.model.ToolType
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history") data class HistoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val toolType: String, val displayName: String, val createdAt: Long, val outputSizeBytes: Long, val inputSizeBytes: Long, val outputUriString: String?)
@Dao interface HistoryDao { @Query("SELECT * FROM history ORDER BY createdAt DESC LIMIT 100") fun observe(): Flow<List<HistoryEntity>>; @Insert suspend fun insert(entity: HistoryEntity); @Query("DELETE FROM history") suspend fun clear() }
@Database(entities = [HistoryEntity::class], version = 1) abstract class HistoryDatabase : RoomDatabase() { abstract fun dao(): HistoryDao; companion object { fun create(context: Context) = Room.databaseBuilder(context, HistoryDatabase::class.java, "history.db").build() } }
class HistoryRepository(private val dao: HistoryDao) { fun observe() = dao.observe(); suspend fun add(item: HistoryItem) = dao.insert(HistoryEntity(toolType = item.toolType.name, displayName = item.displayName, createdAt = item.createdAt, outputSizeBytes = item.outputSizeBytes, inputSizeBytes = item.inputSizeBytes, outputUriString = item.outputUriString)); suspend fun clear() = dao.clear() }
fun HistoryEntity.toModel() = HistoryItem(id, ToolType.valueOf(toolType), displayName, createdAt, outputSizeBytes, inputSizeBytes, outputUriString)
