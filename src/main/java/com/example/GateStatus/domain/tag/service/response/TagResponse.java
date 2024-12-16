package com.example.GateStatus.domain.tag.service.response;

import com.example.GateStatus.domain.tag.Tag;

public record TagResponse(Long tagId, String tagName) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getTagName());
    }
}
