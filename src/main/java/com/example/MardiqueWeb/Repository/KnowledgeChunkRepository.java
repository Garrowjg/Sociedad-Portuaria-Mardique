package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    List<KnowledgeChunk> findBySourceContainingIgnoreCase(String source);
    void deleteBySource(String source);

    @Query(value = "SELECT * FROM knowledge_chunks WHERE to_tsvector('spanish', content) @@ plainto_tsquery('spanish', :query) ORDER BY ts_rank(to_tsvector('spanish', content), plainto_tsquery('spanish', :query)) DESC LIMIT :limit", nativeQuery = true)
    List<KnowledgeChunk> searchByText(@Param("query") String query, @Param("limit") int limit);

    @Query(value = "SELECT * FROM knowledge_chunks WHERE content ILIKE '%' || :query || '%' LIMIT :limit", nativeQuery = true)
    List<KnowledgeChunk> searchByLike(@Param("query") String query, @Param("limit") int limit);

    long count();
}
