package com.example.GateStatus.domain.tag.controller;

import com.example.GateStatus.domain.figure.service.response.FigureResponse;
import com.example.GateStatus.domain.tag.service.TagService;
import com.example.GateStatus.domain.tag.service.request.AddTagCommand;
import com.example.GateStatus.domain.tag.service.request.RemoveTagCommand;
import com.example.GateStatus.domain.tag.service.response.TagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping
    public ResponseEntity<TagResponse> addTag(@PathVariable Long figureId, @RequestBody String tagName) {
        return ResponseEntity.ok(tagService.addTagToFigure(AddTagCommand.of(figureId, tagName)));
    }

    @DeleteMapping
    public ResponseEntity<Void> removeTag(@PathVariable Long figureId,
                                          @PathVariable String tagName) {
        tagService.removeTagFromFigure(RemoveTagCommand.of(figureId, tagName));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<TagResponse>> getTags(@PathVariable Long figureId) {
        return ResponseEntity.ok(tagService.getFigureTags(figureId));
    }

    @GetMapping("/search/{tagName}")
    public ResponseEntity<List<FigureResponse>> getFiguresByTags(@PathVariable String tagName) {
        return ResponseEntity.ok(tagService.getFigureByTags(tagName));
    }
}
