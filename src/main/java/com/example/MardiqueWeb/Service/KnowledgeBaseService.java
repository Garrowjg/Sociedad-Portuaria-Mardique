package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.KnowledgeChunk;
import com.example.MardiqueWeb.Repository.KnowledgeChunkRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeBaseService {

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 100;

    public int processPdf(MultipartFile file, String filename) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return processText(text, filename);
        }
    }

    public int processText(String text, String source) {
        knowledgeChunkRepository.deleteBySource(source);
        List<String> chunks = chunkText(text);
        List<KnowledgeChunk> entities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk kc = new KnowledgeChunk(chunks.get(i), source);
            kc.setSection("Parte " + (i + 1));
            entities.add(kc);
        }
        knowledgeChunkRepository.saveAll(entities);
        return entities.size();
    }

    public int processRawText(String text, String title) {
        List<String> chunks = chunkText(text);
        List<KnowledgeChunk> entities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk kc = new KnowledgeChunk(chunks.get(i), title);
            kc.setSection("Parte " + (i + 1));
            entities.add(kc);
        }
        knowledgeChunkRepository.saveAll(entities);
        return entities.size();
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        text = text.replaceAll("\\s+", " ").trim();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf(". ", end);
                if (lastPeriod > start + CHUNK_SIZE / 2) {
                    end = lastPeriod + 1;
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end - CHUNK_OVERLAP;
            if (start >= text.length() - CHUNK_OVERLAP) break;
        }
        return chunks;
    }

    public void deleteBySource(String source) {
        knowledgeChunkRepository.deleteBySource(source);
    }

    public long count() {
        return knowledgeChunkRepository.count();
    }

    public List<KnowledgeChunk> findAll() {
        return knowledgeChunkRepository.findAll();
    }
}
